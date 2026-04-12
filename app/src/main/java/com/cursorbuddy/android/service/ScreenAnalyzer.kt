package com.cursorbuddy.android.service

import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.cursorbuddy.android.model.UiNode

object ScreenAnalyzer {
    
    private const val TAG = "CursorBuddy"
    
    interface ScreenChangeListener {
        fun onScreenChanged(packageName: String, uiTree: UiNode?)
        fun onTargetClicked()
    }
    
    var listener: ScreenChangeListener? = null
    
    private var lastPackage: String? = null
    private var debounceTime: Long = 0
    
    fun onScreenChanged(packageName: String) {
        val now = System.currentTimeMillis()
        if (now - debounceTime < 200 && packageName == lastPackage) return
        debounceTime = now
        lastPackage = packageName
        
        val uiTree = getCurrentUiTree()
        listener?.onScreenChanged(packageName, uiTree)
    }
    
    fun onViewClicked(event: AccessibilityEvent) {
        listener?.onTargetClicked()
    }
    
    fun getCurrentUiTree(): UiNode? {
        val svc = CursorBuddyAccessibilityService.instance
        if (svc == null) {
            Log.w(TAG, "ScreenAnalyzer: accessibility service instance is null (PID=${android.os.Process.myPid()}, isRunning=${CursorBuddyAccessibilityService.isRunning})")
            return null
        }
        return svc.getUiTree()
    }
    
    fun getCurrentPackage(): String? {
        val svc = CursorBuddyAccessibilityService.instance
        if (svc == null) {
            Log.w(TAG, "ScreenAnalyzer: cannot get package, accessibility instance null")
            return null
        }
        return svc.getCurrentPackage()
    }
    
    fun isAccessibilityConnected(): Boolean {
        return CursorBuddyAccessibilityService.instance != null
    }
}
