package ui

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.transition.AutoTransition
import android.transition.ChangeBounds
import android.transition.TransitionManager
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import com.algorigo.algorigoble.BleManager
import com.algorigo.pressurego.RxPDMSDevice
import com.algorigo.pressuregoapp.R
import com.algorigo.pressuregoapp.databinding.ActivityNewMainBinding
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.Disposable

class NewMainActivity : AppCompatActivity(), MyDevicesDialog.Callback {

    private lateinit var binding: ActivityNewMainBinding
    private var pdmsDevice: RxPDMSDevice? = null
    private var pdmsDisposable: Disposable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNewMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.btnPgS03S04.isSelected = true
        initView()

        intent.getStringExtra(MAC_ADDRESS_KEY)?.let {
            initDevice(it)
        }
    }

    override fun onResume() {
        super.onResume()
        if(pdmsDisposable == null) {
            pdmsDisposable = BleManager.getInstance().getConnectedDevices()
                .mapNotNull { it as? RxPDMSDevice }
                .getOrNull(0)?.sendDataOn()
                ?.doFinally {
                    pdmsDisposable = null
                }
                ?.observeOn(AndroidSchedulers.mainThread())
                ?.subscribe({
                    with(binding) {
                        if(it[0] != 0) {
                            ivSensorPgS03S04LeftTop.setImageResource(R.drawable.sensor_s0304_on)
                        } else {
                            ivSensorPgS03S04LeftTop.setImageResource(R.drawable.sensor_s0304)
                        }
                        if(it[1] != 0) {
                            ivSensorPgS03S04RightTop.setImageResource(R.drawable.sensor_s0304_on)
                        } else {
                            ivSensorPgS03S04RightTop.setImageResource(R.drawable.sensor_s0304)
                        }
                        if(it[2] != 0) {
                            ivSensorPgS03S04LeftBottom.setImageResource(R.drawable.sensor_s0304_on)
                        } else {
                            ivSensorPgS03S04LeftBottom.setImageResource(R.drawable.sensor_s0304)
                        }
                        if(it[3] != 0) {
                            ivSensorPgS03S04RightBottom.setImageResource(R.drawable.sensor_s0304_on)
                        } else {
                            ivSensorPgS03S04RightBottom.setImageResource(R.drawable.sensor_s0304)
                        }
                        tvSensorPgS03S04LeftTop.text = "${it[0]}"
                        tvSensorPgS03S04RightTop.text = "${it[1]}"
                        tvSensorPgS03S04LeftBottom.text = "${it[2]}"
                        tvSensorPgS03S04RightBottom.text = "${it[3]}"
                    }
                }, {
                    Log.d(TAG, it.toString())
                })

        }
    }

    override fun onPause() {
        pdmsDisposable?.dispose()
        super.onPause()
    }


    private fun initView() {
        with(binding) {
            newMainMyDevices.setOnClickListener {
                MyDevicesDialog().apply {
                    show(supportFragmentManager, MyDevicesDialog::class.java.simpleName)
                }
            }

            btnPgS01S02.setOnClickListener {
                Log.d(TAG, it.isSelected.toString())
                if (!it.isSelected) {
                    it.isSelected = !it.isSelected
                    clCenterPgS01S02.isInvisible = false
                    clCenterPgS03S04.isInvisible = true
                    btnPgS03S04.isSelected = false
                }
            }

            btnPgS03S04.setOnClickListener {
                Log.d(TAG, it.isSelected.toString())
                if (!it.isSelected) {
                    it.isSelected = !it.isSelected
                    clCenterPgS01S02.isInvisible = true
                    clCenterPgS03S04.isInvisible = false
                    btnPgS01S02.isSelected = false
                }
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

            etInterval.addTextChangedListener(object: TextWatcher {
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
                    if(s.toString().isEmpty()) {
                        tilInterval.error = null
                        btnInterval.isEnabled = false
                    } else {
                        s.toString().toIntOrNull()?.let {
                            if(it % 25 != 0) {
                                tilInterval.error = resources.getString(R.string.main_interval_warning)
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

            etAmplification.addTextChangedListener(object: TextWatcher {
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
                    if(s.toString().isEmpty()) {
                        tilAmplification.error = null
                        btnAmplification.isEnabled = false
                    } else {
                        s.toString().toIntOrNull()?.let {
                            if(it > 254) {
                                tilAmplification.error = resources.getString(R.string.main_amplification_warning)
                                btnAmplification.isEnabled = false
                            } else {
                                tilAmplification.error = null
                                btnAmplification.isEnabled = true
                            }
                        } ?: run {
                            tilAmplification.error = resources.getString(R.string.main_amplification_warning)
                            btnAmplification.isEnabled = false
                        }
                    }
                }
            })

            etSensitivity.addTextChangedListener(object: TextWatcher {
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
                    if(s.toString().isEmpty()) {
                        tilSensitivity.error = null
                        btnSensitivity.isEnabled = false
                    } else {
                        s.toString().toIntOrNull()?.let {
                            if(it > 254) {
                                tilSensitivity.error = resources.getString(R.string.main_amplification_warning)
                                btnSensitivity.isEnabled = false
                            } else {
                                tilSensitivity.error = null
                                btnSensitivity.isEnabled = true
                            }
                        } ?: run {
                            tilSensitivity.error = resources.getString(R.string.main_amplification_warning)
                            btnSensitivity.isEnabled = false
                        }
                    }
                }
            })

        btnInterval.setOnClickListener {
            if (!etInterval.text.isNullOrEmpty()) {
                etInterval.text.toString().toIntOrNull()?.let {
                    Log.d(TAG, binding.etInterval.text.toString())
                    pdmsDevice?.setSensingIntervalMillisCompletable(it)
                        ?.observeOn(AndroidSchedulers.mainThread())
                        ?.doOnSuccess {
                            Log.d(TAG, "doOnSuccess = ${it}")
                        }
                        ?.doOnTerminate {
                            Log.d(TAG, "terminated")
                        }
                        ?.subscribe({
                            tvIntervalValue.text = "$it"
                                    Log.d(TAG, "onSuccess = ${it}")
                        }, {
                            Log.d(TAG, it.toString())
                        })
                }
            }
        }

            btnAmplification.setOnClickListener {
                if (!etAmplification.text.isNullOrEmpty()) {
                    etAmplification.text.toString().toIntOrNull()?.let {
                            pdmsDevice?.setAmplificationCompletable(it)
                                ?.observeOn(AndroidSchedulers.mainThread())
                                ?.doOnSuccess {
                                    Log.d(TAG, "doOnSuccess = ${it}")
                                }
                                ?.doOnTerminate {
                                    Log.d(TAG, "terminated")
                                }
                                ?.subscribe({
                                    binding.tvAmplificationValue.text = "$it"
                                    Log.d(TAG, "onSuccess = ${it}")
                                }, {
                                    Log.d(TAG, it.toString())
                                })
                    }
                }
            }

            btnSensitivity.setOnClickListener {
                Log.d(TAG, "${binding.etSensitivity.text.toString()}")
                if (!etSensitivity.text.isNullOrEmpty()) {
                        etSensitivity.text.toString().toIntOrNull()?.let {
                            pdmsDevice?.setSensitivityCompletable(it)
                                ?.observeOn(AndroidSchedulers.mainThread())
                                ?.doOnSuccess {
                                    Log.d(TAG, "doOnSuccess = ${it}")
                                }
                                ?.doOnTerminate {
                                    Log.d(TAG, "terminated")
                                }
                                ?.subscribe({
                                    tvSensitivityValue.text = "$it"
                                    Log.d(TAG, "onSuccess = ${it}")
                                }, {
                                    Log.d(TAG, it.toString())
                                })
                        }
                }
            }
        }

    }

    private fun initDevice(macAddress: String) {
        pdmsDevice = BleManager.getInstance().getDevice(macAddress) as? RxPDMSDevice
        pdmsDevice?.apply {
            binding.clCenterPgS01S02.isInvisible = true
            binding.clCenterPgS03S04.isInvisible = false

            binding.tvIntervalValue.isInvisible = false
            binding.tvAmplificationValue.isInvisible = false
            binding.tvSensitivityValue.isInvisible = false

            binding.tvIntervalValue.text = "${getSensingIntervalMillis()}ms"
            binding.tvAmplificationValue.text = "${getAmplification()}"
            binding.tvSensitivityValue.text = "${getSensitivity()}"

            if(pdmsDisposable == null) {
                pdmsDisposable = sendDataOn()
                    ?.doFinally {
                        pdmsDisposable = null
                    }
                    ?.observeOn(AndroidSchedulers.mainThread())
                    ?.subscribe({
                        with(binding) {
                            if(it[0] != 0) {
                                ivSensorPgS03S04LeftTop.setImageResource(R.drawable.sensor_s0304_on)
                            } else {
                                ivSensorPgS03S04LeftTop.setImageResource(R.drawable.sensor_s0304)
                            }
                            if(it[1] != 0) {
                                ivSensorPgS03S04RightTop.setImageResource(R.drawable.sensor_s0304_on)
                            } else {
                                ivSensorPgS03S04RightTop.setImageResource(R.drawable.sensor_s0304)
                            }
                            if(it[2] != 0) {
                                ivSensorPgS03S04LeftBottom.setImageResource(R.drawable.sensor_s0304_on)
                            } else {
                                ivSensorPgS03S04LeftBottom.setImageResource(R.drawable.sensor_s0304)
                            }
                            if(it[3] != 0) {
                                ivSensorPgS03S04RightBottom.setImageResource(R.drawable.sensor_s0304_on)
                            } else {
                                ivSensorPgS03S04RightBottom.setImageResource(R.drawable.sensor_s0304)
                            }
                            tvSensorPgS03S04LeftTop.text = "${it[0]}"
                            tvSensorPgS03S04RightTop.text = "${it[1]}"
                            tvSensorPgS03S04LeftBottom.text = "${it[2]}"
                            tvSensorPgS03S04RightBottom.text = "${it[3]}"
                        }
                    }, {
                        Log.d(TAG, it.toString())
                    })
            }


            // test Log
            Log.d(TAG, getSensingIntervalMillis().toString())
            Log.d(TAG, getAmplification().toString())
            Log.d(TAG, getSensitivity().toString())
        }
    }

    private fun expandCollapseIntervalView(expand: Boolean) {
        val duration = 200L
        if (expand) {
            binding.ivIntervalArrow.setImageResource(R.drawable.ic_up_arrow)
        } else {
            binding.ivIntervalArrow.setImageResource(R.drawable.ic_down_arrow)
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
            binding.ivAmplificationArrow.setImageResource(R.drawable.ic_up_arrow)
        } else {
            binding.ivAmplificationArrow.setImageResource(R.drawable.ic_down_arrow)
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
            binding.ivSensitivityArrow.setImageResource(R.drawable.ic_up_arrow)
        } else {
            binding.ivSensitivityArrow.setImageResource(R.drawable.ic_down_arrow)
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

    companion object {
        val TAG: String = NewMainActivity::class.java.simpleName

        const val MAC_ADDRESS_KEY = "MAC_ADDRESS_KEY"
    }
}