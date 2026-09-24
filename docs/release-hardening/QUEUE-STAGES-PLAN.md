# Separate schedule, worker and main-queue delay before changing them

September 25, 2026, after LANGUAGE-STAGES-RESULTS.md and before implementation.
Diagnostic tooling only. The accepted runtime remains `6bffd17` / publication
`e89a337`. Existing provider averages and main-callback costs do not account for
the entire measured request-to-delivery interval; they do not identify its cause.

Build an isolated diagnostic copy of AsyncDecoder with bounded synthetic-session
timestamps at request/schedule, actual worker start, provider completion, main
post, main callback entry and completion. Preserve the same 8 ms delay, single
scheduled worker, ordinary Handler, provider order, cancellation guards and
exception behavior. Do not change thread priority or mark callbacks asynchronous.
Use request IDs and retain cancelled/stale/undelivered episodes. No user text,
surrounding editor data, persistent telemetry or production instrumentation.

Keep the diagnostic source patch and package identities outside ordinary release
source sets; verify accepted production sources and packaged assets are restored
after building it. Instrumentation may read the diagnostic observations only
after the test session, and may not use them to schedule work or modify input.
Test monotonicity, request identity, cancellation and callback boundaries before
phone replay. Timestamp overhead prevents release-latency or speedup claims.

Use the frozen language-specific shard 0 in Chinese, Taiwanese and Japanese at
both cadences. No selection by slow spellings and no new corpus/parameter tuning.
Report schedule deadline overshoot, provider elapsed work and post-to-main-entry
separately, with full request/cancellation/frame denominators and distributions.
Do not subtract unrelated percentiles or count held/undelivered results as fresh.
Inspect all stages and languages before declaring one production intervention.

Core/provider behavior must remain equivalent. Any later change to scheduling,
threading or main delivery is a separate experiment requiring unchanged candidate
order/acceptance and repeated unhooked phone comparisons protecting raw/Space
tails in every mode. The rejected zero-delay and font experiments stay rejected.

A new explicit SHINE acknowledgement and shared phone lease are required before
any device command. Bound tests/cooldown inside the reserved window, restore
original APK/preferences/learning/IME, verify actual display OFF and release
explicitly. No final package, hosted CI or Play action is authorized by this plan.
