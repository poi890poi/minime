# Notices

## Source material

The counts in this dataset are computed from the `cmn_Hant` portion of
[HPLT 3.0](https://hplt-project.org/datasets/v3.0), a web-derived corpus built from
Internet Archive and Common Crawl crawls. HPLT states that it owns none of the
underlying text and licenses its packaging under CC0 1.0; it operates a notice-and-takedown
policy for the underlying documents.

No text from HPLT or from any crawled page is redistributed here. The dataset contains
aggregate host counts of word sequences only: no document, no sentence, no URL, no host
name. Every published n-gram occurs on at least 40 independent hosts and every sentence
type was counted once, so no source document can be reconstructed from these files.

## Filtering

Variety identification was performed with [twfilter](https://github.com/taiwan-corpora/twfilter)
0.1.0 and its reference tables, archived at
[10.5281/zenodo.21761465](https://doi.org/10.5281/zenodo.21761465) and published as
[taiwan-corpora/twfilter-tables](https://huggingface.co/datasets/taiwan-corpora/twfilter-tables).
Those tables carry their own terms (OGDL v1.0, Apache 2.0, MIT); none of their content is
included in this dataset.

## License

To the extent that the author holds any rights in these counts, they are dedicated to the
public domain under [CC0 1.0](LICENSE). Citation is appreciated but not required; a
citation entry is provided in the dataset card.

Den Patin <hi@dpat.in>
