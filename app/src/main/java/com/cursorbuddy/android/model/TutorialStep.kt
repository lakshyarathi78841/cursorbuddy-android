package com.cursorbuddy.android.model

import android.graphics.RectF

data class TutorialStep(
    val stepNumber: Int,
    val totalSteps: Int,
    val action: StepAction,
    val targetBounds: RectF?,
    val targetDescription: String,
    val caption: String,
    val confidence: Float = 1.0f
)

enum class StepAction {
    TAP, LONG_PRESS, TYPE, SCROLL, SWIPE, WAIT
}

data class Tutorial(
    val id: String,
    val appPackage: String,
    val question: String,
    val steps: List<TutorialStep>,
    val source: TutorialSource = TutorialSource.HARDCODED
)

enum class TutorialSource {
    HARDCODED, AI_GENERATED, CURATED
}
