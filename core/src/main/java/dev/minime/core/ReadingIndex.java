package dev.minime.core;

import java.util.*;
import java.util.function.BiPredicate;

/** Sorted reading ranges with score bounds; no duplicated prefix trie or per-key full scan. */
final class ReadingIndex {
    private final String[] keys;
    private final List<Candidate>[] words;
    private final double[] best;
    private final int base;
    ReadingIndex(BinaryModel.Reader in)throws java.io.IOException {keys=in.strings();words=in.lists();best=in.doubles();base=in.in.readInt();}
    void write(BinaryModel.Writer out)throws java.io.IOException {out.strings(keys);out.lists(words);out.doubles(best);out.out.writeInt(base);}
    @SuppressWarnings("unchecked")
    ReadingIndex(Map<String,List<Candidate>> source) {
        keys=source.keySet().stream().filter(s->!s.startsWith("~")).sorted().toArray(String[]::new);
        words=(List<Candidate>[])new List<?>[keys.length];
        int size=1; while(size<keys.length) size*=2; base=size;
        best=new double[size*2]; Arrays.fill(best,Double.NEGATIVE_INFINITY);
        for(int i=0;i<keys.length;i++) { words[i]=source.get(keys[i]); if(!words[i].isEmpty()) best[base+i]=words[i].get(0).score; }
        for(int i=base-1;i>0;i--) best[i]=Math.max(best[i*2],best[i*2+1]);
    }
    private int lower(String key) { int at=Arrays.binarySearch(keys,key);return at<0?-at-1:at; }
    List<Candidate> exact(String key) {
        int at=Arrays.binarySearch(keys,key);
        return at<0?Collections.emptyList():words[at];
    }
    boolean containsPrefix(String prefix) { int at=lower(prefix);return at<keys.length && keys[at].startsWith(prefix); }
    private static final class Range {
        final int node,left,right;
        Range(int node,int left,int right) {this.node=node;this.left=left;this.right=right;}
    }
    List<Candidate> complete(String prefix,BiPredicate<String,Candidate> accept) {
        return complete(prefix,accept,8);
    }
    List<Candidate> complete(String prefix,BiPredicate<String,Candidate> accept,int limit) {
        if(prefix.isEmpty()) return Collections.emptyList();
        int from=lower(prefix),to=lower(prefix+'\uffff');
        if(from==to) return Collections.emptyList();
        PriorityQueue<Range> queue=new PriorityQueue<>((a,b)->Double.compare(best[b.node],best[a.node]));
        queue.add(new Range(1,0,base));
        Map<String,Candidate> found=new HashMap<>();
        // Only the eighth score is needed for the search bound. Re-sorting every
        // accumulated candidate at every tree node is costly for short prefixes.
        // Keep all found candidates for the unchanged final score/text ordering.
        PriorityQueue<String> top=new PriorityQueue<>(Comparator.comparingDouble(text->found.get(text).score));
        int visited=0;
        while(!queue.isEmpty() && visited++<512) {
            Range r=queue.remove();
            if(r.right<=from || r.left>=to) continue;
            if(r.right-r.left==1) {
                String key=keys[r.left]; if(key.length()<=prefix.length()) continue;
                // Fixed prior for untyped input, independent of the evaluation phrases.
                double penalty=.7+.08*(key.length()-prefix.length());
                int accepted=0;
                for(Candidate c:words[r.left]) {
                    if(!accept.test(key,c)) continue;
                    Candidate value=c.completing(c.score-penalty);
                    Candidate prior=found.get(value.text);
                    if(prior==null || prior.score<value.score) {
                        // Remove before changing a score used by the heap comparator.
                        top.remove(value.text);found.put(value.text,value);top.add(value.text);
                        if(top.size()>limit)top.remove();
                    }
                    if(++accepted==limit) break;
                }
            } else {
                int mid=(r.left+r.right)/2;
                if(mid>from) queue.add(new Range(r.node*2,r.left,mid));
                if(mid<to) queue.add(new Range(r.node*2+1,mid,r.right));
            }
            if(found.size()>=limit) {
                double threshold=found.get(top.element()).score;
                if(queue.isEmpty() || best[queue.peek().node]-.7<=threshold) break;
            }
        }
        List<Candidate> result=new ArrayList<>(found.values());
        result.sort(Comparator.comparingDouble((Candidate c)->c.score).reversed().thenComparing(c->c.text));
        return result.subList(0,Math.min(limit,result.size()));
    }
}
