package ui

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_on_boarding)

        viewPager = findViewById(R.id.on_boarding_view_pager)
        tabLayout = findViewById(R.id.on_boarding_view_pager_tab)

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
    }
}