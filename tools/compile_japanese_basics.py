"""Source-defined single kana and frequency-bounded Japanese kanji; offline."""
from pathlib import Path
import argparse,hashlib,json,re,subprocess,tarfile
from collections import Counter
from sources import require_sources
ROOT=Path(__file__).resolve().parent.parent

def generate():
    require_sources('wanakana','kanjidic')
    mappings=json.loads(subprocess.check_output(['node',str(ROOT/'tools/extract_kana.cjs')]))
    rows=set()
    for item in mappings:
        for kind in ('hiragana','katakana'):
            rows.add((kind,item['key'],item[kind],0,'wanakana:5.3.1'))
    grammar=json.loads(subprocess.check_output(['node',str(ROOT/'tools/extract_kana.cjs'),'--grammar']))
    for item in grammar: rows.add(('romaji',item['key'],item['hiragana'],0,'wanakana:5.3.1'))
    path=ROOT/'third_party/kanjidic/kanjidic2-en.json.tgz'
    source=json.loads((path.parent/'source.json').read_text(encoding='utf8'))
    assert hashlib.sha256(path.read_bytes()).hexdigest()==source['sha256']
    with tarfile.open(path) as archive:
        data=json.load(archive.extractfile(next(m for m in archive.getmembers() if m.name.endswith('.json'))))
    selected=[c for c in data['characters'] if 1 <= (c['misc']['frequency'] or 100000) <= 500]
    readings=[];skipped=[]
    for c in selected:
        for group in (c['readingMeaning'] or {}).get('groups',[]):
            for r in group['readings']:
                if r['type'] not in ('ja_on','ja_kun'):continue
                if not re.fullmatch('[\u3041-\u3096\u30a1-\u30fa]+',r['value']):
                    skipped.append([c['literal'],r['value'],'non-standalone or marked reading']);continue
                readings.append((c,r['value']))
    aliases=json.loads(subprocess.check_output(['node',str(ROOT/'tools/romanize_kana.cjs')],input=json.dumps(sorted({r for _,r in readings}),ensure_ascii=False).encode('utf8')))
    for c,reading in readings:
        key=aliases[reading]['reading']
        if not re.fullmatch("[a-z]+(?:'[a-z]+)*",key):
            skipped.append([c['literal'],reading,'unsupported alias']);continue
        rows.add(('kanji',key,c['literal'],c['misc']['frequency'],'kanjidic:U%04X'%ord(c['literal'])))
    serialized='# kind\treading\toutput\tfrequency_rank\tsource\n'+''.join('\t'.join(map(str,r))+'\n' for r in sorted(rows))
    report=dict(format=1,rows=len(rows),outputs={kind:len({r[2] for r in rows if r[0]==kind}) for kind in ('hiragana','katakana','kanji')},source_characters=len(data['characters']),selected_frequency_characters=len(selected),frequency_cutoff=500,skipped=skipped,asset_sha256=hashlib.sha256(serialized.encode()).hexdigest(),sources={str(p.relative_to(ROOT)).replace('\\','/'):hashlib.sha256(p.read_bytes()).hexdigest() for p in (path,ROOT/'third_party/wanakana/index.js')},policy='All single-kana mappings and complete romanization grammar from WanaKana; KANJIDIC ranks 1-500, complete unmarked Japanese on/kun readings. No nanori or reconstructed stems.')
    return serialized.encode(),(json.dumps(report,ensure_ascii=False,indent=2)+'\n').encode(),report

def main():
    args=argparse.ArgumentParser();args.add_argument('--check',action='store_true');check=args.parse_args().check
    asset,manifest,report=generate()
    for path,content in ((ROOT/'app/src/main/assets/japanese-basic.tsv',asset),(ROOT/'docs/japanese-coverage/basics-manifest.json',manifest)):
        if check:assert path.read_bytes()==content,'Generated data drift: '+str(path)
        else:path.write_bytes(content)
    print(json.dumps({k:v for k,v in report.items() if k not in ('skipped','sources')},indent=2))
if __name__=='__main__':main()
