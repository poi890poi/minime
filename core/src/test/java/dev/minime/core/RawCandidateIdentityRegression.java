package dev.minime.core;

import java.io.StringReader;
import java.util.*;
import static dev.minime.core.Regression.*;

/** Synthetic identities test merging/acceptance, never production vocabulary rank. */
final class RawCandidateIdentityRegression {
    static void run() throws Exception {
        PairedForms pairs=PairedForms.read(new StringReader("poj\tzavora\t甲乙\titaigi:1\n"));
        StringBuilder rows=new StringBuilder();
        for(String pack:Arrays.asList("poj","japanese","taiwan","geography"))
            rows.append(pack+"\tzavora\tzavora\titaigi:1\tfixture\n");
        AddonDictionary words=AddonDictionary.read(new StringReader(rows.toString()),pairs);
        for(String pack:Arrays.asList("poj","japanese","taiwan","geography")) {
            InputMode mode=pack.equals("poj")?InputMode.TAIWANESE_ENGLISH:pack.equals("japanese")?InputMode.JAPANESE_ENGLISH:InputMode.CHINESE;
            Editor e=new Editor();Memory memory=new Memory();CompositionEngine c=engine(e,memory,false);
            c.addons(words,Collections.singleton(pack));c.switchMode(mode,false);type(c,"zavora");
            equal("zavora",c.candidates().get(0).text,"raw spelling stays available");
            yes(c.candidates().get(0).literal,"raw identity remains literal");
            Candidate lexical=lexical(c,pack);
            equal("zavora",lexical.text,"same-text dictionary candidate survives in every pack");
            c.selectCandidate(lexical,c.compositionId());equal("zavora",e.text,"snapshot selects lexical identity");
            if(!mode.pack.isEmpty())equal(1,memory.count("FOCUS:"+pack,"zavora","zavora"),"same text learns focused identity");
        }
        for(boolean han:new boolean[]{false,true})for(boolean paired:new boolean[]{false,true})
            for(boolean priv:new boolean[]{false,true})for(boolean learn:new boolean[]{false,true})
                for(String action:Arrays.asList("tap","space","raw","hold")) {
                    if(action.equals("hold")&&!paired)continue;
                    Editor e=new Editor();Memory memory=new Memory();CompositionEngine c=engine(e,memory,false);
                    c.start(false,false,priv,false);c.addons(words,Collections.singleton("poj"));c.switchMode(InputMode.TAIWANESE_ENGLISH,false);
                    c.pairedTaiwanese(paired,han);c.focusedLearning(learn);type(c,"zavora");
                    Candidate lexical=lexical(c,"poj"),raw=c.candidates().get(0);
                    equal(paired&&han?"甲乙":"zavora",lexical.text,"primary form retained across raw collision");
                    equal(paired,lexical.pair!=null,"pair switch keeps lexical identity");
                    yes(raw.literal&&raw.pair==null&&raw.pack.isEmpty(),"raw never inherits Han or language learning");
                    equal(lexical,c.candidates().get(c.preferred()),"focused match owns Space");
                    if(action.equals("tap"))c.selectCandidate(lexical,c.compositionId());
                    else if(action.equals("hold"))c.selectAlternative(lexical,c.compositionId());
                    else if(action.equals("raw"))c.selectCandidate(raw,c.compositionId());
                    else c.space();
                    String expected=action.equals("raw")?"zavora":action.equals("hold")?lexical.alternateText():lexical.text;
                    equal(expected,e.text,"raw-collision acceptance: "+action);
                    int vote=!priv&&learn&&(action.equals("tap")||action.equals("hold"))?1:0;
                    equal(vote,memory.count("FOCUS:poj","zavora","zavora"),"only explicit enabled lexical choices learn");
                    equal("",c.raw(),"acceptance clears the entire spelling");
                }
        // Same-text raw recovery remains independently learnable, even with a
        // paired lexical choice alongside it; selecting it must not train POJ.
        Memory memory=new Memory();CompositionEngine c=engine(new Editor(),memory,false);
        c.addons(words,Collections.singleton("poj"));c.switchMode(InputMode.TAIWANESE_ENGLISH,false);type(c,"zavora");
        c.selectCandidate(c.candidates().get(0),c.compositionId());
        c.start(false,false,false,false);c.switchMode(InputMode.TAIWANESE_ENGLISH,false);type(c,"zavora");
        equal(0,c.preferred(),"explicit raw recovery retains its separate preference");
        equal(0,memory.count("FOCUS:poj","zavora","zavora"),"raw recovery never trains the lexical twin");
        Learning rawCustom=new Learning() {
            public int count(String context,String raw,String value){return 0;}
            public void choose(String context,String raw,String value){}
            public List<Candidate> custom(String raw){return Collections.singletonList(new Candidate(raw,true,0));}
        };
        c=engine(new Editor(),rawCustom,false);c.addons(words,Collections.singleton("poj"));c.switchMode(InputMode.TAIWANESE_ENGLISH,false);type(c,"zavora");
        yes(lexical(c,"poj").pair!=null,"discarded raw custom duplicate cannot suppress later lexical identity");
        for(InputMode mode:Arrays.asList(InputMode.ENGLISH,InputMode.CHINESE)) {
            c=engine(new Editor(),Learning.NONE,false);c.addons(words,Collections.singleton("poj"));c.switchMode(mode,false);type(c,"hello");
            yes(c.candidates().stream().noneMatch(v->v.supplemental),"POJ cannot leak into English/Chinese mode");
            equal(1L,c.candidates().stream().filter(v->v.text.equals("hello")).count(),"English exact word is not duplicated");
        }
        c=engine(new Editor(),Learning.NONE,false);c.addons(words,Collections.singleton("poj"));c.switchMode(InputMode.TAIWANESE_ENGLISH,false);
        c.start(false,true,true,true);type(c,"zavora");
        yes(c.candidates().stream().noneMatch(v->v.supplemental),"secure literal fields do not reveal lexical twins");
        System.out.println("PASS raw/lexical identity, paired tap/hold/Space, raw recovery and language isolation");
    }
    private static Candidate lexical(CompositionEngine c,String pack) {
        List<Candidate> matches=new ArrayList<>();for(Candidate v:c.candidates())if(v.pack.equals(pack))matches.add(v);
        equal(1,matches.size(),"one lexical identity survives raw collision in "+pack);
        yes(!matches.get(0).literal,"lexical twin cannot acquire raw semantics");return matches.get(0);
    }
}
