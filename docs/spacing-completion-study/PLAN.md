# Auto-spacing and completion observation plan

Observe the installed legacy Google Zhuyin and current MinIME on authorized phone
RFCR91GWXLX, using the same local multiline editor and named field-flag variants.
Freeze semantic action sequences before the run. Native touch targets come from
live accessibility nodes; record editor text (including literal spaces), composing
span, selection and keyboard candidates after each action. Missing candidate or
key actions are unavailable observations, not evidence of spacing behavior.

Distinguish prefix completion, correction on Space, next-word prediction, literal
word acceptance, actual versus deferred spaces, punctuation and deletion. Test
English and Pinyin paths separately, including Chinese/English boundaries and
explicit mode switching. Repeated examples here are mechanism probes; this is not
an independent language-quality corpus or hit-rate benchmark. No runtime changes
or dictionary tuning follow automatically from this study.

Google is observed as installed; prior personalization is not reset and results
must be scoped to observed configuration. MinIME uses fresh study preferences.
Compare only matching delivered inputs and actions. Existing model/data and
reference APK are not copied into production. The existing parity harness has
uncommitted typography work; invoke only testPairedObservations and preserve it.
Record unavailable probes and any revised follow-ups separately from the frozen
plan. Google/reference settings must be observed before claiming default behavior.

Shared-device ACK and mutex are required before any ADB. Preserve prior IME and
MinIME settings/learning through the guarded runner. Verify display OFF after each
batch and final evidence collection, then explicitly release the reservation.
