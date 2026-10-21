# M10 session: 2026-10-21

The observer confirmed readiness before staging. Installed final v0.1.0 debug APK SHA-256 `6bbfb750a4c9d3b414305d5a0cc9f7d7e865c0e3890a3b9d317b77a4ad02b112`, retaining application data. Ordinary UID remains 10128.

## Observations so far

- The open/close fixture emitted all 20 cycle records and `cyclesComplete`, with 20 owned-area release reports and no FAILED states. The final session reached READY. Not every initialization/geometry diagnostic survives rapid replacement because close removes queued UI callbacks; do not treat this as complete per-call instrumentation or physical recovery proof.
- Actual pen input and native point packets arrived in mode 3. The first preserved drawing snapshot had 18 retained stylus strokes, seven native packets, and five frame acknowledgements. Android records also included finger-tool input, which the demo did not retain as black strokes.
- The observer reports real-time writing is possible, but live preview has gaps and hesitations. After the eventual redraw, the complete drawing appears. This confirms a preview-quality problem; it does not prove every sample/stroke is preserved or quantify latency.
- The observer described the subsequent native callback inspection test (mode 0, continuous enablement) as “totally flawless.” The preserved session has 57 stylus stroke starts, 57 save events, and 57 native callback packets, with no recorded failures. These equal counts do not by themselves establish sample-by-sample equivalence.
- That successful mode-0 run still used v0.1.0's MotionEvent repainting and generated three `clearPreview` calls. Therefore unconditional resets or per-MOVE invalidation alone are not established causes of the mode-3 failure. Mode selection and continuous versus per-stroke enablement changed together; isolate them before attributing causality.
- The subsequent ordered button test mapped bottom to button state 32, middle to 64, and top to finger tool type 1 with button state 0. These identify the held-button strokes; hover-only presses did not produce a corresponding complete app-level button sequence.
- The observer confirmed that native ink stayed out of the excluded center rectangle and remained under the tip after rotating to landscape.
- Home/return and force-stop/relaunch preserved all 128 saved strokes byte-for-byte: SHA-256 `04741b8e5587d1f5ca3e25478e1b44c241c92c648405f302ba57ef26976f8f75` before and after. The recreated session reached READY. Server logcat recorded `wwn onCallbackDied` at 00:22:14.297 during force-stop.
- The observer then confirmed clean writing after restart, after sleep/wake, and in stock Notes on a disposable page. Subsequent teardown logs showed `mAreas.size:0`. These are successful observed recovery checks, not a guarantee for every system interruption or concurrent-client configuration.

Evidence directories:

- `recordings/20261021T051154.677681Z-9619126060000465/`: pre-upgrade backup.
- `recordings/20261021T051223.203966Z-9619126060000465/`: fixture completion.
- `recordings/20261021T051313.117604Z-9619126060000465/`: first actual drawing snapshot.
- `recordings/20261021T051424.371979Z-9619126060000465/` and `recordings/20261021T051521.185467Z-9619126060000465/`: subsequent read-only captures.
- `recordings/20261021T051730.571762Z-9619126060000465/`: preserved data before switching to mode 0.
- `recordings/20261021T051906.037926Z-9619126060000465/`: successful visual mode-0 test, session `1792559851017`; 57 stroke starts/save events/native packets, eight frame acknowledgements, and three preview resets. The retained document contains 124 strokes including drawings from earlier sessions.
- `recordings/20261021T052012.170802Z-9619126060000465/`: ordered bottom/middle/top button test. The bottom/middle strokes generated generic button press/release events (`actionButton` 32/64), and the top stroke arrived as finger input. No KeyEvents or native callback packets were recorded for this button-test interval; two stylus strokes were retained by the demo and finger input was not.
- `recordings/20261021T052200.809244Z-9619126060000465/`: clipping/landscape test and pre-recovery drawing.
- `recordings/20261021T052327.601044Z-9619126060000465/`: recovery, callback-death, and teardown evidence.
- `recordings/20261021T052421.795038Z-9619126060000465/`: end-of-test capture. Before/after saved-drawing hashes are also in `artifacts/validation/recovery-before.json` and `recovery-after.json`.

## Pen button mapping

| Physical control, in user-provided order | Observed application input | Current secure setting |
| --- | --- | --- |
| Bottom held during stroke | Stylus tool 2, buttonState 32; generic press/release actionButton 32 | `hanvon_type_pen_key_setting=0` |
| Middle held during stroke | Stylus tool 2, buttonState 64; generic press/release actionButton 64 | `hanvon_type_pen_key_setting2=1` |
| Top held during stroke | Finger tool 1, buttonState 0 | `hanvon_type_pen_key_setting3=3` |

Secure settings were re-read during the test and remained 0/1/3. The top-button result matches the firmware's pen-to-finger configuration for value 3; it does not establish what the same button would emit under a different setting. Two additional finger contacts preceded the ordered held-button strokes; their physical origin is not assigned.

This verifies the Ink Notes compatibility diagnosis: its actual notes editor ignores button states 32/64 and drops all finger-tool input. Ordinary application-level recording succeeded where earlier raw `getevent` attempts were empty.

## Stock Notes comparison

Use `../penstar_re/analysis/jadx/hvNote/sources/hanvon/aebr/hvnote/memo/MemoView.java` as the reference, not only the patched framework View's compatibility hook.

| Behavior | Stock normal pen | v0.1.0 demo preview |
| --- | --- | --- |
| Native mode/enable | `setMode(0)` enables the pen and sets draw mode 0 | Mode 3; asynchronous enable/disable per Android stroke |
| Retained bitmap | `onPenTouchUpStatus` posts to `UpdateMemobitmap`; native points converted and rendered | MotionEvent points rendered into bitmap continuously |
| Android repaint | Native packet processing schedules a 500 ms delayed repaint; runnable returns while `isDrawing()` | Invalidates on every MotionEvent, plus diagnostic UI updates |
| Preview reset | Delayed runnable resets for marker/button conditions; plain black pen goes directly to invalidate | Unconditional reset after retained-frame acknowledgement and 1000 ms settling |

Reference locations: constructor lines 334–350 (delay/runnable), touch handler around 1457 (cancel pending repaint), normal pen mode around 1815, callback dispatch around 2426, native-point persistence/rendering around 2970–3039. The runnable's drawing guard and conditional reset were crosschecked against `analysis/apktool/hvNote/smali_classes2/hanvon/aebr/hvnote/memo/MemoView$5.smali`.

These differences are concrete candidates for the preview failure, not established causes. Next comparison should isolate continuous mode-0 drawing, delayed Android repaint, and the plain-pen no-reset behavior. Preserve the failing mode-3 path for A/B evidence. Native packets remain separately recorded until their delivery/duplication semantics are established.

## Outcome and follow-up build

Mode 0 with continuous enablement is the working SDK baseline on this firmware. v0.1.1 makes that the default `InkConfig` and adds a normal mode-0 demo choice without callback dots; existing inspection and mode-3 comparison choices remain. It changes the default and presentation, not the tested native call sequence. The SDK still accepts matching interfaces on other models without model-name gating.

Numerical optical latency, multiwindow behavior, all four orientations, button-driven tool implementation, and other devices remain unvalidated. The plain black pen's stock-notes repaint strategy is a potential optimization, but this successful test does not justify changing working reset/repaint behavior without another controlled comparison.
