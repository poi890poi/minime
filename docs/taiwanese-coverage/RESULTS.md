# Taiwanese Han coverage — MinIME 0.7.2

The reported gap is real: all 5,163 shipped beginner headwords/variants were
retrievable with their complete source input, but only 258 exposed a Han form.
After this correction, 3,009 do. Phonetic entries and their ranks are unchanged.

## Cause and correction

0.7.1 attached alternatives only to iTaigi provenance. The authored beginner
vocabulary has phonetics and translations, but no explicit Han field, so its
candidates were excluded. The Mandarin single-character frequency cutoff also
vetoed some basic Taiwanese vocabulary.

Reuse the Taiwan-authored **台華線頂對照典**: 鄭良偉's foundation, edited/expanded
by 楊允言 and transcription/proofreading volunteers, distributed by ChhoeTaigi
under CC BY-SA 4.0. Its 91,339-row source snapshot supplies explicit POJ/HanLo
relationships. Only pairs for already shipped complete phonetic outputs are used;
the whole dictionary is not loaded as new candidate vocabulary. The source,
attribution and pin are in [third_party/taihoa](../../third_party/taihoa/README.md).

The compiler adds **2,961 pairs**, bringing the total to **12,346**, while preserving
all 9,385 old choices exactly. Of the resulting pairs, 9,496 use iTaigi records and
2,850 use Taihoa. A source-based beginner-headword rule permits 357 chosen forms
outside the Mandarin familiarity cutoff. General vocabulary retains the original
cutoff; familiar characters still guide selection among attested new alternatives.

No word list, promotion, Mandarin-gloss substitution, fragment join or speculative
pairing of alternate-reading fields was used. One canonical Han example represents
a complete pronunciation and may not represent every meaning of a homophone.
Neither a beginner dictionary nor Mandarin frequency establishes community spelling
agreement. See the [pre-implementation policy](PLAN.md) and [source audit](source-audit.json).

## Retrieval and annotation availability

Every shipped beginner headword/variant is represented by its shortest tone-free
source alias. Complete, half-prefix and one-initial-per-syllable forms produce
15,489 frozen queries. Targets are production-source records: this verifies
retrieval and metadata, not independent language-model accuracy.

| Input condition | Queries | Phonetic retrieved, both versions | Han available, 0.7.1 | Han available, 0.7.2 |
|---|---:|---:|---:|---:|
| Complete | 5,163 | 5,163 | 258 | 3,009 |
| Prefix | 5,163 | 2,893 | 117 | 1,385 |
| Initials | 5,163 | 1,792 | 63 | 602 |

All 15,489 retrieval ranks match the baseline. Among complete queries, Han-bearing
targets in the first three optional results rise from 254 to 2,962; this is an
ordinal check, not a pixel-based first-line metric. Raw [baseline](baseline-retrieval.tsv.gz)
and [current](current-retrieval.tsv.gz) results preserve every query and rank.

## Remaining gaps

The separately published MOE [504-word beginner handout](https://language.moe.gov.tw/upload/download/jts/02%E8%AA%9E%E8%A9%9E1.pdf)
provides an external spelling inventory. All numbered entries were extracted;
seven contain unsupported/damaged Han text, leaving 497 comparable entries.
Exact Han spelling presence in the sidecar improves **62 → 225**. The other 272
are recorded individually by source stage, not hidden in an aggregate:

- 76 have a source Han field but no matching complete phonetic output in the
  currently shipped vocabulary.
- 82 have no exact Han field in the two approved pairing sources.
- 114 encounter the retained eligibility policy or another chosen canonical
  spelling for the same pronunciation.

This counts numbered spellings, including repetitions, not semantic correctness,
dialect coverage or agreement. The handout's extracted romanization loses some
vowels and tone marks, so it is not used as input gold. Handout text never enters
production generation. Shared dictionary lineage also prevents a new independent
language-accuracy claim. The existing 2–6-character Mandarin-gloss gate in the
phonetic iTaigi importer remains separate debt; this release addresses Han metadata.

## Runtime and performance

Core passes 29,201 mechanical assertions, including new beginner-source full,
prefix and initial annotation and direct Han acceptance. All 12,346 pair records
resolve to the exact source POJ and Han fields; regeneration and 19 source-contract
tests pass. The 3,922 previously exposed mixed conversation/essay/retrieval inputs
retain output order with annotations enabled. Their genres remain separate in
[raw lookup telemetry](lookup-samples.tsv.gz).

Pinned desktop Rime runs 628 native queries and produces the exact 0.7.1 output
hash `8e3ec8cd7cf56571a55fd42e0100b646267e9f5ce3b4b0a1f04af8941f86abe1`.
All 15 other production language assets, including model and Rime data, are byte-identical.

Phone latency is last-key dispatch through worker/main callback, excluding display
refresh, with 186 measured inputs per mode after warmup:

| Mode | 0.7.1 median / p95 | 0.7.2 median / p95 |
|---|---:|---:|
| Chinese | 41.2 / 58.2 ms | 40.9 / 58.1 ms |
| English | 0.47 / 1.64 ms | 0.54 / 1.76 ms |
| Taiwanese | 43.3 / 61.0 ms | 42.4 / 61.4 ms |
| Japanese | 41.8 / 59.8 ms | 41.9 / 58.2 ms |

Taiwanese p99 is 68.5 ms versus 68.0 ms; maximum 86.6 versus 68.7 ms. Separate
sessions do not establish a causal speedup or slowdown. Desktop paired-on/off
lookup median is 307 / 301 µs, p95 3.59 / 3.51 ms, p99 7.39 / 6.86 ms; the largest
paired sample was 59.4 ms versus 31.5 ms off. These tails are retained in the
[benchmark log](benchmark.log.gz), not presented as a speed improvement. Metadata
still loads once off the input thread and adds no dictionary lookup during drawing.

## Android and package

Build and lint pass (zero errors, 17 warnings). Seven phone tests pass: a newly
paired beginner source, existing pairs, direct tap/hold, reversed preference,
disable behavior, accessibility action, fixed geometry, hold cancellation/drag,
stale mode change and measured prediction callbacks. See the [phone log](phone.log.gz)
and [raw phone timings](phone-latency.tsv.gz).

Only RFCR91GWXLX was used. Samsung HoneyBoard was restored; settings and learning
files were compared with the original backups. Display State, mScreenState and
mActualState were verified OFF. AOD settings were preserved.

[Download MinIME 0.7.2 APK ZIP](https://que-put-miracle-wal.trycloudflare.com/MinIME-0.7.2.zip).
The signed debug APK, ZIP CRC and embedded APK bytes were verified. The public
Cloudflare download matches the local ZIP SHA-256; the tunnel is temporary.
Package identities are in [verification.json](verification.json).

## Reproduce

`tools/compile_paired_forms.py --check` verifies deterministic extraction;
`tools/audit_taiwanese_han.py` checks exact source pairs, unchanged old forms,
frozen retrieval inputs and the separate handout inventory. Run `tools/test-core.ps1`
and `tools/test-desktop.ps1` before building Android. `TaiwaneseHanAudit` takes the
paired TSV, frozen retrieval-inputs TSV and output TSV; run it against compiled
0.7.1 baseline classes and current classes respectively. The baseline is `9a37682`.
