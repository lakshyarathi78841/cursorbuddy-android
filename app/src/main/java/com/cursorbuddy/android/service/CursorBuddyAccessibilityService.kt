package com.cursorbuddy.android.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.graphics.Rect
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.cursorbuddy.android.model.UiNode

class CursorBuddyAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "CursorBuddy"
        
        // Use @Volatile to ensure visibility across threads
        @Volatile
        var instance: CursorBuddyAccessibilityService? = null
            private set
        
        @Volatile
        var isRunning: Boolean = false
            private set
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        isRunning = true
        Log.d(TAG, "AccessibilityService CONNECTED in PID ${android.os.Process.myPid()}, instance=$this")
        
        serviceInfo = serviceInfo?.apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                         AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED or
                         AccessibilityEvent.TYPE_VIEW_CLICKED or
                         AccessibilityEvent.TYPE_VIEW_SCROLLED or
                         AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED or
                         AccessibilityEvent.TYPE_VIEW_LONG_CLICKED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS or
                    AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS or
                    AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
            notificationTimeout = 200
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return
        
        // Keep instance alive — re-set on every event in case of process issues
        if (instance == null) {
            instance = this
            isRunning = true
            Log.d(TAG, "AccessibilityService re-set instance on event")
        }
        
        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                val packageName = event.packageName?.toString() ?: return
                if (packageName == "com.cursorbuddy.android") return
                ScreenAnalyzer.onScreenChanged(packageName)
            }
            AccessibilityEvent.TYPE_VIEW_CLICKED,
            AccessibilityEvent.TYPE_VIEW_SCROLLED,
            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED,
            AccessibilityEvent.TYPE_VIEW_LONG_CLICKED -> {
                ScreenAnalyzer.onUserInteraction(event)
            }
        }
    }

    override fun onInterrupt() {
        Log.d(TAG, "AccessibilityService onInterrupt")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "AccessibilityService DESTROYED")
        instance = null
        isRunning = false
    }

    fun getUiTree(): UiNode? {
        val root = rootInActiveWindow ?: run {
            Log.w(TAG, "rootInActiveWindow is null")
            return null
        }
        return try {
            val tree = parseNode(root)
            Log.d(TAG, "UI tree parsed: ${tree.flatten().size} nodes")
            tree
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing UI tree", e)
            null
        }
    }

    fun getCurrentPackage(): String? {
        return rootInActiveWindow?.packageName?.toString()
    }

    private fun parseNode(node: AccessibilityNodeInfo): UiNode {
        val bounds = Rect()
        node.getBoundsInScreen(bounds)

        val children = mutableListOf<UiNode>()
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { child ->
                try {
                    children.add(parseNode(child))
                } catch (e: Exception) {
                    // Skip problematic nodes
                }
            }
        }

        return UiNode(
            className = node.className?.toString() ?: "Unknown",
            text = node.text?.toString(),
            contentDescription = node.contentDescription?.toString(),
            viewId = node.viewIdResourceName,
            bounds = bounds,
            isClickable = node.isClickable,
            isScrollable = node.isScrollable,
            isEditable = node.isEditable,
            isPassword = node.isPassword,
            isChecked = if (node.isCheckable) node.isChecked else null,
            children = children
        )
    }
}
