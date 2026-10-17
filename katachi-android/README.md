# Katachi for Android

- `core/` pure Kotlin: tangram geometry and the silhouette builder, the
  pattern generators, the day's set, history, and the Kotlin fallback of the
  Lume math. Tested on the JVM with `gradle -PcoreOnly :core:test`.
- `app/` the Android app: the day's list, and the tangram, matrix and
  building screens.
- `lume/puzzles.lm` the Lume source, `lume/lume_jni.c` the JNI shim,
  `lume/host_check.c` the desktop check.
- `tools/build-core.sh` builds `liblumecore.so` for arm64-v8a and x86_64.

If the library is missing or disagrees with Kotlin on any operation at
startup, the app uses the Kotlin fallback and says so at the bottom of the
day's list. APKs are signed with the debug key in `app/debug.keystore`, so a
new build installs over the old one without losing history.
