# CursorBuddy.app — Product Requirements Document & Technical Specification

**Version:** 1.0 Draft
**Author:** Jason
**Date:** April 11, 2026
**Status:** Proposal

---

## 1. Executive Summary

CursorBuddy is an Android application that provides an always-available floating assistant overlay. When a user taps the floating button and asks a question — "How do I do this?" or "Show me what this app does" — CursorBuddy analyzes the current screen using an on-device vision model, determines the steps needed, and then animates a virtual pointer across the screen with caption bubbles explaining each action. The result is a live, contextual, interactive tutorial that teaches the user how to accomplish tasks on any app, right on their device.

### Why This Matters

Over 3 billion Android devices are in use globally. Many users — particularly older adults, people in emerging markets, and those with low digital literacy — struggle to learn new apps. Existing solutions (YouTube tutorials, help articles) require leaving the app, breaking context. CursorBuddy teaches users in-place, in real-time, without ever leaving the screen they're trying to understand.

---

## 2. Vision & Principles

**Vision:** Make every Android app self-explanatory by providing a personal, AI-powered guide that lives on your screen.

**Design Principles:**

1. **Zero-context-switch learning** — The user never leaves the app they're trying to learn. CursorBuddy teaches them right where they are.
2. **Show, don't tell** — Animated pointer movements and highlighted UI targets are more intuitive than walls of text.
3. **Privacy-first, on-device AI** — Screen content stays on the device. No screenshots are uploaded to the cloud.
4. **Progressive trust** — Start with observation and explanation. Only take action (auto-tapping) with explicit user consent.
5. **Graceful degradation** — If the AI can't confidently determine the steps, it says so honestly and suggests alternatives rather than guessing.

---

## 3. Target Users

### 3.1 Primary Personas

| Persona | Description | Key Need |
|---------|-------------|----------|
| **Digital Newcomer (Aisha, 58)** | Recently got her first smartphone. Overwhelmed by apps like WhatsApp, banking apps. | Step-by-step guidance through basic tasks she's afraid to try alone. |
| **App Explorer (Marco, 24)** | Tech-comfortable but impatient. Downloads many apps, abandons ones he can't figure out in 30 seconds. | Quick "show me what this app can do" overviews to decide if an app is worth keeping. |
| **Accessibility User (David, 41)** | Has cognitive disabilities that make complex multi-step flows difficult to remember. | Repeatable, patient walkthroughs he can trigger as many times as needed. |
| **Support Reducer (IT Admin, Priya)** | Manages devices for a fleet of employees. Tired of answering "how do I…" questions. | A deployable assistant that reduces tier-1 support tickets. |

### 3.2 Non-Target Users (v1)

- Power users who want automation/macros (this is teaching, not automation)
- iOS users (Android-only in v1)
- Users seeking voice-only, hands-free control (v1 requires visual attention)

---

## 4. User Stories & Scenarios

### 4.1 Core User Stories

**US-1: Ask a question about the current screen**
> As a user, I want to tap the CursorBuddy bubble and ask "How do I send a photo in this chat?" so that CursorBuddy shows me exactly where to tap and what to do, step by step.

**US-2: Get an app overview**
> As a user who just installed a new app, I want to say "Show me what this app does" so that CursorBuddy gives me a guided tour of the main features.

**US-3: Replay a tutorial**
> As a user, I want to replay a tutorial I saw earlier so that I can reinforce my learning without having to ask again.

**US-4: Pause and resume**
> As a user, I want to pause a tutorial mid-step, do something else, and resume where I left off.

**US-5: Skip ahead**
> As a user, I want to skip steps I already know so the tutorial respects my time.

**US-6: Get help in my language**
> As a user, I want CursorBuddy to explain things in my preferred language so I can learn comfortably.

### 4.2 Walkthrough Scenario

1. User opens their banking app and sees the home screen.
2. User taps the floating CursorBuddy bubble (bottom-right corner).
3. A small input panel slides up: microphone button + text field. User says: *"How do I set up automatic bill payments?"*
4. CursorBuddy briefly shows "Analyzing screen…" (< 1 second).
5. A translucent overlay appears. An animated pointer (a friendly glowing dot) slides from the center of the screen to the "Pay & Transfer" tab, with a caption bubble: **"First, tap Pay & Transfer."**
6. The pointer pulses on the target. The user taps it.
7. CursorBuddy detects the screen changed. The pointer now moves to "Scheduled Payments" with caption: **"Now tap Scheduled Payments to set up recurring bills."**
8. This continues step by step until the task is complete.
9. A final bubble appears: **"All done! Your automatic payment is set up. Want me to walk you through anything else?"**

---

## 5. Feature Specification

### 5.1 Floating Action Bubble (FAB)

| Attribute | Detail |
|-----------|--------|
| **Appearance** | Semi-transparent circular bubble (48dp), with a subtle pulsing animation when idle. Customizable icon/color. |
| **Position** | Draggable to any screen edge. Remembers last position. Defaults to bottom-right. |
| **States** | Idle → Listening → Thinking → Guiding → Minimized |
| **Interaction** | Single tap opens input panel. Long-press opens settings. Drag repositions. |
| **Persistence** | Visible across all apps (uses Android overlay permission). Can be hidden per-app in settings. |

### 5.2 Input Panel

- **Voice input** — Tap microphone, speak naturally. Uses Android's on-device speech recognition (no network needed).
- **Text input** — Type a question if voice isn't convenient.
- **Quick prompts** — Contextual suggested questions based on the current app (e.g., on Gmail: "How do I schedule an email?", "Show me how to add an attachment").
- **Language** — Supports the same languages as the device's speech recognizer. Text captions are generated by the on-device model in the user's language.

### 5.3 Screen Analysis Engine

The core intelligence: an on-device vision-language model that understands what's on screen.

**Capabilities:**
- Identify UI elements (buttons, text fields, tabs, menus, toggles) and their labels
- Understand the current app and screen context (e.g., "this is the Gmail compose screen")
- Map a user's natural-language question to a sequence of UI actions
- Detect when the screen has changed (to advance to the next step)

**Approach:**
- Leverages Android's **Accessibility Service API** to get the UI tree (element labels, types, bounds) — this is structured data, not pixel-level vision
- Supplements with an on-device vision model (e.g., Gemini Nano, or a fine-tuned MobileVLM) for apps with poor accessibility labeling (custom views, games, canvas-based UI)
- Combines the UI tree + visual understanding + user question to produce a step-by-step action plan

### 5.4 Animated Pointer & Caption System

This is the signature UX of CursorBuddy — the animated pointer that "shows" the user what to do.

**Pointer:**
- A glowing circular cursor (customizable: color, size, trail effect)
- Animates smoothly from its current position to the next target element
- Uses easing curves (ease-in-out) for natural, human-like movement
- Pauses and pulses on the target to draw attention
- Optional: leaves a fading trail to show the path taken

**Caption Bubbles:**
- Speech-bubble style overlays anchored near the target element
- Short, clear text (aim for < 15 words per caption)
- Numbered step indicator ("Step 2 of 5")
- Auto-positioned to avoid covering the target element
- Support for rich content: bold text, small illustrations, and arrows

**Animation Timeline:**
1. Pointer starts at center (or last position)
2. Animate to target (300–500ms)
3. Pulse on target + show caption bubble (hold until user acts or taps "Next")
4. User performs the action (or taps "Skip")
5. Detect screen change → transition to next step

### 5.5 Tutorial Playback Controls

A minimal floating toolbar that appears during a tutorial:

| Control | Function |
|---------|----------|
| ⏸ Pause | Freeze the tutorial at the current step |
| ⏭ Skip | Jump to the next step |
| ⏮ Back | Go back to the previous step |
| 🔄 Replay | Replay the current step's animation |
| ✕ Close | End the tutorial |
| 🐢 / 🐇 Speed | Slow down or speed up pointer animations |

### 5.6 Curated Tutorial Library

While the AI generates tutorials dynamically for any question, popular apps benefit from human-verified, pre-built tutorial flows.

**Library Structure:**
- **App Packs** — curated sets of tutorials for popular apps (WhatsApp, Chrome, Settings, Camera, YouTube, Google Maps, banking apps)
- **Task Templates** — common cross-app tasks ("How to share a photo," "How to change notification settings") with per-app implementations
- **Version-Aware** — tutorials are tagged with app versions; the system selects the correct flow based on the installed version

**Content Pipeline:**
1. **Authoring Tool** — A companion desktop/web app where content creators record tutorial flows by stepping through an app on an emulator. The tool captures the UI tree, screenshots, and action sequence.
2. **Review & QA** — Human reviewers verify accuracy and clarity. Caption text is reviewed for tone and localization.
3. **Publishing** — Approved tutorials are bundled and pushed to devices via a lightweight sync (delta updates, not full downloads).
4. **AI Gap-Filling** — When a user asks a question with no curated match, the AI generates a tutorial dynamically. High-quality AI-generated tutorials can be flagged for human review and promoted into the curated library.

**Fallback Order:**
1. Curated tutorial (exact match for app + version + question) → highest confidence
2. Curated tutorial (adapted to different version via AI) → high confidence
3. Fully AI-generated tutorial → medium confidence (displayed with a subtle "AI-generated" badge)

### 5.7 History & Favorites

- **Tutorial History** — Log of all tutorials triggered, searchable by app, date, and question.
- **Favorites** — Pin frequently-needed tutorials for one-tap access.
- **Offline Replay** — Completed tutorials are cached locally and can be replayed without re-analyzing the screen.

### 5.8 Settings & Customization

- Pointer appearance (color, size, trail, speed)
- Caption font size and language
- Voice vs. text input preference
- Per-app visibility of the floating bubble
- Accessibility options: high-contrast captions, haptic feedback on each step, TalkBack integration
- Data: clear tutorial cache, export history

---

## 6. Technical Architecture

### 6.1 High-Level System Diagram

```
┌─────────────────────────────────────────────────────┐
│                    Android Device                     │
│                                                       │
│  ┌─────────────┐   ┌──────────────────────────────┐  │
│  │  Any App     │   │     CursorBuddy Service       │  │
│  │  (Foreground)│   │                                │  │
│  │             │◄──┤  ┌────────────────────────┐    │  │
│  │             │   │  │  Accessibility Service   │    │  │
│  │             │   │  │  (reads UI tree)         │    │  │
│  └─────────────┘   │  └────────┬───────────────┘    │  │
│                     │           │                     │  │
│  ┌─────────────┐   │  ┌────────▼───────────────┐    │  │
│  │  Overlay     │   │  │  Screen Analyzer        │    │  │
│  │  (Pointer +  │◄──┤  │  (On-device VLM +       │    │  │
│  │   Captions)  │   │  │   UI tree parser)        │    │  │
│  └─────────────┘   │  └────────┬───────────────┘    │  │
│                     │           │                     │  │
│  ┌─────────────┐   │  ┌────────▼───────────────┐    │  │
│  │  Input Panel │   │  │  Tutorial Planner       │    │  │
│  │  (Voice/Text)│──►│  │  (Step sequencer +      │    │  │
│  └─────────────┘   │  │   curated library)       │    │  │
│                     │  └────────┬───────────────┘    │  │
│                     │           │                     │  │
│                     │  ┌────────▼───────────────┐    │  │
│                     │  │  Animation Engine        │    │  │
│                     │  │  (Pointer choreography)  │    │  │
│                     │  └────────────────────────┘    │  │
│                     └──────────────────────────────┘  │
└─────────────────────────────────────────────────────┘
```

### 6.2 Core Android Components

#### 6.2.1 Accessibility Service

**Why:** The Accessibility Service API is the only sanctioned way to read another app's UI tree without root access. It provides `AccessibilityNodeInfo` objects for every visible UI element — including text, content descriptions, class names, clickable state, and screen bounds.

**Implementation:**
```kotlin
class CursorBuddyAccessibilityService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                // Notify the Screen Analyzer that the UI has changed
                val rootNode = rootInActiveWindow ?: return
                ScreenAnalyzer.onUiTreeUpdated(rootNode, event.packageName)
            }
        }
    }

    override fun onServiceConnected() {
        val config = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                         AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS or
                    AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS
            notificationTimeout = 200
        }
        serviceInfo = config
    }
}
```

**Key considerations:**
- Users must explicitly enable the Accessibility Service in system settings. CursorBuddy should have a clear onboarding flow explaining why this permission is needed and what it does (and doesn't do) with their data.
- The `TYPE_WINDOW_CONTENT_CHANGED` event fires frequently; debounce processing to avoid performance issues (200ms timeout above).
- The UI tree is the primary signal; screen pixel analysis via the vision model is a fallback for apps with poor accessibility markup.

#### 6.2.2 Overlay Window (System Alert Window)

**Why:** To draw the pointer, captions, and controls on top of other apps, CursorBuddy uses the `SYSTEM_ALERT_WINDOW` permission (`Settings.canDrawOverlays()`).

**Implementation:**
```kotlin
class OverlayManager(private val context: Context) {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private lateinit var overlayView: CursorBuddyOverlayView

    fun showOverlay() {
        overlayView = CursorBuddyOverlayView(context)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            // Pass through touches except on our UI elements
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )

        windowManager.addView(overlayView, params)
    }
}
```

**Critical detail — touch passthrough:**
The overlay must be transparent to touches everywhere except the pointer, captions, and controls. This is achieved by:
- Setting `FLAG_NOT_TOUCH_MODAL` and `FLAG_NOT_FOCUSABLE` on the window
- Overriding `dispatchTouchEvent()` in the overlay view to only consume touches on CursorBuddy's own UI elements
- All other touches pass through to the app beneath

This is essential — the user must be able to interact with the underlying app while the tutorial is active.

#### 6.2.3 Foreground Service

The CursorBuddy background service must run as a **foreground service** (with a persistent notification) to avoid being killed by the system. This is required on Android 8+ for long-running background work.

```kotlin
class CursorBuddyService : Service() {

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = createNotification()  // "CursorBuddy is ready to help"
        startForeground(NOTIFICATION_ID, notification)
        return START_STICKY
    }
}
```

### 6.3 On-Device Vision-Language Model

#### 6.3.1 Model Strategy

**Primary approach: UI tree analysis (no vision model needed)**
For ~80% of apps, the Accessibility Service UI tree provides enough structured information (element labels, types, hierarchy) to understand the screen and plan tutorial steps. This is fast, lightweight, and requires no ML inference.

**Fallback: On-device vision model**
For apps with poor accessibility labeling (games, heavily custom UIs, canvas-based rendering), CursorBuddy falls back to a lightweight vision-language model:

| Option | Model | Size | Latency | Notes |
|--------|-------|------|---------|-------|
| **A (Recommended)** | Gemini Nano (via Android AICore) | ~2 GB | ~300ms | Natively supported on Pixel 8+, Samsung S24+. Google-managed updates. |
| **B** | Fine-tuned MobileVLM v2 | ~1.5 GB | ~500ms | Open-weight, self-hosted. Requires own fine-tuning pipeline. More control, more work. |
| **C** | Florence-2 Mobile | ~0.8 GB | ~400ms | Microsoft's compact vision model. Good at OCR and UI grounding. |

**Recommendation:** Start with **Gemini Nano via AICore** for supported devices. For unsupported devices, bundle a quantized Florence-2 or MobileVLM as fallback.

#### 6.3.2 Inference Pipeline

```
User Question
     │
     ▼
┌─────────────────┐
│ Speech-to-Text   │  (Android on-device recognizer)
│ "How do I set up │
│  bill payments?" │
└────────┬────────┘
         │
         ▼
┌─────────────────┐     ┌────────────────────┐
│ Curated Library  │────►│ Exact match found?  │──Yes──► Use curated flow
│ Lookup           │     └────────┬───────────┘
└─────────────────┘              │ No
                                  ▼
                    ┌─────────────────────────┐
                    │ UI Tree Extraction       │
                    │ (AccessibilityNodeInfo   │
                    │  → structured JSON)      │
                    └────────────┬────────────┘
                                 │
                    ┌────────────▼────────────┐
                    │ Sufficient labels?       │
                    │ (heuristic check)        │
                    └──┬──────────────┬───────┘
                       │ Yes          │ No
                       ▼              ▼
              ┌────────────┐  ┌──────────────────┐
              │ LLM-based   │  │ Vision Model      │
              │ Planner     │  │ (screenshot +     │
              │ (UI tree +  │  │  UI tree → plan)  │
              │  question)  │  └────────┬─────────┘
              └──────┬─────┘           │
                     │                  │
                     ▼                  ▼
              ┌──────────────────────────┐
              │ Step Sequence             │
              │ [{action: "tap",          │
              │   target: {bounds, label},│
              │   caption: "Tap here..."},│
              │  ...]                     │
              └──────────────────────────┘
```

#### 6.3.3 Prompt Design (for the on-device LLM planner)

The planner receives a structured representation of the screen and the user's question, and outputs a step plan:

```
<system>
You are a UI navigation assistant. Given a user's question and the current
screen's UI element tree, produce a step-by-step action plan. Each step has:
- action: "tap" | "long_press" | "type" | "scroll" | "swipe" | "wait"
- target: the element ID or description to act on
- caption: a short, friendly explanation for the user (< 15 words)
- confidence: 0.0 to 1.0

If you are not confident (< 0.6) in any step, set confident=false and
explain what you're unsure about.
</system>

<user_question>How do I set up automatic bill payments?</user_question>

<current_app>com.example.bankapp v4.2.1</current_app>

<ui_tree>
[
  {id: "tab_home", type: "Tab", text: "Home", selected: true, bounds: [0,100,180,160]},
  {id: "tab_pay", type: "Tab", text: "Pay & Transfer", selected: false, bounds: [180,100,400,160]},
  {id: "tab_invest", type: "Tab", text: "Invest", selected: false, bounds: [400,100,540,160]},
  {id: "balance_card", type: "Card", text: "Checking ••1234\n$4,521.80", bounds: [20,180,520,300]},
  ...
]
</ui_tree>
```

### 6.4 Animation Engine

#### 6.4.1 Pointer Choreography

The animation engine translates a step sequence into smooth on-screen pointer movement.

**Core animation parameters:**

```kotlin
data class PointerAnimation(
    val from: PointF,           // Start position
    val to: PointF,             // Target position (center of target element)
    val duration: Long = 400,   // milliseconds
    val easing: Interpolator = FastOutSlowInInterpolator(),
    val trailEnabled: Boolean = true,
    val pulseOnArrival: Boolean = true,
    val pulseDuration: Long = 600
)
```

**Movement algorithm:**
1. Calculate the straight-line path from current position to target
2. If the path crosses important UI elements, add a slight curve (quadratic Bézier) to route around them
3. Apply easing: accelerate from start, decelerate near target (mimics natural hand movement)
4. On arrival, scale-pulse the pointer (1.0x → 1.3x → 1.0x) and show a highlight ring around the target element

**Screen change detection:**
After the pointer arrives at a target, the system watches for the user to perform the action:
- Monitor accessibility events for `TYPE_VIEW_CLICKED` on the target element
- Monitor `TYPE_WINDOW_STATE_CHANGED` for navigation events
- Timeout after configurable period (default 15 seconds) → show a gentle reminder bubble
- If the screen changes to an unexpected state → pause and re-analyze

#### 6.4.2 Caption Bubble Positioning

Captions must be visible but not obstructive:

```
┌──────────────────────────────┐
│                              │
│     ┌──────────────┐         │
│     │ Caption text  │         │
│     │ "Tap here to  │         │
│     │  open menu"   │         │
│     └──────┬───────┘         │
│            │ (arrow)          │
│         [TARGET ELEMENT]      │
│                              │
└──────────────────────────────┘
```

**Positioning rules (priority order):**
1. Above the target, centered — default position
2. Below the target — if the target is near the top of the screen
3. Left or right — if the target is near a vertical edge
4. The caption bubble must never overlap the target element
5. The caption bubble must never extend off-screen
6. Smooth transition animation when repositioning between steps

### 6.5 Data Architecture

#### 6.5.1 Local Storage

```
/data/data/com.cursorbuddy/
├── databases/
│   ├── tutorials.db         # Curated tutorial library (SQLite)
│   ├── history.db           # User's tutorial history
│   └── app_metadata.db      # Installed app versions, feature maps
├── models/
│   ├── planner/             # On-device LLM weights
│   └── vision/              # Vision model weights (fallback)
├── cache/
│   ├── ui_trees/            # Recent UI tree snapshots
│   └── generated_tutorials/ # AI-generated tutorials pending review
└── prefs/
    └── settings.xml         # User preferences
```

#### 6.5.2 Tutorial Schema

```json
{
  "tutorial_id": "uuid",
  "app_package": "com.example.bankapp",
  "app_version_range": ["4.0.0", "4.9.9"],
  "question_patterns": [
    "set up automatic bill payment",
    "schedule recurring payment",
    "auto-pay bills"
  ],
  "source": "curated",        // "curated" | "ai_generated" | "community"
  "language": "en",
  "steps": [
    {
      "step_number": 1,
      "action": "tap",
      "target": {
        "accessibility_id": "tab_pay_transfer",
        "fallback_text": "Pay & Transfer",
        "fallback_bounds_hint": "top_tab_bar"
      },
      "caption": "First, tap **Pay & Transfer** in the top menu.",
      "expected_screen_after": "pay_transfer_screen",
      "timeout_seconds": 15
    }
  ],
  "metadata": {
    "author": "content_team",
    "reviewed": true,
    "avg_completion_time_seconds": 45,
    "difficulty": "beginner"
  }
}
```

### 6.6 Privacy & Security Architecture

Since CursorBuddy reads screen content via the Accessibility Service, privacy is paramount.

| Principle | Implementation |
|-----------|---------------|
| **No data leaves the device** | All screen analysis happens on-device. No screenshots, UI trees, or user questions are transmitted to any server. |
| **No persistent screen capture** | UI trees are held in memory only during active analysis, then discarded. No screenshots are saved unless the user explicitly enables tutorial recording for contribution. |
| **Encrypted local storage** | Tutorial history and cached data use Android Keystore-backed encryption. |
| **Accessibility Service scope** | The service only processes events from apps the user has not excluded. A per-app blocklist lets users disable CursorBuddy for sensitive apps (banking, health). |
| **No overlay on sensitive inputs** | CursorBuddy automatically hides its overlay when it detects password fields or biometric prompts (via accessibility node `isPassword` flag). |
| **Transparent permissions** | Onboarding clearly explains each permission, what data is accessed, and provides a link to the privacy policy. |

---

## 7. UX Flows

### 7.1 First-Time Onboarding

```
Welcome Screen
  "CursorBuddy teaches you how to use any app,
   right on your screen."
        │
        ▼
Permission: Overlay
  "To show you tutorials, CursorBuddy needs to
   display over other apps."
  [Grant Permission →]
        │
        ▼
Permission: Accessibility Service
  "To understand what's on your screen, CursorBuddy
   uses Android's Accessibility Service. Your screen
   content never leaves your device."
  [Enable in Settings →]
        │
        ▼
Customize
  "Choose your pointer style and caption size."
  [Pointer: ● Blue  ● Green  ● Orange]
  [Caption size: Normal / Large / Extra Large]
        │
        ▼
Try It Out
  "Let's try it! Open any app, then tap the
   CursorBuddy bubble."
  [Start →]
```

### 7.2 Tutorial Flow State Machine

```
         ┌──────────┐
         │  IDLE     │ (Bubble visible, waiting for tap)
         └────┬─────┘
              │ user taps bubble
              ▼
         ┌──────────┐
         │ LISTENING │ (Voice/text input active)
         └────┬─────┘
              │ user submits question
              ▼
         ┌──────────┐
         │ ANALYZING │ (Screen analysis + plan generation)
         └────┬─────┘
              │ plan ready
              ▼
     ┌────────────────┐
     │ GUIDING        │◄─── (loop for each step)
     │ (Pointer moving,│
     │  caption shown) │
     └──┬───────┬─────┘
        │       │ user completes all steps
        │       ▼
        │  ┌──────────┐
        │  │ COMPLETE  │ ("All done!" + follow-up prompt)
        │  └──────────┘
        │
        │ screen changed unexpectedly
        ▼
   ┌─────────────┐
   │ RE-ANALYZING │ (Reassess current screen, adjust plan)
   └──────┬──────┘
          │ plan adjusted
          ▼
   (back to GUIDING)
```

---

## 8. Success Metrics

### 8.1 North Star Metric

**Tutorial Completion Rate** — % of started tutorials where the user successfully completes all steps.

**Target:** > 70% within 3 months of launch.

### 8.2 Supporting Metrics

| Metric | Definition | Target |
|--------|-----------|--------|
| **Daily Active Users (DAU)** | Unique users who trigger at least one tutorial per day | 100K within 6 months |
| **Tutorials per User per Week** | Average tutorials completed per active user per week | ≥ 3 |
| **Time to First Value** | Time from install to completing first tutorial | < 3 minutes |
| **AI Accuracy Rate** | % of AI-generated tutorials rated "correct" by the user (thumbs up/down) | > 85% |
| **Curated Library Hit Rate** | % of questions matched to a curated tutorial (vs. AI-generated) | > 40% |
| **Retry Rate** | % of tutorials the user replays (indicates learning value) | 15–25% |
| **Support Ticket Reduction** | For enterprise deployments: % reduction in how-to support tickets | > 30% |
| **Accessibility Service Opt-In** | % of users who complete the accessibility permission step during onboarding | > 60% |

---

## 9. Curated Tutorial Content Strategy

### 9.1 Launch Library (v1.0)

Cover the top 20 most-used Android apps with the most common tasks:

| App Category | Apps | Tutorials per App |
|-------------|------|-------------------|
| Messaging | WhatsApp, Telegram, Messages | 15–20 |
| Social | Instagram, Facebook, TikTok | 10–15 |
| Productivity | Gmail, Google Calendar, Google Drive | 10–15 |
| Browser | Chrome, Samsung Internet | 10 |
| System | Settings, Camera, Files, Phone | 20+ |
| Finance | Top 5 banking apps by region | 10 |
| Maps | Google Maps, Waze | 8 |

**Total v1 target:** ~400 curated tutorials.

### 9.2 Content Authoring Workflow

```
1. IDENTIFY  →  Analyze user questions (from AI-generated tutorial logs,
                anonymized) to find the most common tasks per app.

2. RECORD    →  Content author uses CursorBuddy Authoring Tool on an
                emulator to step through each task, capturing UI trees
                and action sequences.

3. WRITE     →  Author crafts caption text: clear, friendly, concise.
                Localization team translates to target languages.

4. TEST      →  QA runs the tutorial on 3+ device types and app versions.
                Automated regression tests check tutorials weekly.

5. PUBLISH   →  Approved tutorials pushed via delta-sync to devices.

6. MONITOR   →  Track completion rates and thumbs-down feedback.
                Tutorials below 60% completion are flagged for revision.
```

---

## 10. Monetization Model

| Tier | Price | Includes |
|------|-------|---------|
| **Free** | $0 | 5 AI-generated tutorials/day. Full access to curated library. Ads-free. |
| **Pro** | $4.99/mo | Unlimited AI tutorials. Priority model updates. Custom pointer themes. Tutorial history export. |
| **Enterprise** | Custom | MDM integration. Custom curated libraries for internal apps. Analytics dashboard. Bulk licensing. |

---

## 11. Development Milestones

### Phase 1: Foundation (Months 1–3)
- Accessibility Service integration + UI tree parser
- Overlay system with draggable floating bubble
- Basic pointer animation engine (linear movement, pulse)
- Text-input question handling
- Hardcoded demo tutorials for 3 apps (Settings, Chrome, Gmail)

### Phase 2: Intelligence (Months 4–6)
- Integrate Gemini Nano for on-device planning
- Voice input via Android speech recognizer
- Dynamic tutorial generation from AI
- Screen change detection and step advancement
- Caption bubble positioning system

### Phase 3: Content & Polish (Months 7–9)
- Build authoring tool for curated tutorials
- Create launch library (~400 tutorials)
- Tutorial history and favorites
- Pointer customization and accessibility options
- Localization (top 10 languages)
- Performance optimization (< 50MB RAM idle, < 200MB active)

### Phase 4: Launch & Learn (Months 10–12)
- Closed beta (1,000 users) → iterate on feedback
- Open beta via Google Play
- Analytics and funnel instrumentation
- Enterprise pilot program
- v1.0 public launch

---

## 12. Risks & Mitigations

| Risk | Severity | Likelihood | Mitigation |
|------|----------|-----------|------------|
| **Users won't enable Accessibility Service** — it sounds scary and Google warns about it | High | High | Crystal-clear onboarding explaining exactly what CursorBuddy does/doesn't do. Video demo. Privacy-first architecture as a trust signal. |
| **Google Play policy rejection** — Accessibility Services have strict Play Store review policies | High | Medium | Follow Google's published Accessibility Service policy to the letter. Only use the service for its stated purpose (UI understanding for tutorials). Engage with Google's review team proactively. |
| **On-device model too slow on low-end devices** | Medium | Medium | UI-tree-first approach means the vision model is a fallback, not critical path. Offer a "lite mode" that skips vision inference entirely. |
| **AI generates incorrect tutorials** | High | Medium | Confidence scoring on every step. Clear "AI-generated" badge. Thumbs-down feedback loop. Curated library as high-confidence default. |
| **App updates break curated tutorials** | Medium | High | Version-tagged tutorials. Automated regression testing against new app versions. AI can adapt curated flows to minor UI changes. |
| **Battery drain from always-on service** | Medium | Medium | Aggressive event debouncing. Service does zero work when not actively guiding. Doze-compatible. Target < 2% daily battery impact. |
| **Overlay conflicts with other floating apps** | Low | Medium | Z-order management. Auto-hide when other overlays are detected. User can adjust bubble position. |

---

## 13. Open Questions

1. **Should CursorBuddy auto-tap for the user?** — v1 only shows where to tap. A future "auto-pilot" mode could perform the taps automatically, but this raises trust and safety concerns (e.g., auto-tapping a purchase button). Needs user research.

2. **How to handle apps that detect overlays?** — Some banking and DRM apps use `FLAG_SECURE` or detect overlays and refuse to function. CursorBuddy should detect this gracefully and offer text-only instructions as a fallback.

3. **Community tutorial marketplace?** — Should users be able to create and share tutorials? This could scale content dramatically but introduces moderation challenges and quality variance.

4. **iOS version?** — iOS has much more restrictive accessibility APIs and does not allow third-party overlay windows. An iOS version would require a fundamentally different architecture (possibly using Screen Recording + on-device analysis, presented as a picture-in-picture). Worth exploring in v2.

5. **Integration with app developers?** — Could app developers embed CursorBuddy SDKs to provide first-party tutorials? This could be a compelling developer tool and an enterprise revenue channel.

---

## 14. Appendix

### A. Android Permission Matrix

| Permission | Type | Required | Purpose |
|-----------|------|----------|---------|
| `SYSTEM_ALERT_WINDOW` | Runtime (special) | Yes | Draw overlay (pointer, captions, bubble) |
| `BIND_ACCESSIBILITY_SERVICE` | System setting | Yes | Read UI tree of other apps |
| `FOREGROUND_SERVICE` | Manifest | Yes | Keep service alive |
| `RECORD_AUDIO` | Runtime | Optional | Voice input |
| `INTERNET` | Manifest | Optional | Curated library sync (no screen data transmitted) |
| `RECEIVE_BOOT_COMPLETED` | Manifest | Optional | Auto-start on boot |

### B. Device Compatibility

| Tier | Devices | Experience |
|------|---------|-----------|
| **Full** | Pixel 8+, Samsung S24+, devices with AICore | On-device VLM + UI tree. Best quality. |
| **Standard** | Any device with Android 10+, 4GB+ RAM | UI tree only (no vision fallback). Covers ~80% of use cases. |
| **Lite** | Android 8–9, 2–4GB RAM | UI tree + pre-computed tutorials only. No on-device inference. |

### C. Competitive Landscape

| Product | Approach | CursorBuddy Differentiator |
|---------|----------|---------------------------|
| Google Assistant | Voice command execution | CursorBuddy *teaches* rather than *does*. Users learn to do it themselves. |
| Samsung Galaxy Guide | Pre-built tips for Samsung apps | CursorBuddy works on *any* app, not just first-party. |
| Whatfix / WalkMe (enterprise) | Web-app onboarding overlays | These are web-only and require developer integration. CursorBuddy works on native mobile apps with zero developer involvement. |
| YouTube tutorials | Video walkthroughs | CursorBuddy is in-context and interactive. No context-switching to a video. |

---

*End of document.*