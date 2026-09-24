# First replacement screen: next-word context

September 24, 2026. Offline data/evaluation tooling only. Freeze this plan before
building the pilot count table or inspecting prediction results. Production data,
weights and app behavior remain unchanged. The source is admitted as a pilot,
not approved for production; the spelling-metadata dependency remains separate.

Use every official training conversation after the previously declared whole-
dialogue decontamination. Apply compile_context.py's existing English tokenizer,
sentence resets, within-utterance one/two-word contexts and minimum count two.
Keep original counts, with no score calibration. Deduplication excludes training
dialogues whose normalized full speaker/text sequence appears in development or
test, and retains the first lexical ID among remaining training duplicates.

Evaluate the unchanged shared Java ContextModel with current production counts,
pilot counts and an empty model. Empty is the baseline for unsupported output.
For every token having a preceding word in the same sentence/utterance, compare
the actual following reference token against the first 1, 3 and 8 suggestions.
Report numerator and denominator, nonempty output and output-slot counts, by
source split and instruction domain. A non-reference suggestion is **not**
automatically a meaningless word: next-word prediction has multiple legitimate
answers. Reference-slot matches are a strict reference proxy, not semantic
precision. Zero outputs produce no false claims but zero next-word recall.

Official Taskmaster development/test conversations are first-use prediction
evaluation for this frozen pilot; source/domain/instruction overlap is declared.
Also use every conversation/essay document in the existing GUM train/dev cache,
separately by genre/document. These sources were already used in earlier MinIME
evaluations and are reused evidence, never a fresh holdout. Pin evaluation hashes
and exclusions before Java queries. Do not pool prompted service dialogues with
essays or casual conversations to hide genre losses.

This is a necessary context-only screen, not complete typing-quality acceptance.
Reject replacement by this source alone if either reused GUM genre loses top-3
or top-8 reference hits, even if the source's own service-dialogue test improves.
If it passes, further exact/partial typing, apostrophe recovery, size and runtime
latency evaluation is required before any production proposal. No automatic
admission, test-driven vocabulary edits or repeated tuning against test results.

Record reproducible train export, raw input IDs/contexts/targets, model hashes,
raw predictions and aggregate metrics. Rights concerns do not justify silently
replacing broad vocabulary with a service-domain model. Model loading and lookup
timings here are desktop JVM measurements, not phone typing latency.
