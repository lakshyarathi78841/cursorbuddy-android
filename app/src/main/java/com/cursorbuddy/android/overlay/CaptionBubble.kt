package com.cursorbuddy.android.overlay

import android.content.Context
import android.content.res.Configuration
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView

class CaptionBubble(context: Context) : FrameLayout(context) {

    private val captionText: TextView
    private val stepIndicator: TextView

    private val isDark = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

    init {
        val surfaceColor = if (isDark) 0xF01E1E1E.toInt() else 0xF5FFFFFF.toInt()
        val textColor = if (isDark) 0xFFE0E0E0.toInt() else 0xFF222222.toInt()
        val strokeColor = if (isDark) 0x33FFFFFF else 0x33000000
        val accentColor = 0xFF4FC3F7.toInt()

        val bubbleContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(12), dp(16), dp(12))
            background = GradientDrawable().apply {
                setColor(surfaceColor)
                cornerRadius = dp(16).toFloat()
                setStroke(dp(1), strokeColor)
            }
            elevation = dp(8).toFloat()
        }

        stepIndicator = TextView(context).apply {
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            setTextColor(accentColor)
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setPadding(0, 0, 0, dp(4))
        }
        bubbleContainer.addView(stepIndicator)

        captionText = TextView(context).apply {
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            setTextColor(textColor)
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
            maxWidth = dp(280)
        }
        bubbleContainer.addView(captionText)

        addView(bubbleContainer, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, Gravity.CENTER))
    }

    fun setCaption(text: String, stepNumber: Int, totalSteps: Int) {
        captionText.text = text
        stepIndicator.text = "STEP $stepNumber OF $totalSteps"
        stepIndicator.visibility = if (totalSteps > 1) View.VISIBLE else View.GONE
    }

    private fun dp(value: Int): Int {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value.toFloat(), context.resources.displayMetrics).toInt()
    }
}
