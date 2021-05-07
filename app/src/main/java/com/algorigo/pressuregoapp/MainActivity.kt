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

        test()

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

    fun test() {
        Log.e("!!!", "1111")
        CoroutineScope(IO).async {
            var value = test1()
            Log.e("!!!", "3333:$value")
        }
        Log.e("!!!", "2222")
    }

    suspend fun test1(): String {
        return withContext(IO) {
            Log.e("!!!", "4444")
            Thread.sleep(5000)
            Log.e("!!!", "5555")
            return@withContext "123"
        }
    }
}