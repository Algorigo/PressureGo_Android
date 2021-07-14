package ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.recyclerview.widget.RecyclerView
import com.algorigo.pressurego.RxPDMSDevice
import com.algorigo.pressuregoapp.R
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class MyDevicesDialog : BottomSheetDialogFragment(),
    ConnectedRecyclerAdapter.ConnectedRecyclerDelegate {

    interface Callback {
        fun onDeviceSelected(macAddress: String)
    }

    private var callback: Callback? = null
    private lateinit var bluetoothConnectedRecycler: RecyclerView
    private val connectedRecyclerAdapter = ConnectedRecyclerAdapter(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.CustomBottomSheetDialogTheme)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_my_devices, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bluetoothConnectedRecycler = view.findViewById(R.id.my_devices_connected_device)
        bluetoothConnectedRecycler.adapter = connectedRecyclerAdapter

        view.findViewById<Button>(R.id.my_devices_add_device_button).setOnClickListener {
            onAddDeviceClick()
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        if (context is Callback) {
            callback = context
        }
    }

    override fun onConnectedDeviceSelected(device: RxPDMSDevice) {
        callback?.onDeviceSelected(device.macAddress)
        dismiss()
    }

    override fun onConnectedMoreSelected(device: RxPDMSDevice) {
        activity?.let {
            dismiss()
            DeviceInfoDialog().also {
                it.device = device
            }.apply {
                show(it.supportFragmentManager, DeviceInfoDialog::class.java.simpleName)
            }
        }
    }

    private fun onAddDeviceClick() {
        dismiss()
        context?.apply {
            startActivity(Intent(this, BluetoothScanActivity::class.java))
        }
    }
}