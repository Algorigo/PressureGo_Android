package ui

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.transition.AutoTransition
import android.transition.ChangeBounds
import android.transition.TransitionManager
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import com.algorigo.algorigoble.BleManager
import com.algorigo.library.rx.Rx2ServiceBindingFactory
import com.algorigo.pressurego.RxPDMSDevice
import com.algorigo.pressuregoapp.R
import com.algorigo.pressuregoapp.databinding.ActivityNewMainBinding
import data.BleDevicePreferencesHelper
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.schedulers.Schedulers
import service.CSVRecordService
import util.FileUtil
import java.io.File

class NewMainActivity : AppCompatActivity(), MyDevicesDialog.Callback {

    private lateinit var binding: ActivityNewMainBinding

    private val bleDevicePreferencesHelper: BleDevicePreferencesHelper by lazy {
        BleDevicePreferencesHelper(this@NewMainActivity)
    }

    private var pdmsDevice: RxPDMSDevice? = null
    private var pdmsDisposable: Disposable? = null
    private var csvService: CSVRecordService? = null
    private var serviceDisposable: Disposable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNewMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initView()
        intent.getStringExtra(KEY_MAC_ADDRESS)?.let {
            initDevice(it)
            bleDevicePreferencesHelper.latestShowDeviceMacAddress = it
            Log.d(TAG, it)
        } ?: run {
            onBtnS0102Click()
        }
        bleDevicePreferencesHelper.csvFileName?.let {
            AlertDialog.newInstance(title = "CSV Export",
                content = "저장하시던 파일이 있습니다.\n저장하시겠습니까?",
                yesCallback = {
                    bleDevicePreferencesHelper.csvFileName = null
                },
                noCallback = {
                    FileUtil.deleteFileCompletable(File(it))
                        .subscribeOn(Schedulers.io())
                        .subscribe({

                        }, {

                        })
                    bleDevicePreferencesHelper.csvFileName = null
                },
            ).show(supportFragmentManager, TAG)
        }
    }

    override fun onResume() {
        super.onResume()
        subscribeDevice(pdmsDevice)
    }

    override fun onPause() {
        pdmsDisposable?.dispose()
        super.onPause()
    }


    private fun initView() {
        with(binding) {
            btnInterval.isEnabled = false
            btnAmplification.isEnabled = false
            btnSensitivity.isEnabled = false

            tvMyDevices.setOnClickListener {
                MyDevicesDialog().apply {
                    show(supportFragmentManager, MyDevicesDialog::class.java.simpleName)
                }
            }

            btnPgS01S02.setOnClickListener {
                onBtnS0102Click()
            }

            btnPgS03S04.setOnClickListener {
                onBtnS0304Click()
            }

            ivIntervalArrow.setOnClickListener {
                expandCollapseIntervalView(it.isActivated.not())
            }

            ivAmplificationArrow.setOnClickListener {
                expandCollapseAmplificationView(it.isActivated.not())
            }

            ivSensitivityArrow.setOnClickListener {
                expandCollapseSensitivityView(it.isActivated.not())
            }

            etInterval.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {

                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                }

                override fun afterTextChanged(s: Editable?) {
                    if (s.toString().isEmpty()) {
                        tilInterval.error = null
                        btnInterval.isEnabled = false
                    } else {
                        s.toString().toIntOrNull()?.let {
                            if (it % 25 != 0) {
                                tilInterval.error =
                                    resources.getString(R.string.main_interval_warning)
                                btnInterval.isEnabled = false
                            } else {
                                tilInterval.error = null
                                btnInterval.isEnabled = true
                            }
                        } ?: run {
                            tilInterval.error = resources.getString(R.string.main_interval_warning)
                            btnInterval.isEnabled = false
                        }
                    }
                }
            })

            etAmplification.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {

                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                }

                override fun afterTextChanged(s: Editable?) {
                    if (s.toString().isEmpty()) {
                        tilAmplification.error = null
                        btnAmplification.isEnabled = false
                    } else {
                        s.toString().toIntOrNull()?.let {
                            if (it > 254) {
                                tilAmplification.error =
                                    resources.getString(R.string.main_amplification_warning)
                                btnAmplification.isEnabled = false
                            } else {
                                tilAmplification.error = null
                                btnAmplification.isEnabled = true
                            }
                        } ?: run {
                            tilAmplification.error =
                                resources.getString(R.string.main_amplification_warning)
                            btnAmplification.isEnabled = false
                        }
                    }
                }
            })

            etSensitivity.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {

                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                }

                override fun afterTextChanged(s: Editable?) {
                    if (s.toString().isEmpty()) {
                        tilSensitivity.error = null
                        btnSensitivity.isEnabled = false
                    } else {
                        s.toString().toIntOrNull()?.let {
                            if (it > 254) {
                                tilSensitivity.error =
                                    resources.getString(R.string.main_amplification_warning)
                                btnSensitivity.isEnabled = false
                            } else {
                                tilSensitivity.error = null
                                btnSensitivity.isEnabled = true
                            }
                        } ?: run {
                            tilSensitivity.error =
                                resources.getString(R.string.main_amplification_warning)
                            btnSensitivity.isEnabled = false
                        }
                    }
                }
            })

            btnInterval.setOnClickListener {
                if (!etInterval.text.isNullOrEmpty()) {
                    etInterval.text.toString().toIntOrNull()?.let {
                        pdmsDevice?.setSensingIntervalMillisSingle(it)
                            ?.observeOn(AndroidSchedulers.mainThread())
                            ?.doOnSuccess {
                                tvIntervalValue.text = "${it}ms"
                            }
                            ?.subscribe({
                            }, {
                                Log.d(TAG, it.toString())
                            })
                    }
                }
            }

            btnAmplification.setOnClickListener {
                if (!etAmplification.text.isNullOrEmpty()) {
                    etAmplification.text.toString().toIntOrNull()?.let {
                        pdmsDevice?.setAmplificationSingle(it)
                            ?.observeOn(AndroidSchedulers.mainThread())
                            ?.doOnSuccess {
                                tvAmplificationValue.text = "$it"
                            }
                            ?.subscribe({
                            }, {
                                Log.d(TAG, it.toString())
                            })
                    }
                }
            }

            btnSensitivity.setOnClickListener {
                if (!etSensitivity.text.isNullOrEmpty()) {
                    etSensitivity.text.toString().toIntOrNull()?.let {
                        pdmsDevice?.setSensitivitySingle(it)
                            ?.observeOn(AndroidSchedulers.mainThread())
                            ?.doOnSuccess {
                                tvSensitivityValue.text = "$it"
                            }
                            ?.subscribe({
                            }, {
                                Log.d(TAG, it.toString())
                            })
                    }
                }
            }
            clCsvRecord.setOnClickListener {
                if (serviceDisposable == null) {
                    Log.d(TAG, "it == null")
                    ConfirmDialog.newInstance(
                        title = "CSV Record",
                        content = "Shall we start recording\nsensing data to export CSV?",
                        callback = {
                            startService(Intent(this@NewMainActivity, CSVRecordService::class.java))
                            serviceDisposable =
                                Rx2ServiceBindingFactory.bind<CSVRecordService.LocalBinder>(
                                    this@NewMainActivity,
                                    Intent(this@NewMainActivity, CSVRecordService::class.java)
                                )
                                    .doOnNext {
                                        csvService = it.getService()
                                    }
                                    .doFinally {
                                        serviceDisposable = null
                                    }
                                    .map { it.getService() }
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe({
                                        ivRecord.setImageResource(R.drawable.csv_record_stop)
                                        tvRecord.text = "Stop"
                                        tvRecord.setTextColor(
                                            ContextCompat.getColor(
                                                this@NewMainActivity,
                                                R.color.orangey_red
                                            )
                                        )
                                    }, {

                                    })
                        }).show(supportFragmentManager, TAG)
                } else {
                    AlertDialog.newInstance(
                        title = "CSV Export",
                        content = "Do you want to download CSV\nrecorded so far?",
                        yesCallback = {
                            csvService?.stopSelf()
                            serviceDisposable?.dispose()
                            ivRecord.setImageResource(R.drawable.csv_record_on)
                            tvRecord.text = "CSV Record"
                            tvRecord.setTextColor(
                                ContextCompat.getColor(
                                    this@NewMainActivity,
                                    R.color.soft_green
                                )
                            )
                        },
                        noCallback = {
                            csvService?.noRecordStopSelf()
                            serviceDisposable?.dispose()
                            ivRecord.setImageResource(R.drawable.csv_record_on)
                            tvRecord.text = "CSV Record"
                            tvRecord.setTextColor(
                                ContextCompat.getColor(
                                    this@NewMainActivity,
                                    R.color.soft_green
                                )
                            )
                        }
                    ).show(supportFragmentManager, TAG)
                }
            }
        }
    }

    private fun initDevice(macAddress: String) {
        onBtnS0102Click()
        pdmsDevice = BleManager.getInstance().getDevice(macAddress) as? RxPDMSDevice
        pdmsDevice?.apply {

            binding.tvIntervalValue.isInvisible = false
            binding.tvAmplificationValue.isInvisible = false
            binding.tvSensitivityValue.isInvisible = false

            binding.tvIntervalValue.text = "${getSensingIntervalMillis()}ms"
            binding.tvAmplificationValue.text = "${getAmplification()}"
            binding.tvSensitivityValue.text = "${getSensitivity()}"

            subscribeDevice(this)
        }
    }

    private fun subscribeDevice(device: RxPDMSDevice?) {
        if (pdmsDisposable == null) {
            pdmsDisposable = device?.sendDataOn()
                ?.doFinally {
                    pdmsDisposable = null
                }
                ?.observeOn(AndroidSchedulers.mainThread())
                ?.subscribe({
                    with(binding) {
                        if (it[0] != 0) {
                            ivSensorPgS03S04LeftTop.setImageResource(R.drawable.sensor_s0304_on)
                        } else {
                            ivSensorPgS03S04LeftTop.setImageResource(R.drawable.sensor_s0304)
                        }
                        if (it[1] != 0) {
                            ivSensorPgS03S04RightTop.setImageResource(R.drawable.sensor_s0304_on)
                            ivSensorPgS01S02.setImageResource(R.drawable.sensor_s0102_on)
                        } else {
                            ivSensorPgS03S04RightTop.setImageResource(R.drawable.sensor_s0304)
                            ivSensorPgS01S02.setImageResource(R.drawable.sensor_s0102)
                        }
                        if (it[2] != 0) {
                            ivSensorPgS03S04RightBottom.setImageResource(R.drawable.sensor_s0304_on)
                        } else {
                            ivSensorPgS03S04RightBottom.setImageResource(R.drawable.sensor_s0304)
                        }
                        if (it[3] != 0) {
                            ivSensorPgS03S04LeftBottom.setImageResource(R.drawable.sensor_s0304_on)
                        } else {
                            ivSensorPgS03S04LeftBottom.setImageResource(R.drawable.sensor_s0304)
                        }
                        tvSensorPgS0102.text = "${it[1]}"
                        tvSensorPgS03S04LeftTop.text = "${it[0]}"
                        tvSensorPgS03S04RightTop.text = "${it[1]}"
                        tvSensorPgS03S04RightBottom.text = "${it[2]}"
                        tvSensorPgS03S04LeftBottom.text = "${it[3]}"
                    }
                }, {
                    Log.d(TAG, it.toString())
                })
        }
    }

    private fun expandCollapseIntervalView(expand: Boolean) {
        val duration = 200L
        if (expand) {
            binding.ivIntervalArrow.animate().setDuration(200).rotation(180f)
        } else {
            binding.ivIntervalArrow.animate().setDuration(duration).rotation(0f)
        }
        TransitionManager.beginDelayedTransition(binding.clInterval, AutoTransition().apply {
            addTransition(ChangeBounds())
            this.duration = duration
        })
        binding.ivIntervalArrow.isActivated = expand
        binding.group1.isVisible = expand
    }

    private fun expandCollapseAmplificationView(expand: Boolean) {
        val duration = 200L
        if (expand) {
            binding.ivAmplificationArrow.animate().setDuration(200).rotation(180f)
        } else {
            binding.ivAmplificationArrow.animate().setDuration(duration).rotation(0f)
        }
        TransitionManager.beginDelayedTransition(binding.clAmplification, AutoTransition().apply {
            addTransition(ChangeBounds())
            this.duration = duration
        })
        binding.ivAmplificationArrow.isActivated = expand
        binding.group2.isVisible = expand
    }

    private fun expandCollapseSensitivityView(expand: Boolean) {
        val duration = 200L
        if (expand) {
            binding.ivSensitivityArrow.animate().setDuration(200).rotation(180f)
        } else {
            binding.ivSensitivityArrow.animate().setDuration(duration).rotation(0f)
        }
        TransitionManager.beginDelayedTransition(binding.clSensitivity, AutoTransition().apply {
            addTransition(ChangeBounds())
            this.duration = duration
        })
        binding.ivSensitivityArrow.isActivated = expand
        binding.group3.isVisible = expand
    }

    override fun onDeviceSelected(macAddress: String) {
        initDevice(macAddress)
    }

    private fun onBtnS0102Click() {
        with(binding) {
            if (!btnPgS01S02.isSelected) {
                btnPgS01S02.isSelected = !btnPgS01S02.isSelected
                clCenterPgS01S02.isInvisible = false
                clCenterPgS03S04.isInvisible = true
                btnPgS03S04.isSelected = false
            }
        }
    }

    private fun onBtnS0304Click() {
        with(binding) {
            if (!btnPgS03S04.isSelected) {
                btnPgS03S04.isSelected = !btnPgS03S04.isSelected
                clCenterPgS01S02.isInvisible = true
                clCenterPgS03S04.isInvisible = false
                btnPgS01S02.isSelected = false
            }
        }
    }

    companion object {
        val TAG: String = NewMainActivity::class.java.simpleName

        const val KEY_MAC_ADDRESS = "MAC_ADDRESS_KEY"
    }
}