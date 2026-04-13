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

    private val lensView: LensView

    init {
        val size = dp(72)
        lensView = LensView(context)
        addView(lensView, LayoutParams(size, size, Gravity.CENTER))

        elevation = dp(10).toFloat()

        setOnTouchListener { _, event ->
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

                    lensView.animate().scaleX(0.9f).scaleY(0.9f).setDuration(90).start()
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
                    lensView.animate().scaleX(1f).scaleY(1f).setDuration(120).start()
                    if (!isDragging) {
                        listener?.onBubbleTapped()
                        val cx = (params?.x ?: 0) + dp(36)
                        val cy = (params?.y ?: 0) + dp(36)
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

    /**
     * Glass lens rendering for the floating bubble. Mirrors PointerView so the
     * resting buddy and the guiding cursor feel like the same object.
     */
    class LensView(context: Context) : View(context) {

        private val lensBodyPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val saturationPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val rimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
        }
        private val innerShadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
        }
        private val specularPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }
        private val dropShadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0x55000000.toInt()
            style = Paint.Style.FILL
            setShadowLayer(16f, 0f, 8f, 0x66000000.toInt())
        }

        init {
            setLayerType(LAYER_TYPE_SOFTWARE, null)
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)

            val cx = width / 2f
            val cy = height / 2f
            val radius = Math.min(width, height) / 2f - 8f
            if (radius <= 0f) return

            // Drop shadow
            canvas.drawCircle(cx, cy + 4f, radius, dropShadowPaint)

            // Lens body — transparent with a faint cool tint
            lensBodyPaint.shader = RadialGradient(
                cx - radius * 0.25f, cy - radius * 0.25f, radius * 1.1f,
                intArrayOf(
                    0x38FFFFFF,
                    0x1CAEE9FF,
                    0x22000000
                ),
                floatArrayOf(0f, 0.65f, 1f),
                Shader.TileMode.CLAMP
            )
            canvas.drawCircle(cx, cy, radius, lensBodyPaint)

            // Warm saturation punch
            saturationPaint.shader = RadialGradient(
                cx, cy, radius * 0.95f,
                intArrayOf(
                    0x66FFB347,
                    0x3340E0FF,
                    0x00000000
                ),
                floatArrayOf(0f, 0.55f, 1f),
                Shader.TileMode.CLAMP
            )
            canvas.drawCircle(cx, cy, radius, saturationPaint)

            // Inner shadow ring — fakes the refracted edge
            innerShadowPaint.strokeWidth = radius * 0.22f
            innerShadowPaint.shader = RadialGradient(
                cx, cy, radius,
                intArrayOf(0x00000000, 0x00000000, 0x55000000.toInt()),
                floatArrayOf(0f, 0.78f, 1f),
                Shader.TileMode.CLAMP
            )
            canvas.drawCircle(cx, cy, radius - innerShadowPaint.strokeWidth / 2f, innerShadowPaint)

            // Chrome rim
            rimPaint.strokeWidth = 3.5f
            rimPaint.shader = SweepGradient(
                cx, cy,
                intArrayOf(
                    0xFFFFFFFF.toInt(),
                    0xFFB3E5FC.toInt(),
                    0xFF81D4FA.toInt(),
                    0xFFFFFFFF.toInt(),
                    0xFFB3E5FC.toInt(),
                    0xFFFFFFFF.toInt()
                ),
                null
            )
            canvas.drawCircle(cx, cy, radius - 1f, rimPaint)

            // Specular highlight (top-left)
            specularPaint.shader = RadialGradient(
                cx - radius * 0.35f, cy - radius * 0.45f, radius * 0.6f,
                intArrayOf(0xD0FFFFFF.toInt(), 0x33FFFFFF, 0x00FFFFFF),
                floatArrayOf(0f, 0.5f, 1f),
                Shader.TileMode.CLAMP
            )
            val highlightRect = RectF(
                cx - radius * 0.7f,
                cy - radius * 0.8f,
                cx + radius * 0.05f,
                cy - radius * 0.1f
            )
            canvas.drawOval(highlightRect, specularPaint)

            // Bottom-right secondary highlight
            specularPaint.shader = RadialGradient(
                cx + radius * 0.4f, cy + radius * 0.5f, radius * 0.25f,
                intArrayOf(0x66FFFFFF, 0x00FFFFFF),
                floatArrayOf(0f, 1f),
                Shader.TileMode.CLAMP
            )
            canvas.drawCircle(cx + radius * 0.4f, cy + radius * 0.5f, radius * 0.25f, specularPaint)
        }
    }
}
