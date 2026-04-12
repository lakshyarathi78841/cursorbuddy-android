package com.cursorbuddy.android.overlay

import android.content.Context
import android.graphics.*
import android.view.View

class PointerView(context: Context) : View(context) {

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#4FC3F7")
        style = Paint.Style.FILL
        setShadowLayer(6f, 2f, 2f, Color.parseColor("#44000000"))
    }

    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#29B6F6")
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }

    private val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#80FFFFFF")
        style = Paint.Style.FILL
    }

    var pointerScale: Float = 1.0f
        set(value) {
            field = value
            invalidate()
        }

    private val cursorPath = Path()
    private val highlightPath = Path()

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val cx = width / 2f
        val cy = height / 2f
        val size = (width * 0.35f) * pointerScale

        canvas.save()
        canvas.translate(cx, cy)

        // Cursor arrow pointing up-left
        cursorPath.reset()
        cursorPath.moveTo(-size, -size)                     // Tip (top-left)
        cursorPath.lineTo(-size, size * 0.7f)               // Down left edge
        cursorPath.lineTo(-size * 0.3f, size * 0.25f)       // Notch inward
        cursorPath.lineTo(size * 0.2f, size)                // Tail kick
        cursorPath.lineTo(size * 0.05f, size * 0.15f)       // Back up
        cursorPath.lineTo(size * 0.8f, -size * 0.1f)        // Right point
        cursorPath.close()

        canvas.drawPath(cursorPath, fillPaint)
        canvas.drawPath(cursorPath, strokePaint)

        // Small highlight on left edge for 3D
        highlightPath.reset()
        highlightPath.moveTo(-size + 2f, -size + 4f)
        highlightPath.lineTo(-size + 2f, size * 0.2f)
        highlightPath.lineTo(-size + size * 0.15f, size * 0.05f)
        highlightPath.close()
        canvas.drawPath(highlightPath, highlightPaint)

        canvas.restore()
    }
}
