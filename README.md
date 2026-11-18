# MotoConvoy 🏍️⚡

**Live Tactical Motorcycle Formation Radar & Convoy Tracking for Android**

MotoConvoy is a tactical convoy coordination app built for group motorcycle rides. It solves the blind-spot problem where riders cannot see the relative positions of the **Road Lead**, the **Tail Sweep**, the **Flank Marshals**, or mid-pack sliders.

---

## 🎯 Tactical Convoy Formation Roles

| Role | Badge / Color | Tactical Responsibility | Map / Radar Indicator |
| :--- | :--- | :--- | :--- |
| **LEAD** | 🟡 Gold (`#F59E0B`) | Road Captain: sets pace, navigates route, spots hazards | Gold crown marker, reference point for convoy |
| **LEFT FLANK** | 🔷 Cyan (`#06B6D4`) | Marshals overtaking lanes & intersection merges | Left directional chevron |
| **RIGHT FLANK** | 🔵 Blue (`#3B82F6`) | Marshals highway entries & lane corks | Right directional chevron |
| **MID SLIDER** | 🟣 Purple (`#A855F7`) | Roaming marshal communicating between Lead and Sweep | Velocity vector blip |
| **PACK** | 🟢 Green (`#10B981`) | Convoy riders in staggered pocket | Compact blip with rider initials |
| **TAIL SWEEP** | 🔴 Red (`#EF4444`) | Tail gunner: medic, recovery, ensures no dropped rider | Anchor marker with distance gap to Lead |

---

## 🏗️ Architecture & Clean Separation

MotoConvoy is structured as a clean, multi-module Android project:

- **`:core-domain` (Pure Kotlin JVM):**
  - Zero Android SDK dependencies.
  - Houses the complete domain logic: `ConvoyRole`, `Rider`, `SpatialCalculator` (Haversine distance, bearing calculations, distance delta formatting, staleness detection, and convoy separation alert evaluation).
  - 100% test coverage using **JUnit 5 & AssertJ**. Run locally in seconds via `./gradlew :core-domain:test`.
- **`:app` (Android Jetpack Compose):**
  - High-contrast, dark OLED tactical UI (`ConvoyHudScreen`).
  - Real-time polar 2D Radar Canvas with live rider blips.
  - Interactive Convoy Simulator modeling real-world riding telemetry along the Karak Highway & Genting Highlands twisty loop in Malaysia.
  - Glove-friendly controls: Pause/Resume, Test Gap Stretch (>2.0 km alert), Test SOS Beacon.

---

## 🧪 Test-Driven Development (TDD) Verification

The pure domain layer was built using strict TDD:

```bash
# Run domain test suite
./gradlew :core-domain:test
```

### Verified Test Cases:
1. `convoy roles have strict tactical priority ordering from lead to sweep`
2. `lead and sweep have distinct safety badges and colors`
3. `rider model formats tactical tag with bike model`
4. `haversine distance calculates accurate geographic displacement in meters`
5. `bearing calculates directional angle in degrees 0 to 359`
6. `formatDistanceDelta formats sub-kilometer in meters and long distance in kilometers`
7. `isStale detects dropped riders after 15 seconds threshold`
8. `convoy spread detector evaluates safe vs stretched formation gaps`

---

## 📱 Installing the Mock APK

Every push automatically triggers GitHub Actions to run the full TDD test suite and compile `moto-convoy-mock.apk`.

1. Download `moto-convoy-mock.apk`.
2. Transfer to your Android phone (or download directly via browser / Telegram).
3. Tap the file to install (allow "Install unknown apps" if prompted).
4. Launch **MotoConvoy** to view live radar simulation, stretch alerts, and tactical HUD!
