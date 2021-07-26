package ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.algorigo.algorigoble.BleManager
import com.algorigo.pressurego.RxPDMSDevice
import com.algorigo.pressuregoapp.R
import com.algorigo.pressuregoapp.databinding.DialogMyDevicesBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class MyDevicesDialog : BottomSheetDialogFragment(),
    ConnectedRecyclerAdapter.ConnectedRecyclerDelegate {

    private lateinit var binding: DialogMyDevicesBinding
    private var macAddress: String? = null

    interface Callback {
        fun onDeviceSelected(macAddress: String)
    }

    private var callback: Callback? = null
    private val connectedRecyclerAdapter: ConnectedRecyclerAdapter by lazy {
        ConnectedRecyclerAdapter(this, macAddress, true)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.CustomBottomSheetDialogTheme)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DialogMyDevicesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(binding) {

            rvDevicesConnected.adapter = connectedRecyclerAdapter
            myDevicesAddDeviceButton.setOnClickListener {
                onAddDeviceClick()
            }
            BleManager.getInstance()
                .getConnectedDevices()
                .mapNotNull { it as? RxPDMSDevice }
                .let {
                    if(it.isEmpty()) {
                        clEmpty.visibility = View.VISIBLE
                        rvDevicesConnected.visibility = View.INVISIBLE
                    } else {
                        clEmpty.visibility = View.INVISIBLE
                        rvDevicesConnected.visibility = View.VISIBLE
                    }
                }
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

    companion object {
        const val KEY_MAC_ADDRESS: String = "KEY_MAC_ADDRESS"

        fun newInstance(macAddress: String? = null) = MyDevicesDialog().apply {
            arguments = Bundle().apply {
                putString(KEY_MAC_ADDRESS, macAddress)
            }
            this.macAddress = macAddress
        }
    }
}