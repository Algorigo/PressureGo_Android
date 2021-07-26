package com.algorigo.pressuregoapp

import android.util.Log
import androidx.core.content.ContextCompat
import com.algorigo.pressurego.PDMSDevice
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import java.io.File

class BasicPDMSDeviceActivity : PDMSDeviceActivity() {

    private var pdmsDevice: PDMSDevice? = null
    private var callback: PDMSDevice.DataCallback? = null

    override fun onPause() {
        super.onPause()
        if (callback != null) {
            pdmsDevice?.unregisterDataCallback(callback!!)
            callback = null
        }
    }

    override fun initDevice(macAddress: String) {
        pdmsDevice = BasicActivity.pdmsDevices[macAddress]
    }

    override fun getDeviceName() {
        CoroutineScope(Dispatchers.IO).async {
            val deviceName = try {
                pdmsDevice?.getDeviceName()
            } catch (e: Exception) {
                null
            }
            runBlocking(Dispatchers.Main) {
                deviceNameTextView.text = deviceName
            }
        }
    }

    override fun getManufactureName() {
        CoroutineScope(Dispatchers.IO).async {
            val manufactureName = try {
                pdmsDevice?.getManufacturerName()
            } catch (e: Exception) {
                null
            }
            runBlocking(Dispatchers.Main) {
                manufactureNameTextView.text = manufactureName
            }
        }
    }

    override fun getHardware() {
        CoroutineScope(Dispatchers.IO).async {
            val hardware = try {
                pdmsDevice?.getHardwareVersion()
            } catch (e: Exception) {
                null
            }
            runBlocking(Dispatchers.Main) {
                hardwareVersionTextView.text = hardware
            }
        }
    }

    override fun getFirmware()  {
        CoroutineScope(Dispatchers.IO).async {
            val firmware = try {
                pdmsDevice?.getFirmwareVersion()
            } catch (e: Exception) {
                null
            }
            runBlocking(Dispatchers.Main) {
                firmwareVersionTextView.text = firmware
            }
        }
    }

    override fun getInterval() {
        CoroutineScope(Dispatchers.IO).async {
            val interval = try {
                pdmsDevice?.getSensingIntervalMillis()
            } catch (e: Exception) {
                null
            }
            runBlocking(Dispatchers.Main) {
                intervalEditText.setText(interval.toString())
            }
        }
    }

    override fun setInterval(interval: Int) {
        CoroutineScope(Dispatchers.IO).async {
            val interval = try {
                pdmsDevice?.setSensorScanIntervalMillis(interval)
            } catch (e: Exception) {
                null
            }
            runBlocking(Dispatchers.Main) {
                intervalEditText.setText(interval.toString())
            }
        }
    }

    override fun getAmplification() {
        CoroutineScope(Dispatchers.IO).async {
            val amplification = try {
                pdmsDevice?.getAmplification()
            } catch (e: Exception) {
                null
            }
            runBlocking(Dispatchers.Main) {
                amplificationEditText.setText(amplification.toString())
            }
        }
    }

    override fun setAmplification(amplification: Int) {
        CoroutineScope(Dispatchers.IO).async {
            val amplification = try {
                pdmsDevice?.setAmplification(amplification)
            } catch (e: Exception) {
                null
            }
            runBlocking(Dispatchers.Main) {
                amplificationEditText.setText(amplification.toString())
            }
        }
    }

    override fun getSensitivity() {
        CoroutineScope(Dispatchers.IO).async {
            val sensitivity = try {
                pdmsDevice?.getSensitivity()
            } catch (e: Exception) {
                null
            }
            runBlocking(Dispatchers.Main) {
                sensitivityEditText.setText(sensitivity.toString())
            }
        }
    }

    override fun setSensitivity(sensitivity: Int) {
        CoroutineScope(Dispatchers.IO).async {
            val sensitivity = try {
                pdmsDevice?.setSensitivity(sensitivity)
            } catch (e: Exception) {
                null
            }
            runBlocking(Dispatchers.Main) {
                sensitivityEditText.setText(sensitivity.toString())
            }
        }
    }

    override fun data() {
        if (callback != null) {
            pdmsDevice?.unregisterDataCallback(callback!!)
            callback = null
        } else {
            callback = object : PDMSDevice.DataCallback {
                override fun onData(intArray: IntArray) {
                    runOnUiThread {
                        setData(intArray)
                    }
                }
            }.also {
                pdmsDevice?.registerDataCallback(it)
            }
        }
    }

    override fun battery() {
        CoroutineScope(Dispatchers.IO).async {
            val battery = try {
                pdmsDevice?.getBattery()
            } catch (e: Exception) {
                null
            }
            runBlocking(Dispatchers.Main) {
                batteryTextView.text = "$battery%"
            }
        }
    }

    override fun lowBattery() {
        lowBatteryTextView.text = "Not working with basic"
    }

    override fun checkFirmwareExist() {
        firmwarePath = null
        CoroutineScope(Dispatchers.IO).async {
            val url = try {
                pdmsDevice?.checkUpdateExist()
            } catch (e: Exception) {
                null
            }
            runBlocking(Dispatchers.Main) {
                if (url != null) {
                    firmwareRemotePath = url
                    firmwarePathTextView.text = url
                } else {
                    firmwarePathTextView.text = "Firmware Not Found"
                }
            }
        }
    }

    override fun updateFirmware() {
        if (firmwarePath != null) {
            updateLocally()
        } else if (firmwareRemotePath != null) {
            updateRemotely()
        }
    }

    private fun updateLocally() {
        firmwarePath?.also {
            CoroutineScope(Dispatchers.IO).async {
                try {
                    pdmsDevice?.update(
                        this@BasicPDMSDeviceActivity,
                        DfuService::class.java,
                        it
                    ) {
                        runOnUiThread {
                            firmwareUpdateResultTextView.text = "update $it%"
                        }
                    }
                    runBlocking(Dispatchers.Main) {
                        firmwareUpdateResultTextView.text = "complete"
                    }
                } catch (e: Exception) {
                    Log.e(LOG_TAG, "Dfu Error", e)
                    runBlocking(Dispatchers.Main) {
                        firmwareUpdateResultTextView.text = e.message
                    }
                }
            }
        }
    }

    private fun updateRemotely() {
        firmwareRemotePath?.also {
            val file = File(ContextCompat.getDataDir(this), "temp.zip")
            CoroutineScope(Dispatchers.IO).async {
                try {
                    Utility.download(it, file) {
                        runOnUiThread {
                            firmwareUpdateResultTextView.text = "Download $it%"
                        }
                    }
                    runBlocking(Dispatchers.Main) {
                        firmwareUpdateResultTextView.text = "Downloaded"
                    }
                    pdmsDevice?.update(
                        this@BasicPDMSDeviceActivity,
                        DfuService::class.java,
                        file.absolutePath
                    ) {
                        runOnUiThread {
                            firmwareUpdateResultTextView.text = "update $it%"
                        }
                    }
                    runBlocking(Dispatchers.Main) {
                        firmwareUpdateResultTextView.text = "complete"
                    }
                } catch (e: Exception) {
                    runBlocking(Dispatchers.Main) {
                        firmwareUpdateResultTextView.text = e.message
                    }
                } finally {
                    file.delete()
                }
            }
        }
    }

    companion object {
        private val LOG_TAG = BasicPDMSDeviceActivity::class.java.simpleName
    }
}