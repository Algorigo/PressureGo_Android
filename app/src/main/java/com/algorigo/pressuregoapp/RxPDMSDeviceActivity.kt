package com.algorigo.pressuregoapp

import android.util.Log
import android.widget.Toast
import com.algorigo.algorigoble.BleManager
import com.algorigo.pressurego.RxPDMSDevice
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.Disposable

class RxPDMSDeviceActivity : PDMSDeviceActivity() {

    private var pdmsDevice: RxPDMSDevice? = null
    private var dataDisposable: Disposable? = null
    private var batteryDisposable: Disposable? = null
    private var lowBatteryDisposable: Disposable? = null

    override fun onPause() {
        super.onPause()
        dataDisposable?.dispose()
        batteryDisposable?.dispose()
        lowBatteryDisposable?.dispose()
    }

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

    override fun getInterval() {
        intervalEditText.setText(pdmsDevice?.getSensingInterval()?.toString())
    }

    override fun setInterval(interval: Int) {
        pdmsDevice?.setSensingIntervalCompletable(interval)
            ?.observeOn(AndroidSchedulers.mainThread())
            ?.subscribe({
                dataTextView.text = "Set Sensor Scan Interval OK"
            }, {
                Log.e(LOG_TAG, "", it)
            })
    }

    override fun getAmplification() {
        amplificationEditText.setText(pdmsDevice?.getAmplification()?.toString())
    }

    override fun setAmplification(amplification: Int) {
        pdmsDevice?.setAmplificationCompletable(amplification)
            ?.observeOn(AndroidSchedulers.mainThread())
            ?.subscribe({
                dataTextView.text = "Set Amplification OK"
            }, {
                Log.e(LOG_TAG, "", it)
            })
    }

    override fun getSensitivity() {
        sensitivityEditText.setText(pdmsDevice?.getSensitivity()?.toString())
    }

    override fun setSensitivity(sensitivity: Int) {
        pdmsDevice?.setSensitivityCompletable(sensitivity)
            ?.observeOn(AndroidSchedulers.mainThread())
            ?.subscribe({
                dataTextView.text = "Set Sensitivity OK"
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

    override fun battery() {
        if (batteryDisposable != null) {
            batteryDisposable?.dispose()
        } else {
            batteryDisposable = pdmsDevice?.getBatteryPercentObservable()
                ?.doFinally {
                    batteryDisposable = null
                }
                ?.observeOn(AndroidSchedulers.mainThread())
                ?.subscribe({
                    batteryTextView.text = "$it%"
                }, {
                    Log.e(LOG_TAG, "", it)
                })
        }
    }

    override fun lowBattery() {
        if (lowBatteryDisposable != null) {
            lowBatteryDisposable?.dispose()
        } else {
            lowBatteryDisposable = pdmsDevice?.getLowBatteryObservable(15)
                ?.doFinally {
                    lowBatteryDisposable = null
                }
                ?.observeOn(AndroidSchedulers.mainThread())
                ?.subscribe({
                    Toast.makeText(this, "Low Battery", Toast.LENGTH_LONG).show()
                }, {
                    Log.e(LOG_TAG, "", it)
                })
        }
    }

    companion object {
        private val LOG_TAG = RxPDMSDeviceActivity::class.java.simpleName
    }
}