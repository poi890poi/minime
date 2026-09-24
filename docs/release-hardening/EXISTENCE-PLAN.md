# Exact English completion existence

September 24, 2026. Performance experiment against runtime b47bb2a. The current
conversionInput decision calls englishCompletions(...).isEmpty(), allocating and
ranking up to 24 completions just to answer a boolean. A sampled accepted-runtime
profile attributes 16.04% of application time to conversionInput; this is a
diagnostic share, not an expected typing speedup.

Change only that existence decision to an exact short-circuit dictionary query.
Preserve prefix, casing, apostrophe, source-case and context eligibility, exclusion
of an exact word as its own completion, and all ranking/Space/learning/privacy
behavior. No new cache, vocabulary, weights, heuristics or scheduling. Risks are
eligibility drift and source-case collisions; keep the existing full-completion
implementation unchanged as the independent behavioral oracle.

Compare every distinct source prefix in lower/title/upper/mixed casing, both
context rules, apostrophe and malformed inputs. Include source-derived absent
prefixes and an empty dictionary. Benchmark deterministic source-selected
prefixes separately from correctness, with alternating order and warmed rounds.
No benchmark labels are runtime inputs. Source retrieval is not language accuracy.

Run tools/test-core.ps1 and the pinned tools/test-desktop.ps1 before Android.
Require byte-identical desktop output and exact boolean agreement. Only then
compare unhooked phone typing A/B/B/A with the same frozen development inputs,
retaining raw frame/missing/timely counts and conditional latency distributions.
Require repeated responsiveness gains without material raw-text regression or
lost input; preserve negative results. A faster isolated boolean is insufficient
for a general phone speed claim. Existing release thresholds remain unchanged.

Phone uses only RFCR91GWXLX with explicit reservation, mutex, restoration and
verified OFF. No GitHub CI, release upload or unrelated working-tree changes.
