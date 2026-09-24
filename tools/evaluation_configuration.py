"""Validate actual evaluation configuration before attributing paired changes."""
import hashlib
import json
from pathlib import Path


def compare_configuration(before, after, changed_inputs=()):
    paths=[Path(str(p)+'.config.json') for p in (before,after)]
    if not any(p.exists() for p in paths):
        return dict(verified=False, reason='Legacy outputs have no configuration receipts; configuration equivalence is unverified')
    if not all(p.exists() for p in paths):
        raise ValueError('Only one output has a configuration receipt; rerun both with the current evaluator')
    receipts=[json.loads(p.read_text(encoding='utf-8')) for p in paths]
    for output, receipt in zip((before,after),receipts):
        if receipt.get('format')!=1 or receipt.get('output_sha256')!=hashlib.sha256(Path(output).read_bytes()).hexdigest():
            raise ValueError('Missing, unsupported or stale configuration receipt')
    left,right=receipts
    if left['packs']!=right['packs'] or left['inputs'].keys()!=right['inputs'].keys():
        raise ValueError('Enabled dictionaries differ; this is not a matched configuration')
    allowed=set(changed_inputs)
    if not allowed<=left['inputs'].keys():
        raise ValueError('Declared input is absent from the actual configuration')
    differences={key for key in left['inputs'] if left['inputs'][key]!=right['inputs'][key]}
    if differences-allowed:
        raise ValueError('Undeclared input differences: '+', '.join(sorted(differences-allowed)))
    return dict(verified=True,declared_input_changes=sorted(allowed),actual_input_changes=sorted(differences),receipts=receipts)
