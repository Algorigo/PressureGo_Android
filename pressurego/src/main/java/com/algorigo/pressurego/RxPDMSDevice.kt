package com.algorigo.pressurego

import android.bluetooth.BluetoothDevice
import android.util.Log
import androidx.annotation.IntRange
import com.algorigo.algorigoble.*
import com.jakewharton.rxrelay3.PublishRelay
import io.reactivex.rxjava3.annotations.NonNull
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
    private var sensingInterval = -1
    private var amp = -1
    private var sens = -1
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
            .andThen(getSensingIntervalSingle()!!.ignoreElement())
            .andThen(getAmplificationSingle()!!.ignoreElement())
            .andThen(getSensitivitySingle()!!.ignoreElement())
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

    private fun getSingle(code: PDMSUtil.MessageGetCode): @NonNull Single<Int>? {
        var relay: PublishRelay<ByteArray>? = null
        return writeCharacteristic(UUID.fromString(PDMSUtil.UUID_COMMUNICATION), code.message)
            ?.doOnSubscribe {
                if (relayMap[code] != null) {
                    relay = relayMap[code]
                } else {
                    relay = PublishRelay.create<ByteArray>().also {
                        relayMap[code.byte] = it
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
    }

    fun getSensingInterval(): Int {
        return sensingInterval
    }

    private fun getSensingIntervalSingle(): Single<Int>? {
        val code = PDMSUtil.MessageGetCode.CODE_SENSOR_SCAN_INTERVAL
        return getSingle(code)
            ?.doOnSuccess { sensingInterval = it }
    }

    fun setSensingIntervalCompletable(@IntRange(from = 1, to = 255) sensingInterval: Int): Completable? {
        val code = PDMSUtil.MessageSetCode.CODE_SENSOR_SCAN_INTERVAL
        return writeCharacteristic(UUID.fromString(PDMSUtil.UUID_COMMUNICATION), code.getMessage(sensingInterval.toByte()))
            ?.doOnSuccess {
                Log.i(LOG_TAG, "UUID_AMPLIFICATION:${it.map { it.toUByte().toUInt() }.toTypedArray().contentToString()}")
                this.sensingInterval = sensingInterval
            }
            ?.ignoreElement()
    }

    fun getAmplification(): Int {
        return amp
    }

    private fun getAmplificationSingle(): Single<Int>? {
        val code = PDMSUtil.MessageGetCode.CODE_AMPLIFICATION
        return getSingle(code)
            ?.doOnSuccess { amp = it }
    }

    fun setAmplificationCompletable(@IntRange(from = 1, to = 255) amplification: Int): Completable? {
        val code = PDMSUtil.MessageSetCode.CODE_AMPLIFICATION
        return writeCharacteristic(UUID.fromString(PDMSUtil.UUID_COMMUNICATION), code.getMessage(amplification.toByte()))
            ?.doOnSuccess {
                Log.i(LOG_TAG, "UUID_AMPLIFICATION:${it.map { it.toUByte().toUInt() }.toTypedArray().contentToString()}")
                amp = amplification
            }
            ?.ignoreElement()
    }

    fun getSensitivity(): Int {
        return sens
    }

    private fun getSensitivitySingle(): Single<Int>? {
        val code = PDMSUtil.MessageGetCode.CODE_SENSITIVITY
        return getSingle(code)
            ?.doOnSuccess { sens = it }
    }

    fun setSensitivityCompletable(@IntRange(from = 1, to = 255) sensitivity: Int): Completable? {
        val code = PDMSUtil.MessageSetCode.CODE_SENSITIVITY
        return writeCharacteristic(UUID.fromString(PDMSUtil.UUID_COMMUNICATION), code.getMessage(sensitivity.toByte()))
            ?.doOnSuccess {
                Log.i(LOG_TAG, "UUID_AMPLIFICATION:${it.map { it.toUByte().toUInt() }.toTypedArray().contentToString()}")
                sens = sensitivity
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
