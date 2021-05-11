package com.algorigo.pressuregoapp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.IO

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Example of a call to a native method
        findViewById<Button>(R.id.basic_ble_btn).setOnClickListener {
            startActivity(Intent(this, BasicActivity::class.java))
        }
        findViewById<Button>(R.id.rx_ble_btn).setOnClickListener {
            startActivity(Intent(this, RxActivity::class.java))
        }
    }

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
}