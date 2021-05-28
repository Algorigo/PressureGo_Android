package com.algorigo.pressurego

import android.util.Log
import androidx.annotation.WorkerThread
import com.google.gson.JsonParser
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.URL
import java.net.URLConnection
import kotlin.math.roundToInt


object PDMSUtil {

    private val LOG_TAG = "!!!"//PDMSUtil::class.java.simpleName

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

    private const val FIRMWARE_JSON_URL = "https://pressure-go.s3.ap-northeast-2.amazonaws.com/firmware/firmware.json"

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

    internal const val intervalMillisMin = 25L
    internal const val intervalMillisMax = 6375L

    internal fun intervalValueToMillis(interval: Int): Int {
        return interval * 25
    }

    internal fun intervalMillisToValue(millis: Int): Int {
        return (millis.toFloat()/25).roundToInt()
    }

    @WorkerThread
    internal fun firmwareList(): List<Pair<String, String>>? {
        var count: Int
        var result: String? = null
        try {
            val url = URL(FIRMWARE_JSON_URL)
            val connection: URLConnection = url.openConnection()
            connection.connect()

            // this will be useful so that you can show a tipical 0-100%
            // progress bar
            val lengthOfFile: Int = connection.contentLength

            // download the file
            val input: InputStream = BufferedInputStream(
                url.openStream(),
                8192
            )

            // Output stream
            val output = ByteArrayOutputStream(lengthOfFile)
            val data = ByteArray(1024)
            var total: Long = 0
            while ((input.read(data).also { count = it }) != -1) {
                total += count.toLong()
                // publishing the progress....
                // After this onProgressUpdate will be called
//                publishProgress("" + ((total * 100) / lenghtOfFile).toInt())
                Log.e(LOG_TAG, ((total * 100) / lengthOfFile).toInt().toString())

                // writing data to file
                output.write(data, 0, count)
            }

            result = String(output.toByteArray(), Charsets.UTF_8)
            Log.e(LOG_TAG, "result:$result")

            // flushing output
            output.flush()

            // closing streams
            output.close()
            input.close()
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Get Firmware Error", e)
        }

        val jsonObject = result?.let {
            JsonParser.parseString(it).asJsonObject
        }

        return jsonObject?.let {
            it["firmware"].asJsonArray.map { element ->
                element.asJsonObject.let { obj ->
                    Pair(obj["version"].asJsonPrimitive.asString, obj["url"].asJsonPrimitive.asString)
                }
            }
        }
    }

    fun latestFirmware(
        list: List<Pair<String, String>>,
        firmwareVersion: String
    ): String? {
        return list.maxByOrNull { it.first }?.let { pair ->
            if (pair.first.toLowerCase() > firmwareVersion.toLowerCase()) {
                pair.second
            } else {
                null
            }
        }
    }
}