# KALQ extension, frozen before results

2026-09-14. Offline evaluation only. Preserve the completed four-layout pilot.

The omitted KALQ layout deserves testing because it explicitly targets two thumbs.
Verify the letter/blank positions visually in Figure 1 of the
[authors' CHI 2013 paper](https://www.pokristensson.com/pubs/OulasvirtaEtAlCHI2013.pdf).
The paper's training study involved six people using a 7-inch tablet; its reported
37 WPM does not establish performance on a small portrait phone.

The two blocks, left to right and top to bottom, are:

```text
M B W H       G T O J
P _ X C       I E _ U
R Y S Z       K A L Q
D N F V
```

Underscores reserve the two space-key positions. All letter cells and these holes
remain in place. Use rectangular cells without the illustration's slight row
offset. Align blocks at the top and outer edges, with an 8 mm center gap and four
columns per block. Horizontal pitch is (width - 8) / 8.

Freeze two adaptations before evaluating either:

- `kalq-30mm`: four left rows fit the previous pilot's 30 mm letter-region height;
  vertical pitch 7.5 mm. The right block uses its top three rows.
- `kalq-40mm`: vertical pitch 10 mm, matching QWERTY's row pitch, at a cost of
  10 mm extra letter-region height. This is not an equal-height comparison.

The same 60/70 mm widths, five contact profiles, three seeds, 20,096 eligible GUM
occurrences, 42,025-word dictionary, and four decoding policies from the original
pilot are reused unchanged. No source/weight tuning or KALQ-specific decoder.
Verify all prior input hashes and the original harness hash before running.

The original simulator supplies oracle word boundaries and selects nearest letters
only. Therefore it does not simulate accidental activation of KALQ's internal
space keys. Retaining their geometric holes does not fix this limitation. Results
are a letter-confusion screen; they cannot establish complete KALQ input accuracy.
Omissions, extra taps, learning, physical grip, and timing remain unmeasured.

Report wrong words / attempted words, raw wrong letters / attempted letters, and
harmful correction explicitly. Main conversation denominator is 1,367 occurrences
per seed, or 4,101 attempts across three seeds; essay is 1,067 per seed, or 3,201
attempts. All genres remain available separately. Existing corpus exposure means
no new holdout claim. Reuse the prior QWERTY results only if input/harness hashes
match; confirm its geometry has not changed.

Keep the published result, our letter-only simulation, and any future human study
separate. No app behavior, defaults, production vocabulary, or phone state changes.
