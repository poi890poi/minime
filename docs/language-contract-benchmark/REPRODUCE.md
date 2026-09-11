# Reproduction and boundaries

This directory contains test tooling and isolated experimental patches. It is not
an app implementation. Start from baseline **5dd9bb8dd6ee24500a6e66cd182ae640647ef026**
in a separate checkout and copy this benchmark directory into it. Preserve the
existing generated model/add-on assets and cached GUM files whose hashes are in
`manifest.json`; regenerate missing assets through the repository's existing
documented compiler workflows and verify those hashes before comparison. Do not
silently compare a refreshed dictionary with this baseline.

The original experiment used Windows, Microsoft OpenJDK 17.0.11, a 2 GiB JVM cap,
the repository's pinned desktop Rime DLL and bridge, and Python 3.12 for corpus
preparation (Python 3.7 suffices for analysis). Configure the JDK path in `run.py`
if needed. No Android device is involved.

```powershell
python docs/language-contract-benchmark/prepare.py
python docs/language-contract-benchmark/run.py --compile
python docs/language-contract-benchmark/run.py --coverage
python docs/language-contract-benchmark/run.py --mechanics
python docs/language-contract-benchmark/run.py --perf
python docs/language-contract-benchmark/run.py --working-set
python docs/language-contract-benchmark/analyze.py
```

Do not run other CPU benchmarks during the performance and working-set passes.
Output audits cache provider results and are never used as latency measurements.
JVM heap is not Android PSS; synchronous desktop query time excludes Android
debounce, scheduling, touch handling and frame presentation. Source preparation
overlapped portions of the recorded latency run; small speed differences are
inconclusive. The first query can include native Rime startup.

For the new conversational corpus, committed inputs and reference metadata are
sufficient to reproduce engine scores. To regenerate references independently:

```powershell
python -m pip install --target artifacts/language-contract-benchmark/python-deps -r docs/language-contract-benchmark/conversation-requirements.txt
python docs/language-contract-benchmark/fetch_conversations.py
python docs/language-contract-benchmark/make_conversations.py
python docs/language-contract-benchmark/run.py --conversations
python docs/language-contract-benchmark/analyze_conversations.py
```

Use Python 3.12 for these preparation commands. Downloads are pinned and cached
under `artifacts/language-contract-benchmark/conversation-sources`; all archives
and inputs have SHA-256 fingerprints. Gzip container timestamps may differ on
regeneration; decompressed references and frozen TSV bytes must agree. Reading
tools are evaluation-only dependencies, not app dependencies. See
`CONVERSATION_DATA.md` and `conversations/NOTICE.md` for source rights, exclusions
and attribution.

The initial holdout is consumed by this run. Future implementation tuning must
use development data and obtain additional unseen conversations/files for a new
confirmation run. Neither a source-entry lookup nor a synthetic gesture assertion
is a language-model accuracy measurement. The ordered turns are retained for
future stateful composition tests; this benchmark currently scores independent
word, two-token and clause compositions, not complete interactive dialogues.
