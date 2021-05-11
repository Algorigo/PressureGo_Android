package com.algorigo.pressuregoapp

import android.util.Log
import com.algorigo.algorigoble.BleManager
import com.algorigo.pressurego.RxPDMSDevice
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.Disposable

class RxPDMSDeviceActivity : PDMSDeviceActivity() {

    private var pdmsDevice: RxPDMSDevice? = null
    private var dataDisposable: Disposable? = null

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

    override fun getAmplification() {
        amplificationEditText.setText(pdmsDevice?.getAmplification()?.toString())
    }

    override fun setAmplification(amplification: Int) {
        pdmsDevice?.setAmplificationCompletable(amplification)
            ?.observeOn(AndroidSchedulers.mainThread())
            ?.subscribe({
                amplificationEditText.setText(amplification.toString())
            }, {
                Log.e(LOG_TAG, "", it)
            })
    }

    override fun data() {
        if (dataDisposable != null) {
            dataDisposable?.dispose()
        } else {
            dataDisposable = pdmsDevice?.sendDataOn()
                ?.doFinally {
                    dataDisposable = null
                }
                ?.observeOn(AndroidSchedulers.mainThread())
                ?.subscribe({
                    dataTextView.text = it.contentToString()
                }, {
                    Log.e(LOG_TAG, "", it)
                })
        }
    }

    companion object {
        private val LOG_TAG = RxPDMSDeviceActivity::class.java.simpleName
    }
}