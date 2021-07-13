package ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.algorigo.pressuregoapp.databinding.ActivityNewMainBinding

class NewMainActivity: AppCompatActivity() {

    private lateinit var binding: ActivityNewMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNewMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    companion object {
        const val MAC_ADDRESS_KEY = "MAC_ADDRESS_KEY"
    }
}