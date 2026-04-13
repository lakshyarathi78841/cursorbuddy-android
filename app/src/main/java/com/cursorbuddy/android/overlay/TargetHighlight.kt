package com.cursorbuddy.android.overlay

import android.content.Context
import android.graphics.*
import android.view.View

class TargetHighlight(context: Context) : View(context) {

    private val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x264FC3F7
        style = Paint.Style.FILL
    }

    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF81D4FA.toInt()
        style = Paint.Style.STROKE
        strokeWidth = 5f
        setShadowLayer(14f, 0f, 0f, 0xFF4FC3F7.toInt())
    }

    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFFFFFF.toInt()
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }

    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val padding = 6f
        val rect = RectF(padding, padding, width - padding, height - padding)
        val cornerRadius = 18f
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, highlightPaint)
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, glowPaint)
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, borderPaint)
    }
}
