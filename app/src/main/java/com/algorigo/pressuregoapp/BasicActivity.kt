package com.algorigo.pressuregoapp

import android.Manifest
import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.algorigo.pressurego.PDMSDevice
import io.reactivex.rxjava3.disposables.Disposable
import kotlinx.coroutines.*

class BasicActivity : AppCompatActivity(), PDMSDevice.Callback {
    lateinit var bluetoothAdapter: BluetoothAdapter
    private var devices = mutableMapOf<String, PDMSDevice>()
    private var scanning: Boolean = false

    private val deviceAdapter = DeviceAdapter(object : DeviceAdapter.Callback {
        override fun onConnectBtn(device: DeviceAdapter.Device) {
            if (device.connected) {
                disconnect(device.macAddress)
            } else {
                connect(device.macAddress)
            }
        }

        override fun onItemSelected(device: DeviceAdapter.Device) {
            if (device.connected) {
                devices[device.macAddress]?.also {
                    pdmsDevices[device.macAddress] = it
                    val intent = Intent(this@BasicActivity, BasicPDMSDeviceActivity::class.java)
                    intent.putExtra(PDMSDeviceActivity.MAC_ADDRESS_KEY, device.macAddress)
                    startActivity(intent)
                }
            }
        }
    })

    private val callback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)
            result?.device?.let { device ->
                if (!devices.containsKey(device.address)) {
                    devices[device.address] = PDMSDevice(device, this@BasicActivity)
                    runOnUiThread {
                        adjustRecycler()
                    }
                }
            }
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>?) {
            super.onBatchScanResults(results)
            results?.map { it.device }?.filter { device ->
                !devices.containsKey(device.address)
            }?.map { PDMSDevice(it, this@BasicActivity) }?.also {
                devices.putAll(it.map { Pair(it.address, it) })
                runOnUiThread {
                    adjustRecycler()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scan)

        bluetoothAdapter = (getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter

        findViewById<Button>(R.id.scan_btn).setOnClickListener {
            if (scanning) {
                stopScan()
            } else {
                startScan()
            }
        }
        findViewById<RecyclerView>(R.id.device_recycler).adapter = deviceAdapter
    }

    private fun startScan() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            scanning = true
            findViewById<Button>(R.id.scan_btn).text = getText(R.string.stop_scan)
            bluetoothAdapter.bluetoothLeScanner.startScan(PDMSDevice.scanFilters, PDMSDevice.scanSettings, callback)
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), PERMISSION_REQ_CODE)
        }
    }

    private fun adjustRecycler() {
        deviceAdapter.devices =
            devices.values.map { DeviceAdapter.Device(it.name, it.address, it.connected) }
        deviceAdapter.notifyDataSetChanged()
    }

    private fun stopScan() {
        scanning = false
        findViewById<Button>(R.id.scan_btn).text = getText(R.string.start_scan)
        bluetoothAdapter.bluetoothLeScanner.stopScan(callback)
    }

    private fun connect(macAddress: String) {
        devices[macAddress]?.connect(this)
    }

    private fun disconnect(macAddress: String) {
        devices[macAddress]?.disconnect()
    }

    override fun onStateChanged(device: PDMSDevice) {
        Log.e(LOG_TAG, "onStateChanged:${device.address}")
        runOnUiThread {
            adjustRecycler()
        }
    }

    companion object {
        private val LOG_TAG = BasicActivity::class.java.simpleName

        private const val PERMISSION_REQ_CODE = 0x01

        val pdmsDevices = mutableMapOf<String, PDMSDevice>()
    }
}