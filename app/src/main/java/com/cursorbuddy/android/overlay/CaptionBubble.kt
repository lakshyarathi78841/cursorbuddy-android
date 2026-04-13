package com.cursorbuddy.android.overlay

import android.content.Context
import android.content.res.Configuration
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.cursorbuddy.android.R

class CaptionBubble(context: Context) : FrameLayout(context) {

    private val captionText: TextView
    private val stepIndicator: TextView

    private val isDark = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

    init {
        val glassTop = if (isDark) 0xF0243243.toInt() else 0xFAFFFFFF.toInt()
        val glassBottom = if (isDark) 0xDB1A2432.toInt() else 0xEDF5FAFF.toInt()
        val textColor = if (isDark) 0xFFF2F4F8.toInt() else 0xFF17202A.toInt()
        val strokeColor = if (isDark) 0x66FFFFFF else 0x88E1ECF5.toInt()
        val indicatorTop = if (isDark) 0xD9334B5D.toInt() else 0xF5FFFFFF.toInt()
        val indicatorBottom = if (isDark) 0xBF243445.toInt() else 0xDDE7F5FF.toInt()
        val accentColor = 0xFF4FC3F7.toInt()

        val bubbleContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(14), dp(18), dp(14))
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(glassTop, glassBottom)
            ).apply {
                cornerRadius = dp(20).toFloat()
                setStroke(dp(1), strokeColor)
            }
            elevation = dp(10).toFloat()
            alpha = 0.99f
        }

        val headerRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val brandIcon = ImageView(context).apply {
            setImageResource(R.drawable.cursorbuddy_app_icon)
            scaleType = ImageView.ScaleType.CENTER_CROP
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(indicatorTop, indicatorBottom)
            ).apply {
                shape = GradientDrawable.OVAL
                setStroke(dp(1), strokeColor)
            }
            clipToOutline = true
            setPadding(dp(2), dp(2), dp(2), dp(2))
        }
        headerRow.addView(brandIcon, LinearLayout.LayoutParams(dp(24), dp(24)).apply {
            marginEnd = dp(8)
        })

        stepIndicator = TextView(context).apply {
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            setTextColor(accentColor)
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(indicatorTop, indicatorBottom)
            ).apply {
                cornerRadius = dp(12).toFloat()
                setStroke(dp(1), strokeColor)
            }
            setPadding(dp(10), dp(5), dp(10), dp(5))
        }
        headerRow.addView(stepIndicator)
        bubbleContainer.addView(headerRow, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            bottomMargin = dp(8)
        })

        captionText = TextView(context).apply {
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            setTextColor(textColor)
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
            maxWidth = dp(280)
            setLineSpacing(0f, 1.08f)
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
