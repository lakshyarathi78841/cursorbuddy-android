package com.cursorbuddy.android.overlay

import android.content.Context
import android.graphics.*
import android.view.View

class TargetHighlight(context: Context) : View(context) {

    private val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#334FC3F7")
        style = Paint.Style.FILL
    }
    
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#4FC3F7")
        style = Paint.Style.STROKE
        strokeWidth = 3f
        pathEffect = DashPathEffect(floatArrayOf(10f, 5f), 0f)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        // Draw rounded rect filling this view (positioned via WindowManager params)
        val padding = 4f
        val rect = RectF(padding, padding, width - padding, height - padding)
        val cornerRadius = 12f
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, highlightPaint)
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, borderPaint)
    }
}
