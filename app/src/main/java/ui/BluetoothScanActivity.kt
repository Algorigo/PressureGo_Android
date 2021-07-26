package ui

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
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

    private val connectedRecyclerAdapter: ConnectedRecyclerAdapter by lazy {
        ConnectedRecyclerAdapter(this)
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
        } else {
            binding.btnBack.visibility = View.VISIBLE
            binding.btnBack.setOnClickListener {
                finish()
            }
        }

        connectionStateDisposable = BleManager.getInstance().getConnectionStateObservable()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                adjustRecycler()
            }, {
                Log.e(LOG_TAG, "", it)
            })

        val list = BleManager.getInstance()
            .getConnectedDevices()
            .mapNotNull { it as? RxPDMSDevice }

        Log.d(LOG_TAG, "size == ${list.size}")
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

    private fun initRecyclerView() {
        with(binding) {
            rvBleScanned.adapter = scanRecyclerAdapter
            rvBleConnected.adapter = connectedRecyclerAdapter
        }
    }

    private fun startScan() {
        with(binding) {
            scanDisposable =
                requestPermissionCompletable(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION))
                    .andThen(
                        BleManager.getInstance().scanObservable(5000).subscribeOn(Schedulers.io())
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
            BleManager.getInstance()
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
        Intent(this, NewMainActivity::class.java).apply {
            flags =
                Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra(NewMainActivity.KEY_MAC_ADDRESS, device.macAddress)
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

        const val FIRST_KEY = "FIRST_KEY"
    }
}