"""Prepare conservative dictionary-sourced Pinyin inputs for frozen Wikipedia titles.
Missing/ambiguous readings remain visible in a separate report, never guessed.
"""
from pathlib import Path
import argparse
import gzip,hashlib,json,re,unicodedata,sys
from collections import defaultdict
root=Path(__file__).resolve().parent.parent
parser=argparse.ArgumentParser();parser.add_argument('mode',nargs='?',choices=['fresh']);parser.add_argument('--source',type=Path);parser.add_argument('--output-dir',type=Path);parser.add_argument('--prefix')
options=parser.parse_args()
folder=options.output_dir or root/'docs/addons-learning';folder.mkdir(parents=True,exist_ok=True)
fresh=options.mode=='fresh'
prefix=options.prefix or ('wikipedia-fresh' if fresh else 'wikipedia')
source=options.source or folder/('wikipedia-fresh-holdout.json' if fresh else 'wikipedia-expanded-holdout.json')
data=json.loads(source.read_text(encoding='utf-8'))
readings=defaultdict(set)
syllables={s.split('\t')[0] for s in (root/'app/src/main/assets/syllables.tsv').read_text(encoding='utf-8').splitlines()}
for line in gzip.open(root/'third_party/cedict/cedict.txt.gz','rt',encoding='utf-8'):
    m=re.match(r'(\S+) \S+ \[([^]]+)\] /(.*)/$',line.strip())
    if not m:continue
    word,pinyin,definition=m.groups();tw=re.search(r'Taiwan pr\. \[([^]]+)\]',definition)
    parts=(tw.group(1) if tw else pinyin).lower().replace('u:','v').replace('ü','v').split()
    if not parts or not all(re.fullmatch('[a-zv]+[1-5]',p) and p[:-1] in syllables for p in parts):continue
    readings[word].add(tuple(p[:-1] for p in parts))
def reading(word):
    result=[];at=0
    while at<len(word):
        for end in range(len(word),at,-1):
            found=readings.get(word[at:end],set())
            if len(found)==1:result.extend(next(iter(found)));at=end;break
        else:return None
    return result
rows=[];skipped=[];seen=set();titles=[]
for category in data['records']:
    for item in category['members']:
        if item['pageid'] in seen:continue
        seen.add(item['pageid']);title=item['title'];word=re.sub(r'\s*[（(][^()（）]*[)）]$','',title)
        parts=reading(word) if word and all('CJK' in unicodedata.name(c,'') for c in word) else None
        record=dict(pageid=item['pageid'],title=title,category=category['category'],target=word)
        if not parts:record['reason']='No unambiguous dictionary-derived reading';skipped.append(record);continue
        record['pinyin']=parts;titles.append(record)
        for mode,key in [('full',''.join(parts)),('initial',''.join(p[0] for p in parts)),('mixed',''.join(p if i%2 else p[0] for i,p in enumerate(parts)))]:
            rows.append('\t'.join(['wiki-'+mode,str(item['pageid']),key,word]))
(folder/(prefix+'-inputs.tsv')).write_text('\n'.join(rows)+'\n',encoding='utf-8')
(folder/(prefix+'-reading-coverage.json')).write_text(json.dumps(dict(source_sha256=hashlib.sha256(source.read_bytes()).hexdigest(),unique_titles=len(seen),eligible_titles=titles,skipped=skipped),ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
print('Independent titles:',len(seen),'eligible readings:',len(titles),'unavailable/ambiguous:',len(skipped),'queries:',len(rows))
