"""Record contract outcomes and cleanup identities without screenshots or editor text."""
import hashlib,json
from pathlib import Path
root=Path(__file__).resolve().parents[3]
entries=[
    ('first_contracts','8d1bcbf9-ba4e-405f-bd79-75573bfeaf84','contracts.log',13,1,'Accessibility window envelope was mistaken for keyboard body geometry.'),
    ('corrected_contracts','63741887-b022-4476-8d1d-e09c5ab66be9','contracts2.log',14,0,''),
    ('first_landscape','8ed6d4a1-f3f4-4dc8-874b-91077a8b7696','landscape.log',1,1,'Buffer checks completed; cleanup assertion caught auto-rotation enabled by ROTATION_UNFREEZE. Immediately repaired before further phone work.'),
    ('guarded_landscape','b2cf5234-a4fc-4a24-8226-bbb456f8ea86','landscape-guarded.log',2,0,''),
    ('accepted_baseline_geometry','bf0b48a5-2e35-43ed-be88-981595beb7ca','baseline-geometry.log',1,0,''),
    ('final_apps','d203705a-8d6a-4082-bbc7-3b5fbe903e97','cross-app.log',7,0,'Chrome input unsubmitted; synthetic Keep fields cleared.')]
result={}
for name,identity,log_name,passed,failed,reason in entries:
    session=root/'artifacts/device-tests'/identity
    log=root/'artifacts/single-window'/log_name
    text=log.read_text(encoding='utf-8-sig')
    assert 'Cleanup verified: preferences, previous IME, display OFF.' in text
    if failed:assert f'Tests run: {passed+failed},  Failures: {failed},  Errors: 0' in text
    else:assert f'OK ({passed} test' in text
    files=[session/'instrumentation.txt',session/'display-after.txt',log]
    result[name]=dict(session=identity,passed=passed,failed=failed,reason=reason,
        original_apk_preferences_learning_ime_restored=True,display_off=True,
        hashes={str(p.relative_to(root)):hashlib.sha256(p.read_bytes()).hexdigest() for p in files})
repair=root/'artifacts/single-window/rotation-repair.json'
result['rotation_repair']=json.loads(repair.read_text(encoding='utf-8-sig'))
result['rotation_repair']['hashes']={str(p.relative_to(root)):hashlib.sha256(p.read_bytes()).hexdigest() for p in
    [repair,root/'artifacts/single-window/rotation-repair-display.txt',
     root/'artifacts/single-window/landscape-guarded/rotation-before.json',
     root/'artifacts/single-window/landscape-guarded/rotation-cleanup.txt']}
(Path(__file__).parent/'contracts.json').write_text(json.dumps(result,indent=2)+'\n',encoding='utf-8')
print('Recorded six completed contract sessions, including both failed attempts and rotation repair')
