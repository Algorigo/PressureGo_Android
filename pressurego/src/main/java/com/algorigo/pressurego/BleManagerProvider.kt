package com.algorigo.pressurego

import android.content.Context
import com.algorigo.algorigoble2.BleManager

object BleManagerProvider {

    private var bleManager: BleManager? = null

    @Synchronized
    fun getBleManager(context: Context): BleManager {
        if (bleManager == null) {
            bleManager = BleManager(context, RxPDMSDevice.DeviceDelegate())
        }
        return bleManager!!
    }

}