# Katachi 形

Ten fresh puzzles a day for recognising and building patterns, so they never
repeat the way a fixed level pack does:

- **Tangram ×5**: fill a silhouette with the seven classic pieces. Every
  silhouette is built by snapping the real pieces together edge to edge, so
  each one has a solution; any arrangement that fills it counts. Piece
  outlines shown drop from two to none, and the last two are compact figures.
- **Spot the pattern ×3**: a 3 × 3 matrix of grids following a rule (a turn,
  a mirror, a shift, two grids combined, or one move along rows and another
  down columns); pick the missing one. The rule is explained once solved.
- **Build the pattern ×2**: part of a grid is given; finish it by a mirror,
  half-turn, four-way mirror or tile rule.

The date is the seed, so a day's set is the same however often the app is
opened. Solved counts go to Kasane's "Pattern puzzles" item.

Kotlin and Compose for the screens; the grid transforms and the tangram check
run in compiled [Lume](../../tree/clean/lume) through JNI, with a Kotlin
fallback held to the same results. The app lives in
[`katachi-android/`](katachi-android).

## Build

```sh
cd katachi-android
# 1. The Lume core (needs the clean/lume branch and an Android NDK)
LUME_HOME=/path/to/clean/lume/lume tools/build-core.sh --check   # writes app/src/main/jniLibs/*/liblumecore.so
# 2. The app (needs the Android SDK)
gradle :app:assembleRelease
# Core logic only, no Android SDK needed:
gradle -PcoreOnly :core:test
```

Pushing to this branch builds the APK in GitHub Actions ("Katachi APK"); it
is attached to the run as an artifact.

## What runs where

| Job | Lume (`lume/puzzles.lm`) | Kotlin |
| --- | --- | --- |
| Turn, mirror, flip, shift | Matrix products with fixed 0/1 matrices: `x.T @ J`, `x @ J`, `J @ x`, `x @ S` | Picking rules and shapes |
| Symmetry and tile completions | A grid overlaid with its own images | Which part is given |
| Tangram check | Uncovered + doubled + spilled area on a 96 × 96 raster | Building silhouettes, rasterising, snapping |
| Storage, screens | | JSON file, Compose |
