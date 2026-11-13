# CLAUDE.md - katachi

## App Overview & Key Packages

**Katachi 形** (`katachi-android/`) is a daily pattern recognition and spatial puzzle Android application. It generates 10 fresh daily deterministic puzzles (5 Tangrams, 3 Spot-the-pattern matrices, 2 Build-the-pattern symmetry grids) using compiled Lume matrix math via JNI (`liblumecore.so`) and syncs solved counts with Kasane.

- **Working Directory**: `katachi-android/`
- **Architecture & JNI Integration**:
  - `app/src/main/java/app/katachi/LumeCore.kt`: JNI bindings to native `liblumecore.so` executing matrix multiplications and raster overlap checks for tangram piece placement.
  - `app/src/main/java/app/katachi/`: `KatachiApp.kt`, `MainActivity.kt`, `Store.kt`, `LumeState.kt`.
  - `KasaneLink.kt` & `StatusProvider.kt`: IPC mechanism communicating puzzle completion metrics to Kasane's daily tracker.
  - `FloorModeReceiver.kt` & `TodayWidget.kt`: System broadcast receivers and home screen App Widget.
  - `designsystem/`: `Bars.kt`, `GroupedList.kt`, `LumeColors.kt`.
- **Core Math Verification**:
  - 96x96 raster coverage and collision checks via Lume.
  - `gradle -PcoreOnly :core:test`: pure logic verification against native test cases.

## Build, Test & Lint Commands (Quiet Mode for Token Saving)

Execute all commands from the `katachi-android/` project folder:

```bash
cd katachi-android

# Run unit & Robolectric tests
./gradlew testDebugUnitTest -q --console=plain --no-scan

# Run a single test class
./gradlew testDebugUnitTest --tests "app.katachi.StoreTest" -q --console=plain --no-scan

# Run core logic tests
./gradlew -PcoreOnly :core:test -q --console=plain --no-scan

# Build debug APK
./gradlew assembleDebug -q --console=plain --no-scan

# Lint check
./gradlew lintDebug -q --console=plain --no-scan
```

## Android & Cloud Container Engineering Rules (from innovator-repo/memory)

- **No Android Emulator in Cloud Containers**: Cloud containers lack KVM virtualization; do NOT attempt to launch emulators. Use Robolectric (+ Roborazzi where configured) for fast, headless UI and screenshot verification.
- **Compose Performance & Phases**: Defer rapidly changing State reads (such as scroll offsets and animation values) to the Layout or Draw phases using lambda modifiers (`Modifier.offset { ... }`, `Modifier.graphicsLayer { ... }`, `Modifier.drawBehind { ... }`) to avoid re-triggering composition. Never annotate mutable non-snapshot types with `@Immutable` or `@Stable`.
- **Platform Gotchas**:
  - **AccessibilityEvent recycling**: Copy `AccessibilityEvent` fields (`packageName`, `eventType`, `className`) synchronously on API 26–32 before crossing coroutine or async boundaries to avoid recycled event crashes.
  - **Device Admin Policy XML**: Android `<device-admin>` XML must declare at least one active policy (e.g., `<force-lock />`) to be valid on newer Android releases.
  - **System Settings Intents**: Always wrap system settings intents (`com.android.vpndialogs`, Private DNS settings, Device Admin enrollment) with fallback intents and written in-app user directions to prevent `ActivityNotFoundException` on heavily customized OEM ROMs (Samsung OneUI / Android 16).
- **APK Delivery Pattern**: When pushing debug APKs to an `apk/*` orphan branch, use a separate git worktree (`/tmp/apkwt`) and `git add --force` to prevent binary bloat on source branches.

## Token Hygiene & Compaction Keep-List

- **Token Hygiene**: Never read `.gradle/`, `build/`, `captures/`, `*.apk`, `*.aab`, `*.keystore`, or `*.dex`. Use targeted `read_file` with specific line ranges.
- **Cross-Session Memory**: Long-term project memory and cross-session insights live in `innovator-repo/memory` (`MEMORY.md`).
- **Compaction Keep-List**: When running `/compact`, always preserve:
  1. Current task goal and acceptance criteria.
  2. Kotlin/XML files created or modified.
  3. Build, test, or crash stack traces and identified root causes.
  4. Architectural decisions and design trade-offs.
  5. Pending Gradle, test, or APK delivery commands.

