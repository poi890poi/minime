"""Read an immutable public GUM revision into an isolated local evaluation cache."""
import gzip,hashlib,json,urllib.request
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
OUT=ROOT/'artifacts/source-audit/grammar-holdout'
REVISION='1fe635509c649e376dfb449d528424ab78f4eaee'


def main():
    OUT.mkdir(parents=True,exist_ok=True)
    manifest=dict(revision=REVISION,files={})
    for name in ('README.md','LICENSE.txt','en_gum-ud-train.conllu','en_gum-ud-dev.conllu','en_gum-ud-test.conllu'):
        url=f'https://raw.githubusercontent.com/UniversalDependencies/UD_English-GUM/{REVISION}/{name}'
        target=OUT/(name+'.gz' if name.endswith('.conllu') else name)
        if target.exists():raise ValueError('Refusing to overwrite an existing source capture')
        request=urllib.request.Request(url,headers={'User-Agent':'MinIMESourceAudit/1.0 (https://github.com/poi890poi/minime)'})
        with urllib.request.urlopen(request,timeout=30) as stream:raw=stream.read(64*1024*1024+1)
        if len(raw)>64*1024*1024:raise ValueError('Source size exceeds acquisition bound')
        target.write_bytes(gzip.compress(raw,mtime=0) if name.endswith('.conllu') else raw)
        manifest['files'][name]=dict(url=url,bytes=len(raw),sha256=hashlib.sha256(raw).hexdigest(),file=target.name)
        (OUT/'download.json').write_text(json.dumps(manifest,indent=2)+'\n')
        print(name,len(raw),manifest['files'][name]['sha256'],flush=True)


if __name__=='__main__':main()
