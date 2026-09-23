package dev.minime.core;

import java.util.*;
import java.util.function.*;

/** Frozen old comparator is the equivalence oracle, not a language-quality label. */
final class CandidateOrderRegression {
    static void run() {
        Random random=new Random(20260924);
        int lists=0;
        for(int size:new int[]{0,1,2,8,24,128,512,2048})for(int round=0;round<12;round++) {
            List<Candidate> source=new ArrayList<>();
            Map<Candidate,Integer> votes=new IdentityHashMap<>();
            for(int i=0;i<size;i++) {
                double score=new double[]{-1,0,0,1,Double.NaN,Double.POSITIVE_INFINITY,Double.NEGATIVE_INFINITY,-0.0}[random.nextInt(8)];
                Candidate value=new Candidate("fixture-"+(i%17),false,score,random.nextBoolean()?2:0);
                source.add(value);votes.put(value,new int[]{0,0,1,2,Integer.MIN_VALUE,Integer.MAX_VALUE}[random.nextInt(6)]);
            }
            for(int pass=0;pass<2;pass++) {
                Predicate<Candidate> partial=c->c.consumed>0;
                ToIntFunction<Candidate> vote=c->votes.get(c);
                List<Candidate> expected=new ArrayList<>(source),actual=new ArrayList<>(source);
                expected.sort(Comparator.comparing(partial::test)
                    .thenComparing(Comparator.comparingInt(vote).reversed())
                    .thenComparing(Comparator.comparingDouble((Candidate c)->c.score).reversed()));
                int[] partialCalls={0},voteCalls={0};
                CandidateOrder.sort(actual,c->{partialCalls[0]++;return partial.test(c);},c->{voteCalls[0]++;return vote.applyAsInt(c);});
                Regression.equal(expected,actual,"sort preserves exact object order, ties and numeric edges");
                Regression.equal(size<2?0:size,partialCalls[0],"span work bounded by candidate count");
                Regression.equal(size<2?0:size,voteCalls[0],"learning work bounded by candidate count");
                votes.replaceAll((c,n)->random.nextInt(4));
            }
            lists++;
        }
        System.out.println("PASS candidate ordering: "+lists+" generated lists, refreshed votes, exact old-comparator parity (not language accuracy)");
    }
}
