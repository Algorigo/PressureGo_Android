package service

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import com.algorigo.algorigoble.BleManager
import com.algorigo.pressurego.RxPDMSDevice
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.subjects.BehaviorSubject

class CSVRecordService: Service() {


    private val bleDeviceSubject = BehaviorSubject.create<RxPDMSDevice>()

    private val bleDisposable: Disposable? = null

    private lateinit var bleManager: BleManager

    private val csvBinder: IBinder = LocalBinder()

    private val deviceMap = mutableMapOf<String, RxPDMSDevice>()

    inner class LocalBinder: Binder() {
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
        bleManager = BleManager.getInstance()
        getConnectedDevice()
    }

    override fun stopService(name: Intent?): Boolean {
        return super.stopService(name)
    }

    private fun getConnectedDevice() {
        val device = bleManager.getConnectedDevices()
            .mapNotNull { it as? RxPDMSDevice }
            .firstOrNull()
    }

    private fun onData() {

    }


}