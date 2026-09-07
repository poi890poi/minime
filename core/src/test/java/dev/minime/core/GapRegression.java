package dev.minime.core;

import java.util.*;
import static dev.minime.core.Regression.*;

/** Behavior contracts from paired observations; no evaluation labels enter runtime data. */
final class GapRegression {
    static void run() {
        for(String raw:Arrays.asList("bkq","bukq","xiex")) {
            Editor e=new Editor();CompositionEngine c=engine(e,Learning.NONE,false);type(c,raw);
            String phrase=raw.startsWith("b")?"不客氣":"謝謝";
            equal(phrase,c.candidates().get(c.preferred()).text,"abbreviated Space default "+raw);
            c.space();equal(phrase,e.text,"abbreviated Space acceptance "+raw);
        }
        for(String raw:Arrays.asList("meeting","pronun","adb","git","ssh","npm","camelCase","name@example.com","v2")) {
            Editor e=new Editor();CompositionEngine c=engine(e,Learning.NONE,false);type(c,raw);c.space();
            equal(raw+" ",e.text,"mixed literal path preserved "+raw);
        }
        Editor e=new Editor();CompositionEngine c=engine(e,Learning.NONE,false);type(c,"bkq");c.select(0);c.space();
        equal("bkq ",e.text,"abbreviation raw recovery");
    }
}
