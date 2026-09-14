# Compact phone layout comparison

Every word-error percentage means incorrect whole words / attempted words × 100. One wrong letter makes a word wrong. Counts combine three matched noise seeds. These are simulated outcomes, not measured typing speed or human accuracy.

All layouts use a 30 mm letter-region envelope. Primary condition: 60 mm wide, Gaussian scatter with 1.5 mm standard deviation on each axis. QWERTY and split KALQ were rerun and reproduce prior results exactly. Colemak and Dvorak reuse the pinned prior run.

Raw means nearest letter. Conservative correction replaces only unknown literal words when the fixed confidence criteria pass; it leaves recognized words unchanged. Forced correction always chooses a dictionary word. Neither is the Android decoder.

## Conversation

| Layout | Raw wrong words | Conservative wrong words | Forced wrong words |
| --- | ---: | ---: | ---: |
| QWERTY | 580/4,101 (14.14%) | 208/4,101 (5.07%) | 134/4,101 (3.27%) |
| Colemak | 562/4,101 (13.70%) | 226/4,101 (5.51%) | 156/4,101 (3.80%) |
| Dvorak | 575/4,101 (14.02%) | 252/4,101 (6.14%) | 155/4,101 (3.78%) |
| Split KALQ | 426/4,101 (10.39%) | 166/4,101 (4.05%) | 134/4,101 (3.27%) |
| Joined KALQ | 257/4,101 (6.27%) | 87/4,101 (2.12%) | 124/4,101 (3.02%) |
| KALQ reflow 9/8/9 | 368/4,101 (8.97%) | 140/4,101 (3.41%) | 129/4,101 (3.15%) |
| Aligned 9/8/9 | 363/4,101 (8.85%) | 118/4,101 (2.88%) | 124/4,101 (3.02%) |
| Staggered 9/8/9 | 363/4,101 (8.85%) | 118/4,101 (2.88%) | 124/4,101 (3.02%) |

Valid-word confusion means a wrong literal word that nevertheless exists in the source dictionary; membership does not certify that it is common English. Damage means correction spoiled a previously correct word.

| Layout | Valid-word confusions / attempts | Conservative damage / attempts | Same-thumb travel per move |
| --- | ---: | ---: | ---: |
| QWERTY | 44/4,101 (1.07%) | 1/4,101 (0.02%) | 13.40 mm |
| Colemak | 75/4,101 (1.83%) | 0/4,101 (0.00%) | 13.01 mm |
| Dvorak | 80/4,101 (1.95%) | 2/4,101 (0.05%) | 12.11 mm |
| Split KALQ | 52/4,101 (1.27%) | 2/4,101 (0.05%) | 11.58 mm |
| Joined KALQ | 28/4,101 (0.68%) | 0/4,101 (0.00%) | 12.35 mm |
| KALQ reflow 9/8/9 | 40/4,101 (0.98%) | 3/4,101 (0.07%) | 13.27 mm |
| Aligned 9/8/9 | 24/4,101 (0.59%) | 0/4,101 (0.00%) | 14.78 mm |
| Staggered 9/8/9 | 24/4,101 (0.59%) | 0/4,101 (0.00%) | 14.80 mm |

## Conversation stress conditions

Conservative wrong words / attempts. Travel above uses fixed left/right thumb assignment; no timing model.

| Layout | 2.5 mm scatter, 60 mm | Inward bias, 60 mm | Slips, 60 mm | 1.5 mm scatter, 70 mm |
| --- | ---: | ---: | ---: | ---: |
| QWERTY | 1,651/4,101 (40.26%) | 575/4,101 (14.02%) | 736/4,101 (17.95%) | 90/4,101 (2.19%) |
| Colemak | 1,717/4,101 (41.87%) | 604/4,101 (14.73%) | 760/4,101 (18.53%) | 111/4,101 (2.71%) |
| Dvorak | 1,759/4,101 (42.89%) | 596/4,101 (14.53%) | 790/4,101 (19.26%) | 115/4,101 (2.80%) |
| Split KALQ | 1,658/4,101 (40.43%) | 391/4,101 (9.53%) | 711/4,101 (17.34%) | 76/4,101 (1.85%) |
| Joined KALQ | 1,485/4,101 (36.21%) | 266/4,101 (6.49%) | 606/4,101 (14.78%) | 48/4,101 (1.17%) |
| KALQ reflow 9/8/9 | 1,486/4,101 (36.24%) | 395/4,101 (9.63%) | 665/4,101 (16.22%) | 58/4,101 (1.41%) |
| Aligned 9/8/9 | 1,416/4,101 (34.53%) | 379/4,101 (9.24%) | 596/4,101 (14.53%) | 49/4,101 (1.19%) |
| Staggered 9/8/9 | 1,424/4,101 (34.72%) | 383/4,101 (9.34%) | 610/4,101 (14.87%) | 48/4,101 (1.17%) |

## Essay

| Layout | Raw wrong words | Conservative wrong words | Forced wrong words |
| --- | ---: | ---: | ---: |
| QWERTY | 560/3,201 (17.49%) | 248/3,201 (7.75%) | 121/3,201 (3.78%) |
| Colemak | 544/3,201 (16.99%) | 248/3,201 (7.75%) | 125/3,201 (3.91%) |
| Dvorak | 542/3,201 (16.93%) | 251/3,201 (7.84%) | 137/3,201 (4.28%) |
| Split KALQ | 403/3,201 (12.59%) | 163/3,201 (5.09%) | 118/3,201 (3.69%) |
| Joined KALQ | 235/3,201 (7.34%) | 96/3,201 (3.00%) | 107/3,201 (3.34%) |
| KALQ reflow 9/8/9 | 347/3,201 (10.84%) | 149/3,201 (4.65%) | 112/3,201 (3.50%) |
| Aligned 9/8/9 | 353/3,201 (11.03%) | 160/3,201 (5.00%) | 113/3,201 (3.53%) |
| Staggered 9/8/9 | 353/3,201 (11.03%) | 159/3,201 (4.97%) | 113/3,201 (3.53%) |

Valid-word confusion means a wrong literal word that nevertheless exists in the source dictionary; membership does not certify that it is common English. Damage means correction spoiled a previously correct word.

| Layout | Valid-word confusions / attempts | Conservative damage / attempts | Same-thumb travel per move |
| --- | ---: | ---: | ---: |
| QWERTY | 46/3,201 (1.44%) | 2/3,201 (0.06%) | 13.74 mm |
| Colemak | 44/3,201 (1.37%) | 0/3,201 (0.00%) | 12.97 mm |
| Dvorak | 63/3,201 (1.97%) | 1/3,201 (0.03%) | 12.73 mm |
| Split KALQ | 32/3,201 (1.00%) | 0/3,201 (0.00%) | 11.45 mm |
| Joined KALQ | 12/3,201 (0.37%) | 0/3,201 (0.00%) | 12.28 mm |
| KALQ reflow 9/8/9 | 23/3,201 (0.72%) | 0/3,201 (0.00%) | 12.99 mm |
| Aligned 9/8/9 | 29/3,201 (0.91%) | 1/3,201 (0.03%) | 15.23 mm |
| Staggered 9/8/9 | 29/3,201 (0.91%) | 1/3,201 (0.03%) | 15.07 mm |

## Essay stress conditions

Conservative wrong words / attempts. Travel above uses fixed left/right thumb assignment; no timing model.

| Layout | 2.5 mm scatter, 60 mm | Inward bias, 60 mm | Slips, 60 mm | 1.5 mm scatter, 70 mm |
| --- | ---: | ---: | ---: | ---: |
| QWERTY | 1,544/3,201 (48.23%) | 579/3,201 (18.09%) | 730/3,201 (22.81%) | 118/3,201 (3.69%) |
| Colemak | 1,580/3,201 (49.36%) | 565/3,201 (17.65%) | 731/3,201 (22.84%) | 117/3,201 (3.66%) |
| Dvorak | 1,587/3,201 (49.58%) | 606/3,201 (18.93%) | 733/3,201 (22.90%) | 127/3,201 (3.97%) |
| Split KALQ | 1,556/3,201 (48.61%) | 393/3,201 (12.28%) | 658/3,201 (20.56%) | 80/3,201 (2.50%) |
| Joined KALQ | 1,387/3,201 (43.33%) | 254/3,201 (7.94%) | 588/3,201 (18.37%) | 57/3,201 (1.78%) |
| KALQ reflow 9/8/9 | 1,393/3,201 (43.52%) | 404/3,201 (12.62%) | 640/3,201 (19.99%) | 57/3,201 (1.78%) |
| Aligned 9/8/9 | 1,375/3,201 (42.96%) | 418/3,201 (13.06%) | 625/3,201 (19.53%) | 62/3,201 (1.94%) |
| Staggered 9/8/9 | 1,382/3,201 (43.17%) | 420/3,201 (13.12%) | 626/3,201 (19.56%) | 61/3,201 (1.91%) |

## Separate control-boundary audit

Common bottom control row adds 10 mm: total 40 mm height. The diagrams show abstract nearest-center hit regions, not proposed Android key shapes. Internal KALQ Space keys also participate. Letter-to-control and intended-Space errors are not included in the word-decoder figures, which assume correct word boundaries.

| Layout / genre | Letter taps that hit controls / letter taps | Space taps that hit letters / Space taps |
| --- | ---: | ---: |
| QWERTY / conversation | 0/15,303 (0.00%) | 1/4,101 (0.02%) |
| QWERTY / essay | 1/14,955 (0.01%) | 2/3,201 (0.06%) |
| Split KALQ / conversation | 37/15,303 (0.24%) | 0/4,101 (0.00%) |
| Split KALQ / essay | 55/14,955 (0.37%) | 1/3,201 (0.03%) |
| Joined KALQ / conversation | 23/15,303 (0.15%) | 2/4,101 (0.05%) |
| Joined KALQ / essay | 32/14,955 (0.21%) | 2/3,201 (0.06%) |
| KALQ reflow 9/8/9 / conversation | 1/15,303 (0.01%) | 1/4,101 (0.02%) |
| KALQ reflow 9/8/9 / essay | 2/14,955 (0.01%) | 2/3,201 (0.06%) |
| Aligned 9/8/9 / conversation | 0/15,303 (0.00%) | 1/4,101 (0.02%) |
| Aligned 9/8/9 / essay | 1/14,955 (0.01%) | 2/3,201 (0.06%) |
| Staggered 9/8/9 / conversation | 0/15,303 (0.00%) | 1/4,101 (0.02%) |
| Staggered 9/8/9 / essay | 1/14,955 (0.01%) | 2/3,201 (0.06%) |

## Scope and reproducibility

- 20,096 eligible word occurrences from 26 documents / 15 genres; 42,025 dictionary words. Conversations: 1,367 occurrences from two documents, 4,101 attempts per condition. Essays: 1,067 occurrences from one document, 3,201 attempts. All genres remain in raw results.
- Same inspected GUM corpus as earlier experiments. No fresh holdout or human trial. Layout differences were frozen before results, with no corpus-driven search or weight tuning.
- Apostrophe-containing tokens, non-ASCII and over-20-letter tokens are excluded. 738 eligible occurrences are outside the dictionary. Forced correction cannot retain those spellings.
- Nearest-center geometry may absorb otherwise blank regions. The separate control audit is not a complete segmentation, deletion, insertion, correction-effort, fatigue or learning model.
- Staggered packing means stretched hexagonal Voronoi interiors, not regular hexagons or Typewise. KALQ reflow changes thumb assignments and is not the published layout.
- 10,800 new aggregate rows reconcile with per-document telemetry; both historical baselines match. Clean letter and control taps pass. All raw conditions, including negative results, are preserved.
- Batch simulation took 513.4 seconds on desktop; this is not typing latency.

Run `python tools/simulate_compact_layouts.py --out artifacts/compact-new`, then `python tools/report_compact_layouts.py artifacts/compact-new --out docs/two-thumb-english/compact`.
