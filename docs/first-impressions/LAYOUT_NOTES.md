# Layout slice

Flat keys, 59 dp portrait QWERTY row pitch, centered lower hints, a six-control
bottom row, language-named space bar, independent geometric control icons and an
idle Chinese/English toolbar replace the boxed layout. Capital slide actions remain
accessible; the duplicate uppercase corner legends are removed. The bottom globe
switches Chinese/English directly; the top globe is the separate next-IME control.
Emoji remains available by holding comma; its symbols switch is in the palette header.

32/32 phone tests passed in 128.221 seconds after corrections. The first run found
a removed emoji-to-symbol control and an unstable first Zhuyin slide after resizing.
The palette route was restored in its owning header. Capturing the settled Zhuyin
layout and first slide reproduced the intended U; a focused run and the repeated
full suite passed with the stronger first-letter assertion and visual evidence.
This observation does not establish a general animation timing guarantee.

Six Google/MinIME layout pairs were observed with real touches in a synthetic editor.
The baseline images and observations are retained here; final candidate/layout images
will follow the next slice. Only Java/UI code changed: builds reused the unchanged,
previously verified four-ABI native libraries. Phone wrappers restored keyboard and
preferences and slept the display after both passing and failing attempts.
