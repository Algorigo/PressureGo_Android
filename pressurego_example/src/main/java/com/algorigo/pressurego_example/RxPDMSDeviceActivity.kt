package com.algorigo.pressurego_example

import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.algorigo.algorigoble.BleManager
import com.algorigo.pressurego.RxPDMSDevice
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.Disposable
import java.io.File

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
        intervalEditText.setText(pdmsDevice?.getSensingIntervalMillis()?.toString())
    }

    override fun setInterval(interval: Int) {
        pdmsDevice?.setSensingIntervalMillisSingle(interval)
            ?.observeOn(AndroidSchedulers.mainThread())
            ?.subscribe({
            }, {
                Log.e(LOG_TAG, "", it)
            })
    }

    override fun getAmplification() {
        amplificationEditText.setText(pdmsDevice?.getAmplification()?.toString())
    }

    override fun setAmplification(amplification: Int) {
        pdmsDevice?.setAmplificationSingle(amplification)
            ?.observeOn(AndroidSchedulers.mainThread())
            ?.subscribe({
            }, {
                Log.e(LOG_TAG, "", it)
            })
    }

    override fun getSensitivity() {
        sensitivityEditText.setText(pdmsDevice?.getSensitivity()?.toString())
    }

    override fun setSensitivity(sensitivity: Int) {
        pdmsDevice?.setSensitivitySingle(sensitivity)
            ?.observeOn(AndroidSchedulers.mainThread())
            ?.subscribe({
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
                    setData(it)
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

    override fun checkFirmwareExist() {
        firmwarePath = null
        pdmsDevice?.checkUpdateExist()
            ?.observeOn(AndroidSchedulers.mainThread())
            ?.subscribe({
                firmwareRemotePath = it
                firmwarePathTextView.text = it
            }, {
                Log.e(LOG_TAG, "", it)
                firmwarePathTextView.text = "Firmware Error : ${it.message}"
            }, {
                firmwarePathTextView.text = "Firmware Not Found"
            })
    }

    var disposable: Disposable? = null

    override fun updateFirmware() {
        if (disposable != null) {
            disposable?.dispose()
            return
        }

        if (firmwarePath != null) {
            updateLocally()
        } else if (firmwareRemotePath != null) {
            updateRemotely()
        }
    }

    private fun updateLocally() {
        disposable = firmwarePath?.let {
            pdmsDevice
                ?.update(this, DfuService::class.java, it)
                ?.doFinally {
                    disposable = null
                }
                ?.observeOn(AndroidSchedulers.mainThread())
                ?.subscribe({
                    Log.i(LOG_TAG, "update $it%")
                    firmwareUpdateResultTextView.text = "update $it%"
                }, {
                    Log.e(LOG_TAG, "", it)
                    firmwareUpdateResultTextView.text = it.message
                }, {
                    Log.i(LOG_TAG, "update complete")
                    firmwareUpdateResultTextView.text = "complete"
                })
        }
    }

    private fun updateRemotely() {
        disposable = firmwareRemotePath?.let {
            val file = File(ContextCompat.getDataDir(this), "temp.zip")
            Utility.downloadObservable(it, file)
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext {
                    firmwareUpdateResultTextView.text = "Download $it%"
                }
                .ignoreElements()
                .andThen(pdmsDevice?.update(this, DfuService::class.java, file.absolutePath))
                .doFinally {
                    disposable = null
                    file.delete()
                }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    Log.i(LOG_TAG, "update $it%")
                    firmwareUpdateResultTextView.text = "update $it%"
                }, {
                    Log.e(LOG_TAG, "", it)
                    firmwareUpdateResultTextView.text = it.message
                }, {
                    Log.i(LOG_TAG, "update complete")
                    firmwareUpdateResultTextView.text = "complete"
                })
        }
    }

    companion object {
        private val LOG_TAG = RxPDMSDeviceActivity::class.java.simpleName
    }
}