package dev.minime.core;

import java.util.*;
import java.util.function.Predicate;

/** Unknown-confidence Chinese assembly keeps a first-page recovery slot but
 * cannot displace the first three dictionary choices. Never promotes a late
 * construction. Raw input at zero retains its independent recovery identity. */
final class ConstructionPolicy {
    private ConstructionPolicy() {}
    static List<Candidate> rank(List<Candidate> input,Predicate<Candidate> unverified) {
        if(input.size()<2)return input;
        List<Candidate> output=new ArrayList<>(input.size()),deferred=new ArrayList<>();
        output.add(input.get(0));int early=0;
        for(int i=1;i<input.size();i++) {
            Candidate c=input.get(i);
            if(early<3 && unverified.test(c))deferred.add(c);
            else {
                output.add(c);
                if(++early==3){output.addAll(deferred);deferred.clear();}
            }
        }
        output.addAll(deferred);return output;
    }
}
