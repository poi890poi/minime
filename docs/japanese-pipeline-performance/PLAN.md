# Full Japanese pipeline performance

Baseline: 8f2003b. Type: performance improvement, preserving candidate order,
acceptance, partial readings, source data and the existing 8 ms worker coalescing.
The previous phone whole-burst p95 is 50.46 ms (gate <=50 ms), with main dispatch
around 21–24 ms and add-on/conversion around 19–21 ms. These aggregate stage
percentiles overlap and cannot be added. No physical touch timing claim.

Profile the actual core refresh path before changing it. Hypothesis to investigate:
repeated string regular-expression compilation, including per-syllable kana
validation, costs more than necessary on Android. If supported, reuse immutable
compiled patterns with fresh per-call matchers; preserve exact patterns and call
semantics, no data-specific shortcuts. Do not change scheduling or the gate.

Freeze/reuse the prior 64-clause, 1311-keystroke stream and the 13,595 multilingual
lookup regression inputs. Runtime sees typed input and dictionary only. Compare
ordered candidate metadata and acceptance; consumed inputs are not fresh accuracy
holdouts. Use independent processes, first/warm results and raw timings. Run core
and pinned Rime before Android, then old/new APK comparisons under explicit phone
acknowledgement, shared mutex, restoration and verified display OFF.

Acceptance: unchanged outputs and repeat phone whole-burst p95 <=50 ms with
meaningful margin, no weakening other language or cancellation contracts. Native
provider stays test-only; passing latency alone cannot admit its ranking quality.

Second isolated experiment after regex phone results: regex alone measured 48.22
then 50.33 ms warm p95, so it failed repeat acceptance. CompositionEngine currently
coerces Chinese intent to Latin when Chinese is excluded. For non-English input,
the Pinyin legality/completeness result cannot change that final intent. Pass the
existing Chinese-enabled flag into classification and skip only this dead work.
English ambiguity and all early technical/Zhuyin/case checks remain unchanged.
Compare against the regex-only class files and preserve its negative phone run.
