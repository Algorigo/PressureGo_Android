package ui

import android.graphics.Paint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import com.algorigo.pressurego.RxPDMSDevice
import com.algorigo.pressuregoapp.R
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class DeviceInfoDialog : BottomSheetDialogFragment() {

    lateinit var device: RxPDMSDevice

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
        view.findViewById<TextView>(R.id.device_info_display_name_view).run {
            text = device.getDisplayName()
        }
        view.findViewById<TextView>(R.id.device_info_mac_address_view).run {
            text = device.macAddress
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

            }
        }
    }
}