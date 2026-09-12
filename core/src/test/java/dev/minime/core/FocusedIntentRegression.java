package dev.minime.core;

import java.util.*;

final class FocusedIntentRegression {
    static void run(PhoneticDictionary dictionary) {
        IntentClassifier classifier=new IntentClassifier();List<String> probes=new ArrayList<>();
        for(char a='a';a<='z';a++)for(char b='a';b<='z';b++) {
            String raw=""+a+b;
            probes.add(raw);probes.add(raw.toUpperCase(Locale.ROOT));probes.add(a+"'"+b);
            probes.add(raw+"ㄅ");probes.add(raw+"\n");probes.add(raw+"1");
        }
        int ambiguous=0;
        for(String raw:probes)for(boolean context:new boolean[]{false,true}) {
            IntentClassifier.Intent original=classifier.classify(raw,false,false,dictionary,context);
            if(original==IntentClassifier.Intent.AMBIGUOUS)ambiguous++;
            IntentClassifier.Intent expected=original==IntentClassifier.Intent.CHINESE_PHONETIC && raw.codePoints().noneMatch(IntentClassifier::isZhuyin)
                ?IntentClassifier.Intent.LATIN_LITERAL:original;
            Regression.equal(expected,classifier.classify(raw,false,false,dictionary,context,false),"excluded Chinese intent matches prior core coercion");
            Regression.equal(original,classifier.classify(raw,false,false,dictionary,context,true),"enabled Chinese intent unchanged");
        }
        Regression.yes(ambiguous>0,"English/Pinyin ambiguity exercised");
    }
}
