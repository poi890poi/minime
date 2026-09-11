from pathlib import Path
import sys,shutil,difflib
HERE=Path(__file__).resolve().parent;ROOT=HERE.parents[1]
sys.path.insert(0,str(HERE.parent/'japanese-predictive-review'))
import index_postings

def main():
    index_postings.patch() # verifies pinned archive/source and restores indexed copy
    relative=Path('src/path_algorithm/find_path.cpp')
    for variant,source in [('stable',index_postings.ORIGINAL),('indexed-stable',index_postings.COPY)]:
        target=index_postings.WORK/('kazuma-'+variant)
        shutil.copytree(source,target,dirs_exist_ok=True)
        before=(source/relative).read_text(encoding='utf8');after=before
        substitutions=[
          ('        int total;                   // priority (g + f)','        int total;                   // priority (g + f)\n        uint64_t ordinal;            // MinIME: stable order within this search'),
          ('std::shared_ptr<State> next_)','std::shared_ptr<State> next_, uint64_t ordinal_)'),
          ('total(total_), next(std::move(next_))','total(total_), ordinal(ordinal_), next(std::move(next_))'),
          ('then pointer address','then per-search insertion order'),
          ('return a.get() > b.get();','return a->ordinal > b->ordinal;'),
          ('        pq.push(std::make_shared<State>(eos, /*g=*/0, /*total=*/0, /*next=*/nullptr));','        uint64_t nextOrdinal = 0;\n        pq.push(std::make_shared<State>(eos, /*g=*/0, /*total=*/0, /*next=*/nullptr, nextOrdinal++));'),
          ('std::make_shared<State>(p, newG, newTotal, cur)','std::make_shared<State>(p, newG, newTotal, cur, nextOrdinal++)')]
        for old,new in substitutions:
            assert after.count(old)==1,old
            after=after.replace(old,new,1)
        (target/relative).write_text(after,encoding='utf8',newline='\n')
        patch=''.join(difflib.unified_diff(before.splitlines(True),after.splitlines(True),fromfile='a/'+relative.as_posix(),tofile='b/'+relative.as_posix()))
        (HERE/'stable-ties.patch').write_text(patch,encoding='utf8',newline='\n')
    print('Prepared separate stable and indexed-stable source copies')

if __name__=='__main__':main()
