package com.cursorbuddy.android.overlay

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.widget.ImageView
import android.widget.LinearLayout

class TutorialControls(context: Context) : LinearLayout(context) {

    interface ControlListener {
        fun onPause()
        fun onResume()
        fun onPrevious()
        fun onNext()
        fun onReplay()
        fun onClose()
        fun onExecute()
    }

    var controlListener: ControlListener? = null
    private var isPaused = false
    private val pauseBtn: ImageView

    private val isDark = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
    private val glassTop = if (isDark) 0x66202838.toInt() else 0x88FFFFFF.toInt()
    private val glassBottom = if (isDark) 0x441A2030.toInt() else 0x55F4F8FF.toInt()
    private val iconTint = if (isDark) 0xFFF2F4F8.toInt() else 0xFF17202A.toInt()
    private val strokeColor = if (isDark) 0x55FFFFFF else 0x66FFFFFF

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER
        setPadding(dp(10), dp(10), dp(10), dp(10))

        background = GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(glassTop, glassBottom)
        ).apply {
            cornerRadius = dp(28).toFloat()
            setStroke(dp(1), strokeColor)
        }
        elevation = dp(10).toFloat()

        addControlButton(android.R.drawable.ic_media_previous) { controlListener?.onPrevious() }
        
        pauseBtn = addControlButton(android.R.drawable.ic_media_pause) {
            if (isPaused) {
                controlListener?.onResume()
                setPaused(false)
            } else {
                controlListener?.onPause()
                setPaused(true)
            }
        }
        
        addControlButton(android.R.drawable.ic_media_next) { controlListener?.onNext() }
        addControlButton(android.R.drawable.ic_menu_rotate) { controlListener?.onReplay() }
        
        // Execute button — accent colored
        val execBtn = ImageView(context).apply {
            setImageResource(android.R.drawable.ic_media_play)
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            val size = dp(40)
            layoutParams = LayoutParams(size, size).apply {
                marginStart = dp(4)
                marginEnd = dp(4)
            }
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(0xFF4FC3F7.toInt())
            }
            setPadding(dp(8), dp(8), dp(8), dp(8))
            setColorFilter(0xFFFFFFFF.toInt())
            setOnClickListener { controlListener?.onExecute() }
        }
        addView(execBtn)
        
        addControlButton(android.R.drawable.ic_menu_close_clear_cancel) { controlListener?.onClose() }
    }

    fun setPaused(paused: Boolean) {
        isPaused = paused
        pauseBtn.setImageResource(
            if (paused) android.R.drawable.ic_media_play
            else android.R.drawable.ic_media_pause
        )
    }

    private fun addControlButton(iconRes: Int, onClick: () -> Unit): ImageView {
        val btn = ImageView(context).apply {
            setImageResource(iconRes)
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            val size = dp(40)
            layoutParams = LayoutParams(size, size).apply {
                marginStart = dp(2)
                marginEnd = dp(2)
            }
            setPadding(dp(8), dp(8), dp(8), dp(8))
            setColorFilter(iconTint)
            setOnClickListener { onClick() }
        }
        addView(btn)
        return btn
    }

    private fun dp(value: Int): Int {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value.toFloat(), context.resources.displayMetrics).toInt()
    }
}
