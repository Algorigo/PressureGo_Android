package com.algorigo.pressuregoapp

import android.app.Application
import android.content.Intent
import android.util.Log
import com.algorigo.algorigoble.BleManager
import service.AppKilledDetectingService

class PressureGoApplication: Application() {

    override fun onCreate() {
        super.onCreate()
        startService(Intent(this, AppKilledDetectingService::class.java))
        Log.d(TAG, "onCreated")

    }
    companion object {
        val TAG: String = PressureGoApplication::class.java.simpleName
    }
}