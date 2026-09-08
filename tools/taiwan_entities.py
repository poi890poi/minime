"""Compile topic-graph entities using existing source readings; no guessed names."""
from pathlib import Path
from collections import Counter,defaultdict
import gzip,hashlib,json,re,sys,unicodedata
ROOT=Path(__file__).resolve().parent.parent
sys.path.insert(0,str(ROOT/'third_party/opencc_python'))
from opencc import OpenCC

def append_entities(add,cedict_readings,syllables):
    path=ROOT/'third_party/taiwan_encyclopedia/snapshot.json.gz'
    snapshot=json.loads(gzip.decompress(path.read_bytes()).decode('utf-8'))
    type_path=path.with_name('entity-types.json.gz')
    typed=json.loads(gzip.decompress(type_path.read_bytes()).decode('utf-8'))
    assert typed['snapshot_sha256']==hashlib.sha256(path.read_bytes()).hexdigest(),'Entity identity snapshot is stale'
    assert not typed['errors'],'Entity identity fetch must finish before compiling'
    identities={title:value for batch in typed['requests'] for title,value in batch['entities'].items()}
    label_path=path.with_name('traditional-labels.json.gz');label_data=json.loads(gzip.decompress(label_path.read_bytes()).decode('utf-8'))
    assert label_data['types_sha256']==hashlib.sha256(type_path.read_bytes()).hexdigest(),'Traditional label snapshot is stale'
    labels={key:value.get('labels',{}) for batch in label_data['requests'] for key,value in batch['entities'].items()}
    # Detect script changes; changed identities require explicit source labels.
    converter=OpenCC('s2tw')
    units=defaultdict(set)
    for word,choices in cedict_readings.items():units[word].update(choices)
    for line in (ROOT/'app/src/main/assets/zh_tw.tsv').read_text(encoding='utf-8').splitlines():
        p=line.split('\t');plain=p[0].split("'");tones=re.findall('[ˉˊˇˋ˙]',p[4])
        if len(plain)==len(tones):units[p[2]].add(tuple(s+str('ˉˊˇˋ˙'.index(t)+1) for s,t in zip(plain,tones)))
    def resolve(word):
        at=0;plain=[];toned=[];tone_safe=True;segments=[]
        while at<len(word):
            for end in range(len(word),at,-1):
                choices=units.get(word[at:end],set());normalized={tuple(p[:-1] for p in choice) for choice in choices}
                if len(normalized)==1:
                    plain.extend(next(iter(normalized)));segments.append(word[at:end]);at=end
                    if len(choices)==1:toned.extend(next(iter(choices)))
                    else:tone_safe=False
                    break
            else:return None
        return plain,toned if tone_safe else None,segments
    records=[];counts=defaultdict(Counter);outputs=defaultdict(set)
    for key,item in sorted(snapshot['entities'].items(),key=lambda pair:int(pair[0])):
        original=item['title'];word=re.sub(r'\s*[（(][^()（）]*[)）]$','',original).strip()
        identity=identities.get(original);normalization='unchanged_source_title';name_resolved=True
        if converter.convert(word)!=word:
            explicit=labels.get(identity['id'],{}) if identity else {}
            label=next((explicit[lang]['value'] for lang in ('zh-tw','zh-hant') if lang in explicit),None)
            if label:word=re.sub(r'\s*[（(][^()（）]*[)）]$','',label).strip();normalization='explicit_traditional_source_label'
            else:name_resolved=False;normalization='unresolved_script_or_name_variant'
        sectors=item['sectors'];record=dict(pageid=item['pageid'],title=original,output=word,sectors=sectors,roots=item['roots'])
        types={v.get('id') for v in identity['claims']['P31']} if identity else set()
        record['name_normalization']=normalization
        record['wikidata']=identity
        # A singer's category may also contain their albums. Category ancestry
        # supplies topics; linked structured identity validates person membership.
        accepted_sectors=[s for s in sectors if s not in ('people','performers') or 'Q5' in types]
        if 'Q5' in types:accepted_sectors=[s for s in accepted_sectors if s not in ('films','books','songs','animals','plants','foods','beverages')]
        record['accepted_sectors']=accepted_sectors
        for sector in sectors:counts[sector]['source_entities']+=1
        if not name_resolved:record['status']='unresolved_script_or_name_variant'
        elif types.intersection(('Q4167410','Q13406463')):record['status']='disambiguation_or_list_entity'
        elif not accepted_sectors:record['status']='unresolved_person_identity' if not identity else 'category_topic_is_not_person'
        elif not (2<=len(word)<=24 and all('CJK UNIFIED' in unicodedata.name(c,'') or 'CJK COMPATIBILITY IDEOGRAPH' in unicodedata.name(c,'') for c in word)):
            record['status']='unsupported_script_or_length'
        elif word.endswith(('列表','一覽','作品列表')) or re.search(r'^(第[一二三四五六七八九十百]+屆|[一二三四五六七八九十]+年代)',word):
            record['status']='index_or_edition_title'
        else:
            reading=resolve(word)
            if not reading:record['status']='unresolved_reading'
            elif len("'".join(reading[0]))>96:record['status']='reading_too_long'
            else:
                plain,toned,segments=reading;source='zhwiki:'+key+':dictionary-units';category='+'.join(accepted_sectors)
                add('taiwan',"'".join(plain),word,source,category)
                if len(plain)>1:add('taiwan',''.join(s[0] for s in plain),word,source,category)
                if toned:add('taiwan',''.join(syllables[p[:-1]]+'ˉˊˇˋ˙'[int(p[-1])-1] for p in toned),word,source,category)
                record.update(status='included',pinyin=plain,zhuyin_available=bool(toned),reading_units=segments)
                for sector in accepted_sectors:outputs[sector].add(word)
        for sector in sectors:counts[sector][record['status']]+=1
        records.append(record)
    files=[p for p in (ROOT/'third_party/opencc_python/opencc').rglob('*') if p.suffix in ('.py','.txt','.json')]
    report=dict(snapshot_sha256=hashlib.sha256(path.read_bytes()).hexdigest(),entity_types_sha256=hashlib.sha256(type_path.read_bytes()).hexdigest(),traditional_labels_sha256=hashlib.sha256(label_path.read_bytes()).hexdigest(),category_counts={k:dict(v) for k,v in counts.items()},unique_outputs_by_sector={k:len(v) for k,v in outputs.items()},records=records,
      reading_policy='Longest unambiguous existing CC-CEDICT/McBopomofo units. Tonal ambiguity may retain an unambiguous untoned Pinyin key, but no guessed Zhuyin is emitted. Readings are derived, not independently verified names.',
      normalization='Preserve unchanged source titles. Where OpenCC s2tw would change a title, require an explicit Wikidata zh-tw/zh-hant label instead of applying conversion. Strip trailing disambiguator; retain 2-24 Han characters; omit index/edition titles. No guessed surname substitutions or name allowlists.',
      reading_sources={str(p.relative_to(ROOT)).replace('\\','/'):hashlib.sha256(p.read_bytes()).hexdigest() for p in [ROOT/'app/src/main/assets/zh_tw.tsv',ROOT/'third_party/cedict/cedict.txt.gz']},
      opencc=dict(package='opencc-python-reimplemented',version='0.1.7',license='Apache-2.0',files={str(p.relative_to(ROOT/'third_party/opencc_python')).replace('\\','/'):hashlib.sha256(p.read_bytes()).hexdigest() for p in sorted(files)}))
    (ROOT/'docs/taiwan-quality/entity-coverage.json').write_bytes((json.dumps(report,ensure_ascii=False,indent=2)+'\n').encode('utf-8'))
    return {k:v for k,v in report.items() if k!='records'}
