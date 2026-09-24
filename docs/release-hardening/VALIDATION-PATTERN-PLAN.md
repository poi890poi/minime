# Reuse compiled validators in candidate processing

September 25, 2026, before runtime edits. Baseline `6bffd17` / publication
`e89a337`, release-mode APK `2037bfe2a3fe7888596f449aedd54b0f09853e6842f7a44dfd492f4c59942c16`.
The refreshed application profile records 167,503 microseconds of sampled
thread time: sorting 32.10%, conversionInput 17.97%, native regex compilation
13.38% as an exclusive leaf. These are isolated sampled costs, not typing
latency shares, and are not comparable speedup estimates against older traces.

Freeze one intervention: reuse immutable compiled Patterns for the existing
fixed Pinyin/English validation expressions in CompositionEngine and the runtime
English lookup methods of PhoneticDictionary. Each call creates its own Matcher;
no shared mutable matcher, result cache, learning snapshot or input shortcut.
Preserve the exact expressions, evaluation order and full-string matching.
Leave source-loading validation, tone stripping, scheduling, UI, font checks,
dictionary data, candidate policy and limits unchanged.

Verify the complete core suite and byte-identical pinned desktop outputs before
building Android. Retain the old String.matches expressions as the mechanism
benchmark's reference, test realistic and boundary input conditions, and time
both orders after warm-up on ART. Use the same frozen application diagnostic
for traced attribution and untraced cost; expect compilation to leave the hot
path, without claiming that this removes the remaining typing latency.

Then compare unhooked phone typing in ABBA order with identical assets, test APK,
queries and cadence. Keep every result and missed-frame denominator. Reject a
repeated raw/Space tail regression or repeated candidate availability loss in
any mode. Do not admit based only on a favorable language or conditional p95.
This is a small work-reduction experiment, not certification of the large-sample
or physical-touch release gates. Do not relax those requirements.

Phone work needs a new acknowledged window, shared lease, complete restoration
and verified display OFF followed by explicit release. No hosted CI or store
operations. Keep the change and evidence in its own commit if admitted; otherwise
revert runtime edits and preserve the aggregate negative result.

For timing, both A and B use the release build type with an identical, explicitly
injected editor fixture copied unchanged from the existing debug source. Its
manifest adds only that non-exported activity; it does not add Internet permission
or enable debugging. The Gradle init file and fixture live under the ignored
experiment directory and are absent from ordinary release builds. Both packages
include the same validator class so the one test APK can exercise its mechanism;
the baseline's application call sites retain String.matches. Inspect manifest,
assets and APK differences before operating the phone. Final ordinary production
integration is separate from these fixture packages.
