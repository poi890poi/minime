# Decision to integrate conditional pronunciation frequencies

September 25, 2026, before refreshing production assets. Admit the frozen
phrase-share estimator into the existing McBopomofo compiler. Source revision,
vocabulary, readings, phrase counts, English data and optional packs remain
unchanged. No MOE or Google labels are production inputs.

The independent-reading primary screen and Google diagnostic passed; exact
Pinyin access retains all 25,100 pairs. Integration now also retains all 26,489
tone-bearing Zhuyin pairs over 1,395 queries. Both models round-trip through
the binary format with identical full candidate metadata on 1,894 complete and
prefix queries. Both binaries are 50,026,326 bytes; the TSV grows by 4,394 bytes.
Single load observations made alongside other evaluation work are not performance
claims. The estimator runs only during compilation, not on a keypress.

The native/core merged broad comparison preserves all first-one, first-five and
first-eight target and compatible-prefix metrics in each genre/condition. Broad
Space changes only for three unlabelled incomplete-reading probes. The native
chat comparison likewise preserves every first-page target metric and Space
choice. Lower candidate ordering changes as expected from the new base weights;
exact source access remains intact. This complements the already reported core
tradeoffs, including ten net encyclopedic first-eight losses, which remain
visible and are not relabelled away.

Reproducibility boundary: compilation from the pinned source must produce TSV
SHA-256 `5d90b9571c901e9d31e7e0db672e24be8ecfdcfe096313b3d1f204cd34690ce3`.
The original whole-frequency TSV hash is
`69df686f257c478d1a8191af04bc4b59e6761827ad7dc458578bc164115ff473`.
The proposed binary hash is
`d82ec850c067e9300246d487967a19f29ab473eac87503234531dcb3a09d4b13`.
No same-tier parser, logarithm, completion ordering or runtime score change is
combined with this model. The original estimator has no tuned parameters.

This authorizes the scoped data integration, not release certification. Run the
full core checks, rebuild packaged data, verify every other asset remains
unchanged, and test actual packaged candidates on the phone before committing
the shipping slice. The shared count also affects Zhuyin ranking across distinct
Pinyin readings; tone variants of one Pinyin reading remain tied. Completeness
ordering, latency, human touch, source-rights and platform release gates remain
separate work.
