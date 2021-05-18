package com.algorigo.pressuregoapp

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

abstract class PDMSDeviceActivity : AppCompatActivity() {

    private lateinit var deviceNameBtn: Button
    protected lateinit var deviceNameTextView: TextView
    private lateinit var manufactureNameBtn: Button
    protected lateinit var manufactureNameTextView: TextView
    private lateinit var hardwareVersionBtn: Button
    protected lateinit var hardwareVersionTextView: TextView
    private lateinit var firmwareVersionBtn: Button
    protected lateinit var firmwareVersionTextView: TextView
    private lateinit var intervalGetBtn: Button
    protected lateinit var intervalEditText: EditText
    private lateinit var intervalSetBtn: Button
    private lateinit var amplificationGetBtn: Button
    protected lateinit var amplificationEditText: EditText
    private lateinit var amplificationSetBtn: Button
    private lateinit var sensitivityGetBtn: Button
    protected lateinit var sensitivityEditText: EditText
    private lateinit var sensitivitySetBtn: Button
    private lateinit var dataBtn: Button
    protected lateinit var dataTextView1: TextView
    protected lateinit var dataTextView2: TextView
    protected lateinit var dataTextView3: TextView
    protected lateinit var dataTextView4: TextView
    private lateinit var batteryBtn: Button
    protected lateinit var batteryTextView: TextView
    private lateinit var lowBatteryBtn: Button
    protected lateinit var lowBatteryTextView: TextView

    final override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pdms_device)

        val macAddress = intent.getStringExtra(MAC_ADDRESS_KEY)
        macAddress?.also {
            initDevice(it)
        }

        deviceNameBtn = findViewById(R.id.device_name_btn)
        deviceNameTextView = findViewById(R.id.device_name_textview)
        manufactureNameBtn = findViewById(R.id.manufacture_name_btn)
        manufactureNameTextView = findViewById(R.id.manufacture_name_textview)
        hardwareVersionBtn = findViewById(R.id.hardware_version_btn)
        hardwareVersionTextView = findViewById(R.id.hardware_textview)
        firmwareVersionBtn = findViewById(R.id.firmware_version_btn)
        firmwareVersionTextView = findViewById(R.id.firmware_textview)
        intervalGetBtn = findViewById(R.id.interval_get_btn)
        intervalEditText = findViewById(R.id.interval_edittext)
        intervalSetBtn = findViewById(R.id.interval_set_btn)
        amplificationGetBtn = findViewById(R.id.amplification_get_btn)
        amplificationEditText = findViewById(R.id.amplification_edittext)
        amplificationSetBtn = findViewById(R.id.amplification_set_btn)
        sensitivityGetBtn = findViewById(R.id.sensitivity_get_btn)
        sensitivityEditText = findViewById(R.id.sensitivity_edittext)
        sensitivitySetBtn = findViewById(R.id.sensitivity_set_btn)
        dataBtn = findViewById(R.id.data_btn)
        dataTextView1 = findViewById(R.id.data_textview_1)
        dataTextView2 = findViewById(R.id.data_textview_2)
        dataTextView3 = findViewById(R.id.data_textview_3)
        dataTextView4 = findViewById(R.id.data_textview_4)
        batteryBtn = findViewById(R.id.battery_btn)
        batteryTextView = findViewById(R.id.battery_textview)
        lowBatteryBtn = findViewById(R.id.low_battery_btn)
        lowBatteryTextView = findViewById(R.id.low_battery_textview)

        deviceNameBtn.setOnClickListener {
            getDeviceName()
        }
        manufactureNameBtn.setOnClickListener {
            getManufactureName()
        }
        hardwareVersionBtn.setOnClickListener {
            getHardware()
        }
        firmwareVersionBtn.setOnClickListener {
            getFirmware()
        }
        intervalGetBtn.setOnClickListener {
            getInterval()
        }
        intervalSetBtn.setOnClickListener {
            intervalEditText.text.toString().toIntOrNull()?.let {
                setInterval(it)
            }
        }
        amplificationGetBtn.setOnClickListener {
            getAmplification()
        }
        amplificationSetBtn.setOnClickListener {
            amplificationEditText.text.toString().toIntOrNull()?.let {
                setAmplification(it)
            }
        }
        sensitivityGetBtn.setOnClickListener {
            getSensitivity()
        }
        sensitivitySetBtn.setOnClickListener {
            sensitivityEditText.text.toString().toIntOrNull()?.let {
                setSensitivity(it)
            }
        }
        dataBtn.setOnClickListener {
            data()
        }
        batteryBtn.setOnClickListener {
            battery()
        }
        lowBatteryBtn.setOnClickListener {
            lowBattery()
        }
    }

    protected abstract fun initDevice(macAddress: String)

    protected abstract fun getDeviceName()

    protected abstract fun getManufactureName()

    protected abstract fun getHardware()

    protected abstract fun getFirmware()

    protected abstract fun getInterval()

    protected abstract fun setInterval(interval: Int)

    protected abstract fun getAmplification()

    protected abstract fun setAmplification(amplification: Int)

    protected abstract fun getSensitivity()

    protected abstract fun setSensitivity(sensitivity: Int)

    protected abstract fun data()

    protected abstract fun battery()

    protected abstract fun lowBattery()

    protected fun setData(intArray: IntArray) {
        dataTextView1.text = intArray[3].toString()
        dataTextView2.text = intArray[2].toString()
        dataTextView3.text = intArray[0].toString()
        dataTextView4.text = intArray[1].toString()
    }

    companion object {
        const val MAC_ADDRESS_KEY = "MAC_ADDRESS_KEY"
    }
}