package dev.minime.core;

import java.io.StringReader;
import java.util.*;
import static dev.minime.core.Regression.*;

/** Synthetic provider collisions exercise policy, never production word exceptions. */
final class ChineseAlternativeRegression {
    static void run() throws Exception {
        PhoneticDictionary tiny=PhoneticDictionary.load(new StringReader(""),
            new StringReader("alpha\t100\nalphabet\t90\nalphanumeric\t80\nbeta\t100\n"),
            new StringReader(""),new StringReader(""));
        Candidate han=new Candidate("甲乙",false,100),second=new Candidate("丙丁",false,90);
        for(boolean priv:new boolean[]{false,true})for(String raw:Arrays.asList("al","alpha")) {
            Editor e=new Editor();CompositionEngine c=new CompositionEngine(e,Learning.NONE);
            c.dictionary(tiny);c.start(false,false,priv,false);
            c.decoder((d,r,z,context,done)->done.accept(Arrays.asList(han,second)),()->{});
            type(c,raw);
            equal("甲乙",c.candidates().get(1).text,"Chinese alternative precedes English extensions in Chinese mode");
            equal(0,c.preferred(),"Language presentation never silently replaces raw Space default");
            List<String> english=new ArrayList<>();for(Candidate v:c.candidates())if(v.literal && !v.text.equals(raw))english.add(v.text);
            List<String> expected=new ArrayList<>();for(Candidate v:tiny.englishCompletions(raw))expected.add(v.text);
            equal(expected,english,"All English extensions retain their source order");
            equal("丙丁",c.candidates().get(4).text,"Other alternatives retain the established interleave");
            c.space();equal(raw+" ",e.text,"Space output preserved");
            type(c,raw);yes(c.candidates().get(1).literal,"Accepted Latin context prefers English extensions");
            c.enter();type(c,raw);equal("甲乙",c.candidates().get(1).text,"New line releases preceding English intent");
            c.switchMode(InputMode.ENGLISH,false);
            yes(c.candidates().stream().allMatch(v->v.literal),"English mode stays isolated");
            c.switchMode(InputMode.CHINESE,false);equal("甲乙",c.candidates().get(1).text,"Mode switch restores Chinese priority without consuming input");
            c.select(0);yes(e.text.endsWith(raw),"Explicit raw selection survives priority change");
        }
        for(boolean literal:new boolean[]{false,true}) {
            Editor e=new Editor();CompositionEngine c=new CompositionEngine(e,Learning.NONE);
            c.dictionary(tiny);c.start(false,literal,false,false);
            c.decoder((d,r,z,context,done)->done.accept(Collections.singletonList(new Candidate("甲",false,100,1))),()->{});
            type(c,"al");yes(c.candidates().get(c.preferred()).literal,"Prefix-only recovery cannot become a whole-input default");
            if(literal)equal(1,c.candidates().size(),"Literal editor excludes suggestion policy");
            else yes(c.candidates().get(1).literal,"Partial spans do not masquerade as whole-input matches");
        }
        for(boolean zhuyin:new boolean[]{false,true})for(String raw:Arrays.asList("Al","AL","Alpha","ALPHA")) {
            CompositionEngine c=new CompositionEngine(new Editor(),Learning.NONE);c.dictionary(tiny);c.start(zhuyin,false,false,false);
            c.decoder((d,r,z,context,done)->done.accept(Arrays.asList(han,second)),()->{});type(c,raw);
            yes(c.candidates().get(1).literal,"Explicit Latin casing and Zhuyin-board Latin retain English alternatives");
        }
        CompositionEngine zhuyin=new CompositionEngine(new Editor(),Learning.NONE);zhuyin.dictionary(tiny);zhuyin.start(true,false,false,false);
        zhuyin.decoder((d,r,z,context,done)->done.accept(Arrays.asList(han,second)),()->{});type(zhuyin,"al");
        yes(zhuyin.candidates().get(1).literal,"Zhuyin-board lowercase Latin is not Pinyin intent");
        System.out.println("PASS Chinese alternative priority, English context, raw acceptance, privacy and mode boundaries");
    }
}
