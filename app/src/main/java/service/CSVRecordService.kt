package service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import com.algorigo.algorigoble.BleManager
import com.algorigo.pressurego.RxPDMSDevice
import com.algorigo.pressuregoapp.R
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.schedulers.Schedulers
import io.reactivex.rxjava3.subjects.PublishSubject
import io.reactivex.rxjava3.subjects.ReplaySubject
import io.reactivex.rxjava3.subjects.Subject
import notification.NotificationUtil
import org.joda.time.DateTime
import ui.NewMainActivity
import util.FileUtil

class CSVRecordService : Service() {

    private lateinit var bleManager: BleManager
    private lateinit var dateTime: DateTime

    private val csvBinder: IBinder = LocalBinder()

    private val deviceMap = mutableMapOf<String, RxPDMSDevice>()
    private val dataDisposableMap = mutableMapOf<String, Disposable>()
    private val dataSubjectMap = mutableMapOf<String, Subject<IntArray>>()

    private val devicesSubject = ReplaySubject.create<RxPDMSDevice>()
    private val streamingSubject = PublishSubject.create<Unit>()

    private var streamingDisposable: Disposable? = null


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
        dateTime = DateTime.now()
        bleManager = BleManager.getInstance()
        startForegroundNotification()
        startStreaming()
//        initConnectedDevice()
    }


    private fun startForegroundNotification() {
        val pendingIntent: PendingIntent =
            Intent(this, NewMainActivity::class.java).let { notificationIntent ->
                PendingIntent.getActivity(this, 0, notificationIntent, 0)
            }

        NotificationUtil.createCSVRecordChannel(this@CSVRecordService)

        val notification: Notification = NotificationUtil.getNotificationChannel(
            this@CSVRecordService,
            type = NotificationUtil.NotificationType.CSVRecordChannel,
            titleRes = R.string.app_name,
            contentRes = R.string.csv_record_recording_text,
            iconRes = R.drawable.csv_record_on,
            pendingIntent
        )

        startForeground(1, notification)
    }

    private fun initConnectedDevice() {
        bleManager.getConnectedDevices()
            .mapNotNull { it as? RxPDMSDevice }
            .forEach {
                deviceMap[it.macAddress] = it
                dataSubjectMap[it.macAddress] = PublishSubject.create()
                devicesSubject.onNext(it)
            }
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
        streamingDisposable = initConnectedDevicesSingle()
            .doOnSuccess {
                it.forEach { device ->
                    deviceMap[device.macAddress] = device
                    dataSubjectMap[device.macAddress] = PublishSubject.create()
                    devicesSubject.onNext(device)
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
                onDataInner()
            }
    }


    private fun onDataInner() {
        Log.d(TAG, "deviceMapSize == ${deviceMap.size}")
        deviceMap.keys.forEach { key ->
            Log.d(TAG, "key == $key")
            if (!dataDisposableMap.keys.contains(key)) {
                Log.d(TAG, "containskey == ${deviceMap[key]}")
                deviceMap[key]?.sendDataOn()
                    ?.doOnNext {
                        Log.d(TAG, "doOnNext == ${it[1]}")
                        val file = FileUtil.getFile(this@CSVRecordService, key)
                        FileUtil.saveStringToFile(file, writeCsvLine(deviceMap[key]!!, it))
                            .subscribe({

                            }, {
                                Log.d(TAG, "saveStringToFile error = $it")
                            })
                    }
                    ?.doFinally {
                        dataDisposableMap[key]?.dispose()
                        dataDisposableMap.remove(key)
                    }
                    ?.subscribe({
                        dataSubjectMap[key]?.onNext(it)
                    }, {
                        Log.d(TAG, it.toString())
                    })?.also {
                        dataDisposableMap[key] = it
                    }
            }
        }
    }

    private fun writeCsvLine(device: RxPDMSDevice, intArray: IntArray): String {
        val builder = StringBuilder()
        builder.append(
            "${device.macAddress},${device.getDeviceName()}, ${
                DateTime.now().toString("yyyy-MM-dd-hh:mm:ss")
            },${device.getAmplification()},${device.getSensitivity()},${
                intArray.contentToString().let { it.substring(1, it.length - 2) }
            }\n"
        )
        return builder.toString()
    }

    override fun onDestroy() {
        deviceMap.keys.forEach {
            dataDisposableMap[it]?.dispose()
            dataDisposableMap.remove(it)
            dataSubjectMap.remove(it)
            deviceMap.remove(it)
        }
        streamingDisposable?.dispose()
        Log.d(TAG, "onDestroy")
        super.onDestroy()
    }


    companion object {
        val TAG: String = CSVRecordService::class.java.simpleName
    }
}