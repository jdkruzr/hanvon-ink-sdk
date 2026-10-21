# Next hands-on device session

**Coordinate with the observer first.** Give a heads-up, wait until they confirm they are watching, then stage the app and explain the next short action. Do not start a timed capture before that acknowledgement. The demo records continuously and its counters show immediately whether events arrive.

The user deferred further M10 testing on 2026-10-20. Do not resume device UI changes simply because the implementation is ready.

## Setup and lifecycle check

1. Preserve existing demo recordings with `scripts/collect.py`. Record final APK SHA-256, firmware fingerprint, app UID, target SDK, and selected mode.
2. Install the final debug APK with `adb install -r`, retaining app data. Do not uninstall to bypass signature errors. Launch `dev.hanvon.ink.demo/.MainActivity` with `--es mode preview` after force-stopping the demo so the requested mode is applied to a fresh Activity.
3. Confirm interface availability and READY. Inspect logcat/recordings for initialization or cleanup failures.
4. Run the debug-only 20-cycle fixture while the observer is watching. It repeatedly closes and opens the demo's native session without synthesizing pen input:

```bash
adb -s SERIAL shell am force-stop dev.hanvon.ink.demo
adb -s SERIAL shell am start -W -n dev.hanvon.ink.demo/.MainActivity --es mode preview --ei cycles 20
```

5. Confirm 20 cycle records and `cyclesComplete`, successful configuration/teardown, and a usable canvas. Counts alone do not prove the native overlay recovered physically.

## Ink and button comparison

Use the mode selector to alternate Android baseline and native preview, keeping width, background, and display optimization settings the same. The recorder logs display mode on mode changes. Do not change the device's speed/quality setting during a comparison.

- Draw dots, diagonals, loops, and several fast consecutive strokes; pause at least two seconds after pen-up. Observe whether preview leads the retained stroke and whether its position/width changes or disappears on clearing.
- Do 100 deliberately counted strokes across comparable baseline/native batches. Compare physical observations, retained count, and saved points. This is separate from the synthetic emulator fixture.
- Press bottom, middle, then top while hovering; repeat while drawing. Test the rear eraser. Read the recorded tool types, button bitmasks, and keycodes before assigning physical labels. Finger-conversion events must remain visible in the recording.
- Switch to callback inspection and compare native packets to Android traces at the corners and center. Validate the actual coordinate transform and packet delivery semantics. Gray circles represent packet points, not retained ink.
- Enable the excluded center rectangle and cross its boundaries. Native ink must not appear outside the drawing area or in the exclusion; check physically, since screenshots can miss the native overlay.

Use **Observe** to attach a physical result to the current recording. It suspends the native area while the dialog is open.

## Lifecycle/recovery

Test portrait and landscape; screen insets; center exclusion; observation dialog open/close; Home/return; screen off/on; resize/multiwindow; and force-stop/relaunch. Verify drawing never escapes onto controls or other apps. After force-stop, verify stock Notes can still write and the demo can reinitialize. Rotation should preserve the retained document's shape while fitting it into the resized View.

Finally export the recording and collect package/logcat evidence. For any failure, preserve the exact mode, firmware, lifecycle sequence, and last successful operation; do not conceal it behind an Android fallback label.

## Acceptance and latency

A successful native milestone needs all four: visible native preview, intact retained strokes, correct clipping/coordinates, and clean recovery. Interface lookup and returned setter calls satisfy none of the physical checks by themselves.

Software event/worker/callback/frame timings are diagnostic only. Optional high-speed video should show the pen tip and panel simultaneously, use identical stroke direction/length/speed and display settings in both modes, record camera frame rate, and report measurement uncertainty. No numerical pen-to-visible-ink claim without optical evidence. Diagnostic logging adds overhead and should be disclosed in comparisons.
