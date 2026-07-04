# TryOn — Real-Time Avatar & Body Tracking Virtual Try-On App

An Android application that tracks the human body in real time through the camera and overlays garments on the user, creating a live virtual try-on experience. Users can browse a garment library, scan their own clothes with automatic background removal, estimate body measurements from their pose, and save try-on snapshots to a personal outfit gallery.

---

## Features

### Real-Time Body Tracking
- Live pose detection using **MediaPipe Pose Landmarker** (BlazePose, 33 body landmarks)
- Works with both **front and back cameras** with seamless switching
- Landmark smoothing (exponential moving average) for stable, jitter-free tracking
- On-screen skeleton overlay with a pose-detected status indicator

### Virtual Garment Try-On
- Garments are anchored to shoulder/hip landmarks and scale with the body in real time
- Placement adapts to distance from the camera and partial visibility (hips out of frame)
- Toggle the overlay on/off during the session
- Works for tops, outerwear, bottoms, and dresses

### Garment Scanning
- Scan any real garment with the camera (place it flat on a light surface)
- **Automatic background removal** — multi-pass pipeline: color classification, connected-component flood fill, hole filling, edge dilation, and alpha feathering
- Transparent margins are trimmed so overlays fit the body precisely
- Scanned garments are persisted and survive app restarts

### Body Measurements
- Automatic estimation of shoulder width, torso length, and other measurements from pose landmarks
- Measurement history stored locally

### Outfit Gallery
- **Save Outfit** captures the live camera view with the garment overlay (screenshot-based capture via PixelCopy)
- Grid gallery of saved looks with name and timestamp
- Full-screen viewer with **pinch-to-zoom and pan**
- Delete with confirmation

### Avatar Profile
- Personal avatar with editable name and body details, stored locally

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin 2.0 |
| UI | Jetpack Compose + Material 3 |
| Architecture | MVVM + Clean Architecture (data / domain / presentation) |
| Dependency Injection | Hilt |
| Camera | CameraX 1.4 (Preview, ImageAnalysis, ImageCapture) |
| ML / Pose Tracking | MediaPipe Tasks Vision 0.10.26 (16 KB page-size compatible) |
| Local Storage | Room (SQLite) + internal file storage for images |
| Navigation | Navigation Compose |
| Async | Kotlin Coroutines + Flow |

---

## Project Structure

```
app/src/main/java/com/vikas/tryon/
├── data/
│   ├── local/          # Room database, entities, DAOs
│   ├── model/          # Domain models (Garment, Avatar, BodyMeasurement)
│   └── repository/     # Garment, Avatar, Outfit repositories
├── di/                 # Hilt modules
├── domain/usecase/     # Business logic use cases
├── navigation/         # Navigation graph and routes
├── presentation/
│   ├── home/           # Home dashboard
│   ├── camera/         # Live try-on: camera, pose overlay, garment overlay
│   ├── garment/        # Garment library and selection
│   ├── scan/           # Garment scanning with background removal
│   ├── measurement/    # Body measurement screen
│   ├── outfit/         # Saved outfit gallery + full-screen viewer
│   └── avatar/         # Avatar profile
├── ui/theme/           # Compose theme
└── utils/              # Pose smoothing, background removal, bitmap utilities
```

---

## Setup Instructions

### Prerequisites
- **Android Studio** Ladybug (2024.2) or newer
- **JDK 11+** (bundled with Android Studio)
- Android device or emulator running **Android 7.0 (API 24)** or higher
  - A physical device is strongly recommended — camera-based tracking does not work well on emulators

### 1. Clone the repository
```bash
git clone https://github.com/vikas9489/SMARRTIFAIAssignment.git
cd SMARRTIFAIAssignment
```

### 2. Add the MediaPipe pose model
The pose model is not committed to the repository. Download **pose_landmarker_lite.task** from the official MediaPipe model page:

https://ai.google.dev/edge/mediapipe/solutions/vision/pose_landmarker#models

Place it at:
```
app/src/main/assets/pose_landmarker_lite.task
```

### 3. Build and run

**From Android Studio:** open the project, let Gradle sync, select a device, and press Run.

**From the command line:**
```bash
# Debug build
./gradlew assembleDebug        # Windows: gradlew.bat assembleDebug

# Install on a connected device
./gradlew installDebug

# Release APK
./gradlew assembleRelease
```
The APK is generated at `app/build/outputs/apk/`.

### 4. Grant permissions
On first launch the app requests the **Camera** permission — required for try-on and garment scanning.

---

## How to Use

1. **Start Try-On** from the home screen — stand 2–3 m from the camera so your upper body is visible. The "Pose Detected" chip confirms tracking.
2. **Pick a garment** via the Garments button, or scan your own:
   - Tap **Scan Garment** in the wardrobe
   - Lay the garment flat on a light, plain surface inside the on-screen frame
   - Capture → the background is removed automatically → name it → **Save & Try On**
3. The garment overlays your body live and follows your movement. Use the flip button to switch cameras.
4. Tap **Save Outfit** to capture the current look. Browse saved looks in **My Outfits** (tap any image for full-screen with pinch-to-zoom).
5. Open **Measure** to see body measurements estimated from your pose.

---

## Troubleshooting

| Issue | Fix |
|---|---|
| "Loading model..." never goes away | Verify `pose_landmarker_lite.task` exists in `app/src/main/assets/` |
| Pose not detected | Improve lighting, step back so shoulders and hips are in frame |
| Background removal keeps parts of the background | Rescan on a plainer, lighter surface with even lighting |
| Garment looks misplaced | Stand facing the camera squarely; tracking is tuned for frontal poses |

---

## Key Implementation Notes

- **Garment fitting**: overlay width is derived from the shoulder-landmark spread (`≈2.1×`), height from shoulder-to-hip distance, with the collar lifted above the shoulder joints to match a real neckline. Scanned bitmaps are trimmed to their visible pixels so bitmap edges equal garment edges.
- **Background removal** runs fully on-device: garment colors are sampled from the image center, background colors from brightness-biased corner/edge samples (robust to dark clutter), followed by BFS connectivity from the center, hole filling for prints/logos, dilation, and feathered alpha edges.
- **Outfit capture** uses `PixelCopy` on the window so the saved image is exactly what the user sees (the camera preview renders on a separate surface that ordinary view drawing cannot read). UI chrome is hidden for one frame during capture.
- **16 KB page-size compliance**: MediaPipe 0.10.26 ships 16 KB-aligned native libraries, keeping the app compatible with Android 15+ devices.
