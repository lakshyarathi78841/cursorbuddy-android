package com.cursorbuddy.android.tutorial

import android.content.Context
import android.util.Log
import com.cursorbuddy.android.model.*
import com.cursorbuddy.android.service.*
import kotlinx.coroutines.*

class TutorialEngine(private val context: Context? = null) : ScreenAnalyzer.ScreenChangeListener {

    companion object {
        private const val TAG = "CursorBuddy"
    }

    interface TutorialCallback {
        fun onStateChanged(state: BuddyState)
        fun onStepChanged(step: TutorialStep)
        fun onTutorialComplete(tutorial: Tutorial)
        fun onError(message: String)
    }

    var callback: TutorialCallback? = null
    var state: BuddyState = BuddyState.IDLE
        private set
    
    private var currentTutorial: Tutorial? = null
    private var currentStepIndex: Int = 0
    private var screenChangeCount: Int = 0
    private var lastStepAdvanceTime: Long = 0
    
    // AI Planner
    private var claudePlanner: ClaudeAIPlanner? = null
    private var screenCapturer: ScreenCapturer? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    // Action performer
    private val actionPerformer = ActionPerformer()
    
    fun setApiKey(key: String) {
        Log.d(TAG, "API key set (${key.take(8)}...)")
        claudePlanner = ClaudeAIPlanner(key)
    }
    
    fun setScreenCapturer(capturer: ScreenCapturer) {
        screenCapturer = capturer
    }
    
    fun startTutorialWithCache(question: String, cachedPackage: String?, cachedUiTree: UiNode?) {
        setState(BuddyState.ANALYZING)
        
        // Use cached data (captured before input panel stole focus), fall back to live
        val packageName = cachedPackage ?: ScreenAnalyzer.getCurrentPackage()
        val uiTree = cachedUiTree ?: ScreenAnalyzer.getCurrentUiTree()
        
        Log.d(TAG, "startTutorialWithCache: question='$question' pkg=$packageName hasTree=${uiTree != null} usedCache=${cachedUiTree != null} hasAI=${claudePlanner != null}")
        
        if (uiTree != null) {
            val flat = uiTree.flatten()
            Log.d(TAG, "UI tree: ${flat.size} total nodes, ${flat.count { it.isClickable }} clickable")
        }
        
        if (claudePlanner != null) {
            planWithAI(question, packageName, uiTree)
            return
        }
        
        val tutorial = TutorialPlanner.planTutorialWithContext(question, packageName, uiTree, null)
            ?: TutorialPlanner.planTutorial(question, packageName, uiTree)
        
        if (tutorial == null || tutorial.steps.isEmpty()) {
            if (uiTree == null) {
                callback?.onError("Cannot read screen. Please re-enable CursorBuddy in Accessibility Settings.")
            } else {
                callback?.onError("Could not find what you need on this screen. Try \"show me around\"!")
            }
            setState(BuddyState.IDLE)
            return
        }
        
        startWithTutorial(tutorial)
    }
    
    fun startTutorial(question: String) {
        setState(BuddyState.ANALYZING)
        
        val packageName = ScreenAnalyzer.getCurrentPackage()
        val uiTree = ScreenAnalyzer.getCurrentUiTree()
        
        Log.d(TAG, "startTutorial: question='$question' pkg=$packageName hasTree=${uiTree != null} hasAI=${claudePlanner != null}")
        
        if (uiTree != null) {
            val flat = uiTree.flatten()
            Log.d(TAG, "UI tree: ${flat.size} total nodes, ${flat.count { it.isClickable }} clickable")
        } else {
            Log.w(TAG, "UI tree is NULL - accessibility service may not be running")
        }
        
        // Always prefer AI if available
        if (claudePlanner != null) {
            planWithAI(question, packageName, uiTree)
            return
        }
        
        // Fallback to local pattern matching
        val tutorial = TutorialPlanner.planTutorialWithContext(question, packageName, uiTree, null)
            ?: TutorialPlanner.planTutorial(question, packageName, uiTree)
        
        if (tutorial == null || tutorial.steps.isEmpty()) {
            if (uiTree == null) {
                callback?.onError("Cannot read screen. Please re-enable CursorBuddy in Accessibility Settings.")
            } else {
                callback?.onError("Could not find what you need on this screen. Try \"show me around\"!")
            }
            setState(BuddyState.IDLE)
            return
        }
        
        startWithTutorial(tutorial)
    }
    
    private fun planWithAI(question: String, packageName: String?, uiTree: UiNode?) {
        scope.launch {
            try {
                // Serialize UI tree
                val uiTreeJson = if (uiTree != null) {
                    UiTreeSerializer.toCompactJson(uiTree)
                } else "[]"
                
                Log.d(TAG, "AI planning: uiTreeJson length=${uiTreeJson.length}")
                
                // Get app name
                val appName = if (packageName != null && context != null) {
                    try {
                        val ai = context.packageManager.getApplicationInfo(packageName, 0)
                        context.packageManager.getApplicationLabel(ai).toString()
                    } catch (e: Exception) { null }
                } else null
                
                // Capture screenshot if available
                var screenshotBase64: String? = null
                if (screenCapturer != null && ScreenCapturer.hasPermission()) {
                    Log.d(TAG, "Capturing screenshot...")
                    screenshotBase64 = withContext(Dispatchers.Main) {
                        suspendCancellableCoroutine { cont ->
                            screenCapturer!!.captureScreen { bitmap ->
                                if (bitmap != null) {
                                    val b64 = screenCapturer!!.bitmapToBase64(bitmap)
                                    bitmap.recycle()
                                    Log.d(TAG, "Screenshot captured: ${b64.length} chars base64")
                                    cont.resume(b64) {}
                                } else {
                                    Log.w(TAG, "Screenshot capture returned null")
                                    cont.resume(null) {}
                                }
                            }
                        }
                    }
                } else {
                    Log.d(TAG, "No screen capture permission, sending UI tree only")
                }
                
                // Call Claude
                Log.d(TAG, "Calling Claude API... pkg=$packageName app=$appName hasScreenshot=${screenshotBase64 != null}")
                
                val steps = claudePlanner!!.planFromScreenshot(
                    question = question,
                    screenshotBase64 = screenshotBase64,
                    uiTreeJson = uiTreeJson,
                    packageName = packageName,
                    appName = appName
                )
                
                Log.d(TAG, "Claude returned ${steps.size} steps")
                
                if (steps.isEmpty()) {
                    // Fallback to local planner
                    Log.d(TAG, "Claude returned empty, falling back to local planner")
                    val localTutorial = TutorialPlanner.planTutorial(question, packageName, uiTree)
                    if (localTutorial != null && localTutorial.steps.isNotEmpty()) {
                        startWithTutorial(localTutorial)
                    } else {
                        if (uiTree == null) {
                            callback?.onError("Cannot read screen. Re-enable CursorBuddy in Accessibility Settings, then try again.")
                        } else {
                            callback?.onError("AI could not figure out the steps. Try a different question!")
                        }
                        setState(BuddyState.IDLE)
                    }
                    return@launch
                }
                
                val tutorial = Tutorial(
                    id = "ai_${System.currentTimeMillis()}",
                    appPackage = packageName ?: "unknown",
                    question = question,
                    steps = steps,
                    source = TutorialSource.AI_GENERATED
                )
                
                startWithTutorial(tutorial)
                
            } catch (e: Exception) {
                Log.e(TAG, "AI planning error", e)
                callback?.onError("AI error: ${e.message?.take(100) ?: "Unknown"}")
                setState(BuddyState.IDLE)
            }
        }
    }
    
    private fun startWithTutorial(tutorial: Tutorial) {
        Log.d(TAG, "Starting tutorial: ${tutorial.steps.size} steps, source=${tutorial.source}")
        currentTutorial = tutorial
        currentStepIndex = 0
        screenChangeCount = 0
        lastStepAdvanceTime = System.currentTimeMillis()
        setState(BuddyState.GUIDING)
        showCurrentStep()
    }
    
    fun executeCurrentStep() {
        val step = currentTutorial?.steps?.getOrNull(currentStepIndex) ?: return
        Log.d(TAG, "Executing step ${step.stepNumber}: ${step.action} on '${step.targetDescription}'")
        actionPerformer.callback = object : ActionPerformer.ActionCallback {
            override fun onActionComplete(step: TutorialStep, success: Boolean) {
                Log.d(TAG, "Action complete: success=$success")
                scope.launch {
                    delay(500)
                    nextStep()
                }
            }
            override fun onActionError(step: TutorialStep, error: String) {
                Log.e(TAG, "Action error: $error")
                callback?.onError("Could not perform action: $error")
            }
        }
        actionPerformer.performAction(step)
    }
    
    fun nextStep() {
        val tutorial = currentTutorial ?: return
        val now = System.currentTimeMillis()
        
        if (now - lastStepAdvanceTime < 400) return
        lastStepAdvanceTime = now
        
        currentStepIndex++
        if (currentStepIndex >= tutorial.steps.size) {
            setState(BuddyState.COMPLETE)
            callback?.onTutorialComplete(tutorial)
            return
        }
        showCurrentStep()
    }
    
    fun previousStep() {
        if (currentStepIndex > 0) {
            currentStepIndex--
            showCurrentStep()
        }
    }
    
    fun pause() {
        if (state == BuddyState.GUIDING) setState(BuddyState.PAUSED)
    }
    
    fun resume() {
        if (state == BuddyState.PAUSED) {
            setState(BuddyState.GUIDING)
            showCurrentStep()
        }
    }
    
    fun stop() {
        currentTutorial = null
        currentStepIndex = 0
        screenChangeCount = 0
        setState(BuddyState.IDLE)
    }
    
    fun replayCurrent() {
        if (state == BuddyState.GUIDING || state == BuddyState.PAUSED) {
            setState(BuddyState.GUIDING)
            showCurrentStep()
        }
    }
    
    private fun showCurrentStep() {
        val tutorial = currentTutorial ?: return
        val step = tutorial.steps.getOrNull(currentStepIndex) ?: return
        Log.d(TAG, "Showing step ${step.stepNumber}/${step.totalSteps}: '${step.caption}'")
        callback?.onStepChanged(step)
        
        // Auto-advance after showing pointer + TTS
        // Don't auto-tap — just show where to tap, speak it, then move on
        scope.launch {
            delay(4000)
            if (state == BuddyState.GUIDING && currentStepIndex == step.stepNumber - 1) {
                Log.d(TAG, "Auto-advancing past step ${step.stepNumber}")
                nextStep()
            }
        }
    }
    
    private fun setState(newState: BuddyState) {
        state = newState
        callback?.onStateChanged(newState)
    }
    
    override fun onScreenChanged(packageName: String, uiTree: UiNode?) {
        if (state == BuddyState.GUIDING) {
            screenChangeCount++
            if (screenChangeCount >= 3) {
                screenChangeCount = 0
                nextStep()
            }
        }
    }
    
    override fun onTargetClicked() {
        if (state == BuddyState.GUIDING) {
            screenChangeCount = 0
            nextStep()
        }
    }
    
    fun destroy() {
        scope.cancel()
    }
}
