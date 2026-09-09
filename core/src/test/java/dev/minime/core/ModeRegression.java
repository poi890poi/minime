package dev.minime.core;

import java.io.*;
import java.util.*;
import java.util.function.Consumer;
import static dev.minime.core.Regression.*;

final class ModeRegression {
    private static final Set<String> ALL=new HashSet<>(Arrays.asList("taiwan","geography","poj","japanese"));
    static void run()throws Exception {
        AddonDictionary addon=AddonDictionary.read(new StringReader("taiwan\tfixture\t文化\ttest\tfixture\ngeography\tfixture\t山岳\ttest\tfixture\npoj\tfixture\ttâi-gí\ttest\tfixture\njapanese\tfixture\tかな\ttest\tfixture\n"));
        for(InputMode mode:InputMode.values()) {
            Editor editor=new Editor();CompositionEngine engine=engine(editor,Learning.NONE,false);
            engine.addons(addon,ALL);engine.switchMode(mode,false);type(engine,"fixture");
            Set<String> texts=new HashSet<>();for(Candidate c:engine.candidates())texts.add(c.text);
            equal(mode.chineseEnabled(),texts.contains("文化"),"Chinese cultural pack belongs to mixed modes");
            equal(mode.chineseEnabled(),texts.contains("山岳"),"geography belongs to mixed modes");
            equal(mode.taiwanese(),texts.contains("tâi-gí"),"POJ isolated by mode");
            equal(mode.japanese(),texts.contains("かな"),"Japanese isolated by mode");
            Candidate selected=engine.candidates().get(engine.preferred());
            engine.space();equal(selected.text+(selected.literal?" ":""),editor.text,"Space matches mode highlight");
        }
        // Every pair controls all query paths, including empty-buffer predictions.
        for(InputMode mode:InputMode.values()) {
            CompositionEngine scoped=engine(new Editor(),Learning.NONE,false);
            scoped.addons(addon,ALL);scoped.switchMode(mode,false);
            if(!mode.chineseEnabled() && !mode.english())equal(0,scoped.candidates().size(),"no Chinese idle predictions in English-secondary pair");
            List<Boolean> phoneticCalls=new ArrayList<>();
            scoped.decoder(new CompositionEngine.Decoder() {
                public void convert(PhoneticDictionary d,String r,boolean z,String x,Consumer<List<Candidate>> done){throw new AssertionError();}
                public void query(PhoneticDictionary d,String r,boolean z,String x,boolean phonetic,AddonDictionary a,Set<String> enabled,Consumer<List<Candidate>> done) {
                    phoneticCalls.add(phonetic);equal(mode.packs(ALL),enabled,"exact pair scope reaches worker");done.accept(a.lookup(r,enabled));
                }
            },()->{});
            type(scoped,"meet");
            equal(mode.englishEnabled(),scoped.candidates().stream().anyMatch(v->v.literal && !v.text.equals("meet")),"English suggestions follow pair scope");
            for(boolean call:phoneticCalls)equal(mode.chineseEnabled(),call,"Chinese decoder is skipped when absent");
        }
        Editor editor=new Editor();Memory memory=new Memory();CompositionEngine engine=engine(editor,memory,false);
        engine.addons(addon,ALL);List<Runnable> replies=new ArrayList<>();List<Set<String>> queries=new ArrayList<>();
        engine.decoder(new CompositionEngine.Decoder() {
            public void convert(PhoneticDictionary d,String raw,boolean b,String c,Consumer<List<Candidate>> done){throw new AssertionError("query boundary required");}
            public void query(PhoneticDictionary d,String raw,boolean b,String c,boolean phonetic,AddonDictionary a,Set<String> enabled,Consumer<List<Candidate>> done) {
                queries.add(new HashSet<>(enabled));replies.add(()->done.accept(a.lookup(raw,enabled)));
            }
        },()->{});
        type(engine,"fixture");Runnable stale=replies.get(replies.size()-1);long oldComposition=engine.compositionId();
        engine.switchMode(InputMode.JAPANESE,false);equal("fixture",engine.raw(),"mode switch retains exact spelling");
        equal("",editor.text,"switch does not commit");yes(engine.compositionId()!=oldComposition,"old candidate touch invalidated");
        equal(InputMode.JAPANESE.packs(ALL),queries.get(queries.size()-1),"worker receives only active dictionaries");
        stale.run();yes(engine.predictionPending(),"obsolete mode callback cannot settle new query");
        replies.get(replies.size()-1).run();yes(engine.candidates().stream().anyMatch(c->c.text.equals("かな")),"new mode result delivered");
        Candidate old=engine.candidates().get(1);long id=engine.compositionId();engine.switchMode(InputMode.ENGLISH,false);
        equal("fixture",engine.raw(),"English switch retains buffer");yes(!engine.predictionPending(),"English mode avoids phonetic/add-on worker");
        engine.selectCandidate(old,id);equal("",editor.text,"stale selection cannot cross mode");
        engine.space();equal("fixture ",editor.text,"new English acceptance uses preserved spelling");
        // Choice votes have separate mixed-mode namespaces; ordinary legacy Chinese/EN votes remain compatible.
        List<String> contexts=new ArrayList<>();Learning recorder=new Learning() {
            public int count(String c,String r,String v){return 0;}
            public void choose(String c,String r,String v){contexts.add(c);}
        };
        for(InputMode mode:Arrays.asList(InputMode.CHINESE,InputMode.TAIWANESE,InputMode.JAPANESE)) {
            CompositionEngine c=engine(new Editor(),recorder,false);c.switchMode(mode,false);
            c.decoder((d,r,b,context,done)->done.accept(Arrays.asList(new Candidate("甲乙",false,100))),()->{});
            type(c,"abcdef");c.select(1);
        }
        equal(3,new HashSet<>(contexts).size(),"learned choices do not cross mixed modes");
        // Explicit Space already queued before the mode change must not be discarded.
        editor=new Editor();engine=engine(editor,Learning.NONE,false);List<Consumer<List<Candidate>>> pending=new ArrayList<>();
        engine.decoder((d,r,b,c,done)->pending.add(done),()->{});type(engine,"abc");engine.space();
        engine.switchMode(InputMode.ENGLISH,false);pending.get(pending.size()-1).accept(Collections.singletonList(new Candidate("甲乙",false,100)));
        equal("甲乙",editor.text,"earlier acceptance retains event order");equal(InputMode.ENGLISH,engine.inputMode(),"queued mode switch follows acceptance");
        System.out.println("PASS explicit mode isolation, composition, callback, learning and acceptance boundaries");
    }
}
