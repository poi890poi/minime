package dev.minime.core;

import java.util.*;

/** Keep the primary decoder's hypotheses; supplement them with stored entries. */
public final class CandidateMerge {
    private CandidateMerge() {}
    public static List<Candidate> merge(List<Candidate> primary,List<Candidate> fallback) {
        boolean full=primary.stream().anyMatch(c->c.consumed==0);
        List<Candidate> result=new ArrayList<>(primary);Set<String> seen=new HashSet<>();
        for(Candidate c:result)seen.add(c.text);
        for(Candidate c:fallback)if((!full || !c.composed) && seen.add(c.text))result.add(c);
        return result;
    }
}
