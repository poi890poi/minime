# Bind the invariant learning-key prefix once per candidate sort

September 24, 2026. One-variable performance trial against accepted runtime
1ea175a. The sampled application profile still attributes substantial work to
sort-key preparation, including repeated StringBuilder construction in
LocalLearning.key. This is a hypothesis about avoidable work, not proof of
end-to-end benefit.

Add a shared Learning counter binding that fixes context and raw reading for one
sort. Default implementations delegate to the existing count method. Android
LocalLearning precomputes the invariant context/tab/reading/tab prefix once, then
reads the same SharedPreferences integer using prefix plus candidate text.
Values stay live: there is no snapshot, persisted-format change, getAll scan,
cross-query cache or change to choice identity. Private fields must not bind or
read learning. Only the existing converted-candidate sort adopts the binding.

Verify exact candidate order/default/acceptance through shared-core regressions
and unchanged desktop output before Android build. Check the binding against
existing count with empty, populated and cleared/restored preferences, subsequent
votes, Unicode choices and context/reading isolation. Benchmark empty/sparse/full
learning stores as a mechanism test, without treating generated choices as language
accuracy. No changes to data, scores, settings, rendering or scheduling.

After mechanism verification, repeat the existing unhooked phone typing comparison
in A/B and B/A order with the same binary assets and test APK. Require repeatable
Chinese benefit without a material raw-frame regression; mixed gains alone are
insufficient. Record misses and >100 ms tails alongside observed-frame percentiles.
If gains do not reproduce, retain only the experiment evidence and revert runtime.
This short reused replay cannot close the larger release latency or human-touch
gates. Retain independent commits and the exclusive phone lease/cleanup procedure.
