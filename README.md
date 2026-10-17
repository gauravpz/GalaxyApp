# Galaxy Games - Wear OS Multi-Game App

A clean, native Jetpack Compose for Wear OS multi-game application built for 8-10 year olds on Samsung Galaxy Watch (optimized for circular displays including Galaxy Watch 8).

## Overview

The application features three distinct offline games implemented with pure Kotlin Compose state loops, coupled with a secure background configuration synchronization worker using Android WorkManager.

---

## Games Included

### 1. Math Dash (Quick Math Quiz)
- **Gameplay**: Displays age-appropriate arithmetic equations (e.g., $5 + 4$, $12 - 3$, multiplication at higher tiers) dynamically tuned by the fetched configuration difficulty.
- **Controls**: 3 interactive circular touch targets with numeric answers.
- **Scoring**: Selecting the correct answer earns **+10 points** and immediately triggers a new question with celebratory visual feedback.
- **Circular Integrity**: Strict `16.dp` outer padding boundary prevents any UI elements from being clipped by the round bezel.

### 2. Color Memo (Simon Says Clone)
- **Gameplay**: 4-quadrant circular layout mapping to the watch display geometry (Top: Red, Right: Blue, Bottom: Yellow, Left: Green).
- **Sequence Loop**: Coroutine-based sequence playback blinking quadrant highlights at configurable durations (`colorBlinkSpeedMs`).
- **Input Tracking**: Validates player tap order against the active sequence array, advancing rounds on completion and managing win/loss states gracefully.
- **Circular Integrity**: Outer Box bounded with `16.dp` padding and central info hub display.

### 3. Fast Reaction Tapper (Whack-a-Mole style)
- **Gameplay**: A dynamic target indicator that teleports across random coordinates inside a mathematically safe bounded circular area.
- **Safe Area Geometry**: Coordinates are calculated using polar math ($r \le R_{\text{safe}}$) ensuring the target is never rendered outside the visible circular screen.
- **Loop Timing**: Controlled by a Kotlin `while(isPlaying)` coroutine loop cycling every $X$ milliseconds based on remote configuration (`reactionIntervalMs`).
- **Scoring**: Tapping before the target teleports awards **+10 points** and immediately resets the cycle.

---

## Configuration & Background Sync

### Remote Configuration Architecture
- **WorkManager Periodic Request**: `ConfigSyncWorker` scheduled on a **1-hour interval** with `NetworkType.CONNECTED` constraint.
- **Endpoint**: Configured for private GitHub repository (`https://github.com/{OWNER}/{REPO}/contents/config.json`).
- **Headers**:
  - `Accept: application/vnd.github.v3.raw`
  - `Authorization: Bearer YOUR_GITHUB_PAT`
  - `User-Agent: GalaxyApp-WearOS`
- **Offline Sandbox**: Game logic is strictly offline and isolated. Network communication is isolated 100% to the background worker. If network is unavailable or credentials are dummy, default configuration ensures continuous, seamless gameplay.

### Local Storage & State Management
- **Jetpack DataStore**: Configuration values (`difficulty`, `mathMaxRange`, `mathMinRange`, `reactionIntervalMs`, `colorBlinkSpeedMs`, `wordStrings`, `taskArrays`) are persisted locally.
- **Central ViewModel**: `GameViewModel` exposes configuration via Kotlin `StateFlow<GameConfig>` and updates high scores in real-time.

---

## Security & Architecture Highlights
- **Zero 3rd-Party Game Binaries**: 100% pure native Kotlin with Jetpack Compose for Wear OS.
- **No Tracking or External Engines**: No Unity, Godot, or unverified black-box libraries.
- **Galaxy Watch 8 Circular Compliance**: Standard Box layout with minimum `16.dp` outer boundary padding on every game screen.
