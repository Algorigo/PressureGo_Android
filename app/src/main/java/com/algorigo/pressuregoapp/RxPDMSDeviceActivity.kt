package com.algorigo.pressuregoapp

import com.algorigo.algorigoble.BleManager
import com.algorigo.pressurego.RxPDMSDevice

class RxPDMSDeviceActivity : PDMSDeviceActivity() {

    private var pdmsDevice: RxPDMSDevice? = null

    override fun initDevice(macAddress: String) {
        pdmsDevice = BleManager.getInstance().getDevice(macAddress) as? RxPDMSDevice
    }

    override fun getDeviceName() {
        deviceNameTextView.text = pdmsDevice?.getDeviceName()
    }

    override fun getManufactureName() {
        manufactureNameTextView.text = pdmsDevice?.getManufactureName()
    }

    override fun getHardware() {
        hardwareVersionTextView.text = pdmsDevice?.getHardwareVersion()
    }

    override fun getFirmware() {
        firmwareVersionTextView.text = pdmsDevice?.getFirmwareVersion()
    }
}