# Optional Japanese whole-sequence conversion

Type: isolated provider integration. Ordinary app behavior is preserved because
AsyncDecoder's provider is unset; no ordinary/release native model is added.
The test-only provider is installed with `japanese(provider)` and disabled with
`japanese(null)`. Configuration changes cancel old work; callers supply a new
query or change configuration at a composition lifecycle boundary.

The core derives a whole reading from its existing WanaKana grammar. An unfinished
romanization suffix is never silently removed. Japanese must be in the enabled
mode pack. Missing readings, missing providers, unavailable results, exceptions,
invalid output and superseded work retain the dictionary fallback.

The merge preserves existing full lexical and source-ranked character choices,
then adds at most eight whole conversions before kana recovery/incomplete matches.
Native duplicates and Latin-only output are excluded. Current source dictionaries
and their partial candidates remain intact. Costs from different providers are
not compared or tuned. Generated recovery carries an explicit transliteration
flag, preserved through candidate transformations; surface equality is not used
to demote an attested kana word. The native provider receives kana alone, not evaluation
labels, learning data, surrounding text or personal context.

Existing request generations suppress late callbacks. Candidate display and Space
acceptance share CompositionEngine's winner. Core regressions cover incomplete
spelling, mode exclusion, fallback, duplicate/malformed output, cancellation,
queued Space and late results after switching to English. The full core suite
passes 289,500 assertions; these are mechanism checks, not language-model accuracy.
The pinned desktop Rime evaluator also completed all 13,014 regression inputs.

The phone test exercises the actual core and AsyncDecoder with a JNI provider;
it does not claim that a normal keyboard service enables the experimental model.
Live typing-time configuration UI and fresh independent ranking evaluation remain
requirements before production activation. See README.md for phone admission.
