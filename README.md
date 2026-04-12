# CursorBuddy Android

Android build of CursorBuddy: a floating assistant that sits on top of any app, reads the accessibility tree, and guides the user with an animated pointer, captions, voice, and optional AI planning.

This repo is still an early prototype, but the core loop is there and it is already demoable.

## What it does

- Floating bubble overlay that stays available across apps
- Onboarding screen for required permissions
- Accessibility service that reads the current UI tree
- Optional screen capture for extra vision context
- Claude-powered tutorial planning when an Anthropic API key is provided
- Local fallback planner when no API key is configured
- Animated pointer, target highlight, and caption bubbles
- Voice input plus spoken guidance
- Tutorial controls: pause, resume, next, previous, replay, close
- Optional action execution through Android accessibility gestures

## Current status

What works now:

- Launch app, grant permissions, start the foreground service
- Tap the floating bubble and ask a question
- Cache the current app + UI tree before the input panel steals focus
- Generate a step list from the local planner or Anthropic
- Show step-by-step guidance on top of the current app

What is still rough:

- This is not polished production software yet
- The local planner is heuristic and limited
- The curated tutorial library from the PRD is not implemented
- History, favourites, and more advanced settings are not implemented
- "Privacy-first, on-device AI" is the goal, but the current AI mode is cloud-backed via Anthropic

## Requirements

- Android 8.0+ (`minSdk 26`)
- Overlay permission
- Accessibility service enabled
- Optional: screen capture permission for screenshot-based AI help
- Optional: microphone permission for voice input
- Optional: Anthropic API key for AI planning

## Build

```bash
./gradlew assembleDebug
```

Debug APK output:

```bash
app/build/outputs/apk/debug/app-debug.apk
```

Install to a connected device:

```bash
./gradlew installDebug
```

You can also open the project in Android Studio and run the `app` target directly.

## How to use it

1. Launch the app.
2. Grant **Display Over Apps**.
3. Enable the **Accessibility Service**.
4. Optionally grant **Screen Capture** for screenshot-aware AI help.
5. Optionally enter an Anthropic API key (`sk-ant-...`) and save it.
6. Tap **Start CursorBuddy**.
7. Open another app.
8. Tap the floating CursorBuddy bubble and ask what you want to do.

## How it works

1. `MainActivity` handles onboarding, permissions, and API key storage.
2. `CursorBuddyService` runs as a foreground service and owns the overlay.
3. `CursorBuddyAccessibilityService` reads the active window and feeds screen changes into `ScreenAnalyzer`.
4. `OverlayManager` shows the bubble, input panel, pointer, captions, and tutorial controls.
5. `TutorialEngine` builds a tutorial from either:
   - `ClaudeAIPlanner` using the UI tree and optional screenshot, or
   - `TutorialPlanner` as a local fallback.
6. `PointerAnimator` moves the guide pointer to each target.
7. `ActionPerformer` can execute some actions using accessibility gestures.

## Key files

```text
app/src/main/java/com/cursorbuddy/android/
├── ui/MainActivity.kt                        # onboarding + permissions
├── service/CursorBuddyService.kt            # foreground overlay service
├── service/CursorBuddyAccessibilityService.kt # UI tree access
├── service/ClaudeAIPlanner.kt               # Anthropic integration
├── service/ActionPerformer.kt               # gesture execution
├── overlay/OverlayManager.kt                # bubble, input panel, pointer, captions
├── tutorial/TutorialEngine.kt               # tutorial orchestration
└── animation/PointerAnimator.kt             # pointer movement
```

## Permissions and privacy

This app asks for scary permissions because the product literally needs them.

- **Overlay**: shows the floating bubble and tutorial UI above other apps
- **Accessibility**: reads visible UI structure so CursorBuddy can understand what is on screen
- **Screen capture**: optional, used for screenshot-based AI context
- **Microphone**: optional, used for voice input
- **Internet**: used for Anthropic API calls in AI mode

Important: if you enter an Anthropic API key, the app sends the current accessibility tree and, when enabled, a screenshot to Anthropic to generate the tutorial steps. Without an API key, it falls back to local rule-based planning.

## Tech stack

- Kotlin
- Android Views + ViewBinding
- Android Accessibility Service API
- Foreground service + system overlay windows
- Coroutines
- Anthropic Messages API

## Roadmap direction

The PRD in [`droid.md`](droid.md) is more ambitious than the current codebase. The direction is clear though:

- better screen understanding
- stronger local planning
- curated tutorial packs
- replay/history/favourites
- better accessibility and localisation
- eventually less cloud dependence, not more

## License

Same as the main CursorBuddy project: [AGPL-3.0-only](LICENSE).
