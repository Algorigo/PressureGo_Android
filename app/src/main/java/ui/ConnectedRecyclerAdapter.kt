package ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.algorigo.algorigoble.BleManager
import com.algorigo.pressurego.RxPDMSDevice
import com.algorigo.pressuregoapp.R

class ConnectedRecyclerAdapter(private val delegate: ConnectedRecyclerDelegate): RecyclerView.Adapter<ConnectedRecyclerAdapter.ConnectedRecyclerViewHolder>() {

    interface ConnectedRecyclerDelegate {
        fun onConnectedDeviceSelected(device: RxPDMSDevice)
        fun onConnectedMoreSelected(device: RxPDMSDevice)
    }

    inner class ConnectedRecyclerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        fun setDevice(device: RxPDMSDevice) {
            itemView.setOnClickListener {
                delegate.onConnectedDeviceSelected(device)
            }
            itemView.findViewById<TextView>(R.id.device_name).text = device.getDisplayName()
            itemView.findViewById<TextView>(R.id.mac_address_view).text = device.macAddress
            itemView.findViewById<ImageButton>(R.id.device_more_button).setOnClickListener {
                delegate.onConnectedMoreSelected(device)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ConnectedRecyclerViewHolder {
        return LayoutInflater.from(parent.context).inflate(R.layout.item_bluetooth_scan_connected_device, parent, false).let {
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
        return BleManager.getInstance()
            .getConnectedDevices()
            .mapNotNull { it as? RxPDMSDevice }
            .count()
    }
}