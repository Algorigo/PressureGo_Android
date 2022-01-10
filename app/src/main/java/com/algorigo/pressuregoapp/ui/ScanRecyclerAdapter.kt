package com.algorigo.pressuregoapp.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.algorigo.algorigoble2.BleDevice
import com.algorigo.pressurego.RxPDMSDevice
import com.algorigo.pressuregoapp.R

class ScanRecyclerAdapter(private val delegate: ScanRecyclerDelegate) : RecyclerView.Adapter<ScanRecyclerAdapter.ScanRecyclerViewHolder>() {

    interface ScanRecyclerDelegate {
        fun getScanItemCount(): Int
        fun getScanDevice(position: Int): RxPDMSDevice
        fun onDeviceSelected(device: RxPDMSDevice)
    }

    inner class ScanRecyclerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        fun setDevice(device: RxPDMSDevice) {
            itemView.setOnClickListener {
                delegate.onDeviceSelected(device)
            }
            itemView.findViewById<TextView>(R.id.device_name).text = device.getDisplayName()
            itemView.findViewById<TextView>(R.id.mac_address_view).text = device.deviceId
            itemView.findViewById<ProgressBar>(R.id.device_progress).visibility = when (device.connectionState) {
                BleDevice.ConnectionState.CONNECTING -> View.VISIBLE
                else -> View.GONE
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScanRecyclerViewHolder {
        return LayoutInflater.from(parent.context).inflate(R.layout.item_bluetooth_scan_scanned_device, parent, false).let {
            ScanRecyclerViewHolder(it)
        }
    }

    override fun onBindViewHolder(holder: ScanRecyclerViewHolder, position: Int) {
        delegate.getScanDevice(position).also {
            holder.setDevice(it)
        }
    }

    override fun getItemCount(): Int {
        return delegate.getScanItemCount()
    }
}