package dev.minime.core;

import java.util.*;
import static dev.minime.core.Regression.*;

/** Behavior contracts from paired observations; no evaluation labels enter runtime data. */
final class GapRegression {
    static void run() {
        ShiftState shift=new ShiftState();shift.automatic(true);yes(shift.upper(),"editor automatic capitals");
        shift.tap(1000,300);yes(!shift.upper(),"manual Shift suppresses automatic capital");
        shift.tap(1100,300);yes(shift.locked(),"double Shift locks from auto capital");
        shift.reset();shift.automatic(true);shift.tap(2000,300);shift.consume();
        yes(shift.upper(),"manual suppression ends after character");shift.automatic(false);yes(!shift.upper(),"editor ends automatic capitals");
        equal("could",dictionary.englishCompletions("co").get(0).text,"prefix ranking includes later alphabetic matches");
        equal("into",dictionary.englishCompletions("in").get(0).text,"in prefix frequency ranking");
        equal("released",dictionary.englishCompletions("re").get(0).text,"re prefix frequency ranking");
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
        for(String suffix:Arrays.asList("test ",",","."," ","\n")) {
            e=new Editor();c=engine(e,Learning.NONE,false);c.start(false,false,false,false,true);
            type(c,"pronun");c.select(find(c,"pronunciation"));
            if(suffix.equals(",") || suffix.equals(".")) c.literal(suffix); else type(c,suffix);
            equal("pronunciation"+(suffix.equals("test ")?" ":"")+suffix,e.text,"deferred completion boundary "+suffix);
        }
    }
}
