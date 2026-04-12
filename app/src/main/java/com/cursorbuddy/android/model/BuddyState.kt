package com.cursorbuddy.android.model

enum class BuddyState {
    IDLE,        // Bubble visible, waiting for tap
    LISTENING,   // Input panel open, waiting for question
    ANALYZING,   // Processing screen + question
    GUIDING,     // Tutorial active, pointer moving
    PAUSED,      // Tutorial paused
    COMPLETE     // Tutorial finished
}
