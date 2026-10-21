# Integration contract

## Ownership

`HanvonInk.probe(context)` returns interface availability, firmware identity, origin, discovered methods, and a failure reason. It does not create a drawing area. `available=true` means the bridge can resolve its required entry points, not that a pen stroke has been observed. Validation remains `unverified` until supported by a device test report.

`HanvonInk.attach(view, config, listener)` returns an `InkSession`. Call it and all session operations from the main thread. Binder operations execute in order on one library worker; callbacks are copied/validated off the UI thread and delivered to the listener on the main thread.

Only one attached SDK session is allowed per process. Close the old session before attaching another. Queued teardown precedes the next initialization. Different apps can still use the global vendor service; this library cannot provide a cross-process lock.

The host retains its own Canvas/bitmap/document. Native preview never becomes the sole copy of the user's strokes. The demo uses MotionEvents as the authoritative retained source in both native modes. If a firmware suppresses these events, that mode is unsuitable for this host contract; use the counters and recording to diagnose it rather than claiming successful retention.

## Minimal wiring

```kotlin
val capability = HanvonInk.probe(context)
val session = if (capability.available) {
    HanvonInk.attach(drawingView, InkConfig(), listener)
} else null

// From Activity lifecycle and after dialogs/multiwindow changes:
session?.setForeground(activityResumed && !activity.isInMultiWindowMode)

// In the host's input handler, in addition to its existing drawing:
val token = session?.observe(event, retainStroke = hostWillRetainThisStroke) ?: -1L

// For UP/CANCEL: redraw the retained bitmap, then acknowledge that frame.
// On API 29+ with hardware rendering, registerFrameCommitCallback is the
// preferred software signal. It does not prove the e-ink panel has updated.
drawingView.viewTreeObserver.registerFrameCommitCallback {
    drawingView.post { session?.notifyFrameCommitted(token) }
}

// On pause/teardown or replacement:
session?.close()
```

The frame-commit API needs an API-29 check and hardware acceleration. The demo records a labeled post-draw approximation on API 28/software rendering. Register a retained-frame acknowledgement for the completed/canceled stroke only, not for every MOVE. Keep the token and session instance associated with that frame; don't deliver old-frame acknowledgements to a replacement session.

`retainStroke=false` at DOWN prevents mode-3 preview for tools the host does not retain. MOVE does not change this choice mid-stroke. Always forward UP/CANCEL for a stroke you began observing.

## Geometry and configuration

- `setBounds(areaInView, excludedInView)` takes pixel coordinates local to the attached View, not dp or document coordinates. Null area means the entire View; null exclusion means no exclusion. One exclusion rectangle is supported by this ABI.
- The adapter adds the View's screen position and clips drawing bounds to its visible rectangle. Layout, scrolling, and focus changes recompute geometry. Bounds must remain within the View.
- Origin 0/1/2 conventions and all four display rotations use recovered axis swaps/reflections. Dimensions come from the active display. Model names are informational only.
- Only axis-aligned, untransformed Views are supported in this prototype. Suspend native drawing during host animations, scale transforms, zoom/pan, or other geometry changes outside the supported contract.
- `setWidth(1..64)` uses native display pixels. Black style 15 is fixed for now. As of v0.1.1, `InkConfig` defaults to continuously enabled mode 0 (`NATIVE_CALLBACK`), the working M10 baseline. Mode 3 (`NATIVE_PREVIEW`) remains an explicit experimental choice. The settling delay is 0–5000 ms (default 1000).

## Session states and handoff

The lifecycle is INITIALIZING → SUSPENDED/READY → DRAWING, with FAILED or CLOSED terminating the usable native session. READY means native configuration calls returned. UI labels deliberately do not call that physical drawing success.

The library initializes an inactive zero-area registration, sets pen parameters, then configures and activates its actual drawing bounds while foregrounded. In preview mode it enables native pen drawing at DOWN and disables it at UP/CANCEL. Callback mode keeps the pen enabled while the area is active to investigate autonomous native delivery.

A retained-frame acknowledgement plus the settling delay permits `resetData()` to remove preview data. A new stroke, focus loss, geometry revision, failure, or close invalidates pending clearing. The reset method is global in this firmware, so it is only called during this session's active foreground interval. Avoid concurrent native clients and disable native drawing in multiwindow; the demo does so.

`suspendDrawing()`/`resumeDrawing()` are available for host dialogs and other occlusion. The SDK also watches window focus and View attachment. `setForeground(false)` suspends; the demo closes on pause to make ownership explicit. Close disables and unregisters only the owned handle and continues teardown after a setter error. It does not call global clear, overwrite persistent display settings, or invoke the special -100/-1000 registration rectangles.

Some vendor wrapper setters swallow remote exceptions. Therefore a returned call is recorded as an attempted configuration, not verified effect. The service owns cleanup on callback death; actual force-stop recovery still needs physical testing.

## Native packets

`NativePacket` contains the firmware-reported flag, callback arrival time, and points `(x, y, renderedWidth)` in native coordinates. The SDK validates array length, integral point count, finite values, nonnegative widths, and a bounded packet size before delivery. Use the session's coordinate map to convert to screen coordinates, then subtract the View's screen position.

The service broadcasts `true` for more than one native action. A packet is not guaranteed to identify one unique completed stroke. Packets lack pressure, tilt, per-point timestamps, and color. Do not synthesize those fields, deduplicate by guesswork, or use callback arrival time as sampling time. UI callbacks queued before a geometry revision or close are discarded.

## Failure behavior

Missing or inaccessible APIs produce an unavailable capability report. Unknown origins are unsupported until a transform exists. Attach does not use root, alternate low-level Binder transactions, hidden-API exemptions, or direct device-node access as fallback.

An exception during native work closes the owned area, reports FAILED with operation details, and leaves the host's retained canvas usable. Listener implementations should return promptly and avoid throwing. Monitor both failures and the absence of actual physical input; successful configuration with no input is not a completed test.

## 0.2 native pressure APIs (experimental)

`InkConfig(brush = NativeBrush.PRESSURE_PEN)` selects recovered style 6. `PENCIL` retains style 15. `setBrush()` and `setWidth()` should be used while suspended; resume after the host renderer and geometry match. Width remains in display pixels.

`InkListener.onRenderedPacket(RenderedStroke, arrivalNanos)` supplies immutable, defensively copied points in attached-View coordinates with reported width diameters. It is still a packet, not an asserted unique completed stroke. `RenderedStroke.transformed()` scales coordinates and widths together. Optional `RenderedInkRenderer` draws the stock plain pressure-pen circle geometry on an Android Canvas, including bitmap or PDF canvases. It has no document or tool dependencies.

`StrokeCapture` is an opt-in conservative association helper. Give it host transaction IDs, original input positions, and timestamps from `InputClock.eventNanos(event)`. Keep your own provisional stroke until `finish()` returns an accepted native result. Call `end()` on UP and finish after a quiet 1050 ms interval. `begin()` returns false at the bounded transaction limit; retain Android ink and drain existing transactions. `finish(force=true)` drains ended transactions at an ownership boundary using already received packets. Clear the batch on revision/ownership changes; late packets must not revise another page or undo state.

The matcher checks the native packet's event-time window, start/end positions and path. Identical packets are coalesced only within an unambiguous host transaction. Multiple plausible owners or different candidate packets produce explicit fallback. A callback received before Android dispatch is valid; callback arrival is not a per-point sample timestamp. The M10 tests do not establish this policy for every firmware.

`PenInput.classify(toolType, buttons, source)` returns signals rather than assigning app tools. Bits 32/64/128 are independent; legacy tool 6 maps to primary. Rear eraser is explicit. Finger tool from a stylus source is `PEN_AS_FINGER`; other finger input remains `FINGER`. Do not infer button identity from finger tool alone. The SDK does not mutate firmware button settings.

Applications must explicitly suspend continuously enabled mode 0 for unsupported tools, same-window occlusion and editing gestures; `observe(retainStroke=false)` alone does not turn off mode 0. Retain a session-specific frame token, invalidate stale work on document/geometry changes, and forward stroke termination even on cancellation. SDK callbacks and state notifications do not own your document's undo transaction.

### Packaging

`./gradlew :ink-sdk:publishReleasePublicationToLocalArtifactsRepository` produces a Maven repository under `artifacts/maven`, including an AAR, source JAR, POM, Gradle metadata and checksums. Consumers declare `implementation("dev.hanvon.ink:ink-sdk:<version>")` and point a Maven repository at a copied artifact repository. No sibling source checkout or vendor runtime binary is needed. Ink Lab uses the module directly; Ink Notes independently consumes a vendored artifact and the same public APIs.
