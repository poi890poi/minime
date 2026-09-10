package dev.minime.core;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.function.Consumer;
import static dev.minime.core.Regression.*;

final class PairedFormsRegression {
    static void run()throws Exception {
        // Invented mechanics fixture, deliberately separate from production selection/ranking.
        PairedForms pairs=PairedForms.read(new StringReader("poj\tabc-def\t甲乙\titaigi:1\n"));
        AddonDictionary addons=AddonDictionary.read(new StringReader("poj\tabc-def\tabc-def\titaigi:1\tfixture\npoj\totherkey\tabc-def\tbeginner:2\tfixture\n"),pairs);
        for(String raw:Arrays.asList("abcdef","abcd","ad","ab'd")) {
            List<Candidate> found=addons.lookup(raw,Collections.singleton("poj"));
            yes(!found.isEmpty(),"paired source survives shared partial matching "+raw);
            equal("甲乙",found.get(0).alternateText(),"pair retained in partial candidate");
            equal("甲乙",found.get(0).withScore(9).completing(8).alternateText(),"copy operations retain pair");
        }
        yes(addons.lookup("otherkey",Collections.singleton("poj")).get(0).pair==null,"unrelated source homophone is not paired");
        // An explicitly attested source pair may annotate an existing beginner reading.
        PairedForms taihoa=PairedForms.read(new StringReader("poj\tabc-def\t甲乙\ttaihoa:9\n"));
        AddonDictionary beginner=AddonDictionary.read(new StringReader("poj\tabc-def\tabc-def\ttaiwanese-basic:8\teveryday_vocabulary\n"),taihoa);
        for(String raw:Arrays.asList("abcdef","abcd","ad","ab'd")) {
            Candidate word=beginner.lookup(raw,Collections.singleton("poj")).get(0);
            equal("甲乙",word.alternateText(),"beginner source carries exact whole-reading Taihoa pair");
            equal("taihoa:9",word.pair.source,"alternate retains its actual Han source");
        }
        Editor basicOutput=new Editor();CompositionEngine basicEngine=engine(basicOutput,Learning.NONE,false);
        basicEngine.addons(beginner,Collections.singleton("poj"));basicEngine.switchMode(InputMode.TAIWANESE,false);type(basicEngine,"abcdef");
        Candidate basicChoice=basicEngine.candidates().stream().filter(v->v.pair!=null).findFirst().get();
        basicEngine.selectAlternative(basicChoice,basicEngine.compositionId());equal("甲乙",basicOutput.text,"beginner Han accepts through shared engine");
        for(boolean han:Arrays.asList(false,true))for(boolean hold:Arrays.asList(false,true)) {
            Editor e=new Editor();Memory memory=new Memory();CompositionEngine c=engine(e,memory,false);
            c.addons(addons,Collections.singleton("poj"));c.switchMode(InputMode.TAIWANESE,false);c.pairedTaiwanese(true,han);type(c,"abcdef");
            Candidate selected=c.candidates().stream().filter(v->v.pair!=null).findFirst().get();
            equal(han?"abc-def":"甲乙",selected.alternateText(),"hint is exact alternate");
            if(hold)c.selectAlternative(selected,c.compositionId());else c.selectCandidate(selected,c.compositionId());
            equal(han!=hold?"甲乙":"abc-def",e.text,"direct output follows tap / hold preference");
            equal("",c.raw(),"paired choice clears composition");equal(1,memory.count("FOCUS:poj","abcdef","abc-def"),"paired source learns canonical language identity");
        }
        for(boolean han:Arrays.asList(false,true)) {
            Editor output=new Editor();CompositionEngine choice=engine(output,Learning.NONE,false);
            choice.addons(addons,Collections.singleton("poj"));choice.switchMode(InputMode.TAIWANESE,false);choice.pairedTaiwanese(true,han);type(choice,"abcdef");
            Candidate preferred=choice.candidates().get(choice.preferred());choice.space();
            equal(preferred.text+(preferred.literal?" ":""),output.text,"Space inserts displayed primary default");
        }
        Editor collisionOutput=new Editor();Memory collisionMemory=new Memory();CompositionEngine collision=engine(collisionOutput,collisionMemory,false);
        collision.addons(addons,Collections.singleton("poj"));collision.switchMode(InputMode.TAIWANESE,false);collision.pairedTaiwanese(true,true);
        collision.decoder((d,r,b,ctx,done)->done.accept(Collections.singletonList(new Candidate("甲乙",false,1000))),()->{});
        type(collision,"abcdef");Candidate pairedCollision=collision.candidates().stream().filter(v->v.pair!=null).findFirst().get();
        collision.selectCandidate(pairedCollision,collision.compositionId());equal("甲乙",collisionOutput.text,"paired primary can share Han text with another candidate");
        equal(1,collisionMemory.count("FOCUS:poj","abcdef","abc-def"),"paired identity cannot learn unrelated Han homophone");
        Editor e=new Editor();CompositionEngine c=engine(e,Learning.NONE,false);
        c.addons(addons,Collections.singleton("poj"));c.switchMode(InputMode.TAIWANESE,false);type(c,"abcdef");
        Candidate stale=c.candidates().stream().filter(v->v.pair!=null).findFirst().get();long id=c.compositionId();
        c.pairedTaiwanese(false,false);equal("abcdef",c.raw(),"disable keeps spelling");
        yes(c.candidates().stream().noneMatch(v->v.pair!=null),"disabled alternatives not advertised");
        c.selectAlternative(stale,id);equal("",e.text,"disable invalidates held selection");
        c.pairedTaiwanese(true,false);stale=c.candidates().stream().filter(v->v.pair!=null).findFirst().get();id=c.compositionId();
        c.switchMode(InputMode.CHINESE,false);c.selectAlternative(stale,id);equal("",e.text,"mode change invalidates held pair");
        c.switchMode(InputMode.TAIWANESE,false);
        Candidate old=c.candidates().stream().filter(v->v.pair!=null).findFirst().get();long oldId=c.compositionId();
        type(c,"zz");c.selectAlternative(old,oldId);equal("",e.text,"unavailable old pair cannot commit after edit");
        c.start(false,true,true,true);type(c,"abcdef");yes(c.candidates().stream().noneMatch(v->v.pair!=null),"secure literal fields do not expose pairs");
        // Pending acceptance waits for the current callback and accepts exactly once.
        e=new Editor();c=engine(e,Learning.NONE,false);c.addons(addons,Collections.singleton("poj"));c.switchMode(InputMode.TAIWANESE,false);
        List<Consumer<List<Candidate>>> replies=new ArrayList<>();
        c.decoder((d,r,b,context,done)->replies.add(done),()->{});type(c,"abcdef");
        Candidate held=addons.lookup("abcdef",Collections.singleton("poj")).get(0);
        c.selectAlternative(held,c.compositionId());equal("",e.text,"hold waits while decoder pending");
        replies.get(replies.size()-1).accept(Collections.emptyList());equal("甲乙",e.text,"queued hold accepts source Han directly");
        System.out.println("PASS paired output, shared partial matching, source isolation, stale and queued acceptance");
        if(Boolean.getBoolean("minime.exhaustivePairs"))productionParity();
    }
    private static void productionParity()throws Exception {
        // Every current lookup key and proper prefix: metadata cannot alter output, score or matching flags.
        Path assets=Paths.get("app/src/main/assets");
        PairedForms production=PairedForms.read(Files.newBufferedReader(assets.resolve("paired-forms.tsv")));
        AddonDictionary plain=AddonDictionary.read(Files.newBufferedReader(assets.resolve("addons.tsv")));
        AddonDictionary paired=AddonDictionary.read(Files.newBufferedReader(assets.resolve("addons.tsv")),production);
        Set<String> inputs=new TreeSet<>();
        for(String line:Files.readAllLines(assets.resolve("addons.tsv"))) {
            String[] p=line.split("\t");if(p.length!=5 || !p[0].equals("poj"))continue;
            String key=p[1].replace("'","").replace("-","").replace(" ","");
            inputs.add(key);for(int n=2;n<key.length();n++)inputs.add(key.substring(0,n));
        }
        int annotated=0;
        for(String raw:inputs) {
            List<Candidate> a=plain.lookup(raw,Collections.singleton("poj")),b=paired.lookup(raw,Collections.singleton("poj"));
            equal(a.size(),b.size(),"metadata does not affect lookup count");
            for(int i=0;i<a.size();i++) {
                Candidate x=a.get(i),y=b.get(i);
                equal(x.text,y.text,"metadata does not affect candidate order");equal(x.score,y.score,"metadata does not affect score");
                equal(x.incomplete,y.incomplete,"metadata does not affect partial match");equal(x.consumed,y.consumed,"metadata does not affect acceptance boundary");
                if(y.pair!=null) {annotated++;equal(y.text,y.pair.phonetic,"annotation belongs to complete phonetic output");}
            }
        }
        yes(annotated>0,"production pairs reachable");
        System.out.println("PASS paired direct output, stale/queued gestures and exhaustive metadata parity: "+inputs.size()+" complete/prefix queries; "+annotated+" paired result occurrences (mechanics, not language accuracy)");
    }
}
