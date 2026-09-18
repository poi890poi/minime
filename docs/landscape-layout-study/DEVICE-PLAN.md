# Native phone trial — 2026-09-18

Type: test/tooling and design experiment. No release keyboard or preference changes.
Use the same four geometries as PLAN.md, at the phone's actual usable viewport.
The compact prototype is a geometry control; also inspect the real compact IME.
Native prototypes use production SlideKey and its landscape label renderer, with
the same 22/16 typography across variants to isolate geometry. Static candidate
labels are examples, not a dictionary evaluation. Split window/inset integration
is not implemented by an activity prototype and cannot be claimed as verified.

Before observing results: require all 26 center taps, all 26 downward symbol
slides, all 26 upward capital slides, and repeated text sequences to emit exactly
the intended input. Capture each layout, actual key bounds and available editor
height. Inspect whether text and editor controls are obscured. Report failures,
including when theoretical hit boxes exceed the OS-deliverable touch area.
These are injected touch checks, not human thumb accuracy, WPM or physical latency.
Run the existing Android landscape ink, fixed-height and portrait golden checks.

Only RFCR91GWXLX, with explicit SHINE acknowledgement and phone mutex. Activity
orientation is temporary; finish it and verify system rotation settings unchanged.
Restore preferences/IME, sleep and verify OFF, then release the reservation.
