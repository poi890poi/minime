package dev.minime.core;

import java.nio.file.*;
import java.util.*;
import static dev.minime.core.Regression.*;

/** Source-derived kana controls plus synthetic provider-boundary contracts, not accuracy. */
final class PrefixAcceptanceRegression {
    static void run() throws Exception {
        JapaneseBasics basics=JapaneseBasics.read(Files.newBufferedReader(Paths.get("app/src/main/assets/japanese-basic.tsv")));
        AddonDictionary addon=AddonDictionary.withJapaneseBasics(AddonDictionary.EMPTY,basics);
        SortedSet<String> words=new TreeSet<>();
        for(String line:Files.readAllLines(Paths.get("docs/japanese-continuity/kana-oracle.tsv"))) {
            String[] p=line.split("\t",-1);
            if(!p[0].equals("input") && p[0].matches("[a-z]{3,16}") && Integer.parseInt(p[3])>0)words.add(p[0]);
        }
        int controls=0;
        for(String word:words)for(String raw:Arrays.asList(word,word.toUpperCase(Locale.ROOT),
                Character.toUpperCase(word.charAt(0))+word.substring(1),"ka-"+word))for(boolean priv:new boolean[]{false,true}) {
            Editor editor=new Editor();Memory memory=new Memory();CompositionEngine c=engine(editor,memory,false);
            c.start(false,false,priv,false);c.addons(addon,Collections.singleton("japanese"));c.switchMode(InputMode.JAPANESE_ENGLISH,false);
            type(c,raw);
            Candidate prefix=c.candidates().stream().filter(v->v.pack.equals("japanese") && v.consumed>0 && v.consumed<raw.length()).findFirst().orElseThrow(AssertionError::new);
            yes(c.candidates().get(c.preferred()).consumed==0,"Default covers all raw input: "+raw);
            String rest=raw.substring(prefix.consumed);long id=c.compositionId();
            c.selectCandidate(prefix,id);
            equal(prefix.text,editor.text,"Tap commits only advertised prefix");equal(rest,c.raw(),"Raw suffix survives casing/separators");
            equal(rest,editor.composing,"Editor owns remaining suffix");
            equal(priv?0:1,memory.count("FOCUS:japanese",raw.substring(0,prefix.consumed),prefix.text),"Partial focused choice learning");
            equal(priv?0:1,memory.votes.size(),"No cross-mode or private learning");
            c.selectCandidate(prefix,id);equal(rest,c.raw(),"Accepted prefix invalidates old gesture");
            c.commitRaw(false);equal(prefix.text+rest,editor.text,"Literal recovery retains every suffix character");controls++;
        }
        // Every producer shares range/code-point validation; literals cannot consume prefixes.
        for(String raw:Arrays.asList("NI'HAO","ka-k","ab'k","\ud840\udc00x"))for(boolean supplement:new boolean[]{false,true}) {
            int end=raw.codePointAt(0)>0xffff?2:1;
            Editor editor=new Editor();Memory memory=new Memory();CompositionEngine c=engine(editor,memory,false);
            c.decoder((d,r,z,ctx,done)->done.accept(Arrays.asList(
                candidate("prefix",end,supplement),candidate("negative",-1,supplement),
                candidate("too-long",100,supplement),new Candidate("literal-prefix",true,100,1),
                candidate("split-surrogate",raw.codePointAt(0)>0xffff?1:100,supplement))),()->{});
            type(c,raw);
            yes(c.candidates().stream().noneMatch(v->v.text.equals("negative") || v.text.equals("too-long") || v.text.equals("literal-prefix") || v.text.equals("split-surrogate")),"Malformed provider spans never displayed");
            Candidate prefix=c.candidates().stream().filter(v->v.text.equals("prefix")).findFirst().orElseThrow(AssertionError::new);
            c.selectCandidate(prefix,c.compositionId());equal(raw.substring(end),c.raw(),"Provider-independent UTF-16 boundary");
        }
        Editor editor=new Editor();Memory memory=new Memory();CompositionEngine c=engine(editor,memory,false);
        c.addons(addon,Collections.singleton("japanese"));c.switchMode(InputMode.JAPANESE_ENGLISH,false);c.focusedLearning(false);
        type(c,words.first());Candidate prefix=c.candidates().stream().filter(v->v.consumed>0).findFirst().orElseThrow(AssertionError::new);
        c.selectCandidate(prefix,c.compositionId());equal(0,memory.votes.size(),"Disabled focused learning covers partial choices");
        System.out.println("PASS prefix acceptance: "+controls+" source-derived casing/privacy controls plus malformed provider spans");
    }
    private static Candidate candidate(String text,int consumed,boolean supplemental) {
        return supplemental?Candidate.supplement(text,100).inPack("taiwan").consuming(consumed):new Candidate(text,false,100,consumed);
    }
}
