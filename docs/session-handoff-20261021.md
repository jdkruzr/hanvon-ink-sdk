# Session handoff: reusable Hanvon ink SDK

## Current state: 0.2.0 complete locally

All three user experiments passed. The implementation checklist is complete.
Read [release-0.2.0.md](release-0.2.0.md) for exact validation, installed package
hashes, artifact contents and limits; the chronological editor report preserves
all failures, fixes and observation captures.

The SDK is the reusable product. Ink Lab and Ink Notes are independent consumers.
M10 is the tested reference, not a model-name eligibility gate. Other Hanvon devices
remain unvalidated. No public push or publication was performed.

- SDK repository: `/home/jtd/hanvon-ink-sdk`, branch `feature/reusable-native-ink`,
  local tag `v0.2.0`.
- Ink Notes repository: `/home/jtd/ink-notes`, branch `feature/hanvon-native-ink`,
  local tag `native-ink-v0.2.0`. It vendors immutable SDK 0.2.0 with sources.
- Release bundle: `artifacts/validation/0.2.0/`, including commit manifest, source
  archives, checksums, build/test logs and installation verification.
- Installed M10 serial `9619126060000465`: Ink Lab 0.2.0/code 11 and separately named
  `app.einknotes.nativeink` 0.20.0-native-sandbox/code 32. Original app is untouched.
- Notebook `nativesdkexperiment2` already contains two pages and the PDF on page 2;
  do not import it again. Stock Notes has a disposable `SDK-coexistence-test` note.
- The device is left in Ink Notes on its PDF, portrait lock 0; original
  accelerometer_rotation=0 and user_rotation=0 restored. Final upgrade retained
  both the editor notebook and Lab strokes byte for byte.

No physical questions remain pending. Final version-only packages have identical
SDK/DEX code to the physically accepted candidates, were installed with matching
signatures, and reopened successfully. 26 SDK and 29 app unit tests, lint/builds,
Lab 100-stroke retention and full editor/PDF emulator instrumentation passed.

The observer authorized announced installations during active testing without
repeated permission prompts. After a pause or absence, ensure they are watching
before staging a new physical experiment. Never modify original app data or use
uninstall to bypass certificate mismatch. Their intended next external step is a
fork/PR; publication and pushing remain separate work.

## Historical morning snapshot

The remainder records the overnight rc1 checkpoint. Installation versions, pending
PDF import and stopped-emulator statements below are historical; use the current
state above for continuing work.

## Start here

The user went to bed after successful editor-tool retesting, before PDF staging.
**Notify them and wait for readiness before installing, launching, importing or
changing device UI.** Do not repeat already completed button discovery or ask about
the unavailable original signing key. Continue independent work between experiments.

The SDK is the reusable product. Ink Lab and Ink Notes are separate consumers.
Capability probing allows other Hanvon devices; M10 is the physically tested device.

Repositories:

- `/home/jtd/hanvon-ink-sdk`, branch `feature/reusable-native-ink`, candidate tag
  `v0.2.0-rc1`. Baseline tag `v0.1.1` remains available.
- `/home/jtd/ink-notes`, branch `feature/hanvon-native-ink`, candidate tag
  `native-ink-rc1`; upstream baseline `ffa318daf3f579c3d38eae6ebf143d8cdffb7153`.
- `/home/jtd/penstar_re`: original firmware extraction, stock Notes decompilation,
  pen/display/regional-feature research. This directory is not a Git repository.

## What passed

- SDK pressure capture: 20/20 native packets matched and retained; observer confirmed
  smooth tracking and unchanged post-lift appearance.
- Source-aware input distinguishes actual touchscreen input from the tested top
  button's stylus-source finger conversion. Bottom/middle use bits 32/64; rear tool 4.
- Ink Notes native writing passed. After fixing idle-frame handoff, bottom/rear
  erase, Undo, middle lasso/move, pen triple-tap zoom, top pan and resumed writing
  passed the physical retest. See [editor evidence](editor-session-20261021.md).
- Candidate checks: SDK 25 unit/replay tests; Ink Notes 29 unit tests; both lint/build
  suites; Ink Lab 100 synthetic strokes saved/reopened on the emulator; editor idle
  frame callback, complete tool gestures, width transform, bitmap rendering, actual
  PDF import/annotation/export/pixel check and v8 notebook reopen.

## Installed versus candidate

M10 serial: `9619126060000465`. Original `app.einknotes` code 24 is untouched.
Installed test app: `app.einknotes.nativeink`, code 25, SDK alpha06, display name
**Ink Notes Native Test**. Test notebook: `nativesdkexperiment2`.

Installed retest APK SHA256:
`464342a4caeb2964f477307a44b9472e19bbada70056d4aca6c5af9d9976cdf1`.
Its archive: `/home/jtd/ink-notes/dist/native-validation/experiment2-retest/ink-notes-native-test.apk`.

Candidate **not installed**: code 26, `0.20.0-native-rc1-sandbox`, SDK `0.2.0-rc1`.
It additionally shares the retained-frame helper between both apps, watches display
changes including same-size rotations, invalidates old geometry capture, and keeps
refresh completion from racing edit handoff. These additions need physical acceptance.

Artifacts and logs: `/home/jtd/hanvon-ink-sdk/artifacts/validation/0.2.0-rc1/`.
Standalone Maven repository: `artifacts/maven/dev/hanvon/ink/ink-sdk/0.2.0-rc1/`.
Ink Notes vendors that version with AAR, sources, metadata and checksums; no sibling
source checkout is needed to build it. SDK source and vendor license are MIT.

Candidate SHA256:

| Artifact | SHA256 |
| --- | --- |
| Ink Notes Native Test APK | `93fb16ca5d0bee1b4e8d82c31757dfa04891b7f54122f4ac4e383cce05dd496a` |
| Ink Lab debug APK | `03bdcb532ade42463848678dc25472f2c70b51375dc4fd2eafc4e12a2b637318` |
| SDK AAR | `c2c1f9b22f8be46f2058560226cb7eaf7eb5e1a11e2278a81c8c791f94b3ecb1` |

Both APKs use the workstation debug certificate SHA256
`e91d14f5065a1eb6cfbd42aee993b51c6cb16f7cc21d4879b0b4db1e136f9680`.
The original app's certificate differs; user explicitly chose a separate package.
No in-place upgrade or uninstall is required. Release-variant APKs are unsigned.

## Remaining physical work

1. **USER: ready for candidate installation.** Collect the installed test files
   first; install the candidate with `adb install -r`; reopen the same notebook.
   Check one native stroke and erase, plus retained content after reopen. Do not
   call the candidate physically validated from the alpha06 results alone.
2. **USER: PDF and tool/overlay check.** Import the prepared disposable one-page PDF
   after the current page. Annotate near the top/center/bottom, zoom/pan, erase/undo,
   then return to fit-page view. Check alignment and retained appearance. Try marker
   or another unsupported brush, open/close Settings or Pages, then return to native
   pen. Check partial erase. Export the test notebook PDF and verify native ink and
   background are preserved. Finger gestures are off; zoom with the **pen tip**.
3. **USER: final lifecycle check.** Save/reopen; Home/return; sleep/wake; force-stop
   and reopen. Check supported rotations, including 180 degrees if firmware permits,
   and multiwindow Android fallback then native recovery. Keep a saved-file capture
   before and after; check no native ink escapes onto controls or other apps.
4. **USER: second consumer and service coexistence.** Stage candidate Ink Lab in
   pressure mode, make a stroke, leave for stock Notes and confirm stock writing,
   then return to the SDK consumer. No synthetic input fixture on the M10.
5. Fix/retest failures, update the checklist and evidence, then produce final 0.2.0
   artifacts and local validation tags. Public publishing/pushing is separate work.

## Commands, only after readiness

```sh
cd /home/jtd/hanvon-ink-sdk
python3 scripts/collect.py --serial 9619126060000465 --package app.einknotes.nativeink
adb -s 9619126060000465 install -r artifacts/validation/0.2.0-rc1/ink-notes-native-test.apk
adb -s 9619126060000465 shell am start -n app.einknotes.nativeink/app.einknotes.notes.NotesActivity --es notebook nativesdkexperiment2
```

The existing pen settings are saved. If needed, the debug-only control configures
the eligible pressure pen:

```sh
adb -s 9619126060000465 shell am broadcast -a app.einknotes.DEBUG -p app.einknotes.nativeink --es cmd nativepen
```

Prepared PDF: `artifacts/validation/editor/hanvon-native-test.pdf`. To import it
through the real debug app import path while the observer is watching:

```sh
adb -s 9619126060000465 shell -T run-as app.einknotes.nativeink tee files/native-sdk-test.pdf < artifacts/validation/editor/hanvon-native-test.pdf > /dev/null
adb -s 9619126060000465 shell am broadcast -a app.einknotes.DEBUG -p app.einknotes.nativeink --es cmd import_path --es path /data/user/0/app.einknotes.nativeink/files/native-sdk-test.pdf
```

Filter logs by current PID plus `NativeInk`, `InkDebug`, `InkView`; old crash-buffer
entries from other apps are unrelated. Native preview can be absent from screenshots,
so physical observations remain necessary.

## Rebuild and evidence

Use JDK `/usr/lib/jvm/java-17-openjdk-amd64`, Android SDK `/home/jtd/Android/Sdk`.
SDK: `scripts/check.sh`, then the local Maven publication task. Ink Notes:
`./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleSandbox :app:assembleDebugAndroidTest`.
Increment artifact versions for changed bytes; do not overwrite an immutable Maven
version. Final 0.2.0 has deliberately not been tagged.

Latest physical capture:
`recordings/20261021T063039.872665Z-9619126060000465` (app files, package, logcat).
Version-8 native strokes carry actual widths; older documents without them save as
v7. First v7-to-v8 save preserves `.inkn.pre-v8.bak`. Never uninstall the original
app or replace original private files to bypass signing.

The disposable emulator was used only for synthetic tests and can be restarted as
`Medium_Phone_API_36.0` on port 5580 with `-read-only -no-window -no-audio -no-snapshot
-gpu swiftshader_indirect`. It is stopped after the overnight checks.
