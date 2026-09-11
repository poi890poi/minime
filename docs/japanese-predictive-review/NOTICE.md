# Evaluation attribution

This directory is isolated development evidence, not an Android dictionary.
The source revisions and source/generated-data hashes remain those in
[source-manifest.json](../japanese-engine-benchmark/source-manifest.json).

Conversation probes and their derived results retain RealPersonaChat (Sanae
Yamashita, Koji Inoue, Ao Guo, Shota Mochizuki, Tatsuya Kawahara and Ryuichiro
Higashinaka / Nagoya University, CC BY-SA 4.0) and ASDC (Yuta Hayashibe / Megagon
Labs, CC BY 4.0) attribution. Combined conversation derivatives use CC BY-SA 4.0.
Source document/turn identities are retained. AJIMEE-derived results retain
azooKey, Kyoto University Japanese Wikipedia Input Error Dataset and original
Wikipedia contributor attribution, CC BY-SA 3.0. See the complete
[evaluation notice](../japanese-engine-benchmark/NOTICE.md) and linked licenses.

The converter and code portions in posting-index.patch derive from Kazuma Naka's
pinned MIT implementation. Retain [KAZUMA-LICENSE.txt](../japanese-engine-benchmark/KAZUMA-LICENSE.txt).
The patch adds loaded posting offsets and changes lookup access only; it does not
change any dictionary bytes. The Mozc/NAIST/ICOT/public-domain dictionary notices
remain in [MOZC-LICENSE.txt](../japanese-engine-benchmark/MOZC-LICENSE.txt).

Synthetic fixture readings are created solely to test graph and prefix contracts;
they never enter production data or language quality denominators. No source or
author endorsement is implied.
