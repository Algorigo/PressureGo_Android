package com.algorigo.pressurego_example

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.recyclerview.widget.RecyclerView
import com.algorigo.algorigoble.BleManager
import com.algorigo.library.rx.permission.PermissionAppCompatActivity
import com.algorigo.pressurego.RxPDMSDevice
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.schedulers.Schedulers

class RxActivity : PermissionAppCompatActivity() {

    private lateinit var stateDisposable: Disposable
    private var scanDisposable: Disposable? = null
    private var devices = listOf<RxPDMSDevice>()

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
                devices.find { it.macAddress == device.macAddress }?.also {
                    val intent = Intent(this@RxActivity, RxPDMSDeviceActivity::class.java)
                    intent.putExtra(PDMSDeviceActivity.MAC_ADDRESS_KEY, device.macAddress)
                    startActivity(intent)
                }
            }
        }
    })

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scan)

        BleManager.init(applicationContext, BleManager.BleManagerEngine.ALGORIGO_BLE)
        BleManager.getInstance().bleDeviceDelegate = RxPDMSDevice.DeviceDelegate()
        stateDisposable = BleManager.getInstance().getConnectionStateObservable()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                Log.d(LOG_TAG, "${it.bleDevice} == ${it.connectionState}")
                adjustRecycler()
            }, {

            })

        findViewById<Button>(R.id.scan_btn).setOnClickListener {
            if (scanDisposable != null) {
                stopScan()
            } else {
                startScan()
            }
        }
        findViewById<RecyclerView>(R.id.device_recycler).adapter = deviceAdapter
    }

    override fun onPause() {
        super.onPause()
        stopScan()
    }

    override fun onDestroy() {
        super.onDestroy()
        stateDisposable.dispose()
    }

    private fun startScan() {
        scanDisposable = requestPermissionCompletable(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION))
            .andThen(BleManager.getInstance().scanObservable(5000).subscribeOn(Schedulers.io()))
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe {
                findViewById<Button>(R.id.scan_btn).text = getText(R.string.stop_scan)
            }
            .doFinally {
                scanDisposable = null
                findViewById<Button>(R.id.scan_btn).text = getText(R.string.start_scan)
            }
            .subscribe({
                devices = it.mapNotNull { it as? RxPDMSDevice }
                adjustRecycler()
            }, {
                Log.e(LOG_TAG, "", it)
            })
    }

    private fun adjustRecycler() {
        deviceAdapter.devices =
            devices.map { DeviceAdapter.Device(it.name, it.macAddress, it.connected) }
        deviceAdapter.notifyDataSetChanged()
    }

    private fun stopScan() {
        scanDisposable?.dispose()
    }

    private fun connect(macAddress: String) {
        devices.find { it.macAddress == macAddress }?.connectCompletable(false)
            ?.observeOn(AndroidSchedulers.mainThread())
            ?.subscribe({
                Log.e(LOG_TAG, "connect complete")
            }, {
                Log.e(LOG_TAG, "connect error", it)
            })
    }

    private fun disconnect(macAddress: String) {
        devices.find { it.macAddress == macAddress }?.disconnect()
    }

    companion object {
        private val LOG_TAG = RxActivity::class.java.simpleName
    }
}