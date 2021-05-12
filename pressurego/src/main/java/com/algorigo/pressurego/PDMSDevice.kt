package com.algorigo.pressurego

import android.bluetooth.*
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.util.Log
import androidx.annotation.IntRange
import kotlinx.coroutines.*
import java.lang.Exception
import java.util.*

class PDMSDevice(val bluetoothDevice: BluetoothDevice, val callback: Callback? = null) {

    interface Callback {
        fun onStateChanged(device: PDMSDevice)
    }
    interface DataCallback {
        fun onData(intArray: IntArray)
    }

    private var bluetoothGatt: BluetoothGatt? = null
    private var characteristicMap = mutableMapOf<UUID, LockGetter<ByteArray>>()
    private var communicationMap = mutableMapOf<Byte, LockGetter<ByteArray>>()
    private var dataCallbacks = mutableListOf<DataCallback>()

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)
            Log.i(LOG_TAG, "onConnectionStateChange:$gatt:$newState")
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    gatt?.let {
                        bluetoothGatt = it
                        it.discoverServices()
                    }

                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    gatt?.let {
                        bluetoothGatt = null
                        callback?.onStateChanged(this@PDMSDevice)
                    }
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            Log.i(LOG_TAG, "onServicesDiscovered:${gatt?.services?.map { it.uuid.toString() }?.toTypedArray()?.contentToString()}")
            super.onServicesDiscovered(gatt, status)
            callback?.onStateChanged(this@PDMSDevice)
            gatt?.getService(UUID.fromString(PDMSUtil.UUID_SERVICE_DATA))
                ?.getCharacteristic(UUID.fromString(PDMSUtil.UUID_DATA_NOTIFICATION))?.let { characteristic ->
                    gatt.setCharacteristicNotification(characteristic, true)
                    val descriptor = characteristic.getDescriptor(UUID.fromString(PDMSUtil.UUID_DATA_DESCRIPTOR))
                    descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                    gatt.writeDescriptor(descriptor)
                }
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?,
            status: Int
        ) {
            Log.i(LOG_TAG, "onCharacteristicRead:${characteristic?.uuid}, ${characteristic?.value?.contentToString()}")
            super.onCharacteristicRead(gatt, characteristic, status)
            characteristic?.uuid?.let { characteristicMap.remove(it) }?.setValue(characteristic.value)
        }

        override fun onDescriptorWrite(
            gatt: BluetoothGatt?,
            descriptor: BluetoothGattDescriptor?,
            status: Int
        ) {
            Log.i(LOG_TAG, "onDescriptorWrite:${descriptor?.uuid}, ${descriptor?.value?.contentToString()}")
            super.onDescriptorWrite(gatt, descriptor, status)
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?
        ) {
//            Log.i(LOG_TAG, "onCharacteristicRead:${characteristic?.uuid}, ${characteristic?.value?.contentToString()}")
            super.onCharacteristicChanged(gatt, characteristic)
            when (characteristic?.value?.get(0)) {
                0x02.toByte() -> {
                    communicationMap.remove(characteristic.value[1])?.setValue(characteristic.value)
                }
                0x01.toByte() -> {
                    val intArray = IntArray(4) {
                        val aLower = characteristic.value[it*2+2].toInt() and 0xff
                        val aUpper = characteristic.value[it*2+3].toInt() and 0xff
                        (aUpper shl 8) + aLower
                    }
                    dataCallbacks.forEach {
                        try {
                            it.onData(intArray)
                        } catch (e: Exception) {
                            Log.e(LOG_TAG, "", e)
                        }
                    }
                }
            }
        }
    }

    init {

    }

    val name: String
        get() = bluetoothDevice.name ?: ""

    val address: String
        get() = bluetoothDevice.address

    val connected: Boolean
        get() = bluetoothGatt != null

    fun connect(context: Context) {
        bluetoothDevice.connectGatt(context, false, gattCallback)
    }

    fun disconnect() {
        bluetoothGatt?.disconnect()
    }

    suspend fun getDeviceName(): String {
        bluetoothGatt?.let { gatt ->
            val uuid = UUID.fromString(PDMSUtil.UUID_DEVICE_NAME)
            gatt.getService(UUID.fromString(PDMSUtil.UUID_GENERIC_ACCESS_SERVICE))
                ?.getCharacteristic(uuid)?.let { characteristic ->
                    LockGetter<ByteArray>().let {
                        characteristicMap[uuid] = it
                        gatt.readCharacteristic(characteristic)
                        val byteArray = it.getValue()
                        return String(byteArray)
                    }
                }
        }
        throw IllegalStateException()
    }

    suspend fun getManufacturerName(): String {
        bluetoothGatt?.let { gatt ->
            val uuid = UUID.fromString(PDMSUtil.UUID_MANUFACTURER_NAME)
            gatt.getService(UUID.fromString(PDMSUtil.UUID_DEVICE_INFO_SERVICE))
                ?.getCharacteristic(uuid)?.let { characteristic ->
                    LockGetter<ByteArray>().let {
                        characteristicMap[uuid] = it
                        gatt.readCharacteristic(characteristic)
                        val byteArray = it.getValue()
                        return String(byteArray)
                    }
                }
        }
        throw IllegalStateException()
    }

    suspend fun getHardwareVersion(): String {
        bluetoothGatt?.let { gatt ->
            val uuid = UUID.fromString(PDMSUtil.UUID_HARDWARE_REVISION)
            gatt.getService(UUID.fromString(PDMSUtil.UUID_DEVICE_INFO_SERVICE))
                ?.getCharacteristic(uuid)?.let { characteristic ->
                    LockGetter<ByteArray>().let {
                        characteristicMap[uuid] = it
                        gatt.readCharacteristic(characteristic)
                        val byteArray = it.getValue()
                        return String(byteArray)
                    }
                }
        }
        throw IllegalStateException()
    }

    suspend fun getFirmwareVersion(): String {
        bluetoothGatt?.let { gatt ->
            val uuid = UUID.fromString(PDMSUtil.UUID_FIRMWARE_REVISION)
            gatt.getService(UUID.fromString(PDMSUtil.UUID_DEVICE_INFO_SERVICE))
                ?.getCharacteristic(uuid)?.let { characteristic ->
                    LockGetter<ByteArray>().let {
                        characteristicMap[uuid] = it
                        gatt.readCharacteristic(characteristic)
                        val byteArray = it.getValue()
                        return String(byteArray)
                    }
            }
        }
        throw IllegalStateException()
    }

    suspend fun getBattery(): Int {
        bluetoothGatt?.let { gatt ->
            val uuid = UUID.fromString(PDMSUtil.UUID_BATTERY_LEVEL)
            gatt.getService(UUID.fromString(PDMSUtil.UUID_BATTERY_SERVICE))
                ?.getCharacteristic(uuid)?.let { characteristic ->
                    LockGetter<ByteArray>().let {
                        characteristicMap[uuid] = it
                        gatt.readCharacteristic(characteristic)
                        val byteArray = it.getValue()
                        return byteArray[0].toInt()
                    }
                }
        }
        throw IllegalStateException()
    }

    private suspend fun sendGetMessage(code: PDMSUtil.MessageGetCode): Int {
        bluetoothGatt?.let { gatt ->
            val uuid = UUID.fromString(PDMSUtil.UUID_COMMUNICATION)
            gatt.getService(UUID.fromString(PDMSUtil.UUID_SERVICE_DATA))
                ?.getCharacteristic(uuid)?.let { characteristic ->
                    LockGetter<ByteArray>().let {
                        communicationMap[code.byte] = it
                        characteristic.value = code.message
                        gatt.writeCharacteristic(characteristic)
                        val byteArray = it.getValue()
                        return byteArray[2].toUInt().toInt()
                    }
                }
        }
        throw IllegalStateException()
    }

    suspend fun getSensorScanInterval(): Int {
        val code = PDMSUtil.MessageGetCode.CODE_SENSOR_SCAN_INTERVAL
        return sendGetMessage(code)
    }

    fun setSensorScanInterval(@IntRange(from = 1, to = 255) interval: Int) {
        val code = PDMSUtil.MessageSetCode.CODE_SENSOR_SCAN_INTERVAL
        sendSetMessage(code, interval.toByte())
    }

    suspend fun getAmplification(): Int {
        val code = PDMSUtil.MessageGetCode.CODE_AMPLIFICATION
        return sendGetMessage(code)
    }

    fun setAmplification(@IntRange(from = 1, to = 255) amp: Int) {
        val code = PDMSUtil.MessageSetCode.CODE_AMPLIFICATION
        sendSetMessage(code, amp.toByte())
    }

    suspend fun getSensitivity(): Int {
        val code = PDMSUtil.MessageGetCode.CODE_SENSITIVITY
        return sendGetMessage(code)
    }

    fun setSensitivity(@IntRange(from = 1, to = 255) sens: Int) {
        val code = PDMSUtil.MessageSetCode.CODE_SENSITIVITY
        sendSetMessage(code, sens.toByte())
    }

    private fun sendSetMessage(code: PDMSUtil.MessageSetCode, value: Byte) {
        bluetoothGatt?.let { gatt ->
            val uuid = UUID.fromString(PDMSUtil.UUID_COMMUNICATION)
            gatt.getService(UUID.fromString(PDMSUtil.UUID_SERVICE_DATA))
                ?.getCharacteristic(uuid)?.let { characteristic ->
                    characteristic.value = code.getMessage(value)
                    gatt.writeCharacteristic(characteristic)
                    return
                }
        }
        throw IllegalStateException()
    }

    fun registerDataCallback(dataCallback: DataCallback) {
        dataCallbacks.add(dataCallback)
    }

    fun unregisterDataCallback(dataCallback: DataCallback) {
        dataCallbacks.remove(dataCallback)
    }

    companion object {
        private val LOG_TAG = PDMSDevice::class.java.simpleName

        val scanFilters: List<ScanFilter>
            get() {
                return listOf(
                    ScanFilter.Builder()
                        .setDeviceName(PDMSUtil.BLE_NAME)
                        .build()
                )
            }
        val scanSettings: ScanSettings
            get() {
                return ScanSettings.Builder().build()
            }
    }
}