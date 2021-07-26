package ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.algorigo.pressurego.RxPDMSDevice
import com.algorigo.pressuregoapp.DfuService
import com.algorigo.pressuregoapp.R
import com.algorigo.pressuregoapp.Utility
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.Disposable
import java.io.File

class FirmwareUpdateDialog : BottomSheetDialogFragment() {

    lateinit var device: RxPDMSDevice
//    var device: RxPDMSDevice? = null
    var firmwareRemote: String? = null
    var firmwareLocal: Uri? = null

    private var updateDisposable: Disposable? = null

    private lateinit var locationView: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var startButton: Button

    private val request = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { activityResult ->
        if (activityResult.resultCode == Activity.RESULT_OK) {
            firmwareRemote = null
            firmwareLocal = activityResult.data?.data
            locationView.text = firmwareLocal?.toString()
            startButton.isEnabled = true
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.CustomBottomSheetDialogTheme)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_firmware_update, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        locationView = view.findViewById(R.id.firmware_update_location_view)
        progressBar = view.findViewById(R.id.firmware_update_progress_bar)
        startButton = view.findViewById(R.id.firmware_update_start_button)

        if (firmwareRemote != null) {
            locationView.text = firmwareRemote
        } else {
            startButton.isEnabled = false
        }
        locationView.setOnClickListener {
            retrieveLocalFirmwareUri()
        }
        startButton.setOnClickListener {
            if (updateDisposable == null) {
                startUpdate()
            } else {
                cancelUpdate()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        cancelUpdate()
    }

    private fun retrieveLocalFirmwareUri() {
        Intent(Intent.ACTION_GET_CONTENT).apply {
            setType("application/zip")
        }.also {
            request.launch(it)
        }
    }

    private fun startUpdate() {
        val updateObservable = if (firmwareRemote != null) {
            getUpdateRemote(firmwareRemote!!)
        } else if (firmwareLocal != null) {
            getUpdateLocal(firmwareLocal!!)
        } else {
            Observable.error(IllegalStateException("firmwareRemote and firmwareLocal is null"))
        }
        updateDisposable = updateObservable
            .doFinally {
                updateDisposable = null
            }
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe {
                isCancelable = false
                progressBar.progress = 0
                progressBar.visibility = View.VISIBLE
                startButton.text = "Cancel update"
            }
            .doFinally {
                isCancelable = true
                progressBar.visibility = View.GONE
                startButton.text = "Start update"
            }
            .subscribe({
                Log.i(LOG_TAG, "update $it%")
                progressBar.progress = it
            }, {
                Log.e(LOG_TAG, "", it)
            }, {
                Log.i(LOG_TAG, "update complete")
                onUpdateComplete()
            })
    }

    private fun getUpdateRemote(path: String): Observable<Int> {
        return context?.let {
            val file = File(ContextCompat.getDataDir(it), "temp.zip")
            Utility.downloadObservable(path, file)
                .map { it / 2 }
                .concatWith(device.update(it, DfuService::class.java, file.absolutePath)
                    .map { it / 2 + 50 })
                .doFinally {
                    file.delete()
                }
        } ?: Observable.error(IllegalStateException())
    }

    private fun getUpdateLocal(uri: Uri): Observable<Int> {
        return context?.let {
            device.update(it, DfuService::class.java, uri)
        } ?: Observable.error(IllegalStateException())
    }

    private fun cancelUpdate() {
        updateDisposable?.dispose()
    }

    private fun onUpdateComplete() {
        activity?.let {
            AlertDialog.Builder(it)
                .setTitle("Update Firmware")
                .setMessage("Firmware is updated successfully.")
                .setCancelable(false)
                .setPositiveButton("OK") { _, _ ->
                    it.finishAffinity()
                }
                .create().show()
        }
    }

    companion object {
        private val LOG_TAG = FirmwareUpdateDialog::class.java.simpleName
    }
}