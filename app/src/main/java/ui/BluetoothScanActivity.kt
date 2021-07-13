package ui

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import androidx.recyclerview.widget.RecyclerView
import com.algorigo.algorigoble.BleManager
import com.algorigo.library.rx.permission.PermissionAppCompatActivity
import com.algorigo.pressurego.RxPDMSDevice
import com.algorigo.pressuregoapp.R
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.schedulers.Schedulers

class BluetoothScanActivity : PermissionAppCompatActivity(),
    ConnectedRecyclerAdapter.ConnectedRecyclerDelegate,
    ScanRecyclerAdapter.ScanRecyclerDelegate {

    private var connectionStateDisposable: Disposable? = null
    private var scanDisposable: Disposable? = null
    private var devices = listOf<RxPDMSDevice>()

    private lateinit var bluetoothConnectedRecycler: RecyclerView
    private val connectedRecyclerAdapter = ConnectedRecyclerAdapter(this)
    private lateinit var scanProgressBar: ProgressBar
    private lateinit var bluetoothScanScannedRecycler: RecyclerView
    private var scanRecyclerAdapter = ScanRecyclerAdapter(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bluetooth_scan)

        bluetoothConnectedRecycler = findViewById(R.id.bluetooth_scan_connected_recycler)
        bluetoothConnectedRecycler.adapter = connectedRecyclerAdapter
        scanProgressBar = findViewById(R.id.bluetooth_scan_scan_progress)
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
        scanDisposable = requestPermissionCompletable(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION))
            .andThen(BleManager.getInstance().scanObservable(5000).subscribeOn(Schedulers.io()))
            .map { it.mapNotNull { it as? RxPDMSDevice } }
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe {
                scanProgressBar.visibility = View.VISIBLE
            }
            .doFinally {
                scanDisposable = null
                scanProgressBar.visibility = View.GONE
            }
            .subscribe({
                devices = it
                adjustRecycler()
            }, {
                Log.e(LOG_TAG, "", it)
            })
    }

    private fun stopScan() {
        scanDisposable?.dispose()
    }

    private fun adjustRecycler() {
        bluetoothConnectedRecycler.adapter?.notifyDataSetChanged()
        scanRecyclerAdapter.notifyDataSetChanged()
    }

    private fun getConnectedDevices() = BleManager.getInstance().getConnectedDevices().mapNotNull { it as? RxPDMSDevice }

    override fun getConnectedItemCount(): Int {
        return getConnectedDevices().count()
    }

    override fun getConnectedDevice(position: Int): RxPDMSDevice {
        return getConnectedDevices()[position]
    }

    override fun onConnectedDeviceSelected(device: RxPDMSDevice) {
        Intent(this, NewMainActivity::class.java).apply {
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