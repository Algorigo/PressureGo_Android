package ui

import android.os.Bundle
import android.transition.AutoTransition
import android.transition.ChangeBounds
import android.transition.TransitionManager
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import com.algorigo.algorigoble.BleManager
import com.algorigo.pressurego.RxPDMSDevice
import com.algorigo.pressuregoapp.R
import com.algorigo.pressuregoapp.databinding.ActivityNewMainBinding
import kotlin.math.abs

class NewMainActivity: AppCompatActivity() {

    private lateinit var binding: ActivityNewMainBinding
    private var pdmsDevice: RxPDMSDevice? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNewMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initDevice()
        initView()
    }

    private fun initView() {
        binding.btnPgS01S02.setOnClickListener {
            Log.d(TAG, it.isSelected.toString())
            if(!it.isSelected) {
                it.isSelected = !it.isSelected
                binding.btnPgS03S04.isSelected = false
            }
        }

        binding.btnPgS03S04.setOnClickListener {
            Log.d(TAG, it.isSelected.toString())
            if(!it.isSelected) {
                it.isSelected = !it.isSelected
                binding.btnPgS01S02.isSelected = false
            }
        }
        
        binding.ivIntervalArrow.setOnClickListener {
            expandCollapseIntervalView(it.isActivated.not())
        }

        binding.ivAmplificationArrow.setOnClickListener {
            expandCollapseAmplificationView(it.isActivated.not())
        }

        binding.ivSensitivityArrow.setOnClickListener {
            expandCollapseSensitivityView(it.isActivated.not())
        }

    }

    private fun initDevice() {
        intent?.getStringExtra(MAC_ADDRESS_KEY)?.let {
            pdmsDevice = BleManager.getInstance().getDevice(it) as? RxPDMSDevice
            Log.d(TAG, pdmsDevice?.getDeviceName()!!)
            binding.tvIntervalValue.isInvisible = false
        }

    }

    private fun expandCollapseIntervalView(expand: Boolean) {
        val duration = 200L

        if(expand) {
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
        if(expand) {
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


        if(expand) {
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

    companion object {
        val TAG: String = NewMainActivity::class.java.simpleName

        const val MAC_ADDRESS_KEY = "MAC_ADDRESS_KEY"
    }
}