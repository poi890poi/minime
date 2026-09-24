"""Screen fixed grammar flags against independent, source-annotated expansions."""
import collections,gzip,hashlib,json,re
from pathlib import Path

ROOT=Path(__file__).resolve().parents[2]
OUT=ROOT/'artifacts/source-audit/grammar-source'


def references(text,source):
    doc='unknown';rows=[];excluded=collections.Counter()
    for block in text.replace('\r\n','\n').split('\n\n'):
        tokens={};spans=[];sentence='unknown'
        for line in block.splitlines():
            if line.startswith('# newdoc id = '):doc=line.split(' = ',1)[1]
            if line.startswith('# sent_id = '):sentence=line.split(' = ',1)[1]
            p=line.split('\t')
            if len(p)!=10:continue
            if p[0].isdigit():tokens[int(p[0])]=p
            elif re.fullmatch(r'\d+-\d+',p[0]):spans.append(p)
        genre_match=re.match(r'GUM_([^_]+)_',doc)
        genre=genre_match[1] if genre_match else 'unspecified-web-genre'
        for span in spans:
            word=span[1].lower().replace('’',"'")
            if not re.fullmatch("[a-z]+(?:'[a-z]+)+",word):excluded['outside-shape']+=1;continue
            first,last=map(int,span[0].split('-'))
            if any(i not in tokens for i in range(first,last+1)):excluded['missing-expansion']+=1;continue
            tags={tokens[i][3] for i in range(first,last+1)}
            category='auxiliary' if 'AUX' in tags else 'verb-without-auxiliary' if 'VERB' in tags else 'nonverbal'
            rows.append(dict(source=source,genre=genre,document=doc,sentence=sentence,word=word,category=category))
    return rows,dict(excluded)


def main():
    inputs={
        'ewt-dev':('UD_English-EWT/en_ewt-ud-dev.conllu.gz','14a82ce2c4c4648c3e7c4efc76872c85c5f4c39643fcb7f9c91a45a58157a270'),
        'ewt-test':('UD_English-EWT/en_ewt-ud-test.conllu.gz','0612e45914359e4b36d187ef2bfeaf66fc8933210925be985c5fbf6a9e311036'),
        'gum-test':('UD_English-GUM/en_gum-ud-test.conllu.gz','99deb5746618d8ab83e5cafc2d07851e76389b2c8babf5ad9e5ef176a4619114')}
    old={l.split('\t')[1] for l in (ROOT/'app/src/main/assets/en_spelling.tsv').read_text().splitlines() if l.startswith('contraction\t')}
    new={l.split('\t')[1] for l in (OUT/'en_spelling.tsv').read_text().splitlines() if l.startswith('contraction\t')}
    groups=collections.defaultdict(list);all_rows=[];exclusions={}
    for name,(relative,pin) in inputs.items():
        data=gzip.decompress((ROOT/'third_party/ud'/relative).read_bytes())
        assert hashlib.sha256(data).hexdigest()==pin
        rows,exclusions[name]=references(data.decode('utf-8'),name);all_rows.extend(rows)
        for row in rows:
            for key in (name+'/'+row['category'],name+'/'+row['genre']+'/'+row['category']):groups[key].append(row)
    result={}
    for key,rows in sorted(groups.items()):
        words={r['word'] for r in rows}
        result[key]=dict(occurrences=len(rows),unique_surfaces=len(words),old_occurrences=sum(r['word'] in old for r in rows),new_occurrences=sum(r['word'] in new for r in rows),old_unique=len(words&old),new_unique=len(words&new),gained_occurrences=sum(r['word'] in new-old for r in rows),lost_occurrences=sum(r['word'] in old-new for r in rows))
    (OUT/'grammar-reference-rows.json').write_text(json.dumps(all_rows,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    report=dict(scope='All eligible multiword surfaces in pinned reused evaluation splits; grammar-flag inclusion, not runtime restoration or safe mixed-language intent',source_pins=inputs,exclusions=exclusions,groups=result)
    (OUT/'grammar-reference-summary.json').write_text(json.dumps(report,indent=2)+'\n')
    print(json.dumps({k:v for k,v in result.items() if k.count('/')==1},indent=2))


if __name__=='__main__':main()
