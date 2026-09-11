"""Independent conversation references. Never query MinIME while preparing labels."""
from pathlib import Path
import sys,json,zipfile,hashlib,csv,gzip,re,unicodedata,collections,shutil
from importlib.metadata import version
ROOT=Path(__file__).resolve().parents[2];WORK=ROOT/'artifacts/language-contract-benchmark'
sys.path.insert(0,str(WORK/'python-deps'))
from sudachipy import dictionary,tokenizer
import jaconv
from 臺灣言語工具.音標系統.閩南語.臺灣閩南語羅馬字拼音 import 臺灣閩南語羅馬字拼音 as TL
from kesi.susia.TL import tsuanTL
EXPECTED_VERSIONS={'sudachipy':'0.6.10','sudachidict_core':'20250129','jaconv':'0.4.0','tai5-uan5-gian5-gi2-kang1-ku7':'1.1.1','KeSi':'1.6.0'}
VERSIONS={name:version(name) for name in EXPECTED_VERSIONS}
assert VERSIONS==EXPECTED_VERSIONS,('Reference tools must match the frozen versions',VERSIONS)
OUT=Path(sys.argv[1]) if len(sys.argv)>1 else ROOT/'docs/language-contract-benchmark/conversations';OUT.mkdir(parents=True,exist_ok=True)
def sha(b):return hashlib.sha256(b).hexdigest()
def order(s):return sha(('dialogue-holdout-v1:'+s).encode())
def nfc(s):return unicodedata.normalize('NFC',s)
def plain(s):
    value=unicodedata.normalize('NFD',s.lower()).replace('\u0358','o').replace('ⁿ','nn').replace('ı','i')
    value=''.join(c for c in value if not unicodedata.combining(c))
    return value if re.fullmatch('[a-z -]+',value) else None
plans={};archives={}
for source in ['real-persona-chat','asdc']:
    d=WORK/'conversation-sources'/source;z=zipfile.ZipFile(d/'source.zip');archives[source]=z
    part='/real_persona_chat/dialogues/' if source=='real-persona-chat' else '/data/main/dialog/json/'
    names=[n for n in z.namelist() if part in n and n.endswith('.json') and Path(n).stem not in ['00001','001']]
    names.sort(key=lambda n:order(source+':'+Path(n).stem));plans[source]={'development':names[:32],'holdout':names[32:96],'eligible_conversations':len(names)}
    for name in ['README.md','LICENSE','LICENSE.txt']:
        if (d/name).exists():shutil.copyfile(d/name,OUT/(source+'-'+name))
(OUT/'split-plan.json').write_text(json.dumps(plans,ensure_ascii=False,indent=2)+'\n',encoding='utf8')
tokenizer_obj=dictionary.Dictionary().create()
excluded=collections.Counter();examples=[];turns=[];stats=collections.Counter();seen_by_role=collections.defaultdict(set)
for source,plan in plans.items():
    for role in ['development','holdout']:
        for name in plan[role]:
            dialog=json.loads(archives[source].read(name));doc=source+':'+Path(name).stem;pools=collections.defaultdict(list)
            for i,u in enumerate(dialog['utterances']):
                text=nfc(u['text']);turnid=str(u.get('utterance_id',i));speaker=u.get('interlocutor_id',u.get('name',''))
                turns.append({'source':source,'role':role,'conversation':doc,'turn':turnid,'speaker':speaker,'text':text})
                # Do not turn masked personal information or Latin/numeric strings into Japanese gold.
                for ci,m in enumerate(re.finditer('[\u3041-\u3096\u30a1-\u30fa\u30fc\u3400-\u9fff々]+',text)):
                    surface=m.group();tokens=list(tokenizer_obj.tokenize(surface,tokenizer.Tokenizer.SplitMode.C));parsed=[]
                    for ti,t in enumerate(tokens):
                        kana=jaconv.kata2hira(t.reading_form());raw=jaconv.kana2alphabet(kana)
                        if t.is_oov() or not re.fullmatch("[a-z'-]+",raw):excluded[source+'/unreadable_or_oov_token']+=1;parsed.append(None);continue
                        # Reference adapter validation is independent of MinIME/WanaKana.
                        if jaconv.alphabet2kana(raw)!=kana:excluded[source+'/romanization_roundtrip']+=1;parsed.append(None);continue
                        status='original-kana' if re.fullmatch('[\u3041-\u3096\u30a1-\u30fa\u30fc]+',t.surface()) else 'sudachi-silver'
                        item=(t.surface(),raw,status,kana);parsed.append(item)
                        pools['word'].append((turnid+':'+str(ci)+':'+str(ti),t.surface(),raw,[raw],status))
                    for start in range(len(parsed)-1):
                        a=parsed[start:start+2]
                        if all(a):pools['two-token'].append((turnid+':'+str(ci)+':'+str(start),''.join(t[0] for t in a),jaconv.kana2alphabet(''.join(t[3] for t in a)),[t[1] for t in a],'original-kana' if all(t[2]=='original-kana' for t in a) else 'sudachi-silver'))
                    if parsed and all(parsed):pools['clause'].append((turnid+':'+str(ci),surface,jaconv.kana2alphabet(''.join(t[3] for t in parsed)),[t[1] for t in parsed],'original-kana' if all(t[2]=='original-kana' for t in parsed) else 'sudachi-silver'))
            for unit,limit in [('word',12),('two-token',4),('clause',4)]:
                values=sorted(pools[unit],key=lambda x:order(doc+unit+x[0]))[:limit]
                for identity,target,raw,units,status in values:
                    if len(raw)>96:excluded[source+'/raw_over_96']+=1;continue
                    examples.append((source,role,doc,identity,unit,raw,target,units,status));stats[source+'/'+role+'/'+unit]+=1
# Modern authored aligned Taiwanese examples. The script never generates a reading from Han.
modernpath=WORK/'conversation-sources/suisiann-thousand/source.zip';modern=zipfile.ZipFile(modernpath)
modernfiles=sorted([n for n in modern.namelist() if n.endswith('_hanlo.txt')],key=order)
plans['suisiann-thousand']={'development':modernfiles[:6],'holdout':modernfiles[6:]}
(OUT/'split-plan.json').write_text(json.dumps(plans,ensure_ascii=False,indent=2)+'\n',encoding='utf8')
(OUT/'suisiann-thousand-README.md').write_bytes(modern.read(next(n for n in modern.namelist() if n.endswith('/README.md') and len(Path(n).parts)==2)))
modern_syllables={};conversion_audit=[]
def convert_word(w):
    parts=re.split('(-+)',w);converted=[]
    for part in parts:
        if not part or part.startswith('-'):converted.append(part);continue
        if part not in modern_syllables:
            parser=TL(part.lower());poj=parser.轉白話字()
            good=poj is not None and nfc(tsuanTL(poj))==nfc(parser.轉調符())
            modern_syllables[part]=poj if good else None
            conversion_audit.append({'tailo':part,'poj':poj,'valid_roundtrip':good})
        value=modern_syllables[part]
        if value is None:return None
        converted.append(value)
    return ''.join(converted)
for role in ['development','holdout']:
    for name in plans['suisiann-thousand'][role]:
        lines=modern.read(name).decode('utf-8-sig').splitlines();doc='/'.join(Path(name).parts[1:]);pools=collections.defaultdict(list)
        for i in range(len(lines)-1):
            han=lines[i].strip();reading=lines[i+1].strip()
            if not re.search('[\u3400-\u9fff]',han) or re.search('[\u3400-\u9fff]',reading) or not re.search('[A-Za-z]',reading):continue
            turns.append({'source':'suisiann-thousand','role':role,'document':doc,'example':str(i+1),'text':han,'tailo':reading,'kind':'authored-situational-example'})
            for ci,clause in enumerate(re.split('[,.!?;:，。！？；：「」“”()（）…—]+',reading)):
                words=clause.split();cw=[convert_word(w) for w in words]
                for wi,word in enumerate(cw):
                    if not word or not plain(word):excluded['suisiann-thousand/non_taiwanese_or_unconvertible_word']+=1;continue
                    units=[u for u in re.split('[ -]+',plain(word)) if u]
                    if units:pools['word'].append((str(i+1)+':'+str(ci)+':'+str(wi),word,''.join(units),units,'source-tailo-to-poj'))
                if words and all(cw):
                    sentence=' '.join(cw);clean=plain(sentence)
                    if clean:
                        units=[u for u in re.split('[ -]+',clean) if u]
                        if units:pools['clause'].append((str(i+1)+':'+str(ci),sentence,''.join(units),units,'source-tailo-to-poj'))
                    else:excluded['suisiann-thousand/unhandled_poj_output']+=1
        for unit in ['word','clause']:
            for identity,target,raw,units,status in sorted(pools[unit],key=lambda x:order(doc+unit+x[0]))[:64]:
                if not 1<=len(raw)<=96:excluded['suisiann-thousand/raw_length']+=1;continue
                examples.append(('suisiann-thousand',role,doc,identity,unit,raw,target,units,status));stats['suisiann-thousand/'+role+'/'+unit]+=1
(OUT/'orthography-audit.json').write_text(json.dumps(conversion_audit,ensure_ascii=False,indent=2)+'\n',encoding='utf8')
# Full authored POJ examples and actual source words, never synthesized turns.
sourcepath=ROOT/'third_party/taiwanese_basic/vocabulary.csv';basic=list(csv.DictReader(sourcepath.open(encoding='utf-8-sig')))
pool=[]
for item in basic:
    for i,s in enumerate(item['LekuPoj'].split('/')):
        s=nfc(s.strip()).strip(' .!?;:，。！？；：「」“”"')
        raw=plain(s)
        if not raw:excluded['taiwanese-basic/unsupported_example_syntax']+=1;continue
        words=s.split();units=[u for u in re.split('[ -]+',raw) if u];joined=''.join(units)
        if not 2<=len(joined)<=96:excluded['taiwanese-basic/raw_length']+=1;continue
        identity=item['DictWordID']+':'+str(i);pool.append((identity,s,joined,units,words,item['PageNumber']))
for identity,s,raw,units,words,page in sorted(pool,key=lambda x:order(x[0]))[:1024]:
    examples.append(('taiwanese-basic','regression','taiwanese-basic:1956',identity,'clause',raw,s,units,'authored-poj'))
    turns.append({'source':'taiwanese-basic','role':'regression','conversation':None,'document':'taiwanese-basic:1956','page':page,'example':identity,'text':s,'kind':'isolated-authored-example'})
    # Select one actual source word per example, deterministically and before engine outputs.
    w=sorted(enumerate(words),key=lambda x:order(identity+':'+str(x[0])))[0]
    normalized=plain(w[1]);wu=[u for u in re.split('[ -]+',normalized) if u]
    if not wu:excluded['taiwanese-basic/separator_only_word']+=1;continue
    examples.append(('taiwanese-basic','regression','taiwanese-basic:1956',identity+':'+str(w[0]),'word',''.join(wu),w[1],wu,'authored-poj'))
    stats['taiwanese-basic/regression/clause']+=1;stats['taiwanese-basic/regression/word']+=1
# Observe production overlap only after examples and splits have been frozen.
production=collections.defaultdict(set)
for line in (ROOT/'app/src/main/assets/addons.tsv').read_text(encoding='utf8').splitlines():
    p=line.split('\t')
    if len(p)==5:production[p[0]].add(nfc(p[2]).lower())
rows=[];metadata=[];seen_target=collections.defaultdict(set)
neighbors={}
keyboard=['qwertyuiop','asdfghjkl','zxcvbnm']
for row in keyboard:
    for i,c in enumerate(row):neighbors[c]=row[i+1] if i+1<len(row) else row[i-1]
for source,role,doc,identity,unit,raw,target,units,status in examples:
    target=nfc(target);pack='poj' if source in ['taiwanese-basic','suisiann-thousand'] else 'japanese'
    stable=source+doc+identity+unit;at=int(order(stable)[:8],16)%max(1,len(raw)-1)
    forms=[('full',raw),('half',raw[:max(1,len(raw)//2)]),('three-quarter',raw[:max(1,len(raw)*3//4)]),
      ('initials',''.join(u[0] for u in units)),('mixed',''.join(u if i%2 else u[0] for i,u in enumerate(units))),
      ('transpose',raw[:at]+raw[at:at+2][::-1]+raw[at+2:]),('omission',raw[:at]+raw[at+1:]),
      ('neighbor',raw[:at]+neighbors.get(raw[at],raw[at])+raw[at+1:])]
    for condition,value in forms:
        if not value:excluded[source+'/empty_error_input']+=1;continue
        row=len(rows);rows.append((pack+'-conversation-'+source,doc,identity+'/'+unit,condition,value,target,''))
        metadata.append({'row':row,'source':source,'role':role,'document':doc,'identity':identity,'unit':unit,'reference':status,'source_target_in_production':target.lower() in production[pack],
          'input_conditions':'deterministic transliteration/error controls, not recorded touchscreen events'})
    seen_target[role].add(target.lower())
with (OUT/'inputs.tsv').open('w',encoding='utf8',newline='') as f:csv.writer(f,delimiter='\t',lineterminator='\n').writerows(rows)
for name,values in [('turns.jsonl.gz',turns),('references.jsonl.gz',metadata)]:
    with gzip.open(OUT/name,'wt',encoding='utf8') as f:
        for r in values:f.write(json.dumps(r,ensure_ascii=False)+'\n')
manifest={'rows':len(rows),'examples':dict(stats),'excluded':dict(excluded),'turns':len(turns),'versions':VERSIONS,
  'input_sha256':sha((OUT/'inputs.tsv').read_bytes()),'source_downloads':json.loads((WORK/'conversation-sources/downloads.json').read_text(encoding='utf8')),
  'taiwanese_source_sha256':sha(sourcepath.read_bytes()),'modern_taiwanese_source_sha256':sha(modernpath.read_bytes()),'holdout_targets_also_in_development':len(seen_target['holdout']&seen_target['development']),
  'limitations':['Japanese kanji readings are silver, not human gold; source words/clauses are naturally occurring but the typed romanization/errors are simulated.',
  'Modern Taiwanese is authored situational text, not spontaneous multi-turn conversation; historical Taiwanese examples additionally share production lineage.',
  'Conversation split preserves whole dialogues but does not enforce speaker-disjoint or phrase-disjoint partitions. Repeated greetings are not removed to game natural coverage.']}
(OUT/'manifest.json').write_text(json.dumps(manifest,ensure_ascii=False,indent=2)+'\n',encoding='utf8')
print(json.dumps({'rows':len(rows),'examples':dict(stats),'excluded':dict(excluded),'turns':len(turns)},ensure_ascii=False))
