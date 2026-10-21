# Native pressure and input experiment — 2026-10-21

Device: M10, serial `9619126060000465`, ordinary application UID. Firmware unchanged. Builds and native pressure experiments are separate from editor/release acceptance.

## Pressure

Stock Notes maps graffiti pen to style 6 and redraws its reported diameters as circles. The SDK implements that geometry independently; it redistributes no stock code or firmware binaries. Style 15 remains the original pencil comparison.

First alpha01 capture (`20261021T054625.767604Z-9619126060000465`, session `1792561526848`) contained six packets but only two accepted matches. The user saw responsive pressure ink with no apparent post-pause change, but four strokes had used their Android recovery copies. UI dispatch timestamps were later than native callback receipt, and the path check was stricter at lift-off than the endpoint check. This was not accepted as complete native retention.

The matcher now converts Android event uptime to the native callback clock, never substitutes UI dispatch time, and permits the bounded native lift-off tail consistently. Native packet JSON serialization moved to the recorder worker. A six-stroke recorded fixture reproduces the failure and passes with the fix.

Alpha02 retest (`20261021T055758.648713Z-9619126060000465`, session `1792562216009`): **20 packets, 20 accepted associations, 20 newly saved native-width strokes, zero association fallbacks**. The user confirmed smooth tracking and stable shape/thickness after the pause. This establishes the tested pressure path, not optical latency or an unconditional one-packet/one-stroke guarantee.

## Input

Ordered button, rear eraser, bare-finger and palm test captured at `20261021T060449.342596Z-9619126060000465` in the same session:

| Input | Tool / buttons | Event source / device |
| --- | --- | --- |
| Bottom held | Stylus / 32 | `0x5002`, `pen_touch`, device 8 |
| Middle held | Stylus / 64 | `0x5002`, `pen_touch`, device 8 |
| Top held | Finger / 0 | `0x5002`, `pen_touch`, device 8 |
| Rear eraser | Eraser / 0 | `0x5002`, `pen_touch`, device 8 |
| Bare finger / palm controls | Finger / 0 | `0x1002`, `fts_ts`, device 2 |

Bare finger had no visible effect because the lab does not draw finger input; it DID reach the app. Source classification distinguishes pen-converted finger events from ordinary touchscreen events here. The SDK exposes `PEN_AS_FINGER` based on the event's stylus-source bits, not the model or device ID. It does not claim that every converted event is universally the third physical button. Ink Notes maps converted-pen input to Pan; ordinary touch remains separately opt-in.

Two side-button strokes had no native packets and retained recovery ink in the diagnostic lab, as expected from the earlier button capture. This is not evidence that native erasing/selection is implemented. Ink Notes assigns these signals to its own tools and suspends native pen drawing.

## Outstanding physical acceptance

- Ink Notes editor integration and native ownership around tools/overlays/viewport changes.
- Final packaged build restart/sleep/rotation/multiwindow and stock Notes coexistence.
- Other devices and numerical optical latency remain unverified.
