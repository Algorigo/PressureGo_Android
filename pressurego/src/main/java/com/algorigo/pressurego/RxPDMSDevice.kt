package com.algorigo.pressurego

import android.bluetooth.BluetoothDevice
import android.util.Log
import androidx.annotation.IntRange
import com.algorigo.algorigoble.*
import com.jakewharton.rxrelay3.PublishRelay
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.subjects.PublishSubject
import java.util.*
import java.util.concurrent.TimeUnit

class RxPDMSDevice : InitializableBleDevice() {

    private var deviceName = ""
    private var manufactureName = ""
    private var hardwareVersion = ""
    private var firmwareVersion = ""
    private var amp = -1
//    private var sens = -1
    private var callbackDisposable: Disposable? = null
    private var relayMap = mutableMapOf<Byte, PublishRelay<ByteArray>>()
    private var dataSubject = PublishSubject.create<IntArray>().toSerialized()

    override fun initializeCompletable(): Completable {
        return getDeviceNameSingle()!!.ignoreElement()
            .andThen(getManufacturerNameSingle()!!.ignoreElement())
            .andThen(getHardwareVersionSingle()!!.ignoreElement())
            .andThen(getFirmwareVersionSingle()!!.ignoreElement())
            .andThen(Completable.create { emitter ->
                callbackDisposable = setupNotification(UUID.fromString(PDMSUtil.UUID_DATA_NOTIFICATION))
                    ?.doFinally {
                        callbackDisposable = null
                    }
                    ?.flatMap {
                        emitter.onComplete()
                        it
                    }
                    ?.doFinally {
                        disconnect()
                    }
                    ?.subscribe({
                        onData(it)
                    }, {
                        Log.e(LOG_TAG, "enableSensor error", Exception(it))
                        dataSubject.onError(it)
                        dataSubject = PublishSubject.create<IntArray>().toSerialized()
                        if (!emitter.isDisposed) {
                            emitter.onError(it)
                        }
                    })
            })
            .andThen(getAmplificationSingle()!!.ignoreElement())
            .doOnComplete {
                heartBeatRead()
            }
    }

    override fun onDisconnected() {
        super.onDisconnected()
        callbackDisposable?.dispose()
    }

    fun sendDataOn(): Observable<IntArray>? {
        return dataSubject
    }

    private fun onData(byteArray: ByteArray) {
//        Log.i(LOG_TAG, "onData:${byteArray.contentToString()}")
        when (byteArray[0]) {
            0x02.toByte() -> {
                relayMap.remove(byteArray[1])?.accept(byteArray)
            }
            0x01.toByte() -> {
                val intArray = IntArray(4) {
                    val aLower = byteArray[it*2+2].toInt() and 0xff
                    val aUpper = byteArray[it*2+3].toInt() and 0xff
                    (aUpper shl 8) + aLower
                }
                dataSubject.onNext(intArray)
            }
        }
    }

    private fun heartBeatRead() {
        Observable.interval(1, TimeUnit.MINUTES)
            .flatMapSingle { getBatteryPercentSingle() }
            .subscribe({
            }, {
                Log.e(LOG_TAG, "heartBeatRead error", Exception(it))
            })
    }

    fun getDeviceName(): String {
        return deviceName
    }

    private fun getDeviceNameSingle(): Single<String>? {
        return readCharacteristic(UUID.fromString(PDMSUtil.UUID_DEVICE_NAME))
            ?.map {
                Log.i(LOG_TAG, "UUID_DEVICE_TYPE:${it.map { it.toUByte().toUInt() }.toTypedArray().contentToString()}")
                String(it)
            }
            ?.doOnSuccess { deviceName = it }
    }

    fun getManufactureName(): String {
        return manufactureName
    }

    private fun getManufacturerNameSingle(): Single<String>? {
        return readCharacteristic(UUID.fromString(PDMSUtil.UUID_MANUFACTURER_NAME))
            ?.map {
                Log.i(LOG_TAG, "UUID_MANUFACTURER_ID:${it.map { it.toUByte().toUInt() }.toTypedArray().contentToString()}")
                String(it)
            }
            ?.doOnSuccess { manufactureName = it }
    }

    fun getFirmwareVersion(): String {
        return firmwareVersion
    }

    private fun getFirmwareVersionSingle(): Single<String>? {
        return readCharacteristic(UUID.fromString(PDMSUtil.UUID_FIRMWARE_REVISION))
            ?.map {
                Log.i(LOG_TAG, "UUID_FIRMWARE_VERSION:${it.map { it.toUByte().toUInt() }.toTypedArray().contentToString()}")
                String(it)
            }
            ?.doOnSuccess { firmwareVersion = it }
    }

    fun getHardwareVersion(): String {
        return hardwareVersion
    }

    private fun getHardwareVersionSingle(): Single<String>? {
        return readCharacteristic(UUID.fromString(PDMSUtil.UUID_HARDWARE_REVISION))
            ?.map {
                Log.i(LOG_TAG, "UUID_FIRMWARE_VERSION:${it.map { it.toUByte().toUInt() }.toTypedArray().contentToString()}")
                String(it)
            }
            ?.doOnSuccess { hardwareVersion = it }
    }

    fun getAmplification(): Int {
        return amp
    }

    private fun getAmplificationSingle(): Single<Int>? {
        var relay: PublishRelay<ByteArray>? = null
        val code = 0xb2.toByte()
        return writeCharacteristic(UUID.fromString(PDMSUtil.UUID_COMMUNICATION), byteArrayOf(0x02, code, 0x03))
            ?.doOnSubscribe {
                if (relayMap[code] != null) {
                    relay = relayMap[code]
                } else {
                    relay = PublishRelay.create<ByteArray>().also {
                        relayMap[code] = it
                    }
                }
            }
            ?.flatMap {
                relay!!.firstOrError()
            }
            ?.map {
                Log.i(LOG_TAG, "UUID_AMPLIFICATION:${it.map { it.toUByte().toUInt() }.toTypedArray().contentToString()}")
                it[2].toInt()
            }
            ?.doOnSuccess { amp = it }
    }

    fun setAmplificationCompletable(@IntRange(from = 1, to = 255) amplification: Int): Completable? {
        return writeCharacteristic(UUID.fromString(PDMSUtil.UUID_COMMUNICATION), byteArrayOf(0x02, 0xb1.toByte(), amplification.toByte(), 0x03))
            ?.doOnSuccess {
                Log.i(LOG_TAG, "UUID_AMPLIFICATION:${it.map { it.toUByte().toUInt() }.toTypedArray().contentToString()}")
                amp = amplification
            }
            ?.ignoreElement()
    }

    fun getBatteryPercentSingle(): Single<Int>? {
        return readCharacteristic(UUID.fromString(PDMSUtil.UUID_BATTERY_LEVEL))
            ?.map {
                Log.i(LOG_TAG, "UUID_VOLTAGE:${it.map { it.toUByte().toUInt() }.toTypedArray().contentToString()}")
                it.toInt()
            }
    }

    class DeviceDelegate : BleManager.BleDeviceDelegate() {
        override fun createBleDevice(bluetoothDevice: BluetoothDevice): BleDevice? {
            return when {
                isMatch(bluetoothDevice) -> RxPDMSDevice()
                else -> null
            }
        }

        override fun getBleScanFilters(): Array<BleScanFilter> {
            return arrayOf(
                getScanFilter()
            )
        }

        override fun getBleScanSettings(): BleScanSettings {
            return BleScanSettings.Builder().build()
        }
    }

    companion object {
        private val LOG_TAG = RxPDMSDevice::class.java.simpleName

        fun isMatch(bluetoothDevice: BluetoothDevice): Boolean {
            return bluetoothDevice.name?.equals(PDMSUtil.BLE_NAME) ?: false
        }

        fun getScanFilter(): BleScanFilter {
            return BleScanFilter.Builder()
                .setDeviceName(PDMSUtil.BLE_NAME)
                .build()
        }
    }
}
