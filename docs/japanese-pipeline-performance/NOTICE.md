# Evaluation lineage

The 64 clauses are the complete existing development phone stream, reduced to
each clause's final raw input for deterministic core replay. Its source IDs,
reference provenance and adaptations remain in
[the provider notice](../japanese-provider-integration/NOTICE.md) and
[conversation attribution](../language-contract-benchmark/conversations/NOTICE.md).
RealPersonaChat (Yamashita et al., Nagoya University) is CC BY-SA 4.0;
ASDC (Yuta Hayashibe / Megagon Labs) corpus material is CC BY 4.0. The combined
derived inputs remain CC BY-SA 4.0. No corpus text becomes production vocabulary.

Multilingual parity reuses the complete 13,595-row input set from
[the Java lookup audit](../java-lookup-performance/NOTICE.md), with its existing
source attribution. This is consumed regression evidence, not a new holdout.
Desktop core replay excludes Android scheduling and native conversion; phone
pipeline includes them but still excludes physical touch and display rendering.
