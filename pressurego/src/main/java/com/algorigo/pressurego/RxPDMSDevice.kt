package com.algorigo.pressurego

import android.bluetooth.BluetoothDevice
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.annotation.IntRange
import androidx.annotation.RawRes
import com.algorigo.algorigoble.*
import com.jakewharton.rxrelay3.BehaviorRelay
import com.jakewharton.rxrelay3.PublishRelay
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.schedulers.Schedulers
import io.reactivex.rxjava3.subjects.PublishSubject
import no.nordicsemi.android.dfu.DfuBaseService
import no.nordicsemi.android.dfu.DfuServiceController
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.NoSuchElementException

class RxPDMSDevice : InitializableBleDevice() {

    private var deviceName = ""
    private var manufactureName = ""
    private var hardwareVersion = ""
    private var firmwareVersion = ""
    private var sensingIntervalMillis = -1
    private var amp = -1
    private var sens = -1
    private var callbackDisposable: Disposable? = null
    private var relayMap = mutableMapOf<Byte, PublishRelay<ByteArray>>()
    private var dataSubject = PublishSubject.create<IntArray>().toSerialized()
    private var batteryRelay = BehaviorRelay.create<Int>().toSerialized()

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
//                        dataSubject.onError(it)
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
        Log.i(LOG_TAG, "onData:${byteArray.contentToString()}")
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
        Observable.interval(0, 1, TimeUnit.MINUTES)
            .flatMapSingle { getBatteryPercentSingle() }
            .subscribe({
                batteryRelay.accept(it)
            }, {
                Log.e(LOG_TAG, "heartBeatRead error", Exception(it))
            })
    }

    fun getDisplayName(): String {
        return "PRESSUREGO"
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
                Log.i(LOG_TAG, "UUID_HARDWARE_REVISION:${it.map { it.toUByte().toUInt() }.toTypedArray().contentToString()}")
                String(it)
            }
            ?.doOnSuccess { hardwareVersion = it }
    }

    fun getBatteryPercentSingle(): Single<Int>? {
        return readCharacteristic(UUID.fromString(PDMSUtil.UUID_BATTERY_LEVEL))
            ?.map {
                Log.i(LOG_TAG, "UUID_BATTERY_LEVEL:${it.map { it.toUByte().toUInt() }.toTypedArray().contentToString()}")
                it.toInt()
            }
    }

    fun getBatteryPercentObservable(): Observable<Int> {
        return batteryRelay
            .scan(Pair(-1, -1), { a, b -> Pair(a.second, b) })
            .filter { it.first != it.second }
            .map { it.second }
    }

    fun getLowBatteryObservable(threshold: Int = 15): Observable<Any> {
        return batteryRelay
            .map { it < threshold }
            .scan(Pair(false, false), { a, b -> Pair(a.second, b)})
            .filter { it.first != it.second }
            .map { it.second }
    }

    fun getSensingIntervalMillis(): Int {
        return sensingIntervalMillis
    }

    private fun getSensingIntervalSingle(): Single<Int>? {
        val code = PDMSUtil.MessageGetCode.CODE_SENSOR_SCAN_INTERVAL
        return getSingle(code)
            ?.doOnSuccess { sensingIntervalMillis = PDMSUtil.intervalValueToMillis(it) }
    }

    fun setSensingIntervalMillisSingle(@IntRange(from = PDMSUtil.intervalMillisMin, to = PDMSUtil.intervalMillisMax) sensingIntervalMillis: Int): Single<Int>? {
        val sensingIntervalValue = PDMSUtil.intervalMillisToValue(sensingIntervalMillis)
        return setSensingIntervalSingle(sensingIntervalValue)
    }

    private fun setSensingIntervalSingle(@IntRange(from = 1, to = 255) sensingIntervalValue: Int): Single<Int>? {
        val code = PDMSUtil.MessageSetCode.CODE_SENSOR_SCAN_INTERVAL
        return setSingle(code, sensingIntervalValue.toByte())
            ?.map {
                PDMSUtil.intervalValueToMillis(sensingIntervalValue)
            }
            ?.doOnSuccess {
                Log.i(LOG_TAG, "$code:$it")
                sensingIntervalMillis = it
            }
    }

    fun getAmplification(): Int {
        return amp
    }

    private fun getAmplificationSingle(): Single<Int>? {
        val code = PDMSUtil.MessageGetCode.CODE_AMPLIFICATION
        return getSingle(code)
            ?.doOnSuccess { amp = it }
    }

    fun setAmplificationSingle(@IntRange(from = 1, to = 255) amplification: Int): Single<Int>? {
        val code = PDMSUtil.MessageSetCode.CODE_AMPLIFICATION
        return setSingle(code, amplification.toByte())
            ?.doOnSuccess {
                Log.i(LOG_TAG, "$code:$it")
                amp = it
            }
    }

    fun getSensitivity(): Int {
        return sens
    }

    private fun getSensitivitySingle(): Single<Int>? {
        val code = PDMSUtil.MessageGetCode.CODE_SENSITIVITY
        return getSingle(code)
            ?.doOnSuccess { sens = it }
    }

    fun setSensitivitySingle(@IntRange(from = 1, to = 255) sensitivity: Int): Single<Int>? {
        val code = PDMSUtil.MessageSetCode.CODE_SENSITIVITY
        return setSingle(code, sensitivity.toByte())
            ?.doOnSuccess {
                Log.i(LOG_TAG, "$code:$it")
                sens = it
            }
    }

    private fun getSingle(code: PDMSUtil.MessageGetCode): Single<Int>? {
        var relay: PublishRelay<ByteArray>? = null
        return writeCharacteristic(UUID.fromString(PDMSUtil.UUID_COMMUNICATION), code.message)
            ?.doOnSubscribe {
                if (relayMap[code.byte] != null) {
                    relay = relayMap[code.byte]
                    Log.d(LOG_TAG, "true")
                } else {
                    relay = PublishRelay.create<ByteArray>().also {
                        relayMap[code.byte] = it
                    }
                    Log.d(LOG_TAG, "false")
                }
            }
            ?.flatMap {
                relay!!.firstOrError()
            }
            ?.map {
                Log.i(LOG_TAG, "$code:${it.map { it.toUByte().toUInt() }.toTypedArray().contentToString()}")
                it[2].toUByte().toInt()
            }
    }

    private fun setSingle(code: PDMSUtil.MessageSetCode, byte: Byte): Single<Int>? {
        var relay: PublishRelay<ByteArray>? = null
        return writeCharacteristic(UUID.fromString(PDMSUtil.UUID_COMMUNICATION), code.getMessage(byte))
            ?.doOnSubscribe {
                if (relayMap[code.byte] != null) {
                    relay = relayMap[code.byte]
                } else {
                    relay = PublishRelay.create<ByteArray>().also {
                        relayMap[code.byte] = it
                    }
                }
            }
            ?.map {
                Log.i(LOG_TAG, "$code:${it.map { it.toUByte().toUInt() }.toTypedArray().contentToString()}")
                it[2].toUByte().toInt()
            }
    }

    fun checkUpdateExist(): Maybe<String> {
        return Single.create<List<Pair<String, String>>> {
            val result = PDMSUtil.firmwareList()
            if (result != null) {
                it.onSuccess(result)
            } else {
                it.onError(NoSuchElementException())
            }
        }
            .subscribeOn(Schedulers.io())
            .flatMapMaybe {
                PDMSUtil.latestFirmware(it, firmwareVersion)?.let {
                    Maybe.just(it)
                } ?: Maybe.empty()
            }
    }

    fun <T : DfuBaseService> update(context: Context, clazz: Class<T>, uri: Uri): Observable<Int> {
        return DfuAdapter(context, macAddress, deviceName, uri).let {
            update(clazz, it)
        }
    }

    fun <T : DfuBaseService> update(context: Context, clazz: Class<T>, path: String): Observable<Int> {
        return DfuAdapter(context, macAddress, deviceName, path).let {
            update(clazz, it)
        }
    }

    fun <T : DfuBaseService> update(context: Context, clazz: Class<T>, @RawRes resRawId: Int): Observable<Int> {
        return DfuAdapter(context, macAddress, deviceName, resRawId).let {
            update(clazz, it)
        }
    }

    private fun <T : DfuBaseService> update(clazz: Class<T>, adapter: DfuAdapter): Observable<Int> {
        if (!connected) {
            return Observable.error(IllegalStateException("Disconnected"))
        }

        var controller: DfuServiceController? = null

        return Observable.create<Int> {
            val callback = object : DfuAdapter.Callback(adapter.address) {
                override fun onProgress(percent: Int) {
                    it.onNext(percent)
                }

                override fun onCompleted() {
                    it.onComplete()
                }

                override fun onAborted() {
                    if (!it.isDisposed) {
                        it.onError(IllegalStateException("Aborted"))
                    }
                }

                override fun onError(error: Exception) {
                    if (!it.isDisposed) {
                        it.onError(error)
                    }
                }
            }

            controller = adapter.start(clazz, callback)
        }
            .doOnDispose {
                controller?.abort()
            }
            .doFinally {
                controller = null
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
