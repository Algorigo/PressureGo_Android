package com.algorigo.pressuregoapp.ui

import android.graphics.Paint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.algorigo.pressurego.RxPDMSDevice
import com.algorigo.pressuregoapp.R
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.Disposable

class DeviceInfoDialog : BottomSheetDialogFragment() {

    lateinit var device: RxPDMSDevice

    private var batteryDisposable: Disposable? = null
    private var checkUpdateDisposable: Disposable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.CustomBottomSheetDialogTheme)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_device_info, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<BatteryView>(R.id.device_info_battery_view).run {
            batteryDisposable = device.getBatteryPercentObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    batteryPercent = it
                }, {
                    Log.e(LOG_TAG, "", it)
                })
        }
        view.findViewById<TextView>(R.id.device_info_display_name_view).run {
            text = device.getDisplayName()
        }
        view.findViewById<TextView>(R.id.device_info_mac_address_view).run {
            text = device.deviceId
        }
        view.findViewById<TextView>(R.id.device_info_hardware_version_view).run {
            text = device.getHardwareVersion()
        }
        view.findViewById<TextView>(R.id.device_info_firmware_version_view).run {
            text = device.getFirmwareVersion()
        }
        view.findViewById<TextView>(R.id.device_info_firmware_update_button).run {
            paintFlags += Paint.UNDERLINE_TEXT_FLAG
            setOnClickListener {
                if (checkUpdateDisposable == null) {
                    checkUpdateDisposable = device.checkUpdateExist()
                        .doFinally {
                            checkUpdateDisposable = null
                        }
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe({
                            showUpdateCheckDialog(it.second)
                        }, {
                            Log.e(LOG_TAG, "", it)
                        }, {
                            showUpdateCheckDialog(null)
                        })
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        batteryDisposable?.dispose()
        checkUpdateDisposable?.dispose()
    }

    private fun showUpdateCheckDialog(remoteFirmware: String?) {
        activity?.let {
            AlertDialog.Builder(it)
                .setTitle("Update Firmware")
                .setMessage("Do you want to update?")
                .setNegativeButton("Cancel") { _, _ -> }
                .setPositiveButton("OK") { _, _ ->
                    dismiss()
                    FirmwareUpdateDialog().also {
                        it.device = device
                        it.firmwareRemote = remoteFirmware
                    }.apply {
                        show(it.supportFragmentManager, FirmwareUpdateDialog::class.java.simpleName)
                    }
                }
                .create().show()
        }
    }

    companion object {
        private val LOG_TAG = DeviceInfoDialog::class.java.simpleName
    }
}