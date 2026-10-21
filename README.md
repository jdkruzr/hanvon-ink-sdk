# Hanvon Ink SDK — experimental

A small Android library that attaches Hanvon native ink preview to an existing View, plus **Hanvon Ink Lab**, a diagnostic app. The app owns its retained strokes; the library owns the native session and preview lifecycle.

**Current result:** an ordinary APK on the connected M10 draws native ink. In the [October 21 device session](docs/device-session-20261021.md), mode 0 with continuous enablement was visually flawless to the observer, with 57 recorded strokes/save events/native packets. Clipping, landscape alignment, restart/sleep recovery, and stock Notes recovery passed the observed checks. Mode 3 exhibited preview gaps and hesitations. Bottom/middle buttons produced states 32/64; the configured top button produced finger input. Numerical latency and broader device/lifecycle coverage remain unvalidated. See [results](docs/results.md).

There is no M10 model-name gate. Matching framework APIs and a recognized coordinate origin enable native preview automatically on other devices; availability is distinct from device validation.

## Build and artifacts

Requires JDK 17 and an Android SDK with platform 35. Gradle 8.9, AGP 8.7.3, Kotlin 2.0.21; minSdk 28, targetSdk 35.

```bash
./scripts/check.sh
```

The script defaults to this workstation's JDK/SDK locations; set `JAVA_HOME` and `ANDROID_HOME` elsewhere.

| Output | Purpose |
| --- | --- |
| `demo/build/outputs/apk/debug/demo-debug.apk` | Installable debug-signed diagnostic app |
| `demo/build/outputs/apk/release/demo-release-unsigned.apk` | Release build, deliberately unsigned |
| `ink-sdk/build/outputs/aar/ink-sdk-release.aar` | Experimental library |
| `demo/build/outputs/apk/androidTest/debug/demo-debug-androidTest.apk` | Emulator-only synthetic retention test |

The demo uses package `dev.hanvon.ink.demo`, independent of Ink Notes. It requests no permissions, bundles no vendor binaries, and does not require root, platform signing, policy changes, or hidden-API exemptions. SDK reflection can still be restricted by another firmware; failures are reported and the demo retains its Android canvas.

Convenient copies of the current v0.1.1 build are in `artifacts/`, alongside `SHA256SUMS-0.1.1` and validation evidence. The earlier v0.1.0 comparison artifacts remain available.

## Demo modes

- **Android baseline:** ordinary MotionEvents and bitmap rendering.
- **Native ink + retained drawing (default):** continuously enabled firmware draw mode 0 assists the retained Android strokes, without gray callback markers. The initial pen uses style 15, black, 4 px; width is adjustable.
- **Mode 3 preview (experimental):** the older compatibility path with per-stroke enable/disable. It had visible gaps/hesitations in the M10 test and remains for comparison.
- **Native callback inspection:** firmware draw mode 0, recording native packets and showing their mapped coordinates as gray circles. Packets are not automatically interpreted as unique completed strokes.

The UI includes live recording counters, a center exclusion rectangle, rotation, clear, physical-observation annotations, and ZIP export through Android's document picker. Recording starts on launch and continues until the Activity is destroyed. Strokes survive restarts in app-private storage. Rotation fits the existing document into the new canvas while keeping its aspect ratio.

Rear eraser, side buttons, finger conversion, hover, and unfamiliar tools are recorded. The demo does **not** implement eraser tools or side-button actions yet. Eraser/finger input is not converted into retained black strokes. Native callback mode remains an investigation surface and may exhibit firmware-specific behavior for those inputs.

## Integrating the library

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

For hands-on tests, arrange a time with the observer **before** installing, launching, or staging the test.

## Design and provenance

The bridge uses public reflection entry points to call vendor framework methods and creates a Java proxy implementing the runtime `HvPenDrawListener`. It does not copy framework DEX, smali, native libraries, or implement raw `/dev/ebc` access.

ABI, constants, and coordinate conventions were recovered from the local M10 capture in `../penstar_re`: `HvPenDrawManager`, `HvPenDrawService`, the patched framework `View` third-party hook, and stock Notes `HandView`/`MemoView`. The patched View's mode-3 path supplies the starting preview behavior; its one-second reset delay is experimental here too.

No public API stability or other-device compatibility is promised yet. Advanced brushes, erasing, undo/redo, transformed Views, multiwindow native drawing, Ink Notes integration, and SDK publication are outside this milestone.
