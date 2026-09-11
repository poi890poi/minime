"""Post-freeze exposure audit. Does not select inputs or change reference labels."""
import collections, gzip, json, re, unicodedata
from run import rows, HERE, ROOT
from make_corpus import dump

def normalize(s):
    return ''.join(c for c in unicodedata.normalize('NFKC',s).casefold() if unicodedata.category(c)[0] not in 'PZS')

def grams(s):return {s[i:i+3] for i in range(len(s)-2)}

def main():
    previous=[json.loads(s)['text'] for s in gzip.open(ROOT/'docs/language-contract-benchmark/conversations/turns.jsonl.gz','rt',encoding='utf8')]
    corpus=rows('utterances.jsonl.gz');reference=[normalize(s) for s in previous]+[normalize(r['text']) for r in corpus if r['role']=='development']
    reference=list(set(reference));sets=[grams(s) for s in reference];index=collections.defaultdict(set)
    for i,s in enumerate(reference):
        if len(s)>=12:
            for gram in sets[i]:index[gram].add(i)
    exact=set(reference);result=[]
    for row in corpus:
        if row['role']!='holdout':continue
        s=normalize(row['text']);g=grams(s);possible=set()
        if len(s)>=12:
            for gram in g:possible.update(index[gram])
        near=any(len(g&sets[i])/len(g|sets[i])>=.8 for i in possible)
        result.append({'id':row['id'],'source':row['source'],'normalized_exact_exposed':s in exact,'near_3gram_jaccard_0_8':near})
    dump(HERE/'overlap-audit.json',{'normalization':'NFKC casefold; remove Unicode punctuation, separators and symbols',
         'near_rule':'At least 12 normalized characters; trigram Jaccard >=0.8 against prior turns or new development turns. Audit only, not an input filter.',
         'counts':{s:{'turns':sum(r['source']==s for r in result),'normalized_exact':sum(r['source']==s and r['normalized_exact_exposed'] for r in result),
                      'near':sum(r['source']==s and r['near_3gram_jaccard_0_8'] for r in result)} for s in ['real-persona-chat','asdc']},'rows':result})
    print('Audited',len(result),'held-out turns for normalized and near repetitions')

if __name__=='__main__':main()
