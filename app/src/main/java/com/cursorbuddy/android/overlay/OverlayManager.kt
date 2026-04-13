package com.cursorbuddy.android.overlay

import android.content.Context
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.graphics.RectF
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import com.cursorbuddy.android.animation.PointerAnimator
import com.cursorbuddy.android.model.BuddyState
import com.cursorbuddy.android.model.TutorialStep
import com.cursorbuddy.android.model.Tutorial
import com.cursorbuddy.android.model.UiNode
import com.cursorbuddy.android.service.AppDetector
import com.cursorbuddy.android.service.ScreenAnalyzer
import com.cursorbuddy.android.service.ScreenCapturer
import com.cursorbuddy.android.service.VoiceEngine
import com.cursorbuddy.android.tutorial.TutorialEngine

class OverlayManager(private val context: Context) :
    FloatingBubble.BubbleListener,
    InputPanel.InputListener,
    TutorialControls.ControlListener,
    PointerAnimator.AnimationCallback,
    TutorialEngine.TutorialCallback,
    VoiceEngine.VoiceCallback {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val handler = Handler(Looper.getMainLooper())
    
    private val isDark = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
    
    // Views
    private var bubble: FloatingBubble? = null
    private var inputPanel: InputPanel? = null
    private var pointerView: PointerView? = null
    private var captionBubble: CaptionBubble? = null
    private var targetHighlight: TargetHighlight? = null
    private var tutorialControls: TutorialControls? = null
    
    // Engines
    private val pointerAnimator = PointerAnimator()
    private val tutorialEngine = TutorialEngine(context)
    private val screenCapturer = ScreenCapturer(context)
    private val voiceEngine = VoiceEngine(context)
    
    // State
    private var bubbleParams: WindowManager.LayoutParams? = null
    private var isShowing = false
    private var lastBubbleX = 0
    private var lastBubbleY = 0
    
    // Cache the UI tree + package before opening input panel
    private var cachedUiTree: UiNode? = null
    private var cachedPackage: String? = null

    init {
        pointerAnimator.callback = this
        tutorialEngine.callback = this
        tutorialEngine.setScreenCapturer(screenCapturer)
        voiceEngine.callback = this
        ScreenAnalyzer.listener = tutorialEngine
    }
    
    fun setApiKey(key: String) {
        tutorialEngine.setApiKey(key)
    }

    fun show() {
        if (isShowing) return
        isShowing = true
        showBubble()
    }

    fun hide() {
        isShowing = false
        voiceEngine.destroy()
        removeBubble()
        hideInputPanel()
        hidePointer()
        hideCaptionBubble()
        hideTargetHighlight()
        hideTutorialControls()
    }

    // ==================== BUBBLE ====================

    private fun showBubble() {
        if (bubble != null) return
        bubble = FloatingBubble(context).apply { listener = this@OverlayManager }

        val screenW = context.resources.displayMetrics.widthPixels
        val screenH = context.resources.displayMetrics.heightPixels
        val bubbleSize = dp(84)
        // Half-pop from the right edge: only ~58% of the lens is on-screen.
        lastBubbleX = screenW - (bubbleSize * 58 / 100)
        lastBubbleY = screenH - dp(220)

        bubbleParams = WindowManager.LayoutParams(
            bubbleSize, bubbleSize,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = lastBubbleX; y = lastBubbleY
        }
        enableBackdropBlur(bubbleParams!!, dp(18))
        windowManager.addView(bubble, bubbleParams)
    }

    private fun removeBubble() {
        bubble?.let { try { windowManager.removeView(it) } catch (_: Exception) {} }
        bubble = null
    }

    private fun hideBubble() {
        bubble?.let { try { windowManager.removeView(it) } catch (_: Exception) {} }
        bubble = null
    }

    // ==================== INPUT PANEL ====================

    private fun showInputPanel() {
        if (inputPanel != null) return
        inputPanel = InputPanel(context).apply { inputListener = this@OverlayManager }

        val currentPkg = cachedPackage ?: ScreenAnalyzer.getCurrentPackage()
        if (currentPkg != null) {
            val appInfo = AppDetector.detect(currentPkg, context)
            inputPanel?.setGreeting(AppDetector.getAppGreeting(appInfo))
            inputPanel?.setQuickPrompts(AppDetector.getQuickPrompts(appInfo))
        }

        val screenW = context.resources.displayMetrics.widthPixels
        val screenH = context.resources.displayMetrics.heightPixels
        val panelWidth = (screenW * 0.88f).toInt().coerceAtMost(dp(380))
        
        var panelX = lastBubbleX - panelWidth + dp(32)
        var panelY = lastBubbleY - dp(180)
        if (panelX < dp(8)) panelX = dp(8)
        if (panelX + panelWidth > screenW - dp(8)) panelX = screenW - dp(8) - panelWidth
        if (panelY < dp(40)) panelY = lastBubbleY + dp(64)
        if (panelY + dp(180) > screenH) panelY = screenH - dp(200)

        val params = WindowManager.LayoutParams(
            panelWidth, WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = panelX; y = panelY
            softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN
        }

        enableBackdropBlur(params, dp(28))
        windowManager.addView(inputPanel, params)
        inputPanel?.alpha = 0f; inputPanel?.scaleX = 0.9f; inputPanel?.scaleY = 0.9f
        inputPanel?.animate()?.alpha(1f)?.scaleX(1f)?.scaleY(1f)?.setDuration(150)?.start()
        
        handler.postDelayed({
            inputPanel?.requestInputFocus()
            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            @Suppress("DEPRECATION")
            imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0)
        }, 200)
    }

    private fun hideInputPanel() {
        inputPanel?.let {
            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(it.windowToken, 0)
            try { windowManager.removeView(it) } catch (_: Exception) {}
        }
        inputPanel = null
    }

    // ==================== POINTER ====================

    private fun showPointer() {
        if (pointerView != null) return
        pointerView = PointerView(context).apply {
            animateLensBaseScale(from = 0.72f, to = 0.82f)
        }
        val params = WindowManager.LayoutParams(
            dp(80), dp(80),
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = context.resources.displayMetrics.widthPixels / 2
            y = context.resources.displayMetrics.heightPixels / 2
        }
        enableBackdropBlur(params, dp(22))
        windowManager.addView(pointerView, params)
    }

    private fun hidePointer() {
        pointerAnimator.cancelAll()
        pointerView?.let { try { windowManager.removeView(it) } catch (_: Exception) {} }
        pointerView = null
    }

    private fun movePointerTo(x: Float, y: Float) {
        pointerView?.let { view ->
            val params = view.layoutParams as? WindowManager.LayoutParams ?: return
            params.x = (x - dp(40)).toInt()
            params.y = (y - dp(40)).toInt()
            try { windowManager.updateViewLayout(view, params) } catch (_: Exception) {}
        }
    }

    // ==================== CAPTION ====================

    private fun showCaptionBubble(text: String, stepNum: Int, totalSteps: Int, targetBounds: RectF?) {
        hideCaptionBubble()
        captionBubble = CaptionBubble(context).apply { setCaption(text, stepNum, totalSteps) }

        val screenHeight = context.resources.displayMetrics.heightPixels
        val captionY = if (targetBounds != null && targetBounds.top > screenHeight / 2) {
            (targetBounds.top - dp(100)).toInt()
        } else if (targetBounds != null) {
            (targetBounds.bottom + dp(16)).toInt()
        } else {
            screenHeight / 3
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = captionY
        }
        enableBackdropBlur(params, dp(24))
        windowManager.addView(captionBubble, params)
        captionBubble?.alpha = 0f
        captionBubble?.animate()?.alpha(1f)?.setDuration(200)?.start()
    }

    private fun hideCaptionBubble() {
        captionBubble?.let { try { windowManager.removeView(it) } catch (_: Exception) {} }
        captionBubble = null
    }

    // ==================== TARGET HIGHLIGHT ====================

    private fun showTargetHighlight(bounds: RectF) {
        hideTargetHighlight()
        targetHighlight = TargetHighlight(context)

        val pad = dp(8)
        val params = WindowManager.LayoutParams(
            (bounds.width() + pad * 2).toInt(),
            (bounds.height() + pad * 2).toInt(),
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = (bounds.left - pad).toInt()
            y = (bounds.top - pad).toInt()
        }
        windowManager.addView(targetHighlight, params)
    }

    private fun hideTargetHighlight() {
        targetHighlight?.let { try { windowManager.removeView(it) } catch (_: Exception) {} }
        targetHighlight = null
    }

    // ==================== TUTORIAL CONTROLS ====================

    private fun showTutorialControls() {
        if (tutorialControls != null) return
        tutorialControls = TutorialControls(context).apply { controlListener = this@OverlayManager }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT, dp(52),
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            y = dp(32)
        }
        enableBackdropBlur(params, dp(24))
        windowManager.addView(tutorialControls, params)
    }

    private fun hideTutorialControls() {
        tutorialControls?.let { try { windowManager.removeView(it) } catch (_: Exception) {} }
        tutorialControls = null
    }

    // ==================== BubbleListener ====================

    override fun onBubbleTapped() {
        if (inputPanel != null) {
            hideInputPanel()
        } else {
            cachedPackage = ScreenAnalyzer.getCurrentPackage()
            cachedUiTree = ScreenAnalyzer.getCurrentUiTree()
            android.util.Log.d("CursorBuddy", "Bubble tapped: cached pkg=$cachedPackage hasTree=${cachedUiTree != null} nodes=${cachedUiTree?.flatten()?.size ?: 0}")
            showInputPanel()
        }
    }

    override fun onBubbleLongPressed() {}
    override fun onBubbleMoved(x: Int, y: Int) {
        lastBubbleX = x; lastBubbleY = y
        bubbleParams?.let { p -> p.x = x; p.y = y; try { windowManager.updateViewLayout(bubble, p) } catch (_: Exception) {} }
    }
    override fun onBubblePosition(x: Int, y: Int) {
        lastBubbleX = bubbleParams?.x ?: x
        lastBubbleY = bubbleParams?.y ?: y
    }

    // ==================== InputListener ====================

    override fun onQuestionSubmitted(question: String) {
        hideInputPanel()
        if (!com.cursorbuddy.android.service.CursorBuddyAccessibilityService.isRunning) {
            showCaptionBubble("Accessibility service not connected. Enable CursorBuddy in Settings > Accessibility.", 0, 0, null)
            handler.postDelayed({ hideCaptionBubble() }, 5000)
            return
        }
        tutorialEngine.startTutorialWithCache(question, cachedPackage, cachedUiTree)
    }

    override fun onDismissed() { hideInputPanel() }

    override fun onMicTapped() {
        inputPanel?.setListeningState(true)
        voiceEngine.startListening()
    }

    // ==================== ControlListener ====================

    override fun onPause() { tutorialEngine.pause(); voiceEngine.stopSpeaking() }
    override fun onResume() { tutorialEngine.resume() }
    override fun onPrevious() { tutorialEngine.previousStep() }
    override fun onNext() { tutorialEngine.nextStep() }
    override fun onReplay() { tutorialEngine.replayCurrent() }
    override fun onExecute() { tutorialEngine.executeCurrentStep() }
    override fun onClose() {
        tutorialEngine.stop()
        voiceEngine.stopSpeaking()
        hidePointer(); hideCaptionBubble(); hideTargetHighlight(); hideTutorialControls()
    }

    // ==================== PointerAnimator.AnimationCallback ====================

    override fun onPointerMoved(x: Float, y: Float, progress: Float) { movePointerTo(x, y) }
    override fun onPointerArrived(targetBounds: RectF) { showTargetHighlight(targetBounds) }
    override fun onPulseUpdate(scale: Float) {
        pointerView?.pulseScale = scale
        pointerView?.invalidate()
    }

    // ==================== TutorialEngine.TutorialCallback ====================

    override fun onStateChanged(state: BuddyState) {
        handler.post {
            when (state) {
                BuddyState.ANALYZING -> showCaptionBubble("Analyzing screen...", 0, 0, null)
                BuddyState.GUIDING -> {
                    hideBubble()
                    hideTutorialControls()
                    showPointer()
                }
                BuddyState.PAUSED -> {
                    pointerAnimator.cancelAll()
                    tutorialControls?.setPaused(true)
                }
                BuddyState.COMPLETE -> {
                    handler.postDelayed({
                        hidePointer(); hideCaptionBubble(); hideTargetHighlight(); hideTutorialControls()
                        showBubble()
                    }, 2000)
                }
                BuddyState.IDLE -> {
                    hidePointer(); hideCaptionBubble(); hideTargetHighlight(); hideTutorialControls()
                    showBubble()
                }
                else -> {}
            }
        }
    }

    override fun onStepChanged(step: TutorialStep) {
        handler.post {
            hideCaptionBubble()
            hideTargetHighlight()
            val bounds = step.targetBounds
            if (bounds != null) {
                pointerAnimator.animateToTarget(bounds)
            }
            showCaptionBubble(step.caption, step.stepNumber, step.totalSteps, bounds)
            // Speak the caption
            voiceEngine.speak(step.caption)
        }
    }

    override fun onTutorialComplete(tutorial: Tutorial) {
        handler.post {
            val msg = "All done! Tap the cursor to ask another question."
            showCaptionBubble(msg, 0, 0, null)
            voiceEngine.speak(msg)
        }
    }

    override fun onError(message: String) {
        handler.post {
            showCaptionBubble(message, 0, 0, null)
            voiceEngine.speak(message)
            handler.postDelayed({ hideCaptionBubble() }, 4000)
        }
    }

    // ==================== VoiceEngine.VoiceCallback ====================

    override fun onListeningStarted() {
        handler.post { inputPanel?.setListeningState(true) }
    }
    override fun onPartialResult(text: String) {
        handler.post { inputPanel?.setPartialText(text) }
    }
    override fun onResult(text: String) {
        handler.post {
            inputPanel?.setListeningState(false)
            inputPanel?.setPartialText(text)
            // Auto-submit after voice input
            handler.postDelayed({ onQuestionSubmitted(text) }, 300)
        }
    }
    override fun onListeningError(error: String) {
        handler.post {
            inputPanel?.setListeningState(false)
            showCaptionBubble(error, 0, 0, null)
            handler.postDelayed({ hideCaptionBubble() }, 3000)
        }
    }
    override fun onSpeakingStarted() {}
    override fun onSpeakingDone() {}

    private fun dp(value: Int): Int = (value * context.resources.displayMetrics.density).toInt()

    /**
     * Backdrop blur is intentionally disabled.
     *
     * Jason called it: blurring the whole screen behind the overlays looks wrong.
     * We keep the frosting in the panel styling itself instead of using
     * FLAG_BLUR_BEHIND.
     */
    @Suppress("UNUSED_PARAMETER")
    private fun enableBackdropBlur(params: WindowManager.LayoutParams, radiusPx: Int) {
        // Deliberately a no-op.
    }
}
