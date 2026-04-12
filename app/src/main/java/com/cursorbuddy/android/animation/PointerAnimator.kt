package com.cursorbuddy.android.animation

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.graphics.PointF
import android.graphics.RectF
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator

class PointerAnimator {

    interface AnimationCallback {
        fun onPointerMoved(x: Float, y: Float, progress: Float)
        fun onPointerArrived(targetBounds: RectF)
        fun onPulseUpdate(scale: Float)
    }

    var callback: AnimationCallback? = null
    var speedMultiplier: Float = 1.0f
    
    private var moveAnimator: ValueAnimator? = null
    private var pulseAnimator: ValueAnimator? = null
    private var currentPosition = PointF(540f, 960f) // Center of screen default

    fun animateToTarget(targetBounds: RectF) {
        cancelAll()
        
        val targetCenter = PointF(targetBounds.centerX(), targetBounds.centerY())
        val startPos = PointF(currentPosition.x, currentPosition.y)
        
        // Calculate distance for duration scaling
        val dx = targetCenter.x - startPos.x
        val dy = targetCenter.y - startPos.y
        val distance = Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
        
        // Duration: 300-600ms based on distance, scaled by speed
        val baseDuration = (300 + (distance / 3).coerceAtMost(300f)).toLong()
        val duration = (baseDuration / speedMultiplier).toLong()
        
        moveAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            this.duration = duration
            interpolator = DecelerateInterpolator(2f)
            
            addUpdateListener { animator ->
                val t = animator.animatedValue as Float
                
                // Quadratic bezier for slight curve (more natural movement)
                val controlX = (startPos.x + targetCenter.x) / 2 + (targetCenter.y - startPos.y) * 0.1f
                val controlY = (startPos.y + targetCenter.y) / 2 - (targetCenter.x - startPos.x) * 0.1f
                
                val oneMinusT = 1 - t
                val x = oneMinusT * oneMinusT * startPos.x + 2 * oneMinusT * t * controlX + t * t * targetCenter.x
                val y = oneMinusT * oneMinusT * startPos.y + 2 * oneMinusT * t * controlY + t * t * targetCenter.y
                
                currentPosition.set(x, y)
                callback?.onPointerMoved(x, y, t)
            }
            
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    currentPosition.set(targetCenter.x, targetCenter.y)
                    callback?.onPointerArrived(targetBounds)
                    startPulse()
                }
            })
            
            start()
        }
    }

    private fun startPulse() {
        pulseAnimator = ValueAnimator.ofFloat(1.0f, 1.3f, 1.0f).apply {
            duration = 600
            interpolator = OvershootInterpolator(1.5f)
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            
            addUpdateListener { animator ->
                callback?.onPulseUpdate(animator.animatedValue as Float)
            }
            
            start()
        }
    }

    fun stopPulse() {
        pulseAnimator?.cancel()
        pulseAnimator = null
        callback?.onPulseUpdate(1.0f)
    }

    fun cancelAll() {
        moveAnimator?.cancel()
        moveAnimator = null
        pulseAnimator?.cancel()
        pulseAnimator = null
    }

    fun getCurrentPosition(): PointF = PointF(currentPosition.x, currentPosition.y)
    
    fun setPosition(x: Float, y: Float) {
        currentPosition.set(x, y)
    }
}
