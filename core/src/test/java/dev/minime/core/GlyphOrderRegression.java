package dev.minime.core;

import java.io.*;
import java.util.*;
import static dev.minime.core.Regression.*;

final class GlyphOrderRegression {
    static void run()throws Exception {
        for(boolean englishOverlap:new boolean[]{false,true})for(String pack:Arrays.asList("taiwan","geography","japanese","poj")) {
            // Synthetic homophones expose duplicate promotion, independently
            // of real character identities and production frequency values.
            PhoneticDictionary d=PhoneticDictionary.load(new StringReader("can\tㄘㄢ\t甲\t100000\tㄘㄢ\ncan\tㄘㄢ\t乙\t1000\tㄘㄢ\ncan\tㄘㄢ\t丙\t1\tㄘㄢ\n"),new StringReader(englishOverlap?"can\t100\ncandy\t90\n":""),new StringReader("can\tㄘㄢ\n"));
            Editor editor=new Editor();CompositionEngine engine=new CompositionEngine(editor,Learning.NONE);engine.dictionary(d);engine.start(false,false,false,false);type(engine,"can");
            List<String> before=new ArrayList<>();for(Candidate c:engine.candidates())before.add(c.text);
            engine.addons(AddonDictionary.read(new StringReader(pack+"\tcan\t丙\tfixture\ttest\n")),Collections.singleton(pack));
            equal(before,engine.candidates().stream().map(c->c.text).collect(java.util.stream.Collectors.toList()),"duplicate source cannot promote a low-ranked homophone: "+pack+" English="+englishOverlap);
            yes(find(engine,"甲")<find(engine,"乙") && find(engine,"乙")<find(engine,"丙"),"entire base homophone order survives");
            engine=new CompositionEngine(new Editor(),Learning.NONE);engine.dictionary(d);engine.start(false,false,false,false);
            engine.decoder((model,raw,b,context,done)->done.accept(Arrays.asList(new Candidate("甲",false,100),new Candidate("乙",false,99),new Candidate("丁",false,98))),()->{});
            type(engine,"can");engine.addons(AddonDictionary.read(new StringReader(pack+"\tcan\t丁\tfixture\ttest\n")),Collections.singleton(pack));
            yes(find(engine,"乙")<find(engine,"丁"),"native single-glyph duplicate retains rank even outside the fallback dictionary");
        }
    }
}
