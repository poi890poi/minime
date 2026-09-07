# Pinyin word-join experiment

Status: stopped before implementation, following the user's instruction to try
existing solutions first. Only baseline results were collected. See
`../existing-engines/` for the native-engine comparison that replaced this work.

Baseline: a3637e3 (0.3.0). Type: behavior change, confined to Pinyin sentence
assembly in PhoneticDictionary and PinyinSyllableIndex. Hypothesis: applying the
existing boundary-only context signal at word joins before beam pruning can
recover coherent sequences that final-list reranking cannot retrieve.

Runtime may use dictionary readings/frequencies, the packaged training-only UD
context counts, raw input and previously committed context. Probe targets are
evaluation-only; no new training data, phrase overrides or model format changes.
English, Zhuyin, literal intent, privacy and editor ownership must remain intact.
Risks: domain-biased counts can worsen conversational phrases, pruning can discard
useful alternatives, and scoring every join can increase latency.

Single candidate: add 0.5 times the difference between contextual and context-free
character scores at each assembled word join (the already established external
boundary weight). Keep word priors, beams and search budgets unchanged. Compare
the exposed 24 reference probes and 24 additional development probes first; reject
clear regression. A surviving method must pass fresh full/initial/mixed probes,
core contracts, binary parity and phone latency/functional checks before shipping.
