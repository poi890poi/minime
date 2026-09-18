# Landscape layout comparison — frozen before evaluation

2026-09-18; baseline `88c32af`. Experimental geometry and preview only. No change
to production layout, dictionaries, ranking or acceptance. Existing unrelated
typography work stays untouched.

Compare on 640×320, 760×360 and 880×400 dp usable landscape viewports (system
bars excluded). These are explicit scenarios, not measurements of the phone.

1. **Compact joined:** current KALQ geometry, 48 dp candidates + 136 dp keys;
   four 25.5 dp letter rows and a 34 dp footer.
2. **Split, same height:** same letter order, row heights and candidate strip;
   two 240 dp blocks anchored to edges. This isolates horizontal geometry for
   letters. Footer controls necessarily get separate left/right Space targets.
3. **Split, taller keys:** same 240 dp blocks; four 44 dp rows, 40 dp footer,
   48 dp candidate strip, total 264 dp. No claim that 44 dp is a universal target.
4. **Full-height sides:** two 192 dp blocks; 48 dp candidate strip across the
   top, remaining height split into four letter rows and a 40 dp footer.
   The middle is transparent. The underlying app does NOT reflow into it.

Preserve the current joined order: MBWH | GTOJ; P·XC | IE·U; RYSZ | KALQ;
Shift Shift D N | F V Delete Delete. Internal spaces retained. Symbols remain
attached to their existing letters, with ASCII/Chinese alternatives. D/N remain
on the left and F/V on the right. This is a split adaptation of current MinIME,
not a claim to reproduce the original KALQ research layout.

Freeze before running: existing GUM test sentences (all genres, separate
conversation and essay reports), and the Chinese essay/conversation inputs from
the frozen recovery corpus. Preserve document IDs and report exclusions. Reused
corpora are not fresh holdouts. No autocorrection or dictionary is applied.

For tap sensitivity, use common random draws across layouts and seeds
20260918/19/20. Profiles: exact centers; independent Gaussian scatter with standard
deviation 6 dp and 10 dp on both axes; 6 dp scatter plus 4 dp toward the screen
center and 2 dp downward; 6 dp scatter with 10% of contacts using 20 dp scatter.
These are assumed stress profiles, not fitted human touch distributions.

Rectangular hit testing must include controls, spaces and dead regions; do not
snap a touch in a gap to its nearest letter. Report correct intended letters /
attempted letters, other-letter substitutions, accidental Space, accidental
controls and misses separately. Space targets are also exercised separately,
using the closest Space to the previous contact as a declared idealized policy.
No gesture, multitouch, insertion/deletion, correction or human-learning claim.

Report geometry independently: letter target dimensions, reserved bottom height,
uncovered screen area (not necessarily usable app area), corpus-weighted inward
reach from the nearest screen edge and within-thumb travel. Travel/reach are
geometric proxies, never WPM, latency or fatigue scores. Report all genres and
seed ranges; do not select the best seed. Require exact-center identity and
nonoverlapping key bounds before trusting the noisy results.

Decision rule: reject designs that hide app controls as a default; retain
tradeoffs rather than claiming a universal winner. Prototype a taller split only
as an optional configuration until phone geometry, symbol gestures, candidate
selection, rotation/composition preservation and real two-thumb input are tested.
Phone work requires direct approval, fresh acknowledged SHINE reservation and
the existing phone lease/restore/display-OFF protocol.
