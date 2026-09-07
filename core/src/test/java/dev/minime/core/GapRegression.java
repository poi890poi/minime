package dev.minime.core;

import java.util.*;
import static dev.minime.core.Regression.*;

/** Behavior contracts from paired observations; no evaluation labels enter runtime data. */
final class GapRegression {
    static void run() {
        for(String word:Arrays.asList("hello","keyboard","world","meeting","thanks","please","tomorrow","people","computer")) {
            float[] path=new float[word.length()*2];for(int i=0;i<word.length();i++){float[] p=EnglishTrace.point(word.charAt(i));path[i*2]=p[0]+.03f;path[i*2+1]=p[1]-.02f;}
            equal(word,dictionary.englishTrace(path).get(0).text,"geometric trace "+word);
        }
        Editor traceEditor=new Editor();CompositionEngine traceEngine=engine(traceEditor,Learning.NONE,false);traceEngine.start(false,false,false,false,true);
        float[] hello=new float[10];for(int i=0;i<5;i++) {float[] p=EnglishTrace.point("hello".charAt(i));hello[i*2]=p[0];hello[i*2+1]=p[1];}
        traceEngine.trace(hello,1);traceEngine.space();equal("Hello ",traceEditor.text,"trace keeps sentence capitalization and Space");
        traceEngine.trace(hello,0);traceEngine.select(0);type(traceEngine,"world ");equal("Hello hello world ",traceEditor.text,"trace choice has completion boundary");
        traceEngine.start(false,true,false,false,true);traceEngine.trace(hello,0);equal("",traceEngine.raw(),"restricted field disables trace");
        Editor delayed=new Editor();CompositionEngine async=engine(delayed,Learning.NONE,false);
        List<Runnable> replies=new ArrayList<>();
        async.decoder((d,r,z,ctx,result)->replies.add(()->result.accept(d.convert(r,z,ctx))),()->{});
        type(async,"nihao");equal("nihao",delayed.composing,"async updates composing text immediately");
        replies.get(0).run();equal(1,async.candidates().size(),"stale prediction ignored");
        async.space();type(async,"bkq ");equal("",delayed.text,"commit waits without choosing unfinished query");
        replies.get(4).run();equal("你好",delayed.text,"matching query commits first word");
        replies.get(replies.size()-1).run();equal("你好不客氣",delayed.text,"queued input preserves word order");
        type(async,"nihao");async.space();async.start(false,false,false,false,true);
        replies.get(replies.size()-1).run();equal("",async.raw(),"field switch discards deferred input");equal("你好不客氣",delayed.text,"old field cannot commit after switch");
        for(boolean enabled:Arrays.asList(false,true)) {
            Editor e=new Editor();CompositionEngine c=engine(e,Learning.NONE,false);c.start(false,false,false,false,true);c.englishOptions(enabled,true);
            type(c,"teh");yes(find(c,"the")>0,"transposition suggestion");c.space(1000);
            equal(enabled?"the ":"teh ",e.text,"optional correction");
            if(enabled) {c.backspace();equal("",e.text,"correction undo deletes only owned output");equal("teh",c.raw(),"correction undo restores spelling");c.select(0);equal("teh",e.text,"undo exact recovery");}
        }
        Editor assistance=new Editor();CompositionEngine assist=engine(assistance,Learning.NONE,false);assist.start(false,false,false,false,true);assist.englishOptions(true,true);
        type(assist,"dont");yes(find(assist,"don't")>0,"contraction alternative");assist.space(1000);equal("don't ",assistance.text,"contraction correction");
        assist.space(1400);equal("don't. ",assistance.text,"double Space period");assist.space(1500);equal("don't.  ",assistance.text,"third Space is ordinary");
        assistance=new Editor();assist=engine(assistance,Learning.NONE,false);assist.start(false,true,false,false,true);assist.englishOptions(true,true);
        type(assist,"teh");assist.space(1000);assist.space(1100);equal("teh  ",assistance.text,"restricted field assistance bypass");
        assistance=new Editor();assist=engine(assistance,Learning.NONE,false);assist.start(false,false,false,false,true);assist.englishOptions(true,true);
        type(assist,"hello");assist.space(1000);assist.space(2100);equal("hello  ",assistance.text,"double Space timeout");
        yes(dictionary.englishCorrections("cant").isEmpty(),"known word is not corrected to contraction");
        yes(dictionary.englishCorrections("fooBar").isEmpty(),"mixed case is literal");
        assistance=new Editor();assist=engine(assistance,Learning.NONE,false);assist.start(false,false,false,false,true);assist.englishOptions(true,true);
        type(assist,"teh");assist.literal(".");equal("teh.",assistance.text,"correction is limited to configured Space acceptance");
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
        e=new Editor();c=engine(e,Learning.NONE,false);c.start(false,false,false,false,true);type(c,"how are ");
        yes(!c.candidates().isEmpty(),"English context offers next words");String next=c.candidates().get(0).text;
        c.select(0);type(c,"today ");equal("how are "+next+" today ",e.text,"next-word choice carries deferred boundary");
        e=new Editor();c=engine(e,Learning.NONE,false);c.start(false,true,false,false,true);type(c,"how are ");
        yes(c.candidates().isEmpty(),"restricted field has no next-word suggestions");
        for(String suffix:Arrays.asList("test ",",","."," ","\n")) {
            e=new Editor();c=engine(e,Learning.NONE,false);c.start(false,false,false,false,true);
            type(c,"pronun");c.select(find(c,"pronunciation"));
            if(suffix.equals(",") || suffix.equals(".")) c.literal(suffix); else type(c,suffix);
            equal("pronunciation"+(suffix.equals("test ")?" ":"")+suffix,e.text,"deferred completion boundary "+suffix);
        }
    }
}
