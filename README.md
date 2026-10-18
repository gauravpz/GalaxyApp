# Galaxy Games - Wear OS Multi-Game & Educational Quiz App

A clean, native Jetpack Compose for Wear OS multi-game and quiz application built for 3rd standard students (~8-10 years old) on Samsung Galaxy Watch (optimized for circular displays including Galaxy Watch 7 & 8 on Wear OS / Android 15).

Repository: [https://github.com/gauravpz/GalaxyApp](https://github.com/gauravpz/GalaxyApp)

---

## Overview

The application features offline educational games and multi-subject quizzes implemented with pure Kotlin Compose state loops, coupled with a secure background configuration synchronization worker using Android WorkManager and local playtime management.

---

## Features & Games Included

### 1. 3rd Standard Educational Quiz (Daily Quiz)
- **Subjects**:
  - 🔢 **Math**: Multiplication tables up to 10 ($7 \times 8$, $9 \times 6$), 2-digit addition with carry, subtraction with borrowing, simple division, time/geometry questions.
  - 🔤 **English**: Antonyms (Huge/Tiny, Brave/Coward), Synonyms (Glad/Happy), Irregular Plurals (Child/Children, Leaf/Leaves, Mouse/Mice), Nouns, and Articles.
  - 🇮🇳 **Hindi (हिंदी)**: विलोम शब्द (मित्र/शत्रु, सच/झूठ), समानार्थी शब्द (जल/पानी, सूर्य/सूरज), लिंग बदलो (शेर/शेरनी), एकवचन-बहुवचन (किताब/किताबें, पत्ता/पत्ते).
  - 🚩 **Marathi (मराठी)**: विरुद्धार्थी शब्द (लहान/मोठा, दिवस/रात्र, खरे/खोटे), समानार्थी शब्द (पाणी/जल, झाड/वृक्ष), लिंग बदला (मुलगा/मुलगी, वाघ/वाघीण), एकवचन-अनेकवचन (फूल/फुले, वही/वह्या).
- **Controls**: 3 large, round touch targets optimized for watch display.
- **Feedback**: Instant celebratory green / corrective red flash with concise educational explanations.

### 2. Math Dash (Quick Math Quiz)
- **Gameplay**: Rapid arithmetic equations dynamically tuned by the fetched difficulty setting.
- **Controls**: 3 interactive circular touch targets with numeric answers.
- **Scoring**: +10 points on correct answer with immediate equation refresh.

### 3. Color Memo (Simon Says Clone)
- **Gameplay**: 4-quadrant circular layout matching round watch bezels (Red, Blue, Yellow, Green).
- **Sequence Loop**: Coroutine-based sequence playback at configurable speeds (`colorBlinkSpeedMs`).

### 4. Fast Reaction Tapper (Whack-a-Mole style)
- **Gameplay**: Dynamic target indicator teleporting inside mathematically safe polar coordinates ($r \le R_{\text{safe}}$).
- **Loop Timing**: Cycles every $X$ ms based on `reactionIntervalMs`.

---

## Remote Configuration & GitHub Sync (`config.json`)

All questions and monitoring settings are defined in [`config.json`](./config.json) in this repository.

### Updating Questions Remotely
Parents can update questions anytime from **any device** (GitHub Web on phone/PC, GitHub mobile app, or git push):
1. Navigate to [`config.json`](https://github.com/gauravpz/GalaxyApp/blob/main/config.json).
2. Edit existing questions or add new ones under `questions.math`, `questions.english`, `questions.hindi`, or `questions.marathi`.
3. Commit changes to `main`.
4. When the watch connects to Wi-Fi at home, `ConfigSyncWorker` automatically downloads the latest configuration (or tap **"Sync Remote Config"** in the app hub).
5. The watch stores the entire question set in local Jetpack DataStore for **100% offline gameplay** when the kid is away from home.

### Playtime Monitoring Configuration
Configured directly in `config.json`:
```json
"monitoring": {
  "enabled": true,
  "maxDailyPlayTimeMinutes": 30,
  "breakReminderMinutes": 15,
  "trackSessionDuration": true,
  "trackSubjects": ["math", "english", "hindi", "marathi", "color_memo", "fast_tapper"],
  "syncPlayStats": true,
  "quietHours": {
    "enabled": true,
    "startHour": 21,
    "endHour": 7
  }
}
```
- **Live Playtime Badge**: Shows current daily playtime vs limit right on the watch hub (e.g., `⏱ 12/30m`).
- **Automatic Daily Reset**: Playtime tracker automatically resets at midnight each day.

---

## Automated Airplane Mode Switch Outside Home

Since normal Android 15 / Wear OS applications cannot silently toggle system Airplane Mode due to OS security permissions (`WRITE_SECURE_SETTINGS`), the recommended and most battery-efficient way to automate this on Samsung Galaxy Watch is **Samsung Modes and Routines**:

### Setup via Samsung Modes and Routines (Galaxy Phone & Watch):
1. On the paired phone, open **Settings → Modes and Routines → Routines**.
2. Tap **+ (Add Routine)**:
   - **If (Trigger)**: Tap **Wi-Fi network** → Select your **Home Wi-Fi** → Choose **Disconnected**.
     *(Alternative trigger: Location → Leaving "Home")*
   - **Then (Action)**: Tap **Flight mode (Airplane mode)** → Set to **Off**.
     *(Optional: Turn on Mobile Data / Location)*
3. Save routine as **"Leaving Home - Online Mode"**.
4. You can also create the reverse routine:
   - **If**: Connected to **Home Wi-Fi**
   - **Then**: Turn on **Flight mode** (with Wi-Fi kept ON).

---

## Build & Test Status

- **Build Target**: Android 15 (API 35), minSdk 30 (Wear OS 3.0+).
- **Compile & Packaging Verification**: Successfully built with Gradle 8.9 (`gradlew assembleDebug` passed).
- **Design Guidelines**: Strict `14-16.dp` boundary padding for Samsung Galaxy Watch circular displays.
