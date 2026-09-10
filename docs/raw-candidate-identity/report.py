import csv,collections,json,pathlib
p=pathlib.Path('artifacts/poj-unmarked')
rows=list(csv.DictReader((p/'results.tsv').open(encoding='utf-8'),delimiter='\t'))
result={}
for group in ['raw_equal','marked_control']:
 r=[x for x in rows if x['group']==group and x.get('default_output') is not None]
 result[group]=dict(targets=len(r),lookup_found=sum(int(x['lookup_rank'])>0 for x in r),lookup_paired=sum(x['lookup_paired']=='true' for x in r),focused_visible=sum(int(x['visible_focused_rank'])>0 for x in r),raw_paired=sum(x['raw_row_paired']=='true' for x in r),default_matches_target=sum(x['default_output']==x['output'] for x in r))
print(json.dumps(result,indent=2))
(p/'summary.json').write_text(json.dumps(result,indent=2),encoding='utf-8')
print('Collision examples (alphabetical, paired):')
for x in [x for x in rows if x['group']=='raw_equal' and x['lookup_paired']=='true'][:10]: print(x['query'],x['default_output'])
