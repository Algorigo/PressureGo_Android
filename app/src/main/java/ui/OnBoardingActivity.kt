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
import com.algorigo.pressuregoapp.databinding.ActivityOnBoardingBinding
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class OnBoardingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOnBoardingBinding

    inner class ImageViewHolder(val imageView: ImageView) : RecyclerView.ViewHolder(imageView)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnBoardingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        with(binding) {
            onBoardingViewPager.adapter = object : RecyclerView.Adapter<ImageViewHolder>() {

                override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder{
                    return ImageView(parent.context).apply {
                        layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                        scaleType = ImageView.ScaleType.FIT_XY
                    }.let {
                        ImageViewHolder(it)
                    }
                }

                override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
                    when (position) {
                        0 -> {
                            holder.imageView.setImageResource(R.drawable.mask_group)
                        }
                        1 -> holder.imageView.setImageResource(R.drawable.mask_group2)
                        else -> throw IllegalArgumentException("viewPager adapter position should be less than 2 : $position")
                    }
                }

                override fun getItemCount(): Int {
                    return 2
                }
            }

            TabLayoutMediator(onBoardingViewPagerTab, onBoardingViewPager) { _, _ -> }
                .attach()

            onBoardingSkipButton.setOnClickListener {
                moveToNewMain()
            }

            onBoardingPairingButton.setOnClickListener {
                moveToBluetoothScan()
            }
        }
    }

    private fun moveToNewMain() {
        startActivity(Intent(this, NewMainActivity::class.java))
        finish()
    }

    private fun moveToBluetoothScan() {
        Intent(this, BluetoothScanActivity::class.java).apply {
            putExtra(BluetoothScanActivity.FIRST_KEY, true)
        }.also {
            startActivity(it)
        }
    }
}