package com.algorigo.pressuregoapp.ui

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.annotation.RequiresApi
import com.algorigo.algorigoble2.BleScanSettings
import com.algorigo.library.rx.permission.PermissionAppCompatActivity
import com.algorigo.pressurego.BleManagerProvider
import com.algorigo.pressurego.RxPDMSDevice
import com.algorigo.pressuregoapp.databinding.ActivityBluetoothScanBinding
import com.algorigo.pressuregoapp.util.AppInfoUtil
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.schedulers.Schedulers
import java.util.concurrent.TimeUnit
import androidx.appcompat.app.AlertDialog

class BluetoothScanActivity : PermissionAppCompatActivity(),
    ConnectedRecyclerAdapter.ConnectedRecyclerDelegate,
    ScanRecyclerAdapter.ScanRecyclerDelegate {

    private lateinit var binding: ActivityBluetoothScanBinding

    private var connectionStateDisposable: Disposable? = null
    private var scanDisposable: Disposable? = null
    private var devices = listOf<RxPDMSDevice>()
    private var connectDisposable: Disposable? = null

    private val connectedRecyclerAdapter: ConnectedRecyclerAdapter by lazy {
        ConnectedRecyclerAdapter(this, this)
    }
    private val scanRecyclerAdapter: ScanRecyclerAdapter by lazy {
        ScanRecyclerAdapter(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBluetoothScanBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initRecyclerView()

        if (intent.getBooleanExtra(FIRST_KEY, false)) {
            binding.btnBack.visibility = View.GONE
            ConfirmDialog.newInstance(
                "Use your Location",
                "PressureGo will use location in the background to access Bluetooth on your phone.\n\n" +
                        "Please allow our access to your location in order to use this app."
            ).show(supportFragmentManager, LOG_TAG)
        } else {
            binding.btnBack.visibility = View.VISIBLE
            binding.btnBack.setOnClickListener {
                finish()
            }
        }

        connectionStateDisposable = BleManagerProvider.getBleManager(this).getConnectionStateObservable()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                adjustRecycler()
            }, {
                Log.e(LOG_TAG, "", it)
            })
    }

    override fun onResume() {
        super.onResume()
        startScan()
    }

    override fun onPause() {
        super.onPause()
        stopScan()
    }

    override fun onDestroy() {
        super.onDestroy()
        connectionStateDisposable?.dispose()
        connectDisposable?.dispose()
    }

    private fun initRecyclerView() {
        with(binding) {
            rvBleScanned.adapter = scanRecyclerAdapter
            rvBleConnected.adapter = connectedRecyclerAdapter
        }
    }

    private fun startScan() {
        with(binding) {
            scanDisposable =
                AppInfoUtil.getSdkVersionSingle()
                    .flatMapCompletable { sdkVersion ->
                        if(sdkVersion >= Build.VERSION_CODES.S) {
                            requestPermissionCompletable(arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT))
                        } else {
                            requestPermissionCompletable(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION))
                        }
                    }
                    .andThen(
                        BleManagerProvider.getBleManager(this@BluetoothScanActivity)
                            .scanObservable(BleScanSettings.Builder().build(), RxPDMSDevice.getScanFilter())
                            .take(5000, TimeUnit.MILLISECONDS)
                            .subscribeOn(Schedulers.io())
                    )
                    .map { it.mapNotNull { it as? RxPDMSDevice } }
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnSubscribe {
                        progressBleScan.visibility = View.VISIBLE
                    }
                    .doFinally {
                        scanDisposable = null
                        progressBleScan.visibility = View.GONE
                    }
                    .subscribe({
                        devices = it
                        adjustRecycler()
                    }, {
                        Log.e(LOG_TAG, "", it)
                    })
        }
    }

    private fun stopScan() {
        scanDisposable?.dispose()
    }

    private fun adjustRecycler() {
        with(binding) {
            BleManagerProvider.getBleManager(this@BluetoothScanActivity)
                .getConnectedDevices()
                .mapNotNull { it as? RxPDMSDevice }
                .count()
                .also {
                    clBleConnected.visibility =
                        if (it > 0) View.VISIBLE else View.GONE
                }
            rvBleConnected.adapter?.notifyDataSetChanged()
            rvBleScanned.adapter?.notifyDataSetChanged()
        }
    }

    override fun onConnectedDeviceSelected(device: RxPDMSDevice) {
        if (connectDisposable == null) {
            Intent(this, NewMainActivity::class.java).apply {
                flags =
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra(NewMainActivity.KEY_MAC_ADDRESS, device.deviceId)
            }.also {
                startActivity(it)
            }
        }
    }

    override fun onConnectedMoreSelected(device: RxPDMSDevice) {
        DeviceInfoDialog().also {
            it.device = device
        }.apply {
            show(supportFragmentManager, DeviceInfoDialog::class.java.simpleName)
        }
    }

    private fun getScannedDevices() = devices.filter { !it.connected }

    override fun getScanItemCount(): Int {
        return getScannedDevices().size
    }

    override fun getScanDevice(position: Int): RxPDMSDevice {
        return getScannedDevices()[position]
    }

    override fun onDeviceSelected(device: RxPDMSDevice) {
        connectDisposable = device.bondCompletable()
            .andThen(device.connectCompletable())
            .andThen(device.checkUpdateExist().timeout(5, TimeUnit.SECONDS))
            .doFinally {
                connectDisposable = null
            }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                adjustRecycler()
                showUpdateCheckDialog(device, it)
            }, {
                Log.e(LOG_TAG, "", it)
            }, {
                adjustRecycler()
            })
    }

    private fun showUpdateCheckDialog(device: RxPDMSDevice, remoteFirmware: Pair<String, String>) {
        AlertDialog.Builder(this)
            .setTitle("Update Firmware exist")
            .setMessage("Do you want to update to version ${remoteFirmware.first}?")
            .setNegativeButton("Cancel") { _, _ -> }
            .setPositiveButton("OK") { _, _ ->
                FirmwareUpdateDialog().also {
                    it.device = device
                    it.firmwareRemote = remoteFirmware.second
                }.apply {
                    show(supportFragmentManager, FirmwareUpdateDialog::class.java.simpleName)
                }
            }
            .create().show()
    }

    companion object {
        private val LOG_TAG = BluetoothScanActivity::class.java.simpleName

        const val FIRST_KEY = "FIRST_KEY"
    }
}