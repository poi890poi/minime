"""Report the frozen Rudy retrieval repair, including losses and resource cost."""
import argparse, collections, csv, gzip, hashlib, itertools, json
from pathlib import Path

def read(path):
    with path.open(encoding='utf-8') as stream:
        return list(csv.DictReader(stream, delimiter='\t'))

def main():
    parser=argparse.ArgumentParser()
    parser.add_argument('artifacts',type=Path)
    parser.add_argument('output',type=Path)
    args=parser.parse_args(); folder=args.artifacts; out=args.output
    groups=collections.defaultdict(collections.Counter); changes=[]
    before=read(folder/'before.tsv');after=read(folder/'after.tsv')
    for a,b in itertools.zip_longest(before,after):
        assert a is not None and b is not None
        assert all(a[k]==b[k] for k in ('group','condition','raw','target','reading'))
        c=groups[a['group']+'/'+a['condition']];c['cases']+=1
        for limit in (1,8):
            old=0<int(a['rank'])<=limit;new=0<int(b['rank'])<=limit
            c[f'before_top{limit}']+=old;c[f'after_top{limit}']+=new
            c[f'gains_top{limit}']+=new and not old;c[f'losses_top{limit}']+=old and not new
        if a['rank']!=b['rank']:changes.append(dict(before=a,after=b))
    old_rows=set((folder/'geography-before.tsv').read_text(encoding='utf-8').splitlines()[1:])
    new_asset=Path('app/src/main/assets/geography.tsv')
    new_rows=set(new_asset.read_text(encoding='utf-8').splitlines()[1:])
    old_names={r.split('\t')[2] for r in old_rows};new_names={r.split('\t')[2] for r in new_rows}
    assert old_rows<=new_rows and old_names==new_names
    assert all(':upstream' in r.split('\t')[3] for r in new_rows-old_rows)
    broad=collections.defaultdict(collections.Counter);space=[];target_losses=[]
    with Path('artifacts/match-evidence/chinese.tsv').open(encoding='utf-8') as left,(folder/'chinese.tsv').open(encoding='utf-8') as right:
        for a,b in itertools.zip_longest(csv.DictReader(left,delimiter='\t'),csv.DictReader(right,delimiter='\t')):
            assert a is not None and b is not None
            assert all(a[k]==b[k] for k in ('genre','id','condition','raw','target'))
            for key in ('all',a['genre'],a['genre']+'/'+a['condition']):
                c=broad[key];c['cases']+=1;c['order_changed']+=a['outputs']!=b['outputs']
                c['space_changed']+=a['space']!=b['space']
                if a['target']:
                    c['labeled']+=1
                    for limit in (1,5,8,1000000):
                        old=0<int(a['target_rank'])<=limit;new=0<int(b['target_rank'])<=limit
                        label=str(limit) if limit<1000000 else 'any'
                        c['gains_'+label]+=new and not old;c['losses_'+label]+=old and not new
            if a['space']!=b['space']:space.append(dict(before=a,after=b))
            if 0<int(a['target_rank'])<=8 and not 0<int(b['target_rank'])<=8:target_losses.append(dict(before=a,after=b))
    resources=[]
    for path in sorted(folder.glob('*.load.json')):
        tsv=Path(str(path).removesuffix('.load.json'))
        unique={row['raw']:int(row['lookup_ns']) for row in read(tsv)}
        times=sorted(unique.values())
        resources.append(dict(run=tsv.name,**json.loads(path.read_text()),unique_queries=len(times),
            lookup_p50_ms=times[len(times)//2]/1e6,lookup_p95_ms=times[int(len(times)*.95)]/1e6,
            lookup_p99_ms=times[int(len(times)*.99)]/1e6,lookup_max_ms=times[-1]/1e6))
    report=dict(scope='Known-source retrieval in up to eight geography-pack candidates; not linguistic accuracy or fresh holdout.',
        groups={k:dict(v) for k,v in sorted(groups.items())},
        asset=dict(before_rows=len(old_rows),after_rows=len(new_rows),added_aliases=len(new_rows-old_rows),
            names=len(new_names),removed_rows=0,added_names=0,before_bytes=(folder/'geography-before.tsv').stat().st_size,after_bytes=new_asset.stat().st_size),
        broad={k:dict(v) for k,v in sorted(broad.items())},resources=resources,
        hashes={p.as_posix():hashlib.sha256(p.read_bytes()).hexdigest() for p in [folder/'before.tsv',folder/'after.tsv',new_asset]})
    out.mkdir(parents=True,exist_ok=True)
    (out/'summary.json').write_bytes((json.dumps(report,ensure_ascii=False,indent=2)+'\n').encode())
    for name,rows in [('rank-changes',changes),('space-changes',space),('broad-target-losses',target_losses)]:
        (out/(name+'.jsonl.gz')).write_bytes(gzip.compress(('\n'.join(json.dumps(r,ensure_ascii=False) for r in rows)+'\n').encode(),mtime=0))
    print(json.dumps(dict(groups=report['groups'],asset=report['asset'],broad=report['broad']['all'],resources=resources),ensure_ascii=False,indent=2))

if __name__=='__main__':main()
