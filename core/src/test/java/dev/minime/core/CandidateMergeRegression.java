package dev.minime.core;

import java.util.*;
import static dev.minime.core.Regression.*;

final class CandidateMergeRegression {
    static void run() {
        Candidate lexical=new Candidate("甲乙",false,1),a=new Candidate("丙",false,1),b=new Candidate("丁",false,1);
        Candidate sequence=Candidate.concatenate(a,b),nativeChoice=new Candidate("戊己",false,100);
        List<Candidate> fallback=Arrays.asList(sequence,lexical);
        equal(Arrays.asList(nativeChoice,lexical),CandidateMerge.merge(Arrays.asList(nativeChoice),fallback),"native success excludes extra fallback combinations");
        equal(fallback,CandidateMerge.merge(Collections.emptyList(),fallback),"native unavailable preserves complete fallback");
        Candidate prefix=new Candidate("丙",false,100,2);
        equal(Arrays.asList(prefix,sequence,lexical),CandidateMerge.merge(Arrays.asList(prefix),fallback),"prefix-only native result still needs whole-token fallback");
        equal(Arrays.asList(lexical),CandidateMerge.merge(Arrays.asList(lexical),fallback),"lexical supplement deduplicates primary without mutating it");
    }
}
