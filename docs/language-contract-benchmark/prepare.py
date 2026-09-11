"""Freeze controls and isolated one-variable core snapshots; never edit production."""
from pathlib import Path
import hashlib,json,re,shutil,subprocess,collections
ROOT=Path(__file__).resolve().parents[2]
OUT=ROOT/'docs/language-contract-benchmark'
WORK=ROOT/'artifacts/language-contract-benchmark'
VARIANTS=['baseline','span','english-context','evidence-rank','dedup','unit96','prefix-cache']
def sha(b):return hashlib.sha256(b).hexdigest()
def order(s):return sha(('contract-benchmark-v1:'+s).encode())
def replace(text,old,new):
    assert text.count(old)==1,(old,text.count(old))
    return text.replace(old,new)
def prepare():
    WORK.mkdir(parents=True,exist_ok=True)
    inputs=['docs/input-modes/coverage-inputs.tsv','docs/japanese-continuity/kana-oracle.tsv',
      'app/src/main/assets/addons.tsv','app/src/main/assets/context.tsv',
      'app/src/main/assets/paired-forms.tsv','app/src/main/assets/japanese-basic.tsv',
      'app/build/generated/minimeAssets/model.bin','app/src/main/assets/en_spelling.tsv',
      'app/build/generated/minimeAssets/addon-taiwan.bin','app/build/generated/minimeAssets/addon-poj.bin',
      'app/build/generated/minimeAssets/addon-japanese.bin','app/src/main/assets/geography.tsv']
    rows=[s.split('\t') for s in (ROOT/inputs[0]).read_text(encoding='utf8').splitlines()]
    olddocs={r[1] for r in rows}
    extra=collections.defaultdict(list)
    for split in ['train','dev']:
        name='artifacts/mode-corpus/en_gum-ud-'+split+'.conllu';inputs.append(name)
        for block in (ROOT/name).read_text(encoding='utf8').split('\n\n'):
            sid=re.search(r'^# sent_id = (.+)$',block,re.M);txt=re.search(r'^# text = (.+)$',block,re.M)
            if not sid or not txt:continue
            doc=sid[1].rsplit('-',1)[0];genre=doc.split('_')[1]
            if doc in olddocs or genre not in ['conversation','essay']:continue
            words=re.findall("[a-z]+(?:'[a-z]+)*",txt[1].lower().replace('’',"'"))
            for i,w in enumerate(words):
                if 2<=len(w)<=24:extra[(genre,doc)].append((sid[1]+':'+str(i),w,' '.join(words[max(0,i-2):i])))
    for (genre,doc),items in sorted(extra.items()):
        for sid,w,ctx in sorted(items,key=lambda x:order(x[0]))[:64]:
            raw=w.replace("'",'');at=int(order(sid)[:8],16)%max(1,len(raw)-1)
            for cond,value in [('full',raw),('half',raw[:max(1,len(raw)//2)]),('transpose',raw[:at]+raw[at:at+2][::-1]+raw[at+2:])]:
                rows.append(['en-reserved-'+genre,doc,sid,cond,value,w,ctx])
    entries=collections.defaultdict(dict)
    for line in (ROOT/'app/src/main/assets/addons.tsv').read_text(encoding='utf8').splitlines():
        p=line.split('\t')
        if len(p)!=5 or p[0] not in ['poj','japanese']:continue
        if not re.fullmatch("[a-z0-9]+(?:[- '][a-z0-9]+)*",p[1]):continue
        entries[p[0]][(p[1],p[2])]=p
    counts={}
    for pack,items in entries.items():
        selected=sorted(items.values(),key=lambda p:order(p[1]+'\t'+p[2]))[:2048]
        longs=[p for p in items.values() if len(re.sub("[- ']+",'',p[1]))>32]
        selected+=sorted(longs,key=lambda p:order(p[1]+'\t'+p[2]))[:256]
        counts[pack]={'eligible':len(items),'sample':len(selected),'long_eligible':len(longs)}
        for p in selected:
            units=re.split("[- ']+",p[1]);raw=''.join(units)
            forms=[('full',raw),('half',raw[:max(1,len(raw)//2)]),('initials',''.join(u[0] for u in units)),('mixed',''.join(u if i%2 else u[0] for i,u in enumerate(units)))]
            for cond,value in forms:
                rows.append([pack+'-source',p[3],sha((p[1]+'\t'+p[2]).encode())[:16],cond,value,p[2],''])
    payload=''.join('\t'.join(r)+'\n' for r in rows).encode();(OUT/'inputs.tsv').write_bytes(payload)
    manifest={'revision':subprocess.check_output(['git','rev-parse','HEAD'],cwd=ROOT,text=True).strip(),
      'inputs':{p:sha((ROOT/p).read_bytes()) for p in inputs},'corpus_sha256':sha(payload),'rows':len(rows),
      'groups':dict(collections.Counter(r[0] for r in rows)),'source_sampling':counts,
      'reserved_documents':[doc for genre,doc in sorted(extra)],'variants':VARIANTS,
      'roles':'Existing genre corpus regression; source probes retrieval only; reserved GUM documents disjoint from prior mode corpus, not a fresh external source.'}
    for variant in VARIANTS:
        dest=WORK/variant/'src';dest.mkdir(parents=True,exist_ok=True)
        for path in (ROOT/'core/src/main/java/dev/minime/core').glob('*.java'):
            text=path.read_text(encoding='utf8')
            if variant=='span' and path.name=='CompositionEngine.java':
                text=replace(text,'c.consumed>0 && c.consumed<raw.length() && raw.matches("[a-zv]+(?:\'[a-zv]+)*")',
                  'c.consumed>0 && c.consumed<raw.length() && !(Character.isLowSurrogate(raw.charAt(c.consumed)) && Character.isHighSurrogate(raw.charAt(c.consumed-1)))')
                text=replace(text,'if(!privateField)learning.choose(contextKey(),reading,choice.text);',
                  'if(!privateField && !literalField) {if(focused(choice) && focusedLearning)learning.choose("FOCUS:"+choice.pack,reading,choiceIdentity(choice));else if(!choice.supplemental)learning.choose(contextKey(),reading,choice.text);}')
            if variant=='english-context' and path.name=='CompositionEngine.java':
                text=replace(text,'if(englishMode && !literalField && c.text.matches(',
                  'if(inputMode.englishEnabled() && !literalField && !c.supplemental && c.literal && c.text.matches(')
                text=replace(text,'if(englishMode && !literalField && dictionary!=null) {\n                if(!privateField)candidates.addAll(learning.predictEnglish(context));',
                  """if(inputMode.englishEnabled() && !literalField && dictionary!=null && (englishMode || context.matches("[a-z]+(?:[ '][a-z]+)*"))) {
                if(!privateField)candidates.addAll(learning.predictEnglish(context));""")
                text=replace(text,'if (!privateField && dictionary != null && !literalField && inputMode.chineseEnabled()) candidates.addAll(dictionary.predict(context));',
                  """if (!privateField && dictionary != null && !literalField && inputMode.chineseEnabled() && !context.matches("[a-z]+(?:[ '][a-z]+)*")) candidates.addAll(dictionary.predict(context));""")
            if variant=='evidence-rank' and path.name=='CompositionEngine.java':
                text=replace(text,'        for(int i=0;i<candidates.size();i++) {\n            Candidate c=candidates.get(i);',
                  '''        if(!inputMode.pack.isEmpty() && !literalField && dictionary!=null && dictionary.validEnglishSpelling(raw)
                && candidates.stream().noneMatch(c->focused(c) && !c.incomplete && c.score!=-10 && c.consumed==0)) preferred=0;
        for(int i=0;i<candidates.size();i++) {
            Candidate c=candidates.get(i);''')
            if variant=='dedup' and path.name=='AddonDictionary.java':
                text=replace(text,'        // Builder pools must not survive',
                  '''        for(Map<String,List<Candidate>> source:prefixEntries.values())source.replaceAll((k,v)->new ArrayList<>(new LinkedHashSet<>(v)));
        for(Map<String,List<Candidate>> source:unitEntries.values())source.replaceAll((k,v)->new ArrayList<>(new LinkedHashSet<>(v)));
        entries.replaceAll((k,v)->new ArrayList<>(new LinkedHashSet<>(v)));
        // Builder pools must not survive''')
            if variant=='unit96' and path.name=='ReadingUnitIndex.java':text=replace(text,'MAX_WORD_INPUT=32','MAX_WORD_INPUT=96')
            if variant=='prefix-cache' and path.name=='PhoneticDictionary.java':
                text=replace(text,'    public List<Candidate> englishCompletions(String raw,boolean latinContext) {',
                  '''    private final Map<String,List<Candidate>> prefixCache=new LinkedHashMap<String,List<Candidate>>(512,.75f,true) {
        protected boolean removeEldestEntry(Map.Entry<String,List<Candidate>> e){return size()>512;}
    };
    public synchronized List<Candidate> englishCompletions(String raw,boolean latinContext) {
        String key=latinContext+"\\t"+raw;
        List<Candidate> hit=prefixCache.get(key);if(hit!=null)return hit;
        List<Candidate> value=Collections.unmodifiableList(uncachedCompletions(raw,latinContext));prefixCache.put(key,value);return value;
    }
    private List<Candidate> uncachedCompletions(String raw,boolean latinContext) {''')
            (dest/path.name).write_text(text,encoding='utf8')
        manifest.setdefault('snapshots',{})[variant]={p.name:sha(p.read_bytes()) for p in sorted(dest.glob('*.java'))}
    (OUT/'manifest.json').write_text(json.dumps(manifest,ensure_ascii=False,indent=2)+'\n',encoding='utf8')
    print('Frozen',len(rows),'conditions',manifest['groups'])
if __name__=='__main__':prepare()
