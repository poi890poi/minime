package dev.minime.core;

import java.io.*;
import java.util.*;
import static dev.minime.core.Regression.*;

/** Synthetic cross-language collisions test policy, never production vocabulary. */
final class ModePriorityRegression {
    static void run() throws Exception {
        for(InputMode mode:Arrays.asList(InputMode.TAIWANESE,InputMode.JAPANESE)) {
            String first=mode==InputMode.TAIWANESE?"á-bé":"あべ",second=mode==InputMode.TAIWANESE?"á-bé-cé":"あべせ";
            AddonDictionary addon=AddonDictionary.read(new StringReader(
                mode.pack+"\tni'hao\t"+first+"\tfixture\tfixture\n"+
                mode.pack+"\tni'hao'ma\t"+second+"\tfixture\tfixture\n"+
                "geography\tni'hao\t地理詞\tfixture\tfixture\n"));
            Set<String> enabled=new HashSet<>(Arrays.asList(mode.pack,"geography"));
            for(String raw:Arrays.asList("nihao","nih","nh")) {
                Editor e=new Editor();CompositionEngine c=engine(e,Learning.NONE,false);
                c.switchMode(mode,false);c.addons(addon,enabled);type(c,raw);
                equal(first,c.candidates().get(1).text,"selected language first for full/prefix/initial input");
                if(!raw.equals("nh"))yes(find(c,second)<find(c,"地理詞"),"selected partial language precedes fallback pack");
                // Two initials represent two syllables; they do not imply a third.
                yes(find(c,first)<find(c,"地理詞"),"focused match precedes fallback pack");
                equal(mode.pack,c.candidates().get(1).pack,"language identity survives matching");
                equal(raw,c.raw(),"promotion never accepts unfinished typing");
                Candidate selected=c.candidates().get(c.preferred());c.space();
                equal(selected.text+(selected.literal?" ":""),e.text,"Space accepts displayed highlight");
            }
            Editor e=new Editor();CompositionEngine c=engine(e,Learning.NONE,false);
            c.switchMode(mode,false);c.addons(addon,enabled);type(c,"nihao");
            equal(first,c.candidates().get(c.preferred()).text,"focused full match overrides Chinese inference");
            c.switchMode(InputMode.CHINESE,false);
            yes(c.candidates().stream().noneMatch(v->v.pack.equals(mode.pack)),"mode switch removes focused tier");
            equal("你好",c.candidates().get(1).text,"Chinese full-reading preference restored");
            c.switchMode(mode,false);c.addons(addon,Collections.emptySet());
            equal("你好",c.candidates().get(1).text,"disabled pack restores base ranking");

            Learning manual=new Learning() {
                public int count(String context,String raw,String value){return 0;}
                public void choose(String context,String raw,String value){}
                public List<Candidate> custom(String raw){return Collections.singletonList(new Candidate("自訂詞",false,0));}
            };
            c=engine(new Editor(),manual,false);c.switchMode(mode,false);c.addons(addon,enabled);type(c,"nihao");
            equal("自訂詞",c.candidates().get(1).text,"manual entry precedes language tier");

            // Known English/raw and apostrophe recovery stay usable in either mixed mode.
            addon=AddonDictionary.read(new StringReader(mode.pack+"\tmeeting\t"+first+"\tfixture\tfixture\n"+
                mode.pack+"\tdont\t"+second+"\tfixture\tfixture\n"));
            e=new Editor();c=engine(e,Learning.NONE,false);c.switchMode(mode,false);c.addons(addon,enabled);type(c,"meeting");
            equal(first,c.candidates().get(1).text,"focused suggestion alongside known English");
            equal(first,c.candidates().get(c.preferred()).text,"focus overrides English literal heuristic");c.space();equal(first,e.text,"Space accepts focus");
            c=engine(new Editor(),Learning.NONE,false);c.switchMode(mode,false);c.addons(addon,enabled);type(c,"dont");
            equal(second,c.candidates().get(1).text,"focused suggestion precedes secondary contraction");
            equal(second,c.candidates().get(c.preferred()).text,"focused highlight remains coherent");
        }
        for(InputMode mode:Arrays.asList(InputMode.TAIWANESE_ENGLISH,InputMode.JAPANESE_ENGLISH)) {
            CompositionEngine self=engine(new Editor(),Learning.NONE,false);self.switchMode(mode,false);
            self.addons(AddonDictionary.read(new StringReader(mode.pack+"\ta\ta\tfixture\ttest\n")),Collections.singleton(mode.pack));
            type(self,"a");equal(0,self.preferred(),"raw-only source match remains valid without an alternate");
        }
        PairedForms pairs=PairedForms.read(new StringReader("poj\tá-bé\t甲乙\titaigi:1\n"));
        AddonDictionary paired=AddonDictionary.read(new StringReader("poj\tab'cd\tá-bé\titaigi:1\tfixture\n"),pairs);
        for(String raw:Arrays.asList("abcd","abc","ac")) {
            Candidate value=paired.lookup(raw,Collections.singleton("poj")).get(0);
            equal("poj",value.pack,"pair lookup retains source pack");
            equal("poj",value.primary(true).alternative().withScore(42).pack,"Han and phonetic transforms retain source pack");
        }
        System.out.println("PASS dedicated language priority, matching, fallback, overrides and acceptance");
    }
}
