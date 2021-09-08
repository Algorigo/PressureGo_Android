package com.algorigo.pressuregoapp.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.algorigo.algorigoble.BleManager
import com.algorigo.pressurego.RxPDMSDevice
import com.algorigo.pressuregoapp.R
import com.algorigo.pressuregoapp.databinding.ItemBluetoothScanConnectedDeviceBinding

class ConnectedRecyclerAdapter(
    private val delegate: ConnectedRecyclerDelegate,
    private val selectedMacAddress: String? = null,
    private val isMyDeviceInfo: Boolean = false
) :
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
                selectedMacAddress?.let {
                    clConnectedDevice.setBackgroundColor(ContextCompat.getColor(itemView.context, R.color.pressure_go_white_opacity_7))
                }
                if(isMyDeviceInfo) {
                    deviceMoreButton.setImageResource(R.drawable.ic_more)
                    deviceMoreButton.setOnClickListener {
                        delegate.onConnectedMoreSelected(device)
                    }
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
        return BleManager.getInstance().getConnectedDevices().mapNotNull { it as? RxPDMSDevice }
            .count()
    }

}