package com.algorigo.pressuregoapp

import android.util.Log
import com.algorigo.pressurego.PDMSDevice
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking

class BasicPDMSDeviceActivity : PDMSDeviceActivity() {

    private var pdmsDevice: PDMSDevice? = null
    private var callback: PDMSDevice.DataCallback? = null

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
            try {
                pdmsDevice?.setAmplification(amplification)
            } catch (e: Exception) {
                return@async
            }
            runBlocking(Dispatchers.Main) {
                amplificationEditText.setText(amplification.toString())
            }
        }
    }

    override fun data() {
        if (callback != null) {
            pdmsDevice?.unregisterDataCallback(callback!!)
        } else {
            callback = object : PDMSDevice.DataCallback {
                override fun onData(intArray: IntArray) {
                    runOnUiThread {
                        dataTextView.text = intArray.contentToString()
                    }
                }
            }.also {
                pdmsDevice?.registerDataCallback(it)
            }
        }
    }
}