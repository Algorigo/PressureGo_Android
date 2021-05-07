package com.algorigo.pressurego

import android.bluetooth.*
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import java.util.*

class PDMSDevice(val bluetoothDevice: BluetoothDevice, val callback: Callback? = null) {

    interface Callback {
        fun onStateChanged(device: PDMSDevice)
    }

    private var bluetoothGatt: BluetoothGatt? = null
    private var getterMap = mutableMapOf<UUID, LockGetter<ByteArray>>()

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)
            Log.e(LOG_TAG, "onConnectionStateChange:$gatt:$newState")
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
            Log.e(LOG_TAG, "onServicesDiscovered:${gatt?.services?.map { it.uuid.toString() }?.toTypedArray()?.contentToString()}")
            super.onServicesDiscovered(gatt, status)
            callback?.onStateChanged(this@PDMSDevice)
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?,
            status: Int
        ) {
            Log.e("!!!", "onCharacteristicRead:${characteristic?.uuid}")
            super.onCharacteristicRead(gatt, characteristic, status)
            characteristic?.uuid?.let { getterMap.remove(it) }?.setValue(characteristic.value)
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?,
            status: Int
        ) {
            Log.e("!!!", "onCharacteristicWrite:${characteristic?.uuid}")
            super.onCharacteristicWrite(gatt, characteristic, status)
            characteristic?.uuid?.let { getterMap.remove(it) }?.setValue(characteristic.value)
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?
        ) {
            super.onCharacteristicChanged(gatt, characteristic)
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
                        getterMap[uuid] = it
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
                        getterMap[uuid] = it
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
                        getterMap[uuid] = it
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
                        getterMap[uuid] = it
                        gatt.readCharacteristic(characteristic)
                        val byteArray = it.getValue()
                        return String(byteArray)
                    }
            }
        }
        throw IllegalStateException()
    }

    suspend fun getAmplification(): Int {
        bluetoothGatt?.let { gatt ->
            val uuid = UUID.fromString(PDMSUtil.UUID_AMPLIFICATION)
            gatt.getService(UUID.fromString(PDMSUtil.UUID_SERVICE_DATA))
                ?.getCharacteristic(uuid)?.let { characteristic ->
                    LockGetter<ByteArray>().let {
                        getterMap[uuid] = it
                        gatt.readCharacteristic(characteristic)
                        val byteArray = it.getValue()
                        return byteArray.toInt()
                    }
                }
        }
        throw IllegalStateException()
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