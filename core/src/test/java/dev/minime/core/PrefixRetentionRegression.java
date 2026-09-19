package dev.minime.core;

import java.io.*;
import java.util.*;

/** Synthetic homophone pressure, never production vocabulary or accuracy. */
public final class PrefixRetentionRegression {
    static void run()throws Exception {
        StringBuilder rows=new StringBuilder("ma'an\tㄇㄚㄢ\t甲乙\t10000\tㄇㄚㄢ\n");
        String[] tails={"ba","bai","ban","bang","bao","bei","ben","beng","bi","bian","biao","bie"};
        for(int i=0;i<tails.length;i++)rows.append("ma'an'").append(tails[i]).append("\tㄇㄚㄢㄅ\t甲乙").appendCodePoint(0x4e00+i).append("\t").append(100-i).append("\tㄇㄚㄢㄅ\n");
        PhoneticDictionary model=PhoneticDictionary.load(new StringReader(rows.toString()),new StringReader(""),new StringReader("ma\nan\n"+String.join("\n",tails)));
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();model.writeBinary(bytes);
        for(PhoneticDictionary d:Arrays.asList(model,PhoneticDictionary.readBinary(new ByteArrayInputStream(bytes.toByteArray())))) {
            List<Candidate> choices=d.convert("mabz",false);
            Regression.yes(choices.stream().anyMatch(c->c.text.equals("甲乙")&&c.consumed==2),"Short high-frequency prefix survives more than eight longer matches");
            Regression.yes(choices.stream().filter(c->c.consumed==3).count()>=8,"Previously retained long prefixes remain reachable");
            Regression.yes(choices.stream().filter(c->c.consumed>0&&c.text.codePointCount(0,c.text.length())>1).count()<=16,"Union stays bounded at sixteen words");
            for(boolean privacy:new boolean[]{false,true}) {
                Regression.Editor editor=new Regression.Editor();CompositionEngine engine=new CompositionEngine(editor,Learning.NONE);engine.dictionary(d);engine.start(false,false,privacy,false);
                "mabz".codePoints().forEach(engine::type);
                Candidate c=engine.candidates().stream().filter(v->v.text.equals("甲乙")).findFirst().orElseThrow();
                Regression.equal(0,engine.preferred(),"Prefix never takes Space from literal input");
                engine.selectCandidate(c,engine.compositionId());Regression.equal("甲乙",editor.text,"Select stored word only");Regression.equal("bz",engine.raw(),"Preserve shorter prefix suffix");
            }
        }
        System.out.println("PASS prefix retention: short frequent words, long alternatives, bounded union, private selection and binary parity");
    }
    public static void main(String[] args)throws Exception {run();}
}
