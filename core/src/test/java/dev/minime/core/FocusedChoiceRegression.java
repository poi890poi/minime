package dev.minime.core;

import java.io.StringReader;
import java.util.*;
import static dev.minime.core.Regression.*;

final class FocusedChoiceRegression {
    static void run() throws Exception {
        StringBuilder rows=new StringBuilder();
        for(String pack:Arrays.asList("poj","japanese"))for(int i=0;i<3;i++)
            rows.append(pack+"\t"+(i==2?"zavorata":"zavora")+"\t"+pack+i+"\tfixture\tfixture\n");
        AddonDictionary addons=AddonDictionary.read(new StringReader(rows.toString()));
        Set<String> all=new HashSet<>(Arrays.asList("poj","japanese"));
        for(InputMode mode:Arrays.asList(InputMode.TAIWANESE_ENGLISH,InputMode.JAPANESE_ENGLISH)) {
            Memory memory=new Memory();Editor editor=new Editor();CompositionEngine c=engine(editor,memory,false);
            c.addons(addons,all);c.switchMode(mode,false);type(c,"zavora");
            String original=c.candidates().get(1).text,wanted=mode.pack+"1";
            c.select(find(c,wanted));
            equal(1,memory.count("FOCUS:"+mode.pack,"zavora",wanted),"explicit focus choice persists in language namespace");
            c.start(false,false,false,false);c.switchMode(mode,false);type(c,"zavora");
            equal(wanted,c.candidates().get(1).text,"personal evidence ranks focus candidates");
            equal(1,c.preferred(),"display and Space share learned winner");c.space();
            equal(wanted+wanted,editor.text,"Space commits learned winner");
            equal(1,memory.count("FOCUS:"+mode.pack,"zavora",wanted),"automatic acceptance does not reinforce itself");
            c.start(false,false,false,false);c.switchMode(mode,false);c.focusedLearning(false);type(c,"zavora");
            equal(original,c.candidates().get(1).text,"off restores static source order");c.select(find(c,wanted));
            equal(1,memory.count("FOCUS:"+mode.pack,"zavora",wanted),"off prevents focused writes");
            c.focusedLearning(true);c.start(false,false,false,false);c.switchMode(mode,false);type(c,"zavora");
            c.select(find(c,mode.pack+"2"));c.start(false,false,false,false);c.switchMode(mode,false);type(c,"zavora");
            yes(!c.candidates().get(1).incomplete,"learned incomplete choice cannot displace complete matches");
            InputMode other=mode.taiwanese()?InputMode.JAPANESE_ENGLISH:InputMode.TAIWANESE_ENGLISH;
            c.switchMode(other,false);equal(other.pack+"0",c.candidates().get(1).text,"choice evidence does not cross languages");
        }
        PairedForms pairs=PairedForms.read(new StringReader("poj\tsecond\t甲乙\titaigi:1\n"));
        addons=AddonDictionary.read(new StringReader("poj\tzavora\tfirst\titaigi:2\tfixture\npoj\tzavora\tsecond\titaigi:1\tfixture\n"),pairs);
        for(boolean han:new boolean[]{false,true})for(boolean alternate:new boolean[]{false,true}) {
            Memory memory=new Memory();CompositionEngine c=engine(new Editor(),memory,false);
            c.addons(addons,all);c.switchMode(InputMode.TAIWANESE_ENGLISH,false);c.pairedTaiwanese(true,han);type(c,"zavora");
            Candidate paired=c.candidates().stream().filter(v->v.pair!=null).findFirst().get();
            if(alternate)c.selectAlternative(paired,c.compositionId());else c.selectCandidate(paired,c.compositionId());
            equal(1,memory.count("FOCUS:poj","zavora","second"),"tap/hold Han/POJ share canonical choice identity");
            c.start(false,false,false,false);c.switchMode(InputMode.TAIWANESE_ENGLISH,false);type(c,"zavora");
            equal(han?"甲乙":"second",c.candidates().get(1).text,"paired display retains learned rank");
        }
        Learning noChinesePhrases=new Learning() {
            public int count(String c,String r,String v){return 0;}public void choose(String c,String r,String v){}
            public List<Candidate> phrases(String r){throw new AssertionError("Chinese phrase history leaked into focused/English mode");}
        };
        for(InputMode mode:Arrays.asList(InputMode.TAIWANESE_ENGLISH,InputMode.JAPANESE_ENGLISH,InputMode.ENGLISH)) {
            CompositionEngine c=engine(new Editor(),noChinesePhrases,false);c.switchMode(mode,false);c.phraseLearning(true);type(c,"zavora");
        }
    }
}
