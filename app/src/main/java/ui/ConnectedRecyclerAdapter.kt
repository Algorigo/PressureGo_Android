package ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.algorigo.algorigoble.BleManager
import com.algorigo.pressurego.RxPDMSDevice
import com.algorigo.pressuregoapp.R
import com.algorigo.pressuregoapp.databinding.ItemBluetoothScanConnectedDeviceBinding

class ConnectedRecyclerAdapter(private val delegate: ConnectedRecyclerDelegate) :
    RecyclerView.Adapter<ConnectedRecyclerAdapter.ConnectedRecyclerViewHolder>() {


    interface ConnectedRecyclerDelegate {
        fun onConnectedDeviceSelected(device: RxPDMSDevice)
        fun onConnectedMoreSelected(device: RxPDMSDevice)
    }

    inner class ConnectedRecyclerViewHolder(binding: ViewBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private val itemBleConnectedBinding = binding as ItemBluetoothScanConnectedDeviceBinding

        fun setDevice(device: RxPDMSDevice) {
            with(itemBleConnectedBinding) {
                root.setOnClickListener {
                    delegate.onConnectedDeviceSelected(device)
                }
                deviceName.text = device.getDisplayName()
                macAddressView.text = device.macAddress
                deviceMoreButton.setOnClickListener {
                    delegate.onConnectedMoreSelected(device)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ConnectedRecyclerViewHolder {
        return ItemBluetoothScanConnectedDeviceBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        ).let {
            ConnectedRecyclerViewHolder(it)
        }
    }

    override fun onBindViewHolder(holder: ConnectedRecyclerViewHolder, position: Int) {
        BleManager.getInstance()
            .getConnectedDevices()
            .mapNotNull { it as? RxPDMSDevice }[position]
            .also {
                holder.setDevice(it)
            }
    }

    override fun getItemCount(): Int {
        return BleManager.getInstance().getConnectedDevices().mapNotNull { it as? RxPDMSDevice }.count()
    }

}