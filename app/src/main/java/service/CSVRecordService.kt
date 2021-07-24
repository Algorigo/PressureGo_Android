package service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import com.algorigo.algorigoble.BleDevice
import com.algorigo.algorigoble.BleManager
import com.algorigo.pressurego.RxPDMSDevice
import com.algorigo.pressuregoapp.R
import data.BleDevicePreferencesHelper
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.schedulers.Schedulers
import io.reactivex.rxjava3.subjects.PublishSubject
import io.reactivex.rxjava3.subjects.Subject
import notification.NotificationUtil
import org.joda.time.DateTime
import ui.NewMainActivity
import util.FileUtil
import java.io.File

class CSVRecordService : Service() {

    private val bleManager: BleManager by lazy {
        BleManager.getInstance()
    }
    private lateinit var startStreamingTime: DateTime
    private lateinit var bleDevicePreferencesHelper: BleDevicePreferencesHelper

    private val csvBinder: IBinder = LocalBinder()

    private val deviceMap = mutableMapOf<String, Pair<RxPDMSDevice, BleDevice.ConnectionState>>()
    private val dataDisposableMap = mutableMapOf<String, Disposable>()
    private val dataSubjectMap = mutableMapOf<String, Subject<IntArray>>()

    private val devicesConnectionSubject =
        PublishSubject.create<Pair<String, Pair<RxPDMSDevice, BleDevice.ConnectionState>>>()
    private val streamingSubject = PublishSubject.create<Unit>()

    private var streamingDisposable: Disposable? = null
    private var connectionStateDisposable: Disposable? = null

    private var file: File? = null


    class NoDataException : IllegalStateException("No Data")

    inner class LocalBinder : Binder() {
        fun getService(): CSVRecordService = this@CSVRecordService
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return csvBinder
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate()")
        bleDevicePreferencesHelper = BleDevicePreferencesHelper(this@CSVRecordService)
        startForegroundNotification()
        startStreaming()
        observeDeviceConnection()
    }

    private fun observeDeviceConnection() {
        connectionStateDisposable = initConnectedDevicesSingle()
            .doOnSuccess {
                it.forEach { device ->
                    deviceMap[device.macAddress] = Pair(device, device.connectionState)
                    devicesConnectionSubject.onNext(
                        Pair(
                            device.macAddress,
                            Pair(device, device.connectionState)
                        )
                    )
                }
            }.flatMapObservable { BleManager.getInstance().getConnectionStateObservable() }
            .doOnNext {
                devicesConnectionSubject.onNext(
                    Pair(
                        it.bleDevice.macAddress,
                        Pair(it.bleDevice as RxPDMSDevice, it.bleDevice.connectionState)
                    )
                )
            }
            .doFinally {
                connectionStateDisposable = null
            }
            .subscribe({}, {
                Log.d(TAG, it.toString())
            })
    }


    private fun startForegroundNotification() {
        val pendingIntent: PendingIntent =
            Intent(this, NewMainActivity::class.java).apply {
                bleDevicePreferencesHelper.latestShowDeviceMacAddress?.let {
                    putExtra(NewMainActivity.KEY_MAC_ADDRESS, it)
                }
            }.let { notificationIntent ->
                PendingIntent.getActivity(this, 0, notificationIntent, 0)
            }

        NotificationUtil.createCSVRecordChannel(this@CSVRecordService)

        val notification: Notification = NotificationUtil.getNotification(
            this@CSVRecordService,
            type = NotificationUtil.NotificationType.CSVRecordChannel,
            titleRes = R.string.app_name,
            contentRes = R.string.csv_record_recording_text,
            iconRes = R.drawable.csv_record_on,
            pendingIntent
        )

        startForeground(1, notification)
    }

    private fun initConnectedDevicesSingle(): Single<List<RxPDMSDevice>> {
        return Single.create { emitter ->
            val devices = bleManager.getConnectedDevices()
                .mapNotNull { it as? RxPDMSDevice }
            if (devices.isNotEmpty()) {
                emitter.onSuccess(devices)
            } else {
                emitter.onError(NoDataException())
            }
        }
    }

    private fun startStreaming() {
        streamingDisposable = devicesConnectionObservable()
            .doOnNext {
                deviceMap[it.first] = Pair(it.second.first, it.second.second)
                Log.d(TAG, "deviceMapSize == ${deviceMap.size}")
                deviceMap.forEach {
                    Log.d(TAG, "${it.key} == ${it.value.first}, ${it.value.second}")
                }
            }
            .flatMapCompletable {
                devicesStreamingCompletable()
            }.subscribe({
                Log.d(TAG, "completed")
            }, {
                Log.e(TAG, it.toString())
            })
    }

    private fun devicesConnectionObservable(): Observable<Pair<String, Pair<RxPDMSDevice, BleDevice.ConnectionState>>> {
        return devicesConnectionSubject.distinctUntilChanged()
    }

    private fun devicesStreamingCompletable(): Completable {
        return streamingSubject.toSerialized().ignoreElements()
            .subscribeOn(Schedulers.io())
            .doFinally {
                deviceMap.keys.forEach {
                    dataDisposableMap[it]?.dispose()
                }
            }
            .doOnSubscribe {
                Log.d(TAG, "doOnSubscribe")
                startStreamingTime = DateTime.now()
                onDataInner()
            }
    }

    private fun onDataInner() {
        deviceMap.entries.forEach {
            if (it.value.second == BleDevice.ConnectionState.DISCONNECTED) {
                if (dataDisposableMap.keys.contains(it.key)) {
                    dataDisposableMap[it.key]?.dispose()
                }
            } else if (it.value.second == BleDevice.ConnectionState.CONNECTED) {
                if (!dataDisposableMap.keys.contains(it.key)) {
                    deviceSendDataOn(macAddress = it.key)
                }
            }
        }
    }

    private fun deviceSendDataOn(macAddress: String) {
        deviceMap[macAddress]?.first!!.sendDataOn()
            ?.doOnNext { intArray ->
                Log.d(TAG, "doOnNext == ${intArray[1]}")
                file = FileUtil.getFile(this@CSVRecordService, macAddress, startStreamingTime)
                file?.let {
                    FileUtil.saveStringToFile(
                        it,
                        writeCsvLine(deviceMap[macAddress]?.first!!, intArray)
                    )
                        .subscribe({

                        }, {
                            Log.d(TAG, "saveStringToFile error = $it")
                        })
                }
            }
            ?.doFinally {
                dataDisposableMap[macAddress]?.dispose()
                dataDisposableMap.remove(macAddress)
            }
            ?.subscribe({
                dataSubjectMap[macAddress]?.onNext(it)
            }, {
                Log.d(TAG, it.toString())
            })?.also {
                dataDisposableMap[macAddress] = it
            }
    }

    fun noRecordStopSelf() {
        FileUtil.deleteFileCompletable(file)
            .doFinally {
                stopSelf()
            }
            .subscribe({

            }, {
                Log.d(TAG, it.toString())
            })
    }

    private fun writeCsvLine(device: RxPDMSDevice, intArray: IntArray): String {
        val builder = StringBuilder()
        builder.append(
            "${device.macAddress},${
                System.currentTimeMillis()
            },${device.getAmplification()},${device.getSensitivity()},${
                intArray.contentToString().let { it.substring(1, it.length - 1).replace(" ", "") }
            }\n"
        )
        return builder.toString()
    }

    override fun onDestroy() {
        disposeMap()
        streamingDisposable?.dispose()
        connectionStateDisposable?.dispose()
        Log.d(TAG, "onDestroy")
        super.onDestroy()
    }

    private fun disposeMap() {
        val deviceIterator = deviceMap.iterator()
        while (deviceIterator.hasNext()) {
            deviceIterator.next()
            deviceIterator.remove()
        }
        val dataDisposableIterator = dataDisposableMap.iterator()
        while (dataDisposableIterator.hasNext()) {
            val item = dataDisposableIterator.next()
            item.value.dispose()
        }
        Log.d(TAG, "deviceMap size = ${deviceMap.size}, deviceMap size = ${dataDisposableMap.size}")
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        file?.let {
            bleDevicePreferencesHelper.setCsvFileNameSingle(file!!.absolutePath)
                .doFinally {
                    stopSelf()
                }
                .subscribe({

                }, {

                })
        } ?: run {
            stopSelf()
        }

        Log.d(TAG, "taskRemoved")
    }

    private fun disconnectDevices(): Completable {
        val subject = PublishSubject.create<Unit>()
        return subject.ignoreElements()
            .doOnSubscribe {
                deviceMap.entries.forEach {
                    Log.d(TAG, "device == ${it.value.first}, connection == ${it.value.second}")
                    if (it.value.second == BleDevice.ConnectionState.CONNECTED) {
                        it.value.first.disconnect()
                    }
                }
            }
    }


    companion object {
        val TAG: String = CSVRecordService::class.java.simpleName
    }
}