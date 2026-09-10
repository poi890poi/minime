package dev.minime.core;

import java.nio.file.*;
import java.util.*;
import static dev.minime.core.Regression.*;

final class JapaneseKanaRegression {
    static void run()throws Exception {
        JapaneseBasics basics=JapaneseBasics.read(Files.newBufferedReader(Paths.get("app/src/main/assets/japanese-basic.tsv")));
        AddonDictionary addon=AddonDictionary.withJapaneseBasics(AddonDictionary.EMPTY,basics);int n=0;
        for(String line:Files.readAllLines(Paths.get("docs/japanese-continuity/kana-oracle.tsv"))) {
            String[] p=line.split("\t",-1);if(p[0].equals("input"))continue;
            List<Candidate> result=basics.merge(p[0],Collections.emptyList());
            if(!p[1].isEmpty())for(int script=1;script<=2;script++) {
                String expected=p[script];int consumed=Integer.parseInt(p[3]);
                yes(result.stream().anyMatch(c->c.text.equals(expected) && c.consumed==consumed),"WanaKana oracle "+p[0]+" -> "+expected);
            }
            n++;
        }
        Editor e=new Editor();CompositionEngine c=engine(e,Learning.NONE,false);c.addons(addon,Collections.singleton("japanese"));c.switchMode(InputMode.JAPANESE_ENGLISH,false);
        type(c,"konnichiwasekai");Candidate selected=c.candidates().get(c.preferred());c.space();equal(selected.text,e.text,"Whole kana Space agrees with highlight");
        type(c,"sakurak");yes(c.candidates().get(c.preferred()).consumed==0,"Space cannot silently consume a kana prefix");Candidate prefix=c.candidates().stream().filter(v->v.consumed==6).findFirst().orElseThrow(AssertionError::new);
        c.select(c.candidates().indexOf(prefix));equal("k",c.raw(),"Unfinished syllable stays owned");c.type('a');yes(c.candidates().stream().anyMatch(v->v.text.equals("か")),"Suffix continues normally");
        c.switchMode(InputMode.ENGLISH,false);yes(c.candidates().stream().noneMatch(v->v.pack.equals("japanese")),"English remains isolated");
        yes(addon.lookup("konnichiwasekai",Collections.emptySet()).isEmpty(),"Disabled pack excludes kana");
        System.out.println("PASS Japanese kana differential probes="+n+"; acceptance, suffix and mode isolation");
    }
}
