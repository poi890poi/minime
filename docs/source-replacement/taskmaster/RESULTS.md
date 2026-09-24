# Taskmaster alone loses general English coverage

September 24, 2026. **Reject as a sole replacement.** The existing context
predictor gains on prompted service dialogues but loses on both independent
genres. Production context, spelling metadata, model and source-rights status
are unchanged. No vocabulary exceptions, mixture weights or threshold tuning.

## Cause and comparison

Taskmaster is licensed authored dialogue, but its six service domains are narrow.
After the predeclared whole-dialogue exclusion rule, 6,165 training conversations
and 135,576 utterances yield 140,815 English context rows (2,583,970 bytes).
Two duplicate training records and one training/evaluation overlap were excluded.
Counts use the existing tokenizer, within-utterance sentence boundaries and
minimum two observations for nonempty contexts. No evaluation tokens enter counts.

The unchanged shared Java ContextModel evaluates 52,004 distinct contexts from
253,275 frozen next-token positions. Table values mean **the percentage of
positions where the actual following word appears among the first three/eight
suggestions**. Every position with preceding context stays in the denominator,
including empty output. These are single-reference recall, not semantic accuracy.

| Source / genre | Positions | Current top 3 | Pilot top 3 | Current top 8 | Pilot top 8 |
|---|---:|---:|---:|---:|---:|
| GUM casual conversations | 9,257 | 15.71% | 14.02% | 23.34% | 21.19% |
| GUM essays | 3,607 | 13.92% | 9.56% | 17.88% | 14.53% |
| Taskmaster development | 120,183 | 20.63% | 48.36% | 28.03% | 60.75% |
| Taskmaster test | 120,228 | 20.60% | 48.41% | 28.14% | 60.91% |

Empty output has zero reference hits and zero emitted slots for every group.
The complete summary preserves hit numerators, denominators, output slots and
nonempty observations for each model and instruction domain. A suggestion other
than the single reference can still be valid English; non-reference slots must
not be described as nonsense or a measured false-positive rate.

## Evidence and limits

[Frozen plan](EVALUATION-PLAN.md), [source/input pins and exclusions](evaluation/manifest.json),
[all metrics](evaluation/summary.json), and compressed inputs, predictions and
pilot counts are retained in evaluation/. The original source notice applies to
Taskmaster-derived material. GUM remains evaluation-only under its recorded source
notices. Source data never becomes new production vocabulary here.

Reproduce from the pinned source cache with pilot.py prepare, compile/run
dev.minime.core.ContextSourcePilot using core/build/manual, then pilot.py summarize.
Preparation freezes model and inputs before Java queries. The pilot does not
alter app assets. Verify decompressed TSV content when comparing gzip archives;
the original input archive retains its creation metadata.

GUM is reused evaluation material, not a fresh holdout. Taskmaster's official
development/test prediction results are now consumed; shared scenarios and short
turns remain even after whole-dialogue decontamination. The test is a context-only
screen, not complete exact/partial typing or apostrophe recovery. Both independent
genres fail the predeclared no-loss screen, so no app build or phone benchmark
is justified for this rejected replacement.

One-pass desktop model load/output times are intentionally not used as a
performance verdict: output I/O, JIT and model language scope differ. A broader
licensed conversational source and a separately sourced grammatical contraction
inventory are still needed before replacing EWT-derived production assets.
