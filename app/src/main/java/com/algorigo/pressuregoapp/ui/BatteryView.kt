package com.algorigo.pressuregoapp.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import com.algorigo.pressuregoapp.R
import kotlin.math.roundToInt


class BatteryView(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : LinearLayout(context, attrs, defStyleAttr) {

    constructor(context: Context) : this(context, null, 0)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    var batteryPercent = 0
        set(value) {
            field = value
            percentTextView.text = "$value"
        }
    var charging = true
        set(value) {
            field = value
            chargingView.visibility = if (charging) View.VISIBLE else View.GONE
            textLayout.visibility = if (!charging) View.VISIBLE else View.GONE
        }

    private val circlePaint = Paint().apply {
        style = Paint.Style.STROKE
        color = Color.parseColor("#434345")
        this.strokeWidth = 3f
    }
    private val arcPaint = Paint().apply {
        style = Paint.Style.STROKE
        color = Color.parseColor("#FCFCFC")
        this.strokeWidth = 3f
    }

    private val chargingView: ImageView
    private val textLayout: LinearLayout
    private val percentTextView: TextView
    private val unitTextView: TextView

    init {
        gravity = Gravity.CENTER
        orientation = HORIZONTAL
        setWillNotDraw(false)

        chargingView = ImageView(context).apply {
            setImageDrawable(ResourcesCompat.getDrawable(resources, R.drawable.vector_146, null))
        }.also {
            addView(it, 0)
        }

        textLayout = LinearLayout(context).apply {
            gravity = Gravity.BOTTOM
            percentTextView = TextView(context).apply {
                text = "$batteryPercent"
            }.also {
                addView(it)
            }
            unitTextView = TextView(context).apply {
                text = "%"
            }.also {
                addView(it)
            }
        }.also {
            addView(it, 0)
        }

        chargingView.visibility = if (charging) View.VISIBLE else View.GONE
        textLayout.visibility = if (!charging) View.VISIBLE else View.GONE
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        chargingView.layoutParams = chargingView.layoutParams.let {
            it as LayoutParams
        }.also {
            it.width = (width/3f).roundToInt()
            it.height = (height*2/3f).roundToInt()
        }

        percentTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX, height / 3f)
        unitTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX, height * 0.2f)

        circlePaint.strokeWidth = height / 12f
        arcPaint.strokeWidth = height / 12f
    }

    override fun onDraw(canvas: Canvas?) {

        super.onDraw(canvas)
        drawBatteryProgress(canvas)
    }

    private fun drawBatteryProgress(canvas: Canvas?) {
        canvas?.drawCircle(width/2f, height/2f, width*11/24f, circlePaint)
        canvas?.drawArc(width/24f, height/24f, width*23/24f, height.toFloat()*23/24f, -90f, batteryPercent * 360 / 100f, false, arcPaint)
    }
}