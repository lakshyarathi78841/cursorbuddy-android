package com.cursorbuddy.android.overlay

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout

@SuppressLint("ClickableViewAccessibility")
class FloatingBubble(context: Context) : FrameLayout(context) {

    interface BubbleListener {
        fun onBubbleTapped()
        fun onBubbleLongPressed()
        fun onBubbleMoved(x: Int, y: Int)
        fun onBubblePosition(x: Int, y: Int)
    }

    var listener: BubbleListener? = null
    
    private var isDragging = false
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var initialParamX = 0
    private var initialParamY = 0
    private var longPressRunnable: Runnable? = null

    private val triangleView: TriangleView

    init {
        val size = dp(56)
        triangleView = TriangleView(context)
        addView(triangleView, LayoutParams(size, size, Gravity.CENTER))
        
        elevation = dp(6).toFloat()

        setOnTouchListener { _, event ->
            // Read current position from WindowManager params
            val params = layoutParams as? WindowManager.LayoutParams
            
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    isDragging = false
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    initialParamX = params?.x ?: 0
                    initialParamY = params?.y ?: 0
                    
                    longPressRunnable = Runnable { listener?.onBubbleLongPressed() }
                    postDelayed(longPressRunnable, 500)
                    
                    triangleView.animate().scaleX(0.85f).scaleY(0.85f).setDuration(100).start()
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - initialTouchX
                    val dy = event.rawY - initialTouchY
                    if (Math.abs(dx) > 10 || Math.abs(dy) > 10) {
                        isDragging = true
                        longPressRunnable?.let { removeCallbacks(it) }
                        val newX = (initialParamX + dx).toInt()
                        val newY = (initialParamY + dy).toInt()
                        listener?.onBubbleMoved(newX, newY)
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    longPressRunnable?.let { removeCallbacks(it) }
                    triangleView.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
                    if (!isDragging) {
                        listener?.onBubbleTapped()
                        // Report current position for input panel placement
                        val cx = (params?.x ?: 0) + dp(28)
                        val cy = (params?.y ?: 0) + dp(28)
                        listener?.onBubblePosition(cx, cy)
                    }
                    true
                }
                else -> false
            }
        }
    }

    private fun dp(value: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            value.toFloat(),
            context.resources.displayMetrics
        ).toInt()
    }

    // Custom triangle view pointing up-left (like a cursor)
    class TriangleView(context: Context) : View(context) {
        
        private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#4FC3F7")
            style = Paint.Style.FILL
            setShadowLayer(8f, 2f, 2f, Color.parseColor("#44000000"))
        }
        
        private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#29B6F6")
            style = Paint.Style.STROKE
            strokeWidth = 2.5f
        }
        
        private val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#80FFFFFF")
            style = Paint.Style.FILL
        }

        private val path = Path()
        private val highlightPath = Path()

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            
            val w = width.toFloat()
            val h = height.toFloat()
            val pad = w * 0.12f

            path.reset()
            path.moveTo(pad, pad)
            path.lineTo(pad, h - pad * 1.5f)
            path.lineTo(w * 0.32f, h * 0.62f)
            path.lineTo(w * 0.55f, h - pad)
            path.lineTo(w * 0.42f, h * 0.55f)
            path.lineTo(w - pad * 1.5f, w * 0.42f)
            path.close()
            
            canvas.drawPath(path, fillPaint)
            canvas.drawPath(path, strokePaint)
            
            highlightPath.reset()
            highlightPath.moveTo(pad + 3f, pad + 6f)
            highlightPath.lineTo(pad + 3f, h * 0.5f)
            highlightPath.lineTo(pad + w * 0.12f, h * 0.42f)
            highlightPath.close()
            canvas.drawPath(highlightPath, highlightPaint)
        }
    }
}
