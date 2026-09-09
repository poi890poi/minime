package dev.minime.core;

import java.util.*;

/** Adversarial ties, repeated texts and changing best scores; no production words. */
final class ReadingIndexParity {
    static void run() {
        Random random=new Random(20260909);
        for(int sample=0;sample<32;sample++) {
            Map<String,List<Candidate>> data=new HashMap<>();
            for(int i=0;i<1200;i++) {
                StringBuilder key=new StringBuilder();int length=2+random.nextInt(9);
                for(int j=0;j<length;j++)key.append((char)('a'+random.nextInt(7)));
                List<Candidate> words=data.computeIfAbsent(key.toString(),k->new ArrayList<>());
                for(int j=0;j<1+random.nextInt(6);j++)words.add(new Candidate("word"+random.nextInt(700),false,random.nextInt(sample%3==0?1:20)));
            }
            for(List<Candidate> words:data.values())words.sort(Comparator.comparingDouble((Candidate c)->c.score).reversed().thenComparing(c->c.text));
            ReadingIndex current=new ReadingIndex(data);ReferenceReadingIndex reference=new ReferenceReadingIndex(data);
            for(String query:Arrays.asList("a","b","c","d","e","f","g","aa","abc","z","","abcdefghi")) {
                for(boolean filtered:new boolean[]{false,true}) {
                    java.util.function.BiPredicate<String,Candidate> accept=(key,c)->!filtered || c.text.hashCode()%3==0;
                    List<Candidate> expected=reference.complete(query,accept),actual=current.complete(query,accept);
                    Regression.equal(expected.size(),actual.size(),"prefix result count");
                    for(int i=0;i<expected.size();i++) {
                        Regression.equal(expected.get(i).text,actual.get(i).text,"prefix tie/duplicate ordering");
                        Regression.equal(Double.doubleToLongBits(expected.get(i).score),Double.doubleToLongBits(actual.get(i).score),"prefix score unchanged");
                    }
                }
            }
        }
        System.out.println("PASS prefix-index parity: 768 generated queries against frozen full-sort reference");
    }
}
