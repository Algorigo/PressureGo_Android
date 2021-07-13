package ui

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.algorigo.pressuregoapp.R
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class OnBoardingActivity : AppCompatActivity() {

    inner class ImageViewHolder(val imageView: ImageView) : RecyclerView.ViewHolder(imageView)

    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout
    private lateinit var skipButton: Button
    private lateinit var pairingButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_on_boarding)

        viewPager = findViewById(R.id.on_boarding_view_pager)
        tabLayout = findViewById(R.id.on_boarding_view_pager_tab)
        skipButton = findViewById(R.id.on_boarding_skip_button)
        pairingButton = findViewById(R.id.on_boarding_pairing_button)

        viewPager.adapter = object : RecyclerView.Adapter<ImageViewHolder>() {

            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder{
                return ImageView(parent.context).apply {
                    this.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                }.let {
                    ImageViewHolder(it)
                }
            }

            override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
                when (position) {
                    0 -> holder.imageView.setImageResource(R.drawable.mask_group)
                    1 -> holder.imageView.setImageResource(R.drawable.mask_group2)
                    else -> throw IllegalArgumentException("viewPager adapter position should be less than 2 : $position")
                }
            }

            override fun getItemCount(): Int {
                return 2
            }
        }

        TabLayoutMediator(tabLayout, viewPager) { _, _ -> }
            .attach()

        skipButton.setOnClickListener {
            moveToNewMain()
        }

        pairingButton.setOnClickListener {
            moveToBluetoothScan()
        }
    }

    private fun moveToBluetoothScan() {
        startActivity(Intent(this, BluetoothScanActivity::class.java))
        finish()
    }
    private fun moveToNewMain() {
        startActivity(Intent(this, NewMainActivity::class.java))
        finish()
    }
}