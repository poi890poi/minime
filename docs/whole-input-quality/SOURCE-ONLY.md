# Stop assembly at its source

**Decision corrected:** no automatic construction is the null baseline. The
earlier rejection below relied on lost whole-clause recall relative to an
unvalidated incumbent; that does not prove the incumbent deserves inclusion.
The measurements remain valid diagnostic evidence, not a net-benefit verdict.
The production edits were reverted; the complete experimental change is preserved
in `source-only/implementation.patch`. See `RECALL-PRECISION.md` for the current
contract and the need for a controlled null comparison. The following describes
the historical experiment, not current runtime behavior.

September 12, 2026. Behavior change requested after the construction-confidence
experiment: Chinese decoders return stored entries rather than constructing new
phrases by joining words. This addresses meaningless output at its origin.

Rime's active script translator now disables sentence generation. Java removes
both its character-span beam and its multi-word reading-trie traversal. Full,
initial, partial and mixed readings still query a single stored entry. Explicit
user phrase learning remains available. Chinese/English intent, other focused
languages, dictionaries and source weights are unchanged.

Construction-specific demotion, confirmation checks, duplicate attestation repair
and automatic-learning exceptions are removed. Native provenance stays in the
evaluation transport so tests can detect accidental re-enabling of construction.
Whole-input matches precede shorter consumed-prefix choices in the shared engine.

Risk: legitimate novel phrases also lose automatic assembly. Existing frozen
prose inputs will measure both coverage losses and gains; they are consumed
regression evidence, not a new holdout or a measure of conversational accuracy.
No source entries, complaint-specific exceptions or model weights are added.

This supersedes the intermediate whole-input ranking workaround. Historical
reports in this directory describe that earlier experiment, not this change.
Required gates: shared core, pinned native evaluator, broad before/after replay,
then Android build and real phone screenshots with restoration and display OFF.

Negative experiment: `translator/enable_sentence: false` does not control Rime
1.16.1's script translator. It controls the table translator. The native probe
still emitted sentences, so that schema change was discarded. The shared CMake
build instead removes the `MakeSentence` call before compiling the pinned script
translator; there is no output filter and the original source archive is intact.

## Results

Baseline: committed `61018b0`, including its construction demotion. Treatment:
source-only native/Java lookup plus whole-input ordering. Same frozen prose
inputs, dictionaries, add-ons and source weights; no target enters inference.
These previously evaluated documents are regression evidence, not a fresh holdout.

| Reserved input (1,530 each; add-ons on) | First choice before → after | First 3 before → after | First 8 before → after |
|---|---:|---:|---:|
| full | 204/1530 → 231/1530 | 207/1530 → 242/1530 | 674/1530 → 244/1530 |
| initials | 107/1530 → 124/1530 | 154/1530 → 168/1530 | 173/1530 → 208/1530 |
| mixed | 147/1530 → 184/1530 | 202/1530 → 228/1530 | 307/1530 → 241/1530 |
| partial | 4/1530 → 68/1530 | 35/1530 → 117/1530 | 35/1530 → 150/1530 |

Native: **0 assembled sentences across 11,220 inputs**. The separate
pinned desktop gate completed 13,014 corpus inputs / 11,272 uncached native
queries, and rejects any native sentence. IPC + decoder p50 1.945 ms, p95
2.945 ms, max 123.738 ms. This is desktop evaluation, not Android touch latency.

The tradeoff is real: many correct novel phrases disappear along with nonsense.
With add-ons on, full-spelling first-page coverage falls from 44.1% to 15.9%.
First-choice and initial/partial lookup improve, but this is not an overall
coverage improvement. Dictionary gaps must be addressed through systematic
attributed sources. No complaint-specific entries or promotions were added.

Shared-core replay flush p95 for reserved full input with add-ons: 11.265 ms
before, 0.547 ms after. This excludes native execution, Android scheduling and
rendering; it is not a phone latency claim. Raw per-input records and all
condition/add-on breakdowns are in `source-only/`.

Core: 306,120 behavioral assertions passed, including full/initial/mixed
source lookup, continuity, English apostrophes, focused languages and learning
privacy. Assertion count is not language accuracy. Old synthetic assembly
expectations now assert absence or explicit word-by-word input. Frozen evaluation
labels are unchanged. Android integration keeps the previous targets in its
coverage report but replaces generator-dependent hit floors with the no-assembly
contract and known stored-phrase/offset/lifecycle checks.

No fresh Mandarin conversation corpus was available for this turn. The older
desktop corpus name includes “conversation” but much Chinese material is prose.

Original decision (withdrawn): reject because full-spelling recall drops.
Current decision: retain as a diagnostic ablation; evaluate construction as an
addition to null. This patch also changed ranking and policy, so it must not be
mistaken for a controlled single-variable baseline or a release-ready change.

Reproduction (local only):
```powershell
# In an isolated checkout, apply source-only/implementation.patch first.
./tools/test-core.ps1
./tools/build-desktop-metadata.ps1 -SourceOnly
python -X utf8 tools/benchmark_native_construction.py --role development --output-dir docs/whole-input-quality/source-only --require-no-assembly
python -X utf8 tools/benchmark_native_construction.py --role reserved --output-dir docs/whole-input-quality/source-only --require-no-assembly
python -X utf8 tools/make_construction_replay.py --input-dir docs/whole-input-quality/source-only --output-dir artifacts/native-metadata/source-only
python -X utf8 tools/run_construction_core.py --variant policy --role development --output-tag source-only --fixture-dir artifacts/native-metadata/source-only
python -X utf8 tools/run_construction_core.py --variant policy --role reserved --output-tag source-only-reserved --fixture-dir artifacts/native-metadata/source-only
```
