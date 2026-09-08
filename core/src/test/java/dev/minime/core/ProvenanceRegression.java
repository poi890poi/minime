package dev.minime.core;

import java.io.StringReader;
import static dev.minime.core.Regression.*;

final class ProvenanceRegression {
    static void run()throws Exception {
        PhoneticDictionary fixture=PhoneticDictionary.load(new StringReader("jia\tㄐㄧㄚ\t甲\t100000000\tㄐㄧㄚˇ\nyi\tㄧ\t乙\t100000000\tㄧˇ\n"),new StringReader(""),new StringReader("jia\tㄐㄧㄚ\nyi\tㄧ\n"));
        for(String context:new String[]{"","今天"}) {
            Candidate single=fixture.convert("jia",false,context).stream().filter(c->c.text.equals("甲")).findFirst().get();
            yes(!single.composed,"one lexical unit is not a speculative sequence");
            for(String raw:new String[]{"jiayi","jy","jiay"}) {
                Candidate joined=fixture.convert(raw,false,context).stream().filter(c->c.text.equals("甲乙")).findFirst().get();
                yes(joined.composed,"joined units retain provenance through full/partial/context paths");
            }
        }
    }
}
