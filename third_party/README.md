# Offline language data

- `unicode`: official Emoji 12.0 data and Unicode license, used to generate 3,010
  fully-qualified emoji sequences. See [source and license](unicode/README.md).

MinIME does not download dictionaries at runtime. `python tools/compile_dictionary.py` regenerates packaged TSV files deterministically from these vendored inputs. The generated inventory and hashes are in `docs/dictionary-report.json`.

- `mcbopomofo`: [McBopomofo tag 3.1](https://github.com/openvanilla/McBopomofo/tree/3.1/Source/Data), MIT, original Taiwan Traditional vocabulary. Vendored `BPMFBase.txt`, `BPMFMappings.txt`, `phrase.occ`, and root license. The base table supplies source Pinyin-to-Zhuyin mappings. Frequencies are not tuned to acceptance phrases. Unmapped readings: zero. No Simplified-to-Traditional conversion is used.
- `aosp`: [LatinIME en_US_wordlist.combined.gz](https://android.googlesource.com/platform/packages/inputmethods/LatinIME/+/127336e9f29d69607eab55982324b210279ae8c5/dictionaries/en_US_wordlist.combined.gz), pinned revision `127336e9f29d69607eab55982324b210279ae8c5`, Apache 2.0. Retain lowercase alphabetic words with internal apostrophes and source frequency at least 70. This includes plurals; no example-specific promotions or exclusions. The source was identified through the user-requested shine_aac English dictionary report.

Attribution is packaged in `app/src/main/assets/NOTICE.txt` and accessible from settings. Source ranking is a unigram baseline, not a trained contextual sentence model. Tone-tolerant Zhuyin alternatives are lower-ranked recovery candidates.

The generated Chinese TSV stores Pinyin, unseparated Zhuyin, output, source frequency, and the source reading with explicit first-tone marks. The final column preserves syllable boundaries when Space enters first tone; it is derived from the same licensed source rather than inferred from Google data.
