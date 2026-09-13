# Richer screenshot refresh

Release-material and test-harness change, September 13, 2026. Replace the sparse
single-line demonstrations with eight actual phone captures: Chinese/English
conversation, English writing, Taiwanese paired forms, Japanese everyday phrases,
the existing 加年端社 regression, more Rudy trail/place suggestions, and expanded
Taiwanese/Japanese candidate views. Use 0.8.5-review1's unchanged keyboard/runtime.

Notes text is authored demonstration context, not a benchmark or a claim that
MinIME is a Notes application. Record the prefilled context separately from typed
keys and visible candidate labels. Never fabricate or retouch the keyboard or
change dictionary entries/ranking to make the images attractive. Keep the original
jianianduan quality check and 加年端社 example. If it fails, preserve the evidence
and report the failure instead of disguising it with another query.

Reuse the verified existing app APK; rebuild the screenshot test APK only. Check
IME identity, all candidate labels, legibility, clipping, realistic example text
and capture dimensions on the phone. Reserve the authorized phone explicitly;
the shared mutex covers viewport reads/changes, capture and cleanup. Restore
preferences/IME/viewport, sleep and verify OFF on failures as well as success.

Deliver unmodified PNGs, input/candidate provenance, accessible descriptions and
a screenshot-only ZIP. Existing 0.8.4 release binaries and their listing kit are
separate from these refreshed review images. No Play publication is authorized.

First visual review: keep the expanded `chiah` capture as a flagged Han-example
finding, with source attribution in `findings/`. Scene 07 instead demonstrates
actual tap/hold selection of phonetics/Han and asserts both editor outputs.
English `hello` has only a literal choice in this snapshot; use `thank` to show
word completion. Neither presentation revision changes runtime behavior or claims
to fix the recorded finding. Deliver a repository-hosted ZIP to avoid relying on
an expiring tunnel for this small screenshot archive.
