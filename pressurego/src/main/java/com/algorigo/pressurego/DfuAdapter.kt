package com.algorigo.pressurego

import android.content.Context
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.annotation.RawRes
import no.nordicsemi.android.dfu.*

internal class DfuAdapter private constructor(private val context: Context, val address: String, name: String) {

    class DfuError(val error: Int, val errorType: Int, message: String?): RuntimeException(String.format("%s(%x:%x)", message ?: "", error, errorType))

    abstract class Callback(macAddress: String) : DfuProgressListenerAdapter() {
        internal lateinit var context: Context
        private val macAddressPre: String

        abstract fun onProgress(percent: Int)
        abstract fun onCompleted()
        abstract fun onAborted()
        abstract fun onError(error: Exception)

        init {
            macAddressPre = macAddress.substring(0, 14)
        }

        override fun onProgressChanged(
            deviceAddress: String,
            percent: Int,
            speed: Float,
            avgSpeed: Float,
            currentPart: Int,
            partsTotal: Int
        ) {
            super.onProgressChanged(
                deviceAddress,
                percent,
                speed,
                avgSpeed,
                currentPart,
                partsTotal
            )
            if (macAddressPre == deviceAddress.substring(0, 14)) {
                onProgress(percent)
            }
        }

        override fun onDfuCompleted(deviceAddress: String) {
            super.onDfuCompleted(deviceAddress)
            if (macAddressPre == deviceAddress.substring(0, 14)) {
                onCompleted()
                unregister()
            }
        }

        override fun onDfuAborted(deviceAddress: String) {
            super.onDfuAborted(deviceAddress)
            if (macAddressPre == deviceAddress.substring(0, 14)) {
                onAborted()
                unregister()
            }
        }

        override fun onError(deviceAddress: String, error: Int, errorType: Int, message: String?) {
            super.onError(deviceAddress, error, errorType, message)
            if (macAddressPre == deviceAddress.substring(0, 14)) {
                onError(DfuError(error, errorType, message))
                unregister()
            }
        }

        private fun unregister() {
            DfuServiceListenerHelper.unregisterProgressListener(context, this)
        }
    }

    constructor(context: Context, address: String, name: String, uri: Uri) : this(context, address, name) {
        initiator = DfuServiceInitiator(address)
            .setDeviceName(name)
            .setKeepBond(true)
            .apply {
                setPrepareDataObjectDelay(300L)
                setZip(uri)
            }
    }

    constructor(context: Context, address: String, name: String, path: String) : this(context, address, name) {
        initiator = DfuServiceInitiator(address)
            .setDeviceName(name)
            .setKeepBond(true)
            .apply {
                setPrepareDataObjectDelay(300L)
                setZip(path)
            }
    }

    constructor(context: Context, address: String, name: String, @RawRes resRawId: Int) : this(context, address, name) {
        initiator = DfuServiceInitiator(address)
            .setDeviceName(name)
            .setKeepBond(true)
            .apply {
                setPrepareDataObjectDelay(300L)
                setZip(resRawId)
            }
    }

    private lateinit var initiator: DfuServiceInitiator

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            DfuServiceInitiator.createDfuNotificationChannel(context)
        }
    }

    fun <T : DfuBaseService> start(clazz: Class<T>, callback: Callback): DfuServiceController {
        callback.context = context
        DfuServiceListenerHelper.registerProgressListener(context, callback)
        return initiator.start(context, clazz)
    }
}
