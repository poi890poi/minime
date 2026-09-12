"""Inventory the built runtime, retaining unresolved release clearance separately."""
import argparse, hashlib, json, struct, zipfile
from pathlib import Path
from xml.etree import ElementTree as ET
ROOT=Path(__file__).resolve().parent.parent

def digest(data):return hashlib.sha256(data).hexdigest()
def elf_segments(data):
    if data[:4]!=b'\x7fELF' or data[5]!=1:raise ValueError('Unsupported ELF')
    wide=data[4]==2
    header=struct.unpack_from('<HHIQQQIHHHHHH' if wide else '<HHIIIIIHHHHHH',data,16)
    offset,entry_size,count=header[4],header[8],header[9]
    result=[]
    for i in range(count):
        p=struct.unpack_from('<IIQQQQQQ' if wide else '<IIIIIIII',data,offset+i*entry_size)
        if p[0]!=1:continue
        file_offset,address,align=(p[2],p[3],p[7]) if wide else (p[1],p[2],p[7])
        result.append({'alignment':align,'offset':file_offset,'address':address,'compatible_16kb':align>=16384 and (file_offset-address)%16384==0})
    if not result:raise ValueError('No ELF load segments')
    return result

def main():
    parser=argparse.ArgumentParser();parser.add_argument('--aab',required=True);parser.add_argument('--manifest',required=True);parser.add_argument('--output',required=True);parser.add_argument('--strict',action='store_true');args=parser.parse_args()
    path=Path(args.aab);ns='{http://schemas.android.com/apk/res/android}';manifest=ET.parse(args.manifest).getroot();app=manifest.find('application');sdk=manifest.find('uses-sdk')
    assert manifest.attrib['package']=='app.minime.keyboard','Unexpected package'
    assert int(sdk.attrib[ns+'targetSdkVersion'])>=36,'Target API below 36'
    assert app.attrib.get(ns+'debuggable','false')=='false','Debuggable release'
    assert app.attrib.get(ns+'allowBackup')=='false','Automatic backup enabled'
    assert not manifest.findall('uses-permission'),'Unexpected app permission; review data flows'
    expected={p.name:p for p in (ROOT/'app/build/generated/minimeAssets').iterdir() if p.is_file()}
    rime_root=ROOT/'app/src/main/rimeAssets'
    expected.update({p.relative_to(rime_root).as_posix():p for p in rime_root.rglob('*') if p.is_file()})
    entries=[];native=[]
    with zipfile.ZipFile(str(path)) as z:
        assert len(z.namelist())==len(set(z.namelist())),'Duplicate archive entry'
        actual={n[len('base/assets/'):] for n in z.namelist() if n.startswith('base/assets/') and not n.endswith('/')}
        assert actual==set(expected),'Asset inventory differs: '+repr(actual.symmetric_difference(expected))
        for name in sorted(actual):
            data=z.read('base/assets/'+name);assert data==expected[name].read_bytes(),'Stale asset: '+name
            entries.append({'path':'base/assets/'+name,'bytes':len(data),'sha256':digest(data)})
        for name in z.namelist():
            if name.startswith('base/lib/') and name.endswith('.so'):
                data=z.read(name);segments=elf_segments(data);assert all(x['compatible_16kb'] for x in segments),'ELF alignment failure: '+name
                native.append({'path':name,'bytes':len(data),'sha256':digest(data),'load_segments':segments})
        assert len(native)==4 and {x['path'].split('/')[2] for x in native}=={'arm64-v8a','armeabi-v7a','x86','x86_64'},'Unexpected ABI inventory'
        signed=any(n.startswith('META-INF/') and n.endswith(('.RSA','.DSA','.EC')) for n in z.namelist())
    catalog=json.loads((ROOT/'sources/catalog.json').read_text(encoding='utf-8'));policy=json.loads((ROOT/'sources/release-policy.json').read_text(encoding='utf-8'))
    components=[{'id':s['id'],'license':s['license'],'source_decision':s['decision'],'pins':s.get('inputs',[])} for s in catalog['sources'] if s['decision'] in ('retain','replace')]
    native_pins=json.loads((ROOT/'third_party/rime/native-sources.json').read_text(encoding='utf-8'))
    report={'format':1,'aab':path.name,'sha256':digest(path.read_bytes()),'bytes':path.stat().st_size,'application_id':manifest.attrib['package'],'target_sdk':int(sdk.attrib[ns+'targetSdkVersion']),'debuggable':False,'signature_block_present':signed,'assets':entries,'native':native,'source_components':components,'native_source_pins':native_pins,'release_rights_open_items':policy['open_items'],'runtime_16kb_certified':False,'status':'prepared-not-cleared-for-upload'}
    dest=Path(args.output);dest.parent.mkdir(parents=True,exist_ok=True);dest.write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    print(json.dumps({'assets':len(entries),'native_libraries':len(native),'elf_alignment':'16 KB','signature_block_present':signed,'open_rights_items':len(policy['open_items']),'report':str(dest)}))
    if args.strict:raise SystemExit('Upload gate closed: this inventory does not certify rights clearance, signing authenticity or Android 16 / 16 KB runtime behavior; complete the release ledger')
if __name__=='__main__':main()
