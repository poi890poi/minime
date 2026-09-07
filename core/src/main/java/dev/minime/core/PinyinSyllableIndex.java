package dev.minime.core;

import java.util.*;

/** Source syllable boundaries, not generated abbreviation combinations. Immutable after load. */
final class PinyinSyllableIndex {
    private String[] syllable;
    private int[] child, sibling;
    private List<Candidate>[] words;
    private double[] best;
    private int size=1;
    private static final int BEAM=6, CANDIDATES=24, SEARCH_BUDGET=2048, MAX_WORD_INPUT=32;

    @SuppressWarnings("unchecked")
    PinyinSyllableIndex(Map<String,List<Candidate>> source, Set<String> readings) {
        int capacity=readings.size()*2+1;
        syllable=new String[capacity]; child=new int[capacity]; sibling=new int[capacity];
        words=(List<Candidate>[])new List<?>[capacity]; best=new double[capacity];
        Map<String,String> pool=new HashMap<>();
        String[] previous=new String[0]; int[] path=new int[32];
        for(String reading:new TreeSet<>(readings)) {
            String[] parts=reading.split("'");
            if(parts.length>=path.length) path=Arrays.copyOf(path,parts.length+1);
            int common=0;
            while(common<previous.length && common<parts.length && previous[common].equals(parts[common])) common++;
            for(int i=common;i<parts.length;i++) {
                if(size==syllable.length) grow(size*2);
                int node=size++,parent=path[i];
                syllable[node]=pool.computeIfAbsent(parts[i],s->s);
                sibling[node]=child[parent]; child[parent]=node; path[i+1]=node;
            }
            words[path[parts.length]]=source.get(reading); previous=parts;
        }
        grow(size);
        Arrays.fill(best,Double.NEGATIVE_INFINITY);
        for(int node=size-1;node>=0;node--) {
            if(words[node]!=null && !words[node].isEmpty()) best[node]=words[node].get(0).score;
            for(int c=child[node];c!=0;c=sibling[c]) best[node]=Math.max(best[node],best[c]);
        }
    }
    private void grow(int capacity) {
        syllable=Arrays.copyOf(syllable,capacity); child=Arrays.copyOf(child,capacity);
        sibling=Arrays.copyOf(sibling,capacity); words=Arrays.copyOf(words,capacity); best=Arrays.copyOf(best,capacity);
    }
    private static double penalty(int missing) { return missing==0?0:.7+.08*missing; }
    private final class State {
        final int node,at,missing;
        final double bound;
        State(int node,int at,int missing) {this.node=node;this.at=at;this.missing=missing;bound=best[node]-penalty(missing);}
    }
    private static void trim(List<Candidate> list,int limit,boolean diverse) {
        list.sort(Comparator.comparingDouble((Candidate c)->c.score).reversed().thenComparing(c->c.text));
        Set<String> seen=new HashSet<>();list.removeIf(c->!seen.add(c.text));
        if(!diverse) {
            if(list.size()>limit) list.subList(limit,list.size()).clear();
            return;
        }
        // A digraph such as sh can be one syllable or two initials. Do not let
        // high-frequency single glyphs evict every multi-syllable interpretation.
        Map<Integer,Integer> lengths=new HashMap<>();
        int kept=0;
        for(Iterator<Candidate> it=list.iterator();it.hasNext();) {
            Candidate c=it.next(); int length=c.text.codePointCount(0,c.text.length());
            int count=lengths.getOrDefault(length,0);
            if(kept>=limit && count>=BEAM) it.remove();
            else { lengths.put(length,count+1); kept++; }
        }
    }
    private List<List<Candidate>> match(String raw,int start,int[] budget) {
        int limit=Math.min(raw.length(),start+MAX_WORD_INPUT);
        List<List<Candidate>> matches=new ArrayList<>();
        for(int i=0;i<=limit;i++) matches.add(new ArrayList<>());
        PriorityQueue<State> queue=new PriorityQueue<>(Comparator.comparingDouble((State s)->s.bound).reversed()
            .thenComparingInt(s->s.node).thenComparingInt(s->s.at));
        queue.add(new State(0,start,0));
        // A node fixes the source reading; only the least omitted input at an offset matters.
        Map<Long,Integer> visited=new HashMap<>();
        int explored=0;
        while(!queue.isEmpty() && explored++<SEARCH_BUDGET && budget[0]-->0) {
            State state=queue.remove();
            long identity=((long)state.node<<32)|state.at;
            Integer prior=visited.get(identity);
            if(prior!=null && prior<=state.missing) continue;
            visited.put(identity,state.missing);
            if(words[state.node]!=null) {
                List<Candidate> at=matches.get(state.at);
                for(Candidate c:words[state.node].subList(0,Math.min(CANDIDATES,words[state.node].size())))
                    at.add(new Candidate(c.text,false,c.score-penalty(state.missing),c.reading));
                trim(at,CANDIDATES,true);
            }
            if(state.at==limit) continue;
            for(int node=child[state.node];node!=0;node=sibling[node]) {
                String reading=syllable[node]; int common=0;
                while(common<reading.length() && state.at+common<limit && reading.charAt(common)==raw.charAt(state.at+common)) common++;
                for(int used=1;used<=common;used++) {
                    int end=state.at+used;
                    // An apostrophe explicitly ends this syllable, including an abbreviated one.
                    if(end<limit && raw.charAt(end)=='\'') end++;
                    queue.add(new State(node,end,state.missing+reading.length()-used));
                }
            }
        }
        return matches;
    }
    List<Candidate> convert(String raw) {
        if(!raw.matches("[a-zv]+(?:'[a-zv]+)*")) return Collections.emptyList();
        List<List<Candidate>> paths=new ArrayList<>();
        for(int i=0;i<=raw.length();i++) paths.add(new ArrayList<>());
        paths.get(0).add(new Candidate("",false,0));
        int[] budget={16384};
        for(int start=0;start<raw.length();start++) {
            List<Candidate> before=paths.get(start); trim(before,BEAM,false);
            if(before.isEmpty()) continue;
            List<List<Candidate>> matches=match(raw,start,budget);
            for(int end=start+1;end<matches.size();end++) {
                List<Candidate> next=paths.get(end);
                for(Candidate prefix:before) for(Candidate word:matches.get(end))
                    next.add(new Candidate(prefix.text+word.text,false,prefix.score+word.score));
                // Intermediate paths have a strict beam. Display diversity belongs
                // only to the final candidate list, not every partial sentence.
                trim(next,end==raw.length()?CANDIDATES:BEAM,end==raw.length());
            }
        }
        return paths.get(raw.length());
    }
}
