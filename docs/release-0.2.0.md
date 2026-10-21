# Hanvon Ink SDK 0.2.0 — local validated release

October 21, 2026. The reusable SDK is packaged as `dev.hanvon.ink:ink-sdk:0.2.0`.
Ink Lab uses a project dependency; Ink Notes consumes an independent vendored Maven
artifact, including sources. No proprietary firmware binaries are distributed.
Local validation tags: SDK `v0.2.0`, Ink Notes `native-ink-v0.2.0`.
Nothing has been pushed or publicly published.

## Delivered behavior

Native continuous preview supports pressure pen and fixed-width pencil, typed
monochrome preview colors, clipping/exclusion, coordinate conversion, normalized
pen signals and conservative association of reported width samples. Hosts retain
ownership of their document, tools and undo. Matching black retained frames can
avoid a global reset; edits and gray marker frames use an explicit clearing handoff.
Foreground ownership, geometry revisions and frame completion guard teardown and
late callbacks. Both consumers fall back to Android drawing in multiwindow.

Ink Notes demonstrates editable width-bearing strokes, v8 persistence with a backup
before upgrading older documents, native marker preview, temporary erase/select/pan,
PDF annotation/export and recovery across tools, overlays and lifecycle changes.

## Physical acceptance

Reference device: Hanvon M10 / Penstar eNote Pro, Android 14, serial
`9619126060000465`. Acceptance was observed on SDK rc3, Ink Lab code 10 and Ink Notes
code 31/rc6. Final package versions only change metadata: the SDK AAR and both APKs'
DEX files match those tested implementations byte for byte. Final packages were
installed and reopened; retained document bytes were unchanged.

| Check | Result |
| --- | --- |
| Pressure preview and retained width | Pass; initial pressure experiment 20/20 associations |
| Bottom/rear erase, Undo/Redo, middle lasso/move, pen triple-tap zoom and top pan | Pass |
| PDF alignment before/after zoom and pan | Pass |
| Black pen handoff without flashing | Pass |
| Native marker, partial erase, Undo, subsequent marker and return to Pen | Pass at tested width/shade |
| Settings/overlays and return to writing | Pass |
| Saved ink, force-stop/reopen, Home/return, sleep/wake | Pass |
| All four orientations, including same-size landscape flip | Pass |
| Split-screen Android fallback and fullscreen native recovery | Pass |
| Ink Lab pressure/retention, Clear and exclusion on/off | Pass |
| Stock Notes writing, then Ink Notes writing/erase/Undo | Pass |
| Device PDF export | Two pages rendered and visually inspected |

See [pressure evidence](pressure-session-20261021.md) and the chronological
[editor/lifecycle evidence](editor-session-20261021.md), including failures and fixes.
Final physical acceptance capture: `recordings/20261021T183819.485246Z-9619126060000465`.
Final package captures: `recordings/20261021T184043.427188Z-9619126060000465` (Lab),
`recordings/20261021T184045.384173Z-9619126060000465` (Ink Notes).
A separate stock notebook, `SDK-coexistence-test`, holds the coexistence scribbles.

## Automated validation and installation

26 SDK and 29 Ink Notes unit tests passed with zero failures/errors/skips. Lint and
builds passed. The final Lab emulator fixture retained/restored 100 synthetic
strokes. The final editor fixture passed frame completion, fallback, erase/select/
pan, Undo/Redo, ignored touch, native-width rendering/transforms, marker partial-
erase pixels, display ownership cancellation, and PDF import/export/reopen.
These emulator checks are separate from physical panel acceptance.

Installed packages are debug-signed with SHA256 certificate
`e91d14f5065a1eb6cfbd42aee993b51c6cb16f7cc21d4879b0b4db1e136f9680`:

| Package | Version | APK SHA256 |
| --- | --- | --- |
| `dev.hanvon.ink.demo` | 0.2.0, code 11 | `b0f8b11ae38a0d8597031288d09a82605f6986a0396189a066c406f050212d50` |
| `app.einknotes.nativeink` | 0.20.0-native-sandbox, code 32 | `a3b786392edc63fa46080c82f0ae6c7afa631220d6ab16575e20fb04eb7e6213` |

SDK AAR SHA256: `b4fe79e6bcc4addcdf2ac33e75698d552f00ba7be75e4a8d0109a1015bccb689`.
All 11 Kotlin SDK files match the source JAR; vendored Maven files match publication.
The original `app.einknotes` package was not replaced, uninstalled or opened for this
release. Its APK hash remains
`b876e9850cb7c2a02d43750ac3de394e7f65604f7d7613e0b5d42f0aab0e9c97`.
Its private data is inaccessible to run-as; no backup or byte verification is claimed.
Original portrait lock (accelerometer_rotation=0, user_rotation=0) was restored.

## Artifacts and reproducibility

Local bundle: `artifacts/validation/0.2.0/`. Includes SDK AAR, versioned Maven
publication/source JAR, debug Lab and sandbox APKs, unsigned Lab release APK,
repository source archives, manifest, SHA256SUMS, build/test logs, signer evidence,
installed-package evidence, before-upgrade test data and document hash verification.
The manifest records the exact source commits. Earlier candidates remain archived.
The unsigned release APK needs a maintainer's signing key before distribution.

Use JDK 17 and Android SDK 35:

```sh
./scripts/check.sh
./gradlew :ink-sdk:publishReleasePublicationToLocalArtifactsRepository
```

In the independent Ink Notes checkout:

```sh
./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleSandbox
```

Maven versions are immutable: publish a new version for future runtime changes.
Installed test-data backups are in the release bundle and diagnostic captures.
Do not uninstall either package to work around a signing mismatch, or open a v8
notebook with an older reader. Restore a pre-v8 backup only when intentionally
abandoning later edits; PDF is the visual interchange route.

## Limits

Only the M10 is physically validated. Capability probing permits other compatible
Hanvon firmware, but does not prove its behavior. Color panels, other gray shades,
wide native markers above 64 display pixels, arbitrary transformed Views and
simultaneous native clients are outside the validated scope. Wide/unsupported ink
uses Android fallback, which may be slower. Numeric pen-to-visible-ink latency is
not measured. Missing/ambiguous packets preserve provisional Android ink; the later
rotation session had such fallbacks despite passing visual acceptance. Marker
partial erase uses conservative centerline splitting with square-cap clearance,
not a pixel mask. The SDK remains experimental with no stable-API promise.
