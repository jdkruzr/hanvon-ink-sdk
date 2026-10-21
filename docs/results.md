# Implementation and validation results

Recorded 2026-10-20 America/Chicago / 2026-10-21 UTC. Further hands-on M10 testing was deferred by the user; arrange readiness before staging it.

**Update:** the user resumed testing with the device in front of them. See [the October 21 device session](device-session-20261021.md): final APK installed, cycle fixture completed, real native packets received, and live preview gaps/hesitations observed despite the retained drawing appearing after redraw. The original matrix below records the earlier implementation handoff.

**Mode comparison:** the user subsequently reported mode 0 with continuous pen enablement as visually flawless. Its recording contains 57 stylus stroke starts, 57 save events, and 57 native packets without recorded failures. Mode 3 remains unsuitable as the default until its preview issue is resolved. This is a successful initial mode-0 drawing test, not completion of clipping, button, or recovery validation.

**Completed session:** button signals were mapped, exclusion clipping and landscape alignment passed the observer's checks, 128 strokes survived Home/return and force-stop unchanged, and the observer confirmed clean writing after restart, sleep/wake, and in stock Notes. v0.1.1 promotes mode 0 to the SDK/fresh-install demo default and keeps diagnostic dots in the inspection mode only. The updated build passes 15 unit tests and the build/lint checks. See the session report for evidence paths and remaining limits.

**v0.1.1 installed:** package versionCode 2 / versionName `0.1.1-experimental`, SHA-256 `ed524b6592175c7343276953f7ba4d095cef8fa4e4c0298161f3672877bc6d9e`, matched the local debug artifact byte-for-byte. Installation retained application data and left stock Notes foreground. The physical tests above used v0.1.0's mode-0 path; v0.1.1 changes the default and marker presentation, not that native service sequence. Artifacts are `artifacts/hanvon-ink-lab-0.1.1-debug.apk` and `artifacts/hanvon-ink-sdk-0.1.1.aar`; checksums and validation evidence are in `artifacts/SHA256SUMS-0.1.1` and `artifacts/validation/0.1.1/`.

## Current matrix

| Check | Result | Limits |
| --- | --- | --- |
| Debug APK, release APK, release AAR | Built | Release APK is unsigned; the debug APK is installable |
| Unit tests | 14 passed | Geometry, packet validation, session cleanup, stale handoff prevention |
| Lint | Passed with warnings | Vendor reflection, UI localization/tooling, API-target/manifest warnings; not evidence of vendor API stability |
| Ordinary M10 app identity | UID 10128, target SDK 35 | Debug build; no special permission or policy change |
| M10 reflection/interface discovery | All required methods found; origin 0 | This captured firmware only |
| M10 area initialization and geometry | Returned a nonzero native area handle; reached READY | Setters can swallow vendor-side exceptions; physical effect unverified |
| Normal Android fallback | API 36 x86_64 emulator reports `hvpen service unavailable` and retains the Android canvas | No native path exists on this emulator |
| Synthetic retained strokes | 100 generated strokes persisted and restored | Android application path only; not physical pen input or native overlay |
| Actual M10 pen/button events | Pending | No physical test was performed in the observed sessions |
| Native visible drawing and handoff | Pending | No latency or rendering success claim |
| 20 M10 open/close cycles | Pending | Fixture is implemented; execution deferred |
| Rotation/clipping/crash/stock Notes recovery | Pending | Unit transforms are not physical validation |

## Device evidence

M10 build fingerprint:

```text
HANVON/M10/M10:14/UQ1A.240205.004.B1/eng.hanvon.20260601.145426:user/release-keys
```

The installed early prototype ran as ordinary UID 10128. Required methods, including initialization, teardown, bounds, exclusions, pen controls, orientation, reset, and origin lookup, were discoverable. Logcat showed the framework wrapper calling `HvPenDrawService` and receiving a nonzero area handle. The demo recorded READY after geometry configuration.

One initial launch had the system Control Center covering the app. The SDK remained suspended until window focus returned, then configured the drawing area. This is observed focus handling, not a complete lifecycle recovery test.

Read-only evidence captures:

- `recordings/20261021T041816.705648Z-9619126060000465/`: M10 package, logs, metadata, and configuration events.
- `recordings/20261021T041817.230250Z-emulator-5580/`: non-Hanvon capability/fallback evidence.
- `recordings/20261021T042124.223581Z-emulator-5580/`: first synthetic 100-stroke persistence/restoration run.
- `recordings/20261021T042627.861728Z-emulator-5580/`: final-build synthetic run, with explicit synthetic provenance, fresh baseline Activity, and stable-canvas-size assertion.

The final local source/artifacts include fixes after the initial M10 installation. That final build has **not** been installed on the M10 because the user deferred further device staging. See the artifact hashes generated by the final build handoff.

## Final artifact handoff

Artifacts are in `artifacts/`, with SHA-256 hashes in `artifacts/SHA256SUMS`. Validation logs, test XML, lint reports, signature verification, emulator summary, and the earlier installed M10 APK hash are in `artifacts/validation/`.

| Artifact | SHA-256 |
| --- | --- |
| `hanvon-ink-lab-0.1.0-debug.apk` | `6bbfb750a4c9d3b414305d5a0cc9f7d7e865c0e3890a3b9d317b77a4ad02b112` |
| `hanvon-ink-sdk-0.1.0.aar` | `efa25c2df9ecbbc22a4342faace34d351d3029002c5b3f06f72232237d65f5c5` |

The final synthetic test passed with 100 retained strokes, 100 frame records, 100 restored strokes, stable canvas dimensions, and no recorded failures. Its input was generated in the app; it says nothing about native/physical pen latency.

## What remains unproven

No observed physical pen signals, visible native overlay, button mapping, clipping correctness, pen-to-pixel measurement, or crash recovery is claimed. A library that can register an area is not yet a validated fast-drawing SDK. The implementation and recording tools are ready to test those questions in the next coordinated session.

## October 21 native pressure and editor integration

See [pressure results](pressure-session-20261021.md) and [editor results](editor-session-20261021.md). The alpha06 editor retest passed temporary erase/select/pan and return to drawing. The 0.2.0 implementation adds shared frame submission, display-change handling, matching-black handoff without reset and typed gray preview colors. PDF, marker editing, lifecycle, all orientations, split-screen fallback/recovery, Lab Clear/exclusion and stock Notes coexistence passed on the M10; see [release evidence](release-0.2.0.md).
