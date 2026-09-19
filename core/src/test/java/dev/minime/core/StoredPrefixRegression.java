package dev.minime.core;

import java.io.*;
import java.util.*;

/** Stored phrase prefixes are explicit choices; never inferred sentences. */
public final class StoredPrefixRegression {
    static void run()throws Exception{
        String rows="ni\tㄋㄧˇ\t你\t9000\tㄋㄧˇ\nhao\tㄏㄠˇ\t好\t8000\tㄏㄠˇ\nma\tㄇㄚ˙\t嗎\t7000\tㄇㄚ˙\n"
            +"ni'hao\tㄋㄧˇㄏㄠˇ\t你好\t100\tㄋㄧˇㄏㄠˇ\nni'hao'ma\tㄋㄧˇㄏㄠˇㄇㄚ˙\t你好嗎\t50\tㄋㄧˇㄏㄠˇㄇㄚ˙\n";
        PhoneticDictionary d=PhoneticDictionary.load(new StringReader(rows),new StringReader("hello\t100\n"),new StringReader("ni\nhao\nma\n"));
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();d.writeBinary(bytes);
        for(PhoneticDictionary model:Arrays.asList(d,PhoneticDictionary.readBinary(new ByteArrayInputStream(bytes.toByteArray()))))
            for(String raw:Arrays.asList("nihaom","nhm","ni'h'm","n'haoma","nihao'zz","nihao"+"z".repeat(40)))for(boolean priv:new boolean[]{false,true}){
                Regression.Editor e=new Regression.Editor();CompositionEngine c=new CompositionEngine(e,Learning.NONE);c.dictionary(model);c.start(false,false,priv,false);
                raw.codePoints().forEach(c::type);
                Candidate word=c.candidates().stream().filter(v->v.text.equals("你好")&&v.consumed>0&&v.consumed<raw.length()).findFirst().orElse(null);
                Regression.yes(word!=null,"Stored phrase prefix is selectable for full/initial/mixed input: "+raw);
                Regression.yes(c.candidates().stream().anyMatch(v->v.text.equals("你")&&v.consumed>0),"First-glyph alternative stays reachable");
                Candidate preferred=c.candidates().get(c.preferred());Regression.yes(preferred.consumed==0,"Prefix phrase cannot own whole-token Space");
                Regression.yes(!word.composed,"Prefix is source-attested");String rest=raw.substring(word.consumed).replaceFirst("^'+","");long version=c.compositionId();
                c.selectCandidate(word,version);Regression.equal("你好",e.text,"Commit only selected stored phrase");Regression.equal(rest,c.raw(),"Keep every unconsumed key");
                c.selectCandidate(word,version);Regression.equal(rest,c.raw(),"Ignore stale prefix selection");
            }
        Regression.yes(d.convert("nihao",false).stream().anyMatch(v->v.text.equals("你好")&&v.consumed==0),"Whole identity wins prefix deduplication");
        PhoneticDictionary collision=PhoneticDictionary.load(new StringReader("xian\tㄒㄧㄢ\t先\t100\tㄒㄧㄢ\nxi'an\tㄒㄧㄢ\t西安\t200\tㄒㄧㄢ\n"),new StringReader(""),new StringReader("xi\nan\nxian\n"));
        bytes=new ByteArrayOutputStream();collision.writeBinary(bytes);
        for(PhoneticDictionary model:Arrays.asList(collision,PhoneticDictionary.readBinary(new ByteArrayInputStream(bytes.toByteArray())))) {
            for(String raw:Arrays.asList("x'x","xi'zz"))Regression.yes(model.convert(raw,false).stream().noneMatch(v->v.text.equals("西安")&&v.consumed>0),"Compact alias cannot lend a two-syllable phrase to one syllable: "+raw);
            for(String raw:Arrays.asList("x'a'zz","xi'an'zz","xia'zz"))Regression.yes(model.convert(raw,false).stream().anyMatch(v->v.text.equals("西安")&&v.consumed>0),"Source syllables retain valid phrase-prefix spelling: "+raw);
            Regression.yes(model.convert("xian",false).stream().anyMatch(v->v.text.equals("西安")&&v.consumed==0),"Exact compact spelling preserves both interpretations");
        }
        System.out.println("PASS stored prefix phrases: full/initial/mixed, separators, later typo, long suffix, privacy, binary and stale selections");
    }
    public static void main(String[] args)throws Exception{run();}
}
