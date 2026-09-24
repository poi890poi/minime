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
        Candidate exact=new Candidate("exact",false,3,"reading");
        Candidate rare=new Candidate("rare",false,1,"reading");
        Candidate completion=new Candidate("completion",false,99,"longer").completing(99);
        Candidate prefix=new Candidate("prefix",false,100,"reading").consuming(2);
        List<Candidate> values=new ArrayList<>(Arrays.asList(completion,exact,rare,prefix));
        Regression.equal(exact,CandidateOrder.bestComplete(values),"only a complete whole-input path can be preferred");
        CandidateOrder.sort(values,c->c.consumed>0,c->0,exact);
        Regression.equal(Arrays.asList(exact,completion,rare,prefix),values,"one preference keeps other frequency and span ordering");
        CandidateOrder.sort(values,c->c.consumed>0,c->c==completion?1:0,exact);
        Regression.equal(Arrays.asList(completion,exact,rare,prefix),values,"explicit learning precedes completeness");
        values.add(new Candidate("native-unknown",false,200));
        Regression.equal(null,CandidateOrder.bestComplete(values),"native transport cannot establish spelling completeness");
        Regression.equal(null,CandidateOrder.bestComplete(Arrays.asList(completion,prefix)),"initial-only and prefix-only lists have no complete preference");
        Regression.equal(null,CandidateOrder.bestComplete(Arrays.asList(exact.asConstructed())),"construction cannot establish an attested full word");
    }
}
