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
    private lateinit var amplificationGetBtn: Button
    protected lateinit var amplificationEditText: EditText
    private lateinit var amplificationSetBtn: Button
    private lateinit var dataBtn: Button
    protected lateinit var dataTextView: TextView

    final override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pdms_device)

        val macAddress = intent.getStringExtra(MAC_ADDRESS_KEY)
        macAddress?.also {
            initDevice(it)
        }

        deviceNameBtn = findViewById(R.id.deviceNameBtn)
        deviceNameTextView = findViewById(R.id.deviceNameTextView)
        manufactureNameBtn = findViewById(R.id.manufactureNameBtn)
        manufactureNameTextView = findViewById(R.id.manufactureNameTextView)
        hardwareVersionBtn = findViewById(R.id.hardwareVersionBtn)
        hardwareVersionTextView = findViewById(R.id.hardwareTextView)
        firmwareVersionBtn = findViewById(R.id.firmwareVersionBtn)
        firmwareVersionTextView = findViewById(R.id.firmwareTextView)
        amplificationGetBtn = findViewById(R.id.amplificationGetBtn)
        amplificationEditText = findViewById(R.id.amplificationEditText)
        amplificationSetBtn = findViewById(R.id.amplificationSetBtn)
        dataBtn = findViewById(R.id.dataBtn)
        dataTextView = findViewById(R.id.dataTextView)

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
        amplificationGetBtn.setOnClickListener {
            getAmplification()
        }
        amplificationSetBtn.setOnClickListener {
            amplificationEditText.text.toString().toIntOrNull()?.let {
                setAmplification(it)
            }
        }
        dataBtn.setOnClickListener {
            data()
        }
    }

    protected abstract fun initDevice(macAddress: String)

    protected abstract fun getDeviceName()

    protected abstract fun getManufactureName()

    protected abstract fun getHardware()

    protected abstract fun getFirmware()

    protected abstract fun getAmplification()

    protected abstract fun setAmplification(amplification: Int)

    protected abstract fun data()

    companion object {
        const val MAC_ADDRESS_KEY = "MAC_ADDRESS_KEY"
    }
}