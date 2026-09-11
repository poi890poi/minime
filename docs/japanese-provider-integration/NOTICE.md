# Evaluation attribution

The instrumentation-only native adapter includes Kazuma Naka's MIT-licensed
converter at f8af0c2e6a7538f83e7483e9171ebec04fd963a0 with the recorded posting
index and stable-tie patches. See [license](../japanese-engine-benchmark/KAZUMA-LICENSE.txt),
[pins](../japanese-engine-benchmark/source-manifest.json),
[posting patch](../japanese-predictive-review/posting-index.patch) and
[tie patch](../japanese-determinism/stable-ties.patch).
The phone follow-up additionally applies [stable beam pruning](portable-beam.patch).
Google Mozc data retains its full [Google/NAIST/ICOT/Okinawa notices](../japanese-engine-benchmark/MOZC-LICENSE.txt).

The timing stream derives from the already consumed RealPersonaChat and ASDC
development conversations. Preserve [author/source attribution and adaptation
licenses](../japanese-engine-benchmark/NOTICE.md); conversation-derived output
files use CC BY-SA 4.0. Existing WanaKana 5.3.1 (MIT) transduction is used by the
core adapter, while the native parity stream retains the old jaconv inputs for
an unchanged desktop/phone comparison. Their behavior is not assumed identical.

No new source vocabulary is hand selected. The native library and model are
included only in an explicitly requested test APK, not ordinary/release app
assets. The normal app's optional provider remains unset.
