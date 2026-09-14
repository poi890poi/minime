# Compact layout decision

The strongest letter-confusion result is joined KALQ. A continuous three-row
9/8/9 arrangement is also promising. Keep both for control-complete prototypes;
neither is established as faster human typing. The app and its defaults are unchanged.

At 60 mm width and 30 mm letter height, with simulated Gaussian scatter of 1.5 mm
standard deviation per axis, conservative correction leaves:

| Layout | Wrong conversation words / attempts | Wrong essay words / attempts |
| --- | ---: | ---: |
| QWERTY | 208 / 4,101 | 248 / 3,201 |
| Split KALQ | 166 / 4,101 | 163 / 3,201 |
| Joined KALQ | 87 / 4,101 | 96 / 3,201 |
| KALQ reflow 9/8/9 | 140 / 4,101 | 149 / 3,201 |
| Aligned 9/8/9 | 118 / 4,101 | 160 / 3,201 |
| Staggered 9/8/9 | 118 / 4,101 | 159 / 3,201 |

These are whole-word errors, not individual touch errors. Counts combine three
matched noise seeds over the same 1,367 conversation occurrences and 1,067 essay
occurrences. Conservative correction never replaces a recognized dictionary word.
No layout or dictionary weights were fitted to these examples or this corpus.

| Candidate | Decision and tradeoff |
| --- | --- |
| Joined KALQ | Retain for prototype. Removing the gap alone improves letter confusion over the same split arrangement. But it retains four rows, internal Spaces and an unused lower-right area. The separate audit finds 23 letter-to-control hits in 15,303 conversation letter taps and 32 in 14,955 essay letter taps. Full segmentation remains untested. |
| Aligned 9/8/9 | Retain as the simpler continuous three-row prototype. Better word-error results than QWERTY under the tested noisy conditions; primary conversation valid-word confusions fall from 44 to 24 out of 4,101 word attempts. However, modeled same-thumb travel grows from 13.40 to 14.78 mm per move in conversations and 13.74 to 15.23 mm in essays. |
| Staggered 9/8/9 | Deprioritize as an additional design. Same primary conversation result as aligned packing; essentially tied essays. Some stress conditions are worse. No consistent evidence that the stretched hexagonal interior cells justify a separate layout. This does not reject regular hexagons, Typewise, or other letter assignments. |
| KALQ reflow 9/8/9 | Retain as a movement/accuracy tradeoff. Worse primary conservative word errors than joined KALQ, and worse conversation errors than aligned packing, but shorter modeled thumb travel than the aligned alternative. The mechanical reflow is not a newly optimized layout. |
| Colemak / Dvorak | Keep earlier comparison baselines. Their primary corrected-word results do not improve consistently on QWERTY. Dvorak's modeled travel advantage remains relevant. |
| QWERTY / split KALQ | Keep controls. Their reproduced results match the earlier evidence exactly. |

The compact variants also improve on QWERTY under heavier scatter, inward bias,
occasional slips and the wider 70 mm condition in the reported conservative
conversation/essay comparisons. Yet all conservative variants have substantial
errors under heavy scatter. Better relative results do not establish acceptable UX.

Forced correction can repair valid-word errors but also damages correct unfamiliar
spellings. Its primary conversation result is worse than conservative correction
for joined KALQ. Do not treat more aggressive correction as an automatic improvement.

The control audit adds the same 10 mm bottom row to the six rerun geometries.
It is separate from the word decoder and cannot simply be added to its error counts:
events can overlap and a premature Space changes segmentation. The raw audit's
`space_errors` means a miss of the intended bottom-Space target, including another
Space target; it is not a semantic wrong-output measure. The report instead shows
Space-to-letter errors. Diagrams show the actual abstract nearest-center regions,
including how otherwise empty areas are absorbed; they are not finished Android UI.

Next: build isolated, switchable prototypes of joined KALQ and the aligned
three-row layout, with complete controls and explicit literal recovery. Include
reflow if movement cost proves material. Freeze new conversations and essays,
measure real two-thumb contact errors, and time tasks including corrections and
learning. Use that evidence to choose a layout, rather than converting modeled
millimetres of travel into WPM. This current corpus is already consumed regression
evidence, with only two conversation documents and one essay document.

Verification: 10,800 aggregate rows reconcile with per-document telemetry;
2,700 control-audit rows have matching word/letter denominators. Clean-center
controls pass. QWERTY and split KALQ reproduce their prior per-genre results.
Geometry checks confirm that gap removal preserves KALQ's vertical positions and
the aligned/staggered pair differs only in the middle row's horizontal offset.
No Android build, phone operation, or hosted CI was used.

[Full results](RESULTS.md) · [Interactive comparisons](index.html) · [Layout diagrams](layouts.png)
