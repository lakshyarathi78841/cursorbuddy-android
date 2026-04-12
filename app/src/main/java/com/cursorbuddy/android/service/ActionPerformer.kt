package com.cursorbuddy.android.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.RectF
import android.os.Build
import android.os.Bundle
import android.view.accessibility.AccessibilityNodeInfo
import com.cursorbuddy.android.model.StepAction
import com.cursorbuddy.android.model.TutorialStep

class ActionPerformer {

    interface ActionCallback {
        fun onActionComplete(step: TutorialStep, success: Boolean)
        fun onActionError(step: TutorialStep, error: String)
    }

    var callback: ActionCallback? = null

    fun performAction(step: TutorialStep) {
        val service = CursorBuddyAccessibilityService.instance
        if (service == null) {
            callback?.onActionError(step, "Accessibility service not running")
            return
        }

        when (step.action) {
            StepAction.TAP -> performTap(service, step)
            StepAction.LONG_PRESS -> performLongPress(service, step)
            StepAction.SCROLL -> performScroll(service, step)
            StepAction.SWIPE -> performSwipe(service, step)
            StepAction.TYPE -> performType(service, step)
            StepAction.WAIT -> {
                // Just wait, nothing to do
                callback?.onActionComplete(step, true)
            }
        }
    }

    private fun performTap(service: AccessibilityService, step: TutorialStep) {
        val bounds = step.targetBounds ?: run {
            callback?.onActionError(step, "No target bounds for tap")
            return
        }

        // Try to find and click the actual node first
        val node = findNodeByBounds(service, bounds)
        if (node != null && node.isClickable) {
            val result = node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            node.recycle()
            callback?.onActionComplete(step, result)
            return
        }
        node?.recycle()

        // Fallback: use gesture API to tap at coordinates
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val x = bounds.centerX()
            val y = bounds.centerY()
            val path = Path().apply { moveTo(x, y) }
            val gesture = GestureDescription.Builder()
                .addStroke(GestureDescription.StrokeDescription(path, 0, 100))
                .build()
            
            service.dispatchGesture(gesture, object : AccessibilityService.GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) {
                    callback?.onActionComplete(step, true)
                }
                override fun onCancelled(gestureDescription: GestureDescription?) {
                    callback?.onActionError(step, "Tap gesture cancelled")
                }
            }, null)
        } else {
            callback?.onActionError(step, "Gesture API not available")
        }
    }

    private fun performLongPress(service: AccessibilityService, step: TutorialStep) {
        val bounds = step.targetBounds ?: run {
            callback?.onActionError(step, "No target bounds for long press")
            return
        }

        // Try node first
        val node = findNodeByBounds(service, bounds)
        if (node != null && node.isLongClickable) {
            val result = node.performAction(AccessibilityNodeInfo.ACTION_LONG_CLICK)
            node.recycle()
            callback?.onActionComplete(step, result)
            return
        }
        node?.recycle()

        // Fallback: long press gesture
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val x = bounds.centerX()
            val y = bounds.centerY()
            val path = Path().apply { moveTo(x, y) }
            val gesture = GestureDescription.Builder()
                .addStroke(GestureDescription.StrokeDescription(path, 0, 800))
                .build()
            
            service.dispatchGesture(gesture, object : AccessibilityService.GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) {
                    callback?.onActionComplete(step, true)
                }
                override fun onCancelled(gestureDescription: GestureDescription?) {
                    callback?.onActionError(step, "Long press cancelled")
                }
            }, null)
        }
    }

    private fun performScroll(service: AccessibilityService, step: TutorialStep) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val screenWidth = service.resources.displayMetrics.widthPixels.toFloat()
            val screenHeight = service.resources.displayMetrics.heightPixels.toFloat()
            
            val startX = screenWidth / 2
            val startY = screenHeight * 0.7f
            val endY = screenHeight * 0.3f
            
            val path = Path().apply {
                moveTo(startX, startY)
                lineTo(startX, endY)
            }
            val gesture = GestureDescription.Builder()
                .addStroke(GestureDescription.StrokeDescription(path, 0, 400))
                .build()
            
            service.dispatchGesture(gesture, object : AccessibilityService.GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) {
                    callback?.onActionComplete(step, true)
                }
                override fun onCancelled(gestureDescription: GestureDescription?) {
                    callback?.onActionError(step, "Scroll cancelled")
                }
            }, null)
        }
    }

    private fun performSwipe(service: AccessibilityService, step: TutorialStep) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val screenWidth = service.resources.displayMetrics.widthPixels.toFloat()
            val screenHeight = service.resources.displayMetrics.heightPixels.toFloat()
            
            val y = screenHeight / 2
            val path = Path().apply {
                moveTo(screenWidth * 0.8f, y)
                lineTo(screenWidth * 0.2f, y)
            }
            val gesture = GestureDescription.Builder()
                .addStroke(GestureDescription.StrokeDescription(path, 0, 300))
                .build()
            
            service.dispatchGesture(gesture, object : AccessibilityService.GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) {
                    callback?.onActionComplete(step, true)
                }
                override fun onCancelled(gestureDescription: GestureDescription?) {
                    callback?.onActionError(step, "Swipe cancelled")
                }
            }, null)
        }
    }

    private fun performType(service: AccessibilityService, step: TutorialStep) {
        val bounds = step.targetBounds
        val node = if (bounds != null) findNodeByBounds(service, bounds) else findFocusedEditText(service)
        
        if (node != null) {
            // Focus the field
            node.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
            
            // Set text
            val text = step.targetDescription // Using targetDescription to carry input text
            val args = Bundle().apply {
                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
            }
            val result = node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
            node.recycle()
            callback?.onActionComplete(step, result)
        } else {
            callback?.onActionError(step, "Could not find text field")
        }
    }

    private fun findNodeByBounds(service: AccessibilityService, targetBounds: RectF): AccessibilityNodeInfo? {
        val root = service.rootInActiveWindow ?: return null
        return findNodeByBoundsRecursive(root, targetBounds)
    }

    private fun findNodeByBoundsRecursive(node: AccessibilityNodeInfo, targetBounds: RectF): AccessibilityNodeInfo? {
        val nodeBounds = android.graphics.Rect()
        node.getBoundsInScreen(nodeBounds)
        val nodeRect = RectF(nodeBounds)
        
        // Check if bounds roughly match (within 20px tolerance)
        if (Math.abs(nodeRect.centerX() - targetBounds.centerX()) < 20 &&
            Math.abs(nodeRect.centerY() - targetBounds.centerY()) < 20) {
            return node
        }
        
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = findNodeByBoundsRecursive(child, targetBounds)
            if (found != null) return found
            child.recycle()
        }
        
        return null
    }

    private fun findFocusedEditText(service: AccessibilityService): AccessibilityNodeInfo? {
        return service.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
    }
}
