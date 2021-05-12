package com.algorigo.pressurego

object PDMSUtil {

    internal const val LOW_BATTERY_RATIO = 10.0

    internal const val BLE_NAME = "Algo_M"

    internal const val UUID_DEVICE_INFO_SERVICE =       "0000180A-0000-1000-8000-00805F9B34FB"
    internal const val UUID_MANUFACTURER_NAME =         "00002A29-0000-1000-8000-00805F9B34FB"
    internal const val UUID_HARDWARE_REVISION =         "00002A27-0000-1000-8000-00805F9B34FB"
    internal const val UUID_FIRMWARE_REVISION =         "00002A26-0000-1000-8000-00805F9B34FB"

    internal const val UUID_GENERIC_ACCESS_SERVICE =    "00001800-0000-1000-8000-00805F9B34FB"
    internal const val UUID_DEVICE_NAME =               "00002A00-0000-1000-8000-00805F9B34FB"
    internal const val UUID_APPEARANCE =                "00002A01-0000-1000-8000-00805F9B34FB"
    internal const val UUID_PERIPHERAL_PREFERRED =      "00002A04-0000-1000-8000-00805F9B34FB"
    internal const val UUID_CENTRAL_ADDRESS_RESOL =     "00002AA6-0000-1000-8000-00805F9B34FB"

    internal const val UUID_GENERIC_ATTRIBUTE_SERVICE = "00001801-0000-1000-8000-00805F9B34FB"
    internal const val UUID_SERVICE_CHANGED =           "00002A05-0000-1000-8000-00805F9B34FB"

    internal const val UUID_BOND_MANAGEMENT_SERVICE =   "0000181E-0000-1000-8000-00805F9B34FB"
    internal const val UUID_BOND_MANAGEMENT_FEATURE =   "00002AA5-0000-1000-8000-00805F9B34FB"
    internal const val UUID_BOND_MANAGEMENT_CONTROL =   "00002AA4-0000-1000-8000-00805F9B34FB"

    internal const val UUID_BATTERY_SERVICE =           "0000180F-0000-1000-8000-00805F9B34FB"
    internal const val UUID_BATTERY_LEVEL =             "00002A19-0000-1000-8000-00805F9B34FB"

    internal const val UUID_SERVICE_DATA =              "6E400001-B5A3-F393-E0A9-E50E24DCCA9E"
    internal const val UUID_COMMUNICATION =             "6E400002-B5A3-F393-E0A9-E50E24DCCA9E"
    internal const val UUID_DATA_NOTIFICATION =         "6E400003-B5A3-F393-E0A9-E50E24DCCA9E"
    internal const val UUID_DATA_DESCRIPTOR =           "00002902-0000-1000-8000-00805f9b34fb"

    internal enum class MessageSetCode(val byte: Byte) {
        CODE_SENSOR_SCAN_INTERVAL(0xa1.toByte()),
        CODE_AMPLIFICATION(0xc1.toByte()),
        CODE_SENSITIVITY(0xb1.toByte()),
        ;

        fun getMessage(value: Byte) = byteArrayOf(0x02, byte, value, 0x03)
    }

    internal enum class MessageGetCode(val byte: Byte) {
        CODE_SENSOR_SCAN_INTERVAL(0xa2.toByte()),
        CODE_AMPLIFICATION(0xc2.toByte()),
        CODE_SENSITIVITY(0xb2.toByte()),
        ;

        val message: ByteArray
            get() = byteArrayOf(0x02, byte, 0x03)
    }
}