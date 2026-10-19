# Gemini Live Background Assistant & Floating Subtitles Overlay

A complete background execution engine and floating system overlay HUD for Google's 4 Live Gemini Models, enabling users to activate an always-on background session that listens continuously, speaks in real-time, and renders floating dual subtitles over any active application.

---

## User Review & Critical Decisions

> [!IMPORTANT]
> The following architectural decisions were confirmed in Phase 1:

- **Confirmed Background HUD Appearance**: Draggable floating pill overlay displaying live subtitle captions, animated audio waveform, current model badge, and quick mic/collapse toggles over all Android apps.
- **Confirmed Voice Mode**: Always-listening hands-free voice detection with speech pause detection and low-latency audio response streaming.
- **Confirmed Subtitle Display**: Dual subtitle format showing the user's spoken phrase alongside the model's live translated or transcribed output with timestamps.
- **Master Activation**: High-visibility "ACTIVATE BACKGROUND ASSISTANT" button on the dashboard that checks `SYSTEM_ALERT_WINDOW` permission, starts the `GeminiLiveOverlayService`, and runs in the background.

---

## 1. Overview & Core Concept

### What It Does
When the user taps **ACTIVATE BACKGROUND ASSISTANT**:
1. The app launches an Android Foreground Service (`GeminiLiveOverlayService`) with an ongoing notification.
2. A sleek, draggable floating HUD appears on top of any active screen (games, browser, social apps, YouTube, etc.).
3. The floating overlay actively listens to the user's voice (via `SpeechRecognizer` and voice activity detection) according to the configured model:
   - **Gemini 3.5 Live Translate**: Captures spoken sentences, translates them into the target language, speaks the translation out loud via TTS, and renders bilingual floating subtitles.
   - **Gemini 3.5 Transcribe Live**: Transcribes continuous audio into crisp, punctuated floating subtitles.
   - **Gemini 3.8 Live**: Engages in hands-free multimodal conversational dialogue with emotional tone and floating captions.
   - **Gemini 3.8 Live Extended Thinking**: Analyzes verbal problems, performs deep multi-step deduction, displays intermediate thinking indicators, and presents the verified answer.
4. The user can drag the floating pill anywhere on screen, minimize it to an edge bubble, or tap to open the full Control Center.

---

## 2. User Experience & Visual Design

### Floating HUD Layout & States

```
┌─────────────────────────────────────────────────────────────┐
│ 🟢 GEMINI 3.5 TRANSLATE  [||||||||]  🎙️ LIVE  [─] [✕]      │
├─────────────────────────────────────────────────────────────┤
│ 👤 USER (13:30): "Where is the departure terminal?"        │
│ 🤖 LIVE ES: "¿Dónde está la terminal de salidas?"           │
└─────────────────────────────────────────────────────────────┘
```

- **Expanded Pill State**:
  - Model indicator with glowing status dot.
  - Live audio waveform reacting to speaker volume.
  - Dual subtitle stream (Original user speech + Gemini translated/transcribed response).
  - Quick action buttons: Mute/Unmute Mic, Minimize to Pill, Close Overlay.
- **Minimized Bubble State**:
  - Compact glowing floating icon (36dp) docked to the edge of the screen that expands back on a single tap.
- **Dashboard Master Activation Banner**:
  - Prominent Neon Cyan / Emerald glowing card directly beneath the Top Bar:
    - Status: `INACTIVE (TAP TO ACTIVATE)` / `RUNNING IN BACKGROUND 🟢`
    - Action Button: `ACTIVATE BACKGROUND HUD ⚡` / `STOP SERVICE ⏹️`
    - Overlay Permission Check: Requests "Display over other apps" permission if not yet granted.

---

## 3. Key Product Decisions & Trade-Offs

- **Android WindowManager Overlay**:
  - Utilizes `TYPE_APPLICATION_OVERLAY` for Android 8.0+ (API 26+) so the floating HUD floats seamlessly over third-party apps, video players, and games.
- **Speech-to-Text & Live TTS Integration**:
  - Integrates Android's native `SpeechRecognizer` for low-latency acoustic streaming into the Gemini Live engine.
  - Uses `TextToSpeech` for hands-free audio playback matching the active model's intonation and audio output toggles.
- **Graceful Permission Handling**:
  - If the user hasn't granted `Settings.canDrawOverlays(context)`, clicking Activate launches the system permission settings with an informative prompt.

---

## 4. Technical Architecture & Data Strategy

```
┌────────────────────────────────────────────────────────┐
│                   MainActivity UI                      │
│   • Master Activate / Deactivate Button                │
│   • Model Selector & Dynamic Toggles                   │
│   • Overlay Permission Contract Launcher               │
└───────────────────────────┬────────────────────────────┘
                            │ Start / Stop Service Intent
┌───────────────────────────▼────────────────────────────┐
│              GeminiLiveOverlayService                  │
│               (Foreground Service)                     │
├────────────────────────────────────────────────────────┤
│  • WindowManager (TYPE_APPLICATION_OVERLAY)            │
│  • Floating Overlay View Hierarchy (Draggable Touch)   │
│  • SpeechRecognizer (Continuous Voice Ingestion)       │
│  • TextToSpeech Engine (Voice Audio Output)            │
│  • GeminiLiveService & PayloadBuilder                  │
└───────────────────────────┬────────────────────────────┘
                            │ Subtitle Flow / Audio Waveform
┌───────────────────────────▼────────────────────────────┐
│                 Floating HUD View                      │
│  • Drag listener (onTouch MotionEvent.ACTION_MOVE)     │
│  • Real-time Dual Subtitle Text Views                  │
│  • Animated Audio Bar Visualizer                       │
│  • Minimize / Expand / Close Controls                  │
└────────────────────────────────────────────────────────┘
```

### Component Breakdown
1. `GeminiLiveOverlayService.kt`:
   - Handles Foreground Service lifecycle, notification, WindowManager layout parameters, and touch dragging.
   - Manages continuous voice listening and speech pause detection.
   - Calls `GeminiLiveService` with current active model, toggles, and reasoning level.
2. `FloatingHudView.kt`:
   - Programmatic View layout or ComposeView attached to WindowManager with neon cyberpunk styling.
   - Displays model tag, audio equalizer, dual subtitles, and action icons.
3. `ActivationCard.kt`:
   - Interactive Compose card on `GeminiLiveControlScreen` with "ACTIVATE BACKGROUND ASSISTANT", status pill, and permission guide.
4. `AndroidManifest.xml`:
   - Register `GeminiLiveOverlayService` with `foregroundServiceType="specialUse"` or `"microphone"` and required permissions.

---

## Verification & Validation Plan
1. **Activation Verification**: Tap "ACTIVATE BACKGROUND ASSISTANT"; verify permission dialog launches if ungranted, and floating HUD appears on screen once granted.
2. **Background Persistence Verification**: Switch to Android home screen or open another app; verify floating HUD stays pinned and responsive.
3. **Continuous Voice & Dual Subtitle Verification**: Speak a sentence; verify speech recognizer captures speech, transmits to Gemini, updates dual subtitles, and speaks the response aloud.
4. **Drag & Minimize Verification**: Drag HUD across the screen; tap minimize button to dock as compact bubble and tap to restore.
5. **Deactivation**: Tap Stop or Close on HUD to cleanly dismiss overlay and release microphone/foreground service.
