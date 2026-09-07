package dev.minime.core;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.function.Consumer;
import static dev.minime.core.Regression.*;

/** Composition ownership under result timing; corpus labels are injected test results, not ranking evidence. */
final class PinyinContinuityRegression {
    static void run() throws Exception {
        int samples=0;
        for(String line:Files.readAllLines(Paths.get("docs/conversation-ranking/corpus/inputs.tsv"),StandardCharsets.UTF_8)) {
            String[] fields=line.split("\t",-1);
            if(!fields[0].startsWith("zh-") || Math.floorMod((fields[0]+fields[1]).hashCode(),19)!=0)continue;
            String raw=fields[2],label=fields[3];samples++;
            for(boolean delayed:new boolean[]{false,true}) {
                Editor editor=new Editor();CompositionEngine engine=engine(editor,Learning.NONE,false);
                List<Consumer<List<Candidate>>> pending=new ArrayList<>();
                List<Candidate> answer=Collections.singletonList(new Candidate(label,false,100));
                engine.decoder((dict,input,zh,context,done)->{if(delayed)pending.add(done);else done.accept(answer);},()->{});
                for(int i=0;i<raw.length();i++) {
                    engine.type(raw.charAt(i));
                    equal("",editor.text,"Letter typing never accepts a result");
                    equal(raw.substring(0,i+1),editor.composing,"Every typed letter remains composing");
                }
                // Latest result first, followed by stale results, cannot commit or replace raw spelling.
                Collections.reverse(pending);for(Consumer<List<Candidate>> done:pending)done.accept(answer);
                equal("",editor.text,"Result delivery never commits a candidate");
                equal(raw,engine.raw(),"Result delivery retains complete phonetics");
                engine.space();yes(!editor.text.isEmpty(),"An explicit Space can accept the completed input");
                equal("",engine.raw(),"Explicit acceptance ends the composition");
            }
        }
        yes(samples>300,"Diverse frozen Pinyin samples exercised");
        System.out.println("Pinyin continuity: "+samples+" frozen full/initial/mixed inputs, immediate and out-of-order callbacks");
    }
}
