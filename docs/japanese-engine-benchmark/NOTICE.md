# Evaluation material and upstream attribution

Nothing in this directory is an Android production dictionary or an integrated
conversion engine. Source revisions and hashes are in source-manifest.json.

Conversation excerpts and machine reading annotations in utterances.jsonl.gz are
distributed under [CC BY-SA 4.0](https://creativecommons.org/licenses/by-sa/4.0/):

- RealPersonaChat, Sanae Yamashita, Koji Inoue, Ao Guo, Shota Mochizuki, Tatsuya
  Kawahara and Ryuichiro Higashinaka / Nagoya University dialogue group,
  [source](https://github.com/nu-dialogue/real-persona-chat), CC BY-SA 4.0.
  [Upstream license](../language-contract-benchmark/conversations/real-persona-chat-LICENSE).
- Accommodation Search Dialog Corpus, Yuta Hayashibe / Megagon Labs,
  [source](https://github.com/megagonlabs/asdc), CC BY 4.0 corpus and annotations.
  [Upstream notice](../language-contract-benchmark/conversations/asdc-LICENSE.txt).

The new excerpts retain their source spelling, conversation IDs, turn IDs and
source document hashes. Readings and error simulations are our adaptations;
they are not original human typing records. Conversation-derived result files
retain the same attribution and CC BY-SA 4.0 terms.

AJIMEE-Bench, azooKey contributors, derives its human-reviewed items from the
Japanese Wikipedia Input Error Dataset v2 (Kyoto University NLP group and the
original Wikipedia contributors). The ajimee.jsonl.gz export preserves all source
fields including original item indices and acceptable spellings. It and the
AJIMEE-derived result files use [CC BY-SA 3.0](https://creativecommons.org/licenses/by-sa/3.0/).
See [pinned source](https://github.com/azooKey/AJIMEE-Bench/tree/401666cd56d1a570c2021798b64b6da4396bfd45)
and AJIMEE-README.md. Our minimum character error rate uses the same definition
as their CC0 evaluation utility; no third-party Python implementation is copied.

The converter evaluated locally is Kazuma Naka's kana-kanji-conversion-c-plus-plus
(MIT; KAZUMA-LICENSE.txt). Its input data is Google Mozc's OSS dictionary and
connection costs. MOZC-LICENSE.txt includes Google's BSD terms, NAIST/ICOT
dictionary notices and public-domain Okinawa dictionary notice. Calling the
entire dictionary simply “BSD-3” would omit these additional notices. Source,
engine and generated dictionary binaries remain in ignored artifacts, outside
the APK. Benchmark adapter code is MinIME code; ConverterServer.cpp includes the
upstream MIT CLI at build time and retains its notice here.
