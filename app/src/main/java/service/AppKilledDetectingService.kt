package service

import android.app.IntentService
import android.content.Intent
import android.util.Log
import com.algorigo.algorigoble.BleManager

class AppKilledDetectingService: IntentService("AppKilledDetectingService") {
    override fun onHandleIntent(intent: Intent?) {}

    override fun onTaskRemoved(rootIntent: Intent?) {
        val iterator = BleManager.getInstance().getConnectedDevices().iterator()
        while (iterator.hasNext()) {
            iterator.next().disconnect()
        }
        Log.d(TAG, "appKilledDetectingService removed")
        stopSelf()
        super.onTaskRemoved(rootIntent)
    }

    companion object {
        val TAG: String = AppKilledDetectingService::class.java.simpleName

    }

}