"""Validate the frozen runs and export aggregate timing/profile evidence only."""
import csv
import hashlib
import itertools
import json
import math
import statistics
from pathlib import Path

ROOT=Path(__file__).resolve().parents[3]
HERE=ROOT/'artifacts/provider-lookup-profile'


def read(path):
    with path.open(encoding='utf-8-sig') as stream:return list(csv.DictReader(stream,delimiter='\t'))


def stats(rows):
    values=sorted(int(r['nanos'])/1000 for r in rows)
    return dict(n=len(values),mean_us=statistics.mean(values),p50_us=statistics.median(values),
        p95_us=values[math.ceil(.95*len(values))-1],p99_us=values[math.ceil(.99*len(values))-1],max_us=max(values))


def main():
    corpus=ROOT/'docs/release-hardening/language-timing/inputs.tsv'
    originals=[r for r in read(corpus) if r['mode']=='taiwanese_english']
    if len(originals)!=224:raise ValueError('Changed corpus')
    inventory=[(r['source'],r['genre'],r['condition'],str(length)) for r in originals for length in range(1,len(r['raw'])+1)]
    prefixes=len(inventory)
    for length in range(1,4):
        inventory.extend(('exhaustive-ascii','synthetic','length-'+str(length),str(length)) for _ in itertools.product(range(26),repeat=length))
    loaded={tag:read(HERE/(tag+'.tsv')) for tag in ['plain','profile-workspace']}
    result=dict(corpus_queries=224,corpus_prefixes=prefixes,structural=18278,queries_per_pass=len(inventory),runs={})
    baseline=None
    for tag,rows in loaded.items():
        if len(rows)!=3*len(inventory):raise ValueError('Incomplete run: '+tag)
        per_pass={}
        for number in range(3):
            selected=rows[number*len(inventory):(number+1)*len(inventory)]
            signatures=[]
            for index,(r,expected) in enumerate(zip(selected,inventory)):
                if int(r['pass'])!=number or int(r['index'])!=index or tuple(r[k] for k in ['source','genre','condition','length'])!=expected:
                    raise ValueError('Different query order or source labels')
                if int(r['nanos'])<0:raise ValueError('Negative time')
                signatures.append((r['candidates'],r['fingerprint']))
            if baseline is None:baseline=signatures
            if signatures!=baseline:raise ValueError('Changed candidate inventory/metadata')
            digest=hashlib.sha256(b''.join(bytes.fromhex(r['fingerprint']) for r in selected)).hexdigest()
            per_pass[str(number)]=dict(all=stats(selected),result_sha256=digest,
                corpus=stats([r for r in selected if r['source']!='exhaustive-ascii']),
                structural=stats([r for r in selected if r['source']=='exhaustive-ascii']))
        strata={}
        for key in dict.fromkeys(tuple(r[k] for k in ['source','genre','condition','length']) for r in rows):
            strata['/'.join(key)]=stats([r for r in rows if tuple(r[k] for k in ['source','genre','condition','length'])==key])
        result['runs'][tag]=dict(lookups=len(rows),passes=per_pass,strata=strata)
    result['all_ordered_fingerprints_equal']=True
    events=read(HERE/'events-normalized.tsv')
    result['profile_events']=[dict(category=r['category'],name=r['name'],unit=r['unit'],count=int(r['count']),total=int(r['total'])) for r in events]
    if any(r['category']=='events' and r['name']=='jdk.DataLoss' for r in events):raise ValueError('Profile data loss')
    result['failed_attempt']=dict(file='profile.log',measured_passes=0,reason='Default JFR temporary directory denied; retry used workspace-owned paths.')
    result['limits']=['HotSpot, not ART or phone latency.','Sampling counts/weights are estimates, not exact CPU or retained-heap measurements.',
        'Fingerprint/output work lies outside timed lookup but inside the recording window; outside-lookup samples remain counted.',
        'Whole-run GC cannot be attributed exclusively to lookup; the harness also allocates.',
        'One allocation sample has no stack; its weight remains unknown, not assigned.',
        'Previously exposed corpus and exhaustive structural inputs do not measure language accuracy.']
    files=[corpus,HERE/'inputs.json',HERE/'plain.log',HERE/'plain.tsv',HERE/'profile.log',HERE/'profile.tsv',
           HERE/'profile-workspace.log',HERE/'profile-workspace.tsv',HERE/'profile-workspace.jfr',HERE/'events-normalized.tsv',
           Path(__file__),Path(__file__).parent/'ProviderProfile.java',Path(__file__).parent/'ProfileEvents.java']
    result['hashes']={str(p.relative_to(ROOT)):hashlib.sha256(p.read_bytes()).hexdigest() for p in files}
    (HERE/'summary.json').write_text(json.dumps(result,indent=2)+'\n',encoding='utf-8')
    print('Validated',sum(r['lookups'] for r in result['runs'].values()),'measured lookups; all ordered results equal')
    for tag,run in result['runs'].items():
        for number,p in run['passes'].items():print(tag,number,'corpus',p['corpus'],'structural',p['structural'])


if __name__=='__main__':main()
