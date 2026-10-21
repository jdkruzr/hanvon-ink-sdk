# 0.2.1: Apache-licensed SDK publication

This is the first public SDK prerelease. Version 0.2.1 changes licensing,
distribution metadata and documentation; it preserves the 0.2.0 drawing code.
No public API or runtime behavior changes are intended. The SDK and Ink Lab are
Apache-2.0; earlier local 0.2.0 checkpoints retain their original MIT terms.

## Artifacts and provenance

Coordinate: `dev.hanvon.ink:ink-sdk:0.2.1`. The public
[source tag](https://github.com/jdkruzr/hanvon-ink-sdk/tree/v0.2.1) owns the AAR,
source JAR and Maven metadata. The Maven ZIP extracts to `vendor/maven/` for
independent applications. LICENSE and NOTICE are included in the library and
source distributions; the POM links to the license and source repository.

The Ink Lab APK is a debug-signed diagnostic build (package `dev.hanvon.ink.demo`,
version 0.2.1/code 12). Recordings stay in its private storage until explicitly
exported. The [Ink Notes fork](https://github.com/jdkruzr/ink-notes/tree/hanvon-native-ink)
is a separate application consumer and retains its own MIT license.

## Validation

The accepted 0.2.0 M10 implementation passed native pressure and retained widths,
black handoff without flashing, marker preview/retention and partial erase/Undo,
pen buttons/rear eraser, lasso/move, zoom/pan, PDF alignment/export, Settings,
restart/Home/sleep, all four orientations, split-screen fallback/fullscreen
recovery, Lab Clear/exclusion and stock Notes coexistence.

0.2.1 checks include unit tests, lint/builds, Lab 100-stroke emulator retention,
editor/PDF instrumentation, source-JAR and artifact checksums, and compiled-code
comparison against the physically accepted implementation. Numerical latency,
other devices, color panels and other marker shades/widths remain unvalidated.
Conservative packet association can fall back to retained Android input.

The SDK remains experimental. Arbitrarily transformed Views and simultaneous
native clients are unsupported; multiwindow uses Android fallback. Native marker
widths above 64 display pixels use fallback. See [integration](integration.md)
for ownership, frame acknowledgement, geometry and failure handling.

Historical [0.2.0 validation](release-0.2.0.md) and session reports describe the
local experiments; their workstation paths and captures are historical evidence,
not dependencies required to build the public source. Raw firmware, device
captures, test notebooks and signing keys are not published as release assets.
