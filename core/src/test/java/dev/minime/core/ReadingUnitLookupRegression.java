package dev.minime.core;

import java.lang.reflect.Method;
import java.util.*;

/** Compare single-entry results to the sentence matcher's retained-offset path. */
final class ReadingUnitLookupRegression {
    @SuppressWarnings("unchecked")
    static void run() throws Exception {
        Map<String,List<Candidate>> source=new HashMap<>();
        String[] syllables={"a","aa","ab","aba","b","ba","bb","c","ca","da","ea","fa"};
        for(String a:syllables)for(String b:syllables) {
            source.put(a+"'"+b,Arrays.asList(new Candidate(a+b,false,2),new Candidate(a+"-"+b,false,1)));
            source.put(a,Collections.singletonList(new Candidate(a,false,3)));
        }
        ReadingUnitIndex index=new ReadingUnitIndex(source,source.keySet());
        Method match=ReadingUnitIndex.class.getDeclaredMethod("match",String.class,int.class,int[].class);
        match.setAccessible(true);
        for(String a:syllables)for(String b:syllables)for(String raw:Arrays.asList(a+b,a+"'"+b,a.substring(0,1)+b,a+b.substring(0,1))) {
            List<Candidate> expected=((List<List<Candidate>>)match.invoke(index,raw,0,new int[]{2048})).get(raw.length());
            List<Candidate> actual=index.lookup(raw);
            Regression.equal(describe(expected),describe(actual),"lookup preserves full and partial terminal matches");
        }
        Regression.yes(index.lookup("abc").stream().noneMatch(c->c.composed),"single-entry lookup never joins words");
        Regression.equal(Collections.emptyList(),index.lookup("a'"),"unfinished separated input remains unavailable");
    }
    private static List<String> describe(List<Candidate> values) {
        List<String> result=new ArrayList<>();
        for(Candidate c:values)result.add(c.text+"|"+c.score+"|"+c.incomplete+"|"+c.composed);
        return result;
    }
}
