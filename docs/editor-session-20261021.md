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
