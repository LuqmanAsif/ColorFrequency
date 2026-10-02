# Color Frequency

Color Frequency is an immersive, fast-paced arcade reflex and rhythm game built for Android using pure Java and custom 2D Canvas rendering.

## Features

- **Dynamic Color-Matching Gameplay**: Shrinking colored rings converge on a rotating central core. Tap anywhere to rotate and match the correct color before impact.
- **Procedurally Generated Audio**: Features an adaptive, real-time synthesized audio loop (`AudioTrack`) that speeds up and shifts tempo dynamically as your score increases.
- **High Score Tracking**: Persistent local high scores managed via `SharedPreferences`.
- **Revive System**: Integrated with Google AdMob rewarded ads to give players a second chance after reaching milestone scores.
- **Modern Arcade Styling**: Sleek dark mode aesthetics featuring glowing neon color palettes, smooth 60 FPS animations, and Material Design touches.
- **Tutorial & About Dialogs**: Clear "How to Play" instructions and an "About" overview.

---

## How to Play

1. **Tap Anywhere**: Each tap rotates the central core 90 degrees clockwise.
2. **Match Colors**: Ensure the color under the top marker matches the incoming shrinking ring.
3. **Survive & Score**: Every successful match increases your score. Missing a ring ends the game.
4. **Speed Scales**: As your score grows, the game accelerates and the procedural music intensifies!

---

## Tech Stack & Architecture

- **Language**: Java 17+
- **UI Framework**: Android Views & Custom `GameView` (Canvas & Path rendering)
- **Audio**: Low-latency PCM synthesis using `AudioTrack`
- **Ads**: Google Mobile Ads SDK (AdMob Rewarded Ads)
- **Architecture**: Lightweight Model-View-Controller / Event Listener pattern

---

## Getting Started

1. Open the project in **Android Studio**.
2. Sync project with Gradle files.
3. Run the app on an Android emulator or physical device (API 24+ recommended).
