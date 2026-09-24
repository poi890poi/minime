package dev.minime.core;

import java.io.StringReader;
import java.util.*;
import static dev.minime.core.Regression.*;

/** Explicit input casing selects the existing Latin vocabulary, not a word exception. */
final class ExplicitLatinCompletionRegression {
    private static final Learning PRIVATE=new Learning() {
        public int count(String context,String raw,String value){throw new AssertionError("Private learning read");}
        public void choose(String context,String raw,String value){throw new AssertionError("Private learning write");}
    };
    private static CompositionEngine session(PhoneticDictionary d,Editor e,InputMode mode,boolean priv,boolean literal) {
        CompositionEngine c=new CompositionEngine(e,priv?PRIVATE:Learning.NONE);c.dictionary(d);
        c.start(false,literal,priv,false,mode.english());c.switchMode(mode,literal);return c;
    }
    static void run()throws Exception {
        PhoneticDictionary d=PhoneticDictionary.load(new StringReader(""),
            new StringReader("Abacus\t180\nabout\t100\n"),new StringReader(""));
        for(InputMode mode:InputMode.values())if(mode.englishEnabled())for(boolean priv:new boolean[]{false,true}) {
            for(String prefix:Arrays.asList("Ab","AB")) {
                Editor e=new Editor();CompositionEngine c=session(d,e,mode,priv,false);type(c,prefix);
                String expected=prefix.equals("AB")?"ABACUS":"Abacus";
                yes(c.candidates().stream().anyMatch(v->v.literal && v.text.equals(expected)),
                    "Explicit Latin completion on "+mode+" private="+priv+" prefix="+prefix);
                c.space();equal(prefix+" ",e.text,"Completion availability never enables automatic acceptance");
                c=session(d,new Editor(),mode,priv,true);type(c,prefix);
                equal(1,c.candidates().size(),"Literal fields expose only exact input");
            }
            CompositionEngine c=session(d,new Editor(),mode,priv,false);type(c,"aB");
            yes(c.candidates().stream().noneMatch(v->v.text.equals("Abacus") || v.text.equals("abacus")),
                "Unusual mixed casing keeps existing completion validation");
        }
        CompositionEngine c=session(d,new Editor(),InputMode.CHINESE,false,false);type(c,"ab");
        yes(c.candidates().stream().noneMatch(v->v.text.equals("abacus")),"Lowercase Pinyin vocabulary scope is unchanged");
        c=session(d,new Editor(),InputMode.ENGLISH,false,false);type(c,"ab");
        yes(c.candidates().stream().anyMatch(v->v.text.equals("abacus")),"Existing English-mode vocabulary remains available");
        System.out.println("PASS explicit Latin completions: modes, casing, raw Space, literal and private fields");
    }
    public static void main(String[] args)throws Exception {run();}
}
