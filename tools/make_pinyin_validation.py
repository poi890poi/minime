"""Fresh boundary probes from held-out UD test tokens; never updates model assets."""
import gzip,re
from pathlib import Path
root=Path(__file__).resolve().parent.parent
readings={}
for line in (root/'app/src/main/assets/zh_tw.tsv').read_text(encoding='utf-8').splitlines():
    p=line.split('\t');value=(float(p[3]),p[0].replace("'",''))
    if p[2] not in readings or readings[p[2]][0]<value[0]:readings[p[2]]=value
seen={tuple(line.split('\t')[1:]) for line in (root/'docs/pinyin-boundary-holdout.tsv').read_text(encoding='utf-8').splitlines() if line}
source=root/'third_party/ud/UD_Chinese-GSD/zh_gsd-ud-test.conllu.gz'
rows=[];context='';eligible=0
for line in gzip.decompress(source.read_bytes()).decode('utf-8').splitlines():
    if not line:context='';continue
    if line.startswith('#'):continue
    p=line.split('\t')
    if not p[0].isdigit():continue
    word=p[1]
    if not re.fullmatch('[\u3400-\u9fff]+',word):context='';continue
    if context and word in readings:
        eligible+=1;case=(context[-3:],readings[word][1],word)
        if eligible>400 and case not in seen:rows.append(case);seen.add(case)
    context=(context+word)[-3:]
    if len(rows)==400:break
(root/'docs/gap-implementation/pinyin-validation.tsv').write_text(''.join('validation-%d\t%s\n'%(i,'\t'.join(row)) for i,row in enumerate(rows)),encoding='utf-8')
print('Fresh nonduplicate cases',len(rows))
