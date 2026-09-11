"""Tiny synthetic contract fixture. Never used for vocabulary or quality scores."""
import subprocess
from evaluate import Worker,WORK,HERE,dump

def main():
    directory=WORK/'predictive-fixture';directory.mkdir(exist_ok=True)
    readings=['か','かき','かく','かきく','さしすせそ']
    for i in range(10):
        (directory/f'dictionary{i:02d}.txt').write_text(''.join(f'{r}\t0\t0\t1000\t{r}\n' for r in readings) if i==0 else '',encoding='utf8')
    subprocess.run([str(WORK/'build/tries_token_builder.exe'),'--in_dir',str(directory),'--out_dir',str(directory)],check=True,capture_output=True)
    (directory/'connection_single_column.bin').write_bytes(b'\x00\x00')
    output={}
    for mode in ['cps','graph','lexical','lexical-fullprefix']:
        worker=Worker(mode,directory)
        try:output[mode]={r:worker.query(r) for r in ['か','かき','かく','さし','さしすせそ','あ']}
        finally:worker.close()
    assert output['cps']['かき']['mismatched_nodes']==0
    assert output['graph']['かき']['mismatched_nodes']>0
    assert 'かく' in output['graph']['かき']['choices']
    assert 'かきく' not in output['graph']['かき']['choices']
    for r in output['lexical']:
        assert output['lexical'][r]['choices']==output['lexical-fullprefix'][r]['choices']
    cmd=[str(WORK/'build/astar_bunsetsu_cli.exe')]
    for key,file in [('yomi_termid','yomi_termid.louds'),('tango','tango.louds'),('tokens','token_array.bin'),('pos_table','pos_table.bin'),('conn','connection_single_column.bin')]:cmd+=['--'+key,str(directory/file)]
    text=subprocess.check_output(cmd+['--stdin','--n','8','--beam','50','--show_prediction','--pred_n','8'],input='かき\n',encoding='utf8')
    lexical=text.split('prediction_candidates:\n')[1]
    choices=[line.split('\t')[1] for line in lexical.splitlines() if '\t' in line]
    assert choices==output['lexical']['かき']['choices']
    dump(HERE/'fixture-verification.json',{'synthetic_fixture_only':True,'graph_untyped_substitution_confirmed':True,
        'graph_excludes_longer_completion':True,'full_prefix_ordered_parity_queries':len(output['lexical']),
        'adapter_matches_pinned_cli':True,'raw':output})
    print('PASS synthetic mechanism checks and pinned CLI adapter parity')

if __name__=='__main__':main()
