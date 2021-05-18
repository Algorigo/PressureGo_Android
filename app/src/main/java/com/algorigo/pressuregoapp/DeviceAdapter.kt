package com.algorigo.pressuregoapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class DeviceAdapter(val callback: Callback) : RecyclerView.Adapter<DeviceAdapter.DeviceViewHolder>() {

    data class Device(val name: String?, val macAddress: String, var connected: Boolean)

    interface Callback {
        fun onConnectBtn(device: Device)
        fun onItemSelected(device: Device)
    }

    var devices = listOf<Device>()

    inner class DeviceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        fun setDevice(device: Device) {
            itemView.setOnClickListener {
                callback.onItemSelected(device)
            }
            itemView.findViewById<TextView>(R.id.title_view).text = device.name
            itemView.findViewById<TextView>(R.id.mac_address_view).text = device.macAddress
            itemView.findViewById<Button>(R.id.connect_btn).apply {
                text = itemView.context.getText(if (device.connected) R.string.disconnect else R.string.connect)
                setOnClickListener {
                    callback.onConnectBtn(device)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        return DeviceViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_device, parent, false))
    }

    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        holder.setDevice(devices[position])
    }

    override fun getItemCount(): Int {
        return devices.count()
    }
}