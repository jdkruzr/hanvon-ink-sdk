# Ink Notes native editor experiment, October 21

M10 serial `9619126060000465`; test package `app.einknotes.nativeink`. The original
`app.einknotes` install and private data were preserved. No original signing key is
available; the observer explicitly chose the separately named test package.

## Observed results

The first editor build used SDK alpha05. Ordinary native pressure writing was
reported as working without anomalies; logs accepted 14 native stroke transactions.
The next trial reported that bottom/rear erase and selection did not work, while
ordinary writing still worked afterward.

The notebook actually contained 13 retained strokes after that trial. A curly line
was absent from both the document and Android screenshot but still visible on the
physical panel. Thus at least one erase had succeeded in the model while old native
preview obscured the result; lassoing that stale line had no document item to select.

An emulator regression waiting for retained-frame completion failed. The callback
was registered during onDraw without guaranteeing a subsequent frame, so an idle
view could strand it. Requesting another frame fixed the regression. Both consumers
now share this behavior through the SDK's `RetainedFrame.afterDraw` helper. Editing
also has a separate suspended-frame acknowledgement because erase/undo/pan can
invalidate the last native stroke token. Ignored touchscreen input no longer
cancels a pending native handoff.

Alpha06 retest APK SHA256:
`464342a4caeb2964f477307a44b9472e19bbada70056d4aca6c5af9d9976cdf1`.

The observer confirmed:

- Bottom-button erase, Undo, then rear-eraser removal visibly worked.
- Middle-button lasso selected the shape; holding middle again moved it.
- Pen triple-tap zoom and top-button pan worked.
- Ordinary writing recovered cleanly after the editing gestures.

Logs show `clearPreview` after retained frame completion, `erase removed=1`,
`lasso selected=1`, and `clearEditedPreview` during edits. Three closely spaced
zoom-tap associations conservatively fell back; the zoom gesture removes those
temporary tap dots. They are not three lost document strokes.

The initial instruction to triple-tap with a finger was wrong: touchscreen gestures
were disabled. Panning at fit-page zoom is clamped, explaining that initial result.

## Evidence

- Before fixes: `recordings/20261021T061854.064807Z-9619126060000465`.
- After successful retest: `recordings/20261021T063039.872665Z-9619126060000465`.
- The installed retest APK is archived in the Ink Notes repository at
  `dist/native-validation/experiment2-retest/ink-notes-native-test.apk`.
- Current candidate artifacts and automated logs are under
  `artifacts/validation/0.2.0-rc1/`.

## Remaining acceptance

The observer stopped for the night before PDF import. The device was not staged
again. Candidate rc1 includes additional shared-helper, display-change/capture and
refresh-race handling beyond the installed alpha06. It is built and tested on the
emulator, not installed or physically accepted on the M10. Continue with the
[morning handoff](session-handoff-20261021.md); final 0.2.0 remains untagged.

## Resumed session: candidate installation and PDF trial

After the observer returned and reconnected USB, the test app was captured at
`recordings/20261021T173508.912413Z-9619126060000465`. Candidate code 26 / SDK rc1
was installed in place over the separately named test app and the same notebook
reopened. The observer reported that it looked good so far. The prepared one-page
PDF was then imported as the second notebook page. Physical PDF alignment,
zoom/pan, tool transitions and lifecycle checks are in progress; do not count
installation or a successful render log as complete physical acceptance.

The observer confirmed PDF alignment, but reported that retained redraw visibly
flashed the drawn ink at both fit-page and zoomed sizes. Logs also showed repeated
session suspension after zoomed pen-up: auto-advance invalidated native capture
before checking whether any page movement was needed. That no-op invalidation was
removed. Stock MemoView's delayed ordinary black-pen path invalidates its retained
bitmap without resetData; the reset is conditional on marker/button cases. A
candidate fix therefore acknowledges matching retained strokes without a global
reset, while keeping reset for recovery ink and explicit suspended edits. The SDK
exposes this as an overload of notifyFrameCommitted; existing callers retain the
previous default. Physical retest is required before counting this as a flash fix.

The rc2 retest (test app code 27, SDK 0.2.0-rc2) passed: the observer reported
"working perfectly" after ordinary and zoomed strokes, delayed redraw and bottom
erase. Capture: `recordings/20261021T174416.866299Z-9619126060000465`.
Matching retained frames now log `retainedMatch` without a global reset; edited
frames still use `clearEditedPreview`. Partial erase and Undo also passed.

The next trial reported missing Marker preview. Capture:
`recordings/20261021T174836.376058Z-9619126060000465`. Logs show Android highlighter
input while the legacy panel policy selected AUTO_DU (22) on DOWN and GC16 (7)
after UP. A grayscale-preserving drawing mode is being tested; marker visibility
and recovery through Settings remain unconfirmed. Experiment 2 is still open.

Follow-up: observer reports Marker never appears physically, while Pen still
works. Android screencap shows the retained gray diagonal in the bottom PDF box,
confirming a panel/display discrepancy rather than missing document geometry.
The app rc3 candidate (code 28, still SDK rc2) requests PART_GC16 for gray Android
ink and invalidates after the asynchronous waveform change. The emulator now
checks that Marker changes the live bitmap before UP and is only committed on UP;
that and the existing editor/PDF regressions pass, as do unit tests and lint.
Candidate SHA256: `31b75046afdf6f58b2a915d0c4fca6e5089a09b807fce2cf099fa1136dcbe5a5`.
Physical marker retest is pending observer readiness.

Observer authorized installation and added that only two tiny starting spots had
been visible on the panel. Captured pre-install state at
`recordings/20261021T175235.170013Z-9619126060000465`, then installed code 28 in
place and reopened the same PDF page with Marker selected. Observer confirmed the
previous marker stroke became visible after reopening. Saved geometry is intact;
live marker visibility on the new build remains the pending physical check.

The code-28 observer reported that Marker became visible but drew in sections
with severe lag. PART_GC16 fixes visibility but is unsuitable for live marker
preview on this tablet. Source review confirms stock Notes uses native mode 0,
style 15, and monochrome DARK_GRAY (3) / LIGHT_GRAY (4), rather than live Android
grayscale frames. It retains an alpha marker layer (`MarkPenView`, PSL type 4),
then resets the preview and invalidates after a 500 ms delay when not drawing.
Evidence: `Pen.PEN_STYLE`, `MemoView.modifyPenColor`, `MemoView.addPSLTrace`,
`MemoView.penInvalidateRunnable`, and framework `HvPenDrawManager` color constants.

SDK rc3 adds typed preview-color control. App rc4 is being prepared to accelerate
Marker preview using that API, retaining the existing editable Android highlighter
geometry and pressure samples, with an explicit clearing handoff. Unlike black
pressure ink, marker samples are not converted to NATIVE_WIDTH circles. Native
preview shades approximate the selected gray; exact visual matching is unproven.
The 1..64 px native-width bound remains; larger widths use Android fallback.

Observer authorized the native marker trial. Pre-install capture:
`recordings/20261021T180036.178724Z-9619126060000465`. Installed app code 29 /
`0.20.0-native-rc4-sandbox`, SDK rc3, APK SHA256
`6a5045b5be70c98e5ce8565b112dcc611820e392a84549a9d034efee96fb3431`.
Same notebook/PDF page reopened with 21 saved items; Marker selected at fit-page
zoom. Logs confirm native brush, previewColor, width and geometry configuration
after the suspended tool-switch redraw. Observer asked to compare slow/fast
tracking and retained position/width/shade after two seconds. Physical outcome
pending; successful service calls alone do not establish preview performance.

The observer reported "very good performance" and correct preview, pause and
retained redraw. Both native marker transactions were accepted. A frame for the
first marker arrived after the second stroke began and was correctly rejected
(`token=6 current=7 delay=null`); the second retained frame then cleared the preview.
Capture: `recordings/20261021T180410.495982Z-9619126060000465`.
This validates the tested marker shade/width at fit-page size, not every shade,
width or firmware. Marker erase/Undo, black-pen color restoration and Settings
recovery are the next physical checks.

Marker editing failed: Partial erase was still selected from the prior test.
Observer saw thin erased slits and artifacts. Capture:
`recordings/20261021T180606.124235Z-9619126060000465`, including Android screenshot
`marker-partial-android.png`. That screenshot contains the fragmented marker too;
this is not solely stale native preview. The app splits only the stroke centerline,
then renders square caps back into the erased disk. A new emulator pixel test
reproduced it (`Marker cap refilled erased disk at 145,135`).

App rc5 candidate accounts for square-cap extent during marker splitting and skips
repeat cuts at numerically equivalent boundaries. This remains conservative stroke
splitting, not exact pixel-mask erasure. It also selects A2_DITHER for erasing over
gray strokes: logs showed AUTO_DU during the failed trial. New rendering tests cover
straight/oblique/edge cuts and repeat erasure, plus editor partial-marker Undo/Redo.
Physical acceptance is pending; the prior marker drawing result remains valid.

App rc5 (code 30, SDK unchanged at rc3) passes the new emulator rendering and
partial-marker Undo/Redo checks, existing editor/PDF regressions, unit tests, lint
and builds. APK: `/home/jtd/ink-notes/dist/native-validation/rc5-marker-erase-retest/ink-notes-native-test.apk`.
SHA256: `c37bf47ec8aff9ce4e231c1bf31caf31114d985f04a80c50001cba216223703a`.
Signature matches the installed test app. Observer readiness requested; rc5 is
not yet installed. Existing saved fragments will remain fragments unless undone;
retest a fresh marker stroke to assess the correction.

Before rc5 installation, observer reported that a fresh marker disappeared at
redraw while an older marker could be erased. Live package verification still
showed rc4/code 29. Capture:
`recordings/20261021T181046.942961Z-9619126060000465`. Android screenshot contains
the new marker; native transaction 3 was accepted and its preview cleared normally.
`getprop sys.eink.mode` returned 22 (AUTO_DU), left behind by the earlier erase.
Thus native gray preview was visible but retained Android gray was suppressed.
Do not record this as an rc5 failure or successful rc5 erase test.

App rc6 candidate includes the rc5 erase fixes and explicitly establishes
A2_DITHER before native activation. Display-mode completion is guarded by ownership
revision; session activation additionally checks generation, session identity,
active/resumed state and configuration. A new emulator race test checks that
relinquished ownership cannot invoke a delayed activation callback. Physical
marker -> erase -> marker/pen handoff remains pending.

Rc6/code 31 built and passed unit/lint checks plus the full emulator fixture,
including canceled display ownership, marker pixels and partial erase Undo/Redo.
APK: `/home/jtd/ink-notes/dist/native-validation/rc6-marker-handoff-retest/ink-notes-native-test.apk`.
SHA256: `ea70d851e19972a228603118f9ccdb23f878ad9ef83c36d024c0c8f63f5df36b`.
Observer readiness to install rc6 is pending. Rc4/code 29 remains installed.

Observer then authorized ongoing new-build installation without repeated permission
requests. Rc6/code 31 installed successfully in place; same PDF reopened with 31
saved items, Marker and 18 px partial eraser selected. Native setup completed and
display mode reads A2_DITHER (13). Requested fresh-marker -> partial erase -> Undo
-> fresh-marker trial; physical result pending. Original app remains untouched.

The observer reported "victory!" for that complete sequence. Capture:
`recordings/20261021T181801.728102Z-9619126060000465`. Both fresh markers were
accepted and handed off, partial erase split the target, Undo restored it, and the
second marker remained visible. Display mode remained A2_DITHER (13), including
after erasing. Return to black Pen and Settings recovery are now being checked.

Observer confirmed return to black Pen and writing before/after Settings worked.
Captured at `recordings/20261021T181921.874152Z-9619126060000465`, explicitly saved,
and exported through the app's real PDF exporter. Output: 2 pages, 532824 bytes,
`content://media/external/downloads/1000003627`, Download/Ink Notes/Notebook.pdf.
Pulled copy: `artifacts/validation/editor/rc6/Notebook.pdf`; SHA256
`a5c9023c67ea9dc9c55a7f1df15c5051c2a8e4a00d26588286e41e3a57f7175c`.
Both pages rendered with pdftoppm and were visually inspected: ruled background,
imported PDF, native pressure ink and gray markers are present. Existing fragments
from the earlier failed partial-erase test remain as expected. Experiment 2 is complete.

Announced save/force-stop/reopen, then reopened the same notebook/PDF successfully
with new process PID 14922. Post-reopen capture:
`recordings/20261021T181955.596730Z-9619126060000465`. Observer is checking retained
content and fresh writing, then Home/return and sleep/wake. Physical outcomes pending.

Observer confirmed all force-stop/reopen, Home/return and sleep/wake checks passed,
with retained content and fresh writing. Capture:
`recordings/20261021T182131.633806Z-9619126060000465`.
Original rotation settings were accelerometer_rotation=0, user_rotation=0.
Staged user-rotation lock 1; display confirmed rotation 1 / 2480x1860. Observer
confirmed top/middle/bottom strokes tracked and retained correctly in landscape.
Then staged rotation 3 to test a same-size 180-degree landscape flip. That physical
check is pending. Restore user-rotation lock 0 after all rotation checks.

Observer confirmed opposite landscape (rotation 3) passed. This exercised a
same-size 180-degree flip from rotation 1. Capture after staging the next orientation:
`recordings/20261021T182455.404673Z-9619126060000465`.
Rotation 2 (upside-down portrait) is now staged and awaiting physical confirmation.
The device reports support for multiwindow and split-screen; no developer-setting
override has been enabled. Original rotation lock 0 must be restored afterward.

Observer confirmed upside-down portrait passed. All four orientations now have
physical acceptance. Restored original portrait lock 0; capture:
`recordings/20261021T182725.608907Z-9619126060000465`.
Rotation-run logs include conservative Android retention for one missing/mismatched
packet and two ambiguous ownership results; the observer reported no lost or
misaligned ink. Do not describe this as 100% native-point association.

Staged firmware split-screen via `hanvon.intent.gotosplitscreen`, selecting Bottom
in the Note location dialog. Ink Notes task 75 occupies the upper half and stock
Notes task 77 the lower half; dumpsys confirms both are multi-window. The editor's
native area closed on entry. Stock Notes shows an empty Reading notebook picker.
Requested upper-window writing, erase and Undo to verify Android fallback and
window containment. Physical result and subsequent fullscreen recovery pending.

Observer confirmed split-screen fallback worked. Returned to fullscreen through
the stock `hanvon.intent.cancelsplitscreen` broadcast; `am start --windowingMode 1`
alone had left the task in split-screen. After the firmware transition, dumpsys
confirmed task 75 fullscreen and native initialization/geometry setup appeared in
logs. Screenshot shows the full PDF and retained ink. Capture:
`recordings/20261021T183352.468090Z-9619126060000465`.
Requested quick loops and a light-to-heavy stroke to confirm physical native
recovery and settled retention. That observation is pending.

Observer confirmed fullscreen native recovery passed. Native stroke association
returned an accepted match after the transition. Explicitly saved the notebook;
capture: `recordings/20261021T183456.221320Z-9619126060000465`.

Backed up existing Ink Lab data at
`recordings/20261021T183503.009335Z-9619126060000465`, installed candidate Lab
code 10 / SDK 0.2.0-rc3 in place, and opened native pressure mode. The 158 prior
test strokes loaded and the session reported READY. Requested Clear, light/heavy
strokes and loops, settled retention, then center exclusion on/off and writing
through the region. Physical Lab acceptance is pending; stock Notes coexistence
remains afterward.

Observer confirmed all candidate Lab checks passed: Clear, pressure/loops,
settled retention, center exclusion enabled and drawing again after disabling it.
Capture: `recordings/20261021T183622.222569Z-9619126060000465`, session
`1792607706445`. Recording contains one Clear, two exclusion toggles, retainedMatch
and clearEditedPreview operations, with no reported service failures.

Opened stock Notes and created a separate disposable ruled notebook titled
`SDK-coexistence-test`; the existing stock notebook was not edited. Stock writing
and retention are now awaiting observation. Next switch back to Ink Notes and
confirm writing recovery before final packaging.

Observer reported stock Notes working normally. Its new test page contains the
short phrase and loops. Returned to the existing Ink Notes activity/PDF with
singleTop; native initialization and geometry setup completed. Requested a fresh
loop, settled retention, bottom-button erase and Undo for the return trip.
This last coexistence observation is pending.

Observer confirmed the stock Notes -> Ink Notes return trip passed: native writing, settled retention, bottom-button erase and Undo. Experiment 3 is complete. Explicit save and final acceptance capture: `recordings/20261021T183819.485246Z-9619126060000465`. SDK 0.2.0 and Lab code 11/app code 32 packaging preserve the tested runtime code; final build/install evidence is recorded in release-0.2.0.md.
