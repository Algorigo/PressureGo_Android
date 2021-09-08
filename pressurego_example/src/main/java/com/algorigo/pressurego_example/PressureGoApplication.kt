package com.algorigo.pressurego_example

import android.app.Application
import android.util.Log

class PressureGoApplication: Application() {

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreated")

    }
    companion object {
        val TAG: String = PressureGoApplication::class.java.simpleName
    }
}