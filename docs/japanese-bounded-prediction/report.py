"""Compare independent reference hits without feeding labels to the predictor."""
import collections
from evaluate import HERE, PRIOR, read, dump

def main():
    result={'passes':{},'comparison':{}}
    first=read(HERE/'bounded-1.jsonl.gz')
    for n in [1,2,3]:
        rows=read(HERE/f'bounded-{n}.jsonl.gz')
        result['passes'][str(n)]=dict(collections.Counter(
            'completed-empty' if r['available'] and not r['choices'] else
            'completed-nonempty' if r['available'] else
            'work-cap' if r['checks']>8192 else 'deadline' for r in rows))
        assert [r['hit'] for r in rows]==[r['hit'] for r in first]
    for unit in ['clause','word']:
        baseline=read(PRIOR/f'minime-{unit}.jsonl.gz')
        for source in ['real-persona-chat','asdc']:
            for condition in ['full','half','three-quarter']:
                a=[r for r in first if r['unit']==unit and r['source']==source and r['condition']==condition]
                b=[r for r in baseline if r['source']==source and r['condition']==condition]
                assert [(r['id'],r['input'],r['target']) for r in a]==[(r['id'],r['input'],r['target']) for r in b]
                result['comparison'][source+'/'+unit+'/'+condition]={
                    'n':len(a),'bounded_available':sum(r['available'] for r in a),
                    'bounded_hits':sum(r['hit'] for r in a),'prior_minime_hits':sum(r['hit'] for r in b)}
    dump(HERE/'coverage.json',result)
    print(result)

if __name__=='__main__':main()
