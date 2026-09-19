# Development decisions before validation

The 21,000 development episodes exercise 3,000 source phrase families with seven
spellings and an appended suffix. These measure access to a specified stored word,
not conversation accuracy. Frequency bands describe the dictionary's own counts.

| Variant | Target prefix available | Target in first eight | Leading target |
| --- | ---: | ---: | ---: |
| Baseline 96db0d6 | 12,957 | 8,482 | 8,345 |
| Union of longest eight and highest-score eight | 13,086 | 8,434 | 8,345 |
| Union plus source-score preview | 13,086 | 619 | 417 |

Reject source-score preview. Frequent short words overwhelm longer intended
prefixes; do not tune new weights or exceptions to recover these labels.

Advance retention alone to the frozen validation split and the earlier composition
corpus. It recovers 129 target-prefix selections with no lost targets, changes to
Space, whole-choice inventory/order or the first two glyph identities. The added
words displace 48 targets below the first eight. Raw comparisons confirm that
new, shorter source words enter those positions; leading-preview targets are
unchanged. This is an expanded-access tradeoff, not a first-page improvement.
No further ranking change will be selected on the validation split.

Desktop lookup p95 was 0.818 ms before and 0.793 ms after retention on development;
these runs do not establish a speedup or touch-to-display performance. The extra
prefix output is bounded at sixteen words instead of eight; traversal remains
bounded at 2,048 states. Inspect the old composition corpus for effects beyond
the newly generated conditions before deciding to land.
