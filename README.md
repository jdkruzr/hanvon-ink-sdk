# Hanvon Ink SDK — experimental

A small Android library that attaches Hanvon native ink preview to an existing View, plus **Hanvon Ink Lab**, a diagnostic app. The app owns its retained strokes; the library owns the native session and preview lifecycle.

**Current release: 0.2.1 (experimental), Apache-2.0.** Download the SDK Maven
bundle, AAR, sources and Ink Lab APK from [GitHub Releases](https://github.com/jdkruzr/hanvon-ink-sdk/releases).
Version 0.2.1 changes licensing and packaging only; drawing code matches the
physically accepted 0.2.0 implementation.

Two independent consumers exercise the SDK: this repository's Ink Lab and
[Ink Notes with native ink](https://github.com/jdkruzr/ink-notes/tree/hanvon-native-ink).
The SDK owns the native session and preview; applications own documents, tools,
undo and retained rendering.

Physical acceptance on the Hanvon M10 / Penstar eNote Pro (Android 14) covers
pressure pen, marker preview/retention, editing, PDF annotation/export, Settings,
restart/Home/sleep, all four orientations, multiwindow Android fallback/fullscreen
recovery, clipping/exclusion and stock Notes coexistence. See the
[0.2.1 release notes](docs/release-0.2.1.md) and [validation results](docs/results.md).

There is no M10 model-name gate: matching framework APIs and a recognized
coordinate origin enable compatible devices. Other models remain unvalidated.
Numeric pen-to-visible-ink latency is unmeasured; missing or ambiguous native
packets preserve provisional Android ink. The API remains experimental.

## Build and artifacts

Requires JDK 17 and an Android SDK with platform 35. Gradle 8.9, AGP 8.7.3, Kotlin 2.0.21; minSdk 28, targetSdk 35.

```bash
./scripts/check.sh
```

Set `JAVA_HOME` to JDK 17 and `ANDROID_HOME` to your Android SDK, or configure the SDK path in an untracked `local.properties` file.

| Output | Purpose |
| --- | --- |
| `demo/build/outputs/apk/debug/demo-debug.apk` | Installable debug-signed diagnostic app |
| `demo/build/outputs/apk/release/demo-release-unsigned.apk` | Release build, deliberately unsigned |
| `ink-sdk/build/outputs/aar/ink-sdk-release.aar` | Experimental library |
| `demo/build/outputs/apk/androidTest/debug/demo-debug-androidTest.apk` | Emulator-only synthetic retention test |

The demo uses package `dev.hanvon.ink.demo`, independent of Ink Notes. It requests no permissions, bundles no vendor binaries, and does not require root, platform signing, policy changes, or hidden-API exemptions. SDK reflection can still be restricted by another firmware; failures are reported and the demo retains its Android canvas.

GitHub release assets contain the reusable SDK and diagnostic APK, with SHA256 checksums. Raw device captures and notebooks are not release assets. Historical local validation bundles are described in the session reports.

## Demo modes

- **Android baseline:** ordinary MotionEvents and bitmap rendering.
- **Native ink + retained drawing (default):** continuously enabled firmware draw mode 0 assists the retained Android strokes, without gray callback markers. The initial pen uses style 15, black, 4 px; width is adjustable.
- **Mode 3 preview (experimental):** the older compatibility path with per-stroke enable/disable. It had visible gaps/hesitations in the M10 test and remains for comparison.
- **Native callback inspection:** firmware draw mode 0, recording native packets and showing their mapped coordinates as gray circles. Packets are not automatically interpreted as unique completed strokes.

The UI includes live recording counters, a center exclusion rectangle, rotation, clear, physical-observation annotations, and ZIP export through Android's document picker. Recording starts on launch and continues until the Activity is destroyed. Strokes survive restarts in app-private storage. Rotation fits the existing document into the new canvas while keeping its aspect ratio.

Rear eraser, side buttons, finger conversion, hover, and unfamiliar tools are recorded. The demo does **not** implement eraser tools or side-button actions yet. Eraser/finger input is not converted into retained black strokes. Native callback mode remains an investigation surface and may exhibit firmware-specific behavior for those inputs.

## Integrating the library

Download `hanvon-ink-sdk-0.2.1-maven.zip` from the release and extract it at the
application repository root; this creates `vendor/maven/`. Configure Gradle:

```kotlin
// settings.gradle.kts, inside dependencyResolutionManagement.repositories:
maven {
    url = uri("vendor/maven")
    content { includeGroup("dev.hanvon.ink") }
}
// Application module build.gradle.kts:
dependencies { implementation("dev.hanvon.ink:ink-sdk:0.2.1") }
```

This versioned vendoring keeps builds independent of the SDK checkout or a hosted
Maven repository. Preserve the bundled Apache license and notice when redistributing.

See [the API and lifecycle contract](docs/integration.md). The library uses Kotlin's standard library as its only runtime dependency beyond Android; a manually consumed AAR needs that dependency in its host. The demo's project dependency resolves it automatically.

The essential contract is: probe, attach, report foreground state, observe retained input, acknowledge retained-frame commits, then close. The SDK does not install itself as an input listener or replace the application's document model.

In v0.1.1, `InkConfig()` defaults to `DrawingMode.NATIVE_CALLBACK`, the observed working mode-0 route. The enum name describes its native interface; the host decides whether to display diagnostic markers. Previously saved demo-mode choices are honored, while fresh installations choose normal native ink automatically when available.

## Tests and recordings

`scripts/check.sh` runs unit tests, lint, and debug/release/library/test-APK builds. Lint warnings include the intentional vendor reflection, experimental English-only UI, and target SDK 35; lint errors are not suppressed.

Run the synthetic fixture on a disposable non-Hanvon emulator. It clears **the demo's** drawing, generates 100 stylus strokes, and verifies restoration after Activity restart. It refuses a device with an available native interface.

```bash
adb -s EMULATOR install -r demo/build/outputs/apk/debug/demo-debug.apk
adb -s EMULATOR install -r demo/build/outputs/apk/androidTest/debug/demo-debug-androidTest.apk
adb -s EMULATOR shell am instrument -w dev.hanvon.ink.demo.test/dev.hanvon.ink.demo.InkLabInstrumentation
```

For a debug build, collect recordings without interacting with its UI:

```bash
python3 scripts/collect.py --serial DEVICE_SERIAL
python3 scripts/summarize.py recordings/CAPTURE_DIRECTORY
```

Each collection gets a new directory. Raw recordings, artifacts, and build output are ignored by Git. Exports contain `metadata.json` and `events.jsonl`; developer collections also include package/logcat evidence and retained strokes. Activity and View records may describe the same physical input, so event totals are not unique input counts. Full diagnostic logging adds overhead; software timing is not pen-to-visible-ink latency.

For hands-on tests, ensure the observer is watching before staging.

## Design and provenance

The bridge uses public reflection entry points to call vendor framework methods and creates a Java proxy implementing the runtime `HvPenDrawListener`. It does not copy framework DEX, smali, native libraries, or implement raw `/dev/ebc` access.

ABI, constants, and coordinate conventions were recovered from the local M10 capture in `../penstar_re`: `HvPenDrawManager`, `HvPenDrawService`, the patched framework `View` third-party hook, and stock Notes `HandView`/`MemoView`. The patched View's mode-3 path supplies the starting preview behavior; its one-second reset delay is experimental here too.

No public API stability or other-device compatibility is promised yet. Applications
own erasing, undo/redo and document semantics; Ink Notes demonstrates those on top
of the independently packaged SDK. Arbitrarily transformed Views, native drawing
in multiwindow and Maven Central publication remain outside this milestone.

## License

Apache-2.0 for 0.2.1 and later; see [LICENSE](LICENSE) and [NOTICE](NOTICE). Earlier local 0.2.0 checkpoints remain under their original MIT terms. The implementation contains no redistributed vendor binaries.
