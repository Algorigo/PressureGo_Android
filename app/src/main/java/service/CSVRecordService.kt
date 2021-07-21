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
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.subjects.BehaviorSubject
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
        initConnectedDevice()
        onData()
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

    private fun onDataCompletable() {
        initConnectedDevicesSingle()
            .doOnSuccess {
                it.forEach { device ->
                    deviceMap[device.macAddress] = device
                    dataSubjectMap[device.macAddress] = PublishSubject.create()
                    devicesSubject.onNext(device)
                }
            }
    }

    private fun startStreaming() {
        devicesSubject
            .filter { it != null}
            .subscribe({

        }, {

        })
    }

    private fun onData() {
        Log.d(TAG, "onData, ${deviceMap.size}")
        deviceMap.keys.forEach { key ->
            if (!dataDisposableMap.keys.contains(key)) {
                deviceMap[key]?.sendDataOn()
                    ?.doOnNext {
                        Log.d(TAG, "${it[1]}")
                        val file = FileUtil.getFile(this@CSVRecordService, key)
                        FileUtil.saveStringToFile(file, writeCsvLine(deviceMap[key]!!, it))
                            .subscribe({

                            }, {

                            })
                    }
                    ?.doFinally {
                        dataDisposableMap[key]?.dispose()
                        dataDisposableMap.remove(key)
                    }
                    ?.subscribe({
                        dataSubjectMap[key]?.onNext(it)
                    }, {

                    })?.also {
                        dataDisposableMap[key] = it
                    }
            }
        }
    }

    private fun writeCsvLine(device: RxPDMSDevice, intArray: IntArray): String {
        val builder = StringBuilder()
        builder.append("${device.macAddress},${device.getDeviceName()}, ${DateTime.now().toString("yyyy-MM-dd-hh:mm:ss")},${device.getAmplification()},${device.getSensitivity()},${intArray[0]},${intArray[0]},${intArray[0]},${intArray[0]}\n")
        return builder.toString()
    }




    override fun onDestroy() {
        deviceMap.keys.forEach {
            dataDisposableMap[it]?.dispose()
            dataDisposableMap.remove(it)
            dataSubjectMap.remove(it)
            deviceMap.remove(it)
        }
        Log.d(TAG, "onDestroy")
        super.onDestroy()
    }


    companion object {
        val TAG: String = CSVRecordService::class.java.simpleName

    }
}