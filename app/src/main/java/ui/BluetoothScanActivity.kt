package ui

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.algorigo.algorigoble.BleManager
import com.algorigo.library.rx.permission.PermissionAppCompatActivity
import com.algorigo.pressurego.RxPDMSDevice
import com.algorigo.pressuregoapp.R
import com.algorigo.pressuregoapp.databinding.ActivityBluetoothScanBinding
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.schedulers.Schedulers

class BluetoothScanActivity : PermissionAppCompatActivity(),
    ConnectedRecyclerAdapter.ConnectedRecyclerDelegate,
    ScanRecyclerAdapter.ScanRecyclerDelegate {

    private lateinit var binding: ActivityBluetoothScanBinding

    private var connectionStateDisposable: Disposable? = null
    private var scanDisposable: Disposable? = null
    private var devices = listOf<RxPDMSDevice>()

    private lateinit var connectedDeviceLayout: ConstraintLayout
    private lateinit var bluetoothConnectedRecycler: RecyclerView
    private val connectedRecyclerAdapter = ConnectedRecyclerAdapter(this)
    private lateinit var bluetoothScanScannedRecycler: RecyclerView
    private var scanRecyclerAdapter = ScanRecyclerAdapter(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBluetoothScanBinding.inflate(layoutInflater)
        setContentView(binding.root)

        connectedDeviceLayout = findViewById(R.id.bluetooth_scan_connected_device)
        bluetoothConnectedRecycler = findViewById(R.id.bluetooth_scan_connected_recycler)
        bluetoothConnectedRecycler.adapter = connectedRecyclerAdapter
        bluetoothScanScannedRecycler = findViewById(R.id.bluetooth_scan_scanned_recycler)
        bluetoothScanScannedRecycler.adapter = scanRecyclerAdapter

        connectionStateDisposable = BleManager.getInstance().getConnectionStateObservable()
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
    }

    private fun startScan() {
        with(binding) {
            scanDisposable = requestPermissionCompletable(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION))
                .andThen(BleManager.getInstance().scanObservable(5000).subscribeOn(Schedulers.io()))
                .map { it.mapNotNull { it as? RxPDMSDevice } }
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe {
                    bluetoothScanScanProgress.visibility = View.VISIBLE
                }
                .doFinally {
                    scanDisposable = null
                    bluetoothScanScanProgress.visibility = View.GONE
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
            BleManager.getInstance()
                .getConnectedDevices()
                .mapNotNull { it as? RxPDMSDevice }
                .count()
                .also {
                    bluetoothScanConnectedDevice.visibility = if (it > 0) View.VISIBLE else View.GONE
                }
            bluetoothScanConnectedRecycler.adapter?.notifyDataSetChanged()
            scanRecyclerAdapter.notifyDataSetChanged()
        }
    }

    override fun onConnectedDeviceSelected(device: RxPDMSDevice) {
        Intent(this, NewMainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra(NewMainActivity.MAC_ADDRESS_KEY, device.macAddress)
        }.also {
            startActivity(it)
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
        device.connectCompletable(false)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                adjustRecycler()
            }, {
                Log.e(LOG_TAG, "", it)
            })
    }

    companion object {
        private val LOG_TAG = BluetoothScanActivity::class.java.simpleName
    }
}