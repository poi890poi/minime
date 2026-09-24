package dev.minime.core;

import dev.minime.testing.CommitObservation;
import java.io.StringReader;
import java.util.*;
import static dev.minime.core.Regression.*;

/** Core acceptance is the independent oracle for the Android timing predicate. */
final class CommitObservationRegression {
    private static final class ObservedEditor implements CompositionEngine.Editor {
        String text="",typed="";int composingEnd=-1;CompositionEngine engine;
        boolean inCommit;String rawDuringCommit;
        public void composing(String s){text=s;composingEnd=s.isEmpty()?-1:s.length();}
        public void commit(String s){text=s;composingEnd=-1;rawDuringCommit=engine.raw();inCommit=seen();}
        public void delete(){} public void enter(){} public void finish(){composingEnd=-1;}
        boolean seen(){return CommitObservation.finished(composingEnd,text,typed,engine.raw());}
    }
    static void run()throws Exception {
        // Invented identity/changed-text pairs; no vocabulary or ranking claims.
        for(boolean same:new boolean[]{true,false})for(boolean deferred:new boolean[]{false,true}) {
            String raw="zavora",output=same?raw:"甲乙";
            AddonDictionary words=AddonDictionary.read(new StringReader("poj\t"+raw+"\t"+output+"\tfixture:1\tfixture\n"));
            ObservedEditor e=new ObservedEditor();CompositionEngine c=new CompositionEngine(e,Learning.NONE);e.engine=c;e.typed=raw;
            c.dictionary(dictionary);c.start(false,false,false,false);c.addons(words,Collections.singleton("poj"));c.switchMode(InputMode.TAIWANESE_ENGLISH,false);
            List<Runnable> replies=new ArrayList<>();
            if(deferred)c.decoder(new CompositionEngine.Decoder(){
                public void convert(PhoneticDictionary d,String r,boolean z,String ctx,java.util.function.Consumer<List<Candidate>> done){throw new AssertionError("query override required");}
                public void query(PhoneticDictionary d,String r,boolean z,String ctx,boolean phonetic,AddonDictionary a,Set<String> enabled,java.util.function.Consumer<List<Candidate>> done){replies.add(()->done.accept(a.lookup(r,enabled)));}
            },()->{});
            type(c,raw);yes(!e.seen(),"composing is not a Space commit");
            c.space();
            if(deferred){yes(!e.seen(),"queued Space is not accepted before its prediction");replies.get(replies.size()-1).run();}
            equal(output,e.text,"source candidate is committed exactly once");equal("",c.raw(),"whole-token commit releases owned spelling");
            equal(raw,e.rawDuringCommit,"editor callback precedes release of engine spelling");
            equal(!same,e.inCommit,"changed text is observable inside commit; same text waits for ownership release");
            yes(e.seen(),"completed Space must be observable even without character changes");
            if(same)yes(!(e.composingEnd<0 && !e.text.equals(raw)),"legacy observer demonstrably misses successful same-text commit");
        }
        yes(!CommitObservation.finished(-1,"abc","abc","abc"),"missing span alone cannot credit unprocessed Space");
        yes(!CommitObservation.finished(3,"other","abc",""),"remaining composition cannot count as committed");
        System.out.println("PASS Space observer: same/changed text, immediate/deferred acceptance, unfinished controls");
    }
}
