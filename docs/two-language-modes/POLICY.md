# Mode policy comparison (unchanged 0.7.3 dictionaries)

Baseline 3ecf81d; 3,922 frozen conditions per mode. Chinese and English outputs
remain identical. Focused mode changes intentionally remove third-language guesses
and English default protection. Core gate: 32,041 mechanical assertions; native
gate: 628 uncached inputs. Per-genre/condition counts are in policy-results.json;
raw outputs accompany it. Targets never enter the decoder. Optional slots exclude
raw input; default counts include it. Source Japanese/POJ probes are not independent
conversation accuracy. Corpus SHA256:
00aaa44a5036725e61ebeb1f6659fde15c2ed3682f678f4c480516b059e1e287.

With English secondary, Japanese half-reading availability is 109/128 compared
with 96/128 for Chinese secondary; Taiwanese is 53/128 versus 51/128. Shared pack
lookup limits still make language scope affect retrieval in this isolated stage.
Full Japanese retrieval is 79/128 and Taiwanese 122/128 for both pairings. The old
mixed probes include Romanized targets that may equal raw input; availability
excludes those cases, whereas the default metric includes them. Dictionary and
search-budget changes are evaluated separately after this stage.
