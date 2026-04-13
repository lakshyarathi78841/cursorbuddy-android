package com.cursorbuddy.android.overlay

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.view.View

/**
 * Glass lens cursor — a circular "globe" that acts like a handheld magnifier.
 * It fakes a warp/saturation-boost by compositing a saturated radial gradient,
 * a chromatic rim, and a bright specular highlight over a semi-transparent core.
 */
class PointerView(context: Context) : View(context) {

    var lensBaseScale: Float = 0.82f
        set(value) {
            field = value
            invalidate()
        }

    var pulseScale: Float = 1.0f
        set(value) {
            field = value
            invalidate()
        }

    fun animateLensBaseScale(from: Float, to: Float, duration: Long = 180L) {
        lensBaseScale = from
        ValueAnimator.ofFloat(from, to).apply {
            this.duration = duration
            addUpdateListener { animator ->
                lensBaseScale = animator.animatedValue as Float
            }
            start()
        }
    }

    // Lens body — very light tint so the underlying pixels read through
    private val lensBodyPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    // Saturation boost layer — warm radial gradient drawn on top of the body
    private val saturationPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    // Outer rim — metallic chrome ring
    private val rimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }
    // Inner shadow — fakes the "warp" by darkening near the rim
    private val innerShadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }
    // Specular highlight (top-left)
    private val specularPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    // Drop shadow under the whole lens
    private val dropShadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x55000000.toInt()
        style = Paint.Style.FILL
        setShadowLayer(14f, 0f, 6f, 0x66000000.toInt())
    }

    init {
        // Required for setShadowLayer to render in a hardware layer
        setLayerType(LAYER_TYPE_SOFTWARE, null)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val cx = width / 2f
        val cy = height / 2f
        val radius = (Math.min(width, height) / 2f - 6f) * lensBaseScale * pulseScale
        if (radius <= 0f) return

        // 1) Drop shadow
        canvas.drawCircle(cx, cy + 3f, radius, dropShadowPaint)

        // 2) Lens body — nearly clear with the faintest cool tint, so the
        //    underlying display reads through. We exaggerate a radial gradient
        //    from darker-edge to lighter-center to suggest refraction / warp.
        lensBodyPaint.shader = RadialGradient(
            cx - radius * 0.25f, cy - radius * 0.25f, radius * 1.1f,
            intArrayOf(
                0x30FFFFFF,  // bright core
                0x18AEE9FF,  // faint cool tint
                0x22000000   // darker rim (fake refraction)
            ),
            floatArrayOf(0f, 0.65f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(cx, cy, radius, lensBodyPaint)

        // 3) Saturation-boost layer — OVERLAY blend exaggerates color/contrast
        //    of whatever is beneath the lens.
        saturationPaint.shader = RadialGradient(
            cx, cy, radius * 0.95f,
            intArrayOf(
                0x66FFB347,  // warm punch in center
                0x3340E0FF,  // cyan mid
                0x00000000   // transparent edge
            ),
            floatArrayOf(0f, 0.55f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(cx, cy, radius, saturationPaint)

        // 4) Inner shadow — thick semi-transparent ring just inside the rim
        //    sells the warped-glass-edge look.
        innerShadowPaint.strokeWidth = radius * 0.22f
        innerShadowPaint.shader = RadialGradient(
            cx, cy, radius,
            intArrayOf(0x00000000, 0x00000000, 0x55000000.toInt()),
            floatArrayOf(0f, 0.78f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(cx, cy, radius - innerShadowPaint.strokeWidth / 2f, innerShadowPaint)

        // 5) Chrome rim — bright outer ring with a subtle gradient
        rimPaint.strokeWidth = 3f
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

        // 6) Specular highlight — curved white gloss, top-left
        specularPaint.shader = RadialGradient(
            cx - radius * 0.35f, cy - radius * 0.45f, radius * 0.55f,
            intArrayOf(0xCCFFFFFF.toInt(), 0x33FFFFFF, 0x00FFFFFF),
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

        // 7) Tiny bottom-right highlight for extra depth
        specularPaint.shader = RadialGradient(
            cx + radius * 0.4f, cy + radius * 0.5f, radius * 0.25f,
            intArrayOf(0x66FFFFFF, 0x00FFFFFF),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(cx + radius * 0.4f, cy + radius * 0.5f, radius * 0.25f, specularPaint)
    }
}
