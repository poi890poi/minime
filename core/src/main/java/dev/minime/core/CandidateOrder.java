package dev.minime.core;

import java.util.*;
import java.util.function.*;

/** Query-local keys: learning and span checks are stable during one application. */
final class CandidateOrder {
    private CandidateOrder() {}
    private static final class Key {
        final Candidate value;
        final boolean partial;
        final int votes;
        Key(Candidate value,boolean partial,int votes) {this.value=value;this.partial=partial;this.votes=votes;}
    }
    private static final Comparator<Key> ORDER=(a,b)-> {
        int order=Boolean.compare(a.partial,b.partial);
        if(order==0)order=Integer.compare(b.votes,a.votes);
        return order!=0?order:Double.compare(b.value.score,a.value.score);
    };
    static void sort(List<Candidate> values,Predicate<Candidate> partial,ToIntFunction<Candidate> votes) {
        if(values.size()<2)return;
        Key[] keys=new Key[values.size()];
        for(int i=0;i<keys.length;i++) {
            Candidate value=values.get(i);keys[i]=new Key(value,partial.test(value),votes.applyAsInt(value));
        }
        // Object-array sorting is stable, preserving source order for exact ties.
        Arrays.sort(keys,ORDER);
        for(int i=0;i<keys.length;i++)values.set(i,keys[i].value);
    }
}
