# Context statistics — Universal Dependencies r2.15

The derived `app/src/main/assets/context.tsv` statistics are distributed under
CC BY-SA 4.0. Modifications: training-split text normalized into English word
and Traditional Chinese character counts; repeated contexts retained; no original
sentences are included in the asset. The notices in each upstream README describe
annotations/database rights and underlying source texts separately.

Sources and exact revisions:

- [English EWT](https://github.com/UniversalDependencies/UD_English-EWT/tree/4dc8e10cf32352e11ab2c46e024b19853b91546e):
  annotations © 2013–2021 The Board of Trustees of The Leland Stanford Junior University;
  contributors listed in the upstream README.
- [Traditional Chinese GSD](https://github.com/UniversalDependencies/UD_Chinese-GSD/tree/a7cb4270c8ea437d7f2cd7978b0dcc0941cb7266):
  Mo Shen, Ryan McDonald, Daniel Zeman, Peng Qi; annotated/converted by Google.
  This public treebank is independent of the legacy Google Zhuyin APK/model.

`sources.json` records upstream URLs and decompressed SHA-256 values. Raw CoNLL-U
downloads are development inputs, not Android assets; their redistribution is
excluded from this repository. Fetch the pinned URLs to the indicated paths to
reproduce `python tools/compile_context.py`. Keep train/dev/test distinct.

English EWT includes web/review/email genres; Chinese GSD is predominantly wiki
text. Their small sizes and domain mismatch limit everyday prediction quality.
No paired keyboard evaluation target supplies a training entry or ranking rule.
