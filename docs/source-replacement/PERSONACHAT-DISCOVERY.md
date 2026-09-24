# PersonaChat: next licensed conversation-source audit

September 24, 2026. Discovery and structural pilot only, no production data or
count-table approval. Taskmaster alone was rejected for cross-genre losses.
PersonaChat supplies human-authored social dialogue conditioned on fictional
personas, a different domain. It is still prompted role-play and must not be
presented as natural population language frequency.

Primary sources checked:
- Authors' paper: https://aclanthology.org/P18-1205/
- Official project: https://parl.ai/projects/personachat/
- Dataset-specific CC BY 4.0 notice, distinct from the repository software license:
  https://github.com/facebookresearch/ParlAI/blob/a29567f7ce76992fd1f03c51ba9e3b155a37ea51/parlai/tasks/personachat/LICENSE_DOCUMENTATION
- Official loader and archive hash:
  https://github.com/facebookresearch/ParlAI/blob/a29567f7ce76992fd1f03c51ba9e3b155a37ea51/parlai/tasks/personachat/build.py

Pin that repository revision and the loader's personachat.tgz SHA-256
507cf8641d333240654798870ea584d854ab5261071c5e3521c20d8fa41d5622.
Download only from the official endpoint, verify hash and inspect the archive
inventory before parsing. Do not execute downloaded code or extract arbitrary
paths. Preserve the dataset-specific notice and source file hashes.

Audit dialog boundaries, split overlap, duplicated perspectives/rewrites,
utterance fields versus distractor candidates, punctuation/orthography and
possible persona text duplication before choosing a complete source variant.
No handpicked rows, phrase promotions or prediction-driven source exclusions.
Do not count candidate distractors or rewritten duplicate views as extra speech.
Declare deterministic parsing and decontamination rules before any count/model
query; retain official conversation splits and an independent genre comparison.
If original orthography cannot be recovered systematically, record that limitation
and do not silently manufacture contractions or infer phrase frequencies.

This is a new source audit after a negative result, not a retuned mixture over
the consumed Taskmaster/GUM test labels. Any model proposal needs a separately
frozen plan, broader evaluation and apostrophe/size/runtime checks. The existing
English and Chinese source-rights gates remain open.

## Structural result: hold pending orthography evidence

The official archive hash matches. Its 21 text files contain repeated views of
the same source, so the audit reads only train/valid/test none_original files and
only their two utterance columns. Every row also has distractor candidates; these
are excluded, never counted as extra conversations. Original files have 8,939 /
1,000 / 968 conversations and 131,438 / 15,602 / 15,024 utterances. Full normalized
dialogue signatures are unique within each split and do not overlap across them.
That does not rule out shared short turns or persona-driven subject repetition.

The punctuation inventory contains no apostrophe, straight or typographic, in
any of these utterances. This is evidence of the supplied orthography, not proof
of the exact upstream preprocessing operation. It cannot by itself support
MinIME's grammatical-contraction recovery. Do not guess missing apostrophe forms
or treat tokenized spellings as ordinary English-frequency evidence.

Keep this source **on hold** until original orthography or an independently
justified systematic mapping is established. No context model was trained, no
prediction evaluated and no production entry added. The complete structural
inventory is [personachat-manifest.json](personachat-manifest.json); reproduce
with personachat-audit.py. The pinned data licence is retained alongside it.
