# Pinyin capitals and prediction improvement

Type: requested behavior correction (upward QWERTY slides produce capitals),
plus candidate-generation and contextual-ranking improvements.

The user clarified that Pinyin is almost the only layout they use. MinIME currently
maps an upward Pinyin slide to lowercase literal output. Although that matched the
tested legacy Pinyin state, it does not meet the requested capital gesture. Change
that mapping to uppercase; preserve downward symbols, direct literal commitment,
one-shot Shift, exact raw recovery, and no persistent language switch.

Prediction diagnosis: current conversion uses exact reading lookup and segmented
complete words. It has no Chinese reading-prefix completion. Preceding Chinese
context is used only for idle suggestions/local votes, not ordinary candidate
ranking. Legacy live traces show useful phrase candidates at incomplete syllables.
The decompiled Java delegates prediction/decoding to its native HMM wrapper, so
those Java interfaces do not supply Google's trained model or its ranking weights.

Experiment contract: runtime may use only current input, previously committed
local Chinese context, explicit local choices, and the licensed McBopomofo data.
Expected phrases and Google's output are evaluation-only. No example-specific
dictionary entries or ranking rules. Secure/literal editor policy remains unchanged.

Record baseline before changes. First evaluate bounded prefix candidate lookup;
then evaluate source-phrase context scoring independently. Compare top-five target
coverage, completed-reading regressions, runtime latency and memory. Run a fresh
phrase set after proposing the method, and keep negative results. Google candidate
agreement is a compatibility probe, not an estimate of unrestricted language quality.

Expanded requirement and live evidence: the user requires any mixture of full
syllables and abbreviated syllables, including one Latin initial per Han glyph.
The 18-sequence Pinyin study confirms nh/nih/nhao → 你好, jt/jtian → 今天,
srf/shrf/srufa → 輸入法, and composition across dictionary words (wxsrf → 我想輸入法).
Whole-reading prefix lookup is insufficient and will be supplemented for Pinyin by
a syllable trie with bounded search and a word lattice. Each typed syllable may
consume a nonempty prefix of a source syllable; apostrophes force boundaries.
Source frequencies and a fixed missing-letter prior rank paths. No Google model,
candidate list or evaluation target is included in the app. Search budgets bound
ambiguous input; that may limit recall, so test fresh mixtures and long input.
Keep exact homophones reachable, raw slot zero, English/URL/privacy policy,
candidate selection, and deletion. Target steady desktop p95 below 20 ms; measure
Android gesture/editor behavior and dictionary load/memory separately.

Measured Android negative case: 32 repeated s initials took median 794 ms and
p95 932 ms despite the visited-state budget. Character-count diversity had leaked
into every intermediate sentence beam, multiplying candidate cross-products.
Keep diversity at word/display boundaries, but cap intermediate sentences to six
paths. Re-run identical phone benchmarks and phrase holdouts before accepting the
performance correction. Contextual scoring remains deferred; no stronger model
is claimed by this iteration.
