# Reusable native ink implementation checklist

The SDK is the product; Ink Lab and Ink Notes are independent consumers. M10 is the verified reference, not a model-name eligibility gate. The approved scope includes native pressure ink using firmware-reported widths; old Android brushes and existing notes retain their behavior.

## Milestones

- [x] Preserve baseline: `v0.1.1`, commit `6e7b025`; create feature branches in both repositories.
- [x] Reusable brush configuration, immutable rendered samples, optional renderer, input classification and conservative stroke association.
- [x] Lab pressure/retention comparison and diagnostic recording.
- [x] USER EXPERIMENT 1: pressure, callback semantics, buttons, finger/palm control. Notify and wait for observer before installation/launch/staging.
- [x] Versioned Maven artifact and independent Ink Notes dependency.
- [x] Width-bearing editable strokes, version-8 storage (v7 when no width strokes), compatibility backups, export/erase/undo/transform support.
- [x] Ink Notes native session adapter, settings, tool/overlay/viewport/refresh ownership and fallback.
- [x] USER EXPERIMENT 2: real editor operations and PDF annotation.
- [x] Automated SDK, persistence, editing, fallback and frame checks; separately signed test build preparation. Physical lifecycle acceptance remains in Experiment 3.
- [x] USER EXPERIMENT 3: signed build acceptance, saved ink, restart, sleep/wake, rotations, multiwindow and stock Notes recovery.
- [x] Release 0.2.0 artifacts, checksums, source publication, documentation and local validation tags.

## Execution rules

Continue independent implementation while waiting for physical experiments. Notify before device mutation or test staging. The observer explicitly authorized installing new builds as active testing proceeds: announce and install without repeatedly asking permission. Reconfirm readiness after a pause or absence before a new physical experiment. Repeat failed experiments after fixes. Never call an experimental path validated based on builds or synthetic events. Preserve provisional Android ink when native packets are missing or ambiguous. The packet boolean does not uniquely identify a completed stroke.

Bottom/middle defaults: erase/select, configurable. Top defaults to pan only with validated identification; otherwise offer an explicit opt-in finger-to-pan policy. Rear eraser remains eraser. Do not change system button settings. Unsupported tools use the Android renderer.

Native preview and saved width appearance must agree. SDK owns firmware behavior, coordinate conversion and normalized signals; app owns document format, undo, tools and UI. No proprietary firmware code or binaries are redistributed. Public publishing/pushing is separate work.

## Evidence / outstanding decisions from experiments

Baseline device evidence: [October 21 session](device-session-20261021.md).
Stock reference: MemoView.UpdatePen / drawPoint path and PslOp.drawPoints. Graffiti style 6 maps to retained circles of radius reported width / 2. Pencil style 15 maps to a constant-width path. Stock callback action 1 and action 3 both emit true; association must remain conservative until measured.

## Implementation progress, October 21

- SDK branch now has typed styles, immutable width samples, optional stock-style circle renderer, normalized button signals, a bounded conservative transaction matcher, event-clock conversion, and Maven/source publication.
- Experiment 1 first pressure pass: six packets, two accepted; dispatch-time association incorrectly rejected queued Android input. Reproduced from capture and fixed using MotionEvent event time. Stock native lift-off tails also required matching the endpoint allowance in the path check.
- Retest on alpha02: user confirmed smooth tracking and unchanged retained appearance. All 20 packets matched 20 retained native strokes; zero association fallbacks. Capture `recordings/20261021T055758.648713Z-9619126060000465`, session `1792562216009`.
- Ordered button/finger/palm experiment completed: stylus-source finger conversion distinguished the tested top-button behavior from touchscreen input; see the pressure-session report.
- Ink Notes now consumes a vendored versioned Maven artifact, includes v8 width-stroke storage with pre-upgrade backup, shared SDK rendering, editable native strokes, configurable button mapping, native ownership/overlay/viewport policy and a separately named sandbox build.
- Automated checks: SDK unit/replay tests, Ink Notes unit tests, lint/builds; emulator Ink Notes fallback/input/v8 reopen/undo/redo/width transform/bitmap/PDF fixture and SDK 100-stroke retention fixture passed. Final checks will be repeated only for subsequent relevant changes.
- Installed Ink Notes certificate SHA256 `462cf807d3cbf43aa2cecf8ee0b973266ad887c166637f0317aeb8e19a3e8fcd` differs from workstation debug certificate `e91d14f5065a1eb6cfbd42aee993b51c6cb16f7cc21d4879b0b4db1e136f9680`. Installed app is non-debuggable, so run-as cannot back up its data. User confirmed that the original key is unavailable. No in-place upgrade/uninstall attempted. Sandbox package `app.einknotes.nativeink` is the independent test fallback; original package and data remain intact.

- Experiment 1 completed: [pressure and source-aware input evidence](pressure-session-20261021.md). True touchscreen contacts are distinguishable from the tested top button by event source.
- User confirmed no matching installed-app key is available and explicitly selected a separately named build. Final install target is now `app.einknotes.nativeink`; in-place upgrade is no longer a completion requirement. Keep upstream/default package unchanged for the future fork/PR.

- Experiment 2 first editor pass: observer reported flawless ordinary native writing; all 14 captured native strokes were accepted. Editing pass exposed stale physical preview: one curly stroke was deleted from the v8 document and screenshot but remained visible on the panel. Lasso then targeted nonexistent ink. Finger triple-tap was an incorrect instruction with finger gestures disabled; top pan was clamped at fit-page zoom.
- Reproduced missing retained-frame completion on the non-native emulator. Registering a callback during onDraw needs a guaranteed subsequent frame. Added frame completion regression, complete bottom/rear erase + undo/redo, middle lasso/move, zoomed top pan and ignored-finger gesture tests; all pass after requesting the subsequent frame.
- Added a reusable suspended-edit redraw acknowledgement to the SDK, guarded against changed ownership, focus loss and late frames. Ignored touch no longer cancels a valid native handoff. Physical editor retest is pending; these fixes are not yet a device validation claim.

- Experiment 2 retest on SDK alpha06: observer confirmed bottom and rear erase now work, then confirmed middle lasso/move, pen triple-tap zoom, top pan and resumed writing all worked as advertised. `clearPreview` and `clearEditedPreview` calls are present in logs. Three zoom-tap associations fell back and were removed by the zoom gesture; do not count them as failed retained drawing strokes.
- User stopped hands-on testing for the night before PDF import. No further M10 UI staging is authorized until the observer is ready. Continue offline work; see the morning handoff.
- Candidate 0.2.0-rc1 additionally shares the retained-frame helper between both consumers, observes display changes including same-size rotation, invalidates capture on coordinate changes, and prevents refresh completion from racing edit handoff. These changes passed build/unit/lint checks but still need candidate-specific physical acceptance. Do not tag final 0.2.0 yet.

- Resumed physical PDF test confirmed alignment at fit-page and zoomed sizes. Ordinary retained redraw flashed until SDK rc2 added matching-black-frame acknowledgement without a global reset; the observer then confirmed no-flash writing and visible erase. Partial erase and Undo also passed.
- Android Marker fallback was invisible in AUTO_DU and visibly laggy in GC16. Recovered stock marker setup: style 15 with monochrome dark/light gray preview, then retained translucent ink and a clearing handoff. SDK rc3 exposes typed preview colors; app rc4 uses native Marker preview while retaining the app's original editable highlighter geometry and pressure samples. Observer confirmed good preview and retained-redraw performance. Marker erase/Undo, black-pen restoration and Settings recovery remain pending.

- Experiment 2 completed on app rc6 / SDK rc3: marker partial erase/Undo/subsequent marker passed after cap-geometry and display-ownership fixes; return to black Pen and Settings recovery passed. Device-exported two-page PDF (532824 bytes) was pulled and both pages rendered/visually checked, preserving backgrounds, pressure ink and marker content. Capture and export details are in the editor-session report. Experiment 3: force-stop/reopen, Home/return, sleep/wake, all four orientations, split-screen fallback and fullscreen native recovery passed; original portrait lock restored. Candidate Ink Lab pressure/retention, Clear and exclusion on/off passed. Stock Notes coexistence is the remaining physical check before final packaging.

- Experiment 3 completed: observer confirmed stock Notes writing and the return to Ink Notes for native writing, erase and Undo. Final capture: `recordings/20261021T183819.485246Z-9619126060000465`. All required physical checks passed. Final 0.2.0 packaging changes only version/dependency metadata; its SDK classes.jar is byte-identical to physically tested rc3.
