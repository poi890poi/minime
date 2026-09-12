package dev.minime.core;
import java.io.StringReader;
import java.util.*;
/** Synthetic mechanism check, never a language accuracy claim or production data. */
public final class NullConstructionContract {
    public static void main(String[] args)throws Exception {
        boolean enabled=Boolean.parseBoolean(args[0]);
        PhoneticDictionary d=PhoneticDictionary.load(new StringReader("ni\tㄋㄧˇ\t甲\t10\tㄋㄧˇ\nhao\tㄏㄠˇ\t乙\t10\tㄏㄠˇ\nni'hao\tㄋㄧˇㄏㄠˇ\t丙丁\t12\tㄋㄧˇㄏㄠˇ\n"),new StringReader("hello\t10\n"),new StringReader("ni\nhao\n"));
        for(String raw:new String[]{"nihao","nh","ni'h"}) {
            List<Candidate> c=d.convert(raw,false);
            if(c.stream().noneMatch(x->x.text.equals("丙丁")))throw new AssertionError("Stored entry lost: "+raw);
            if(!enabled && c.stream().anyMatch(x->x.composed))throw new AssertionError("Null constructed: "+raw);
        }
        boolean joined=d.convert("nihao",false).stream().anyMatch(x->x.text.equals("甲乙") && x.composed);
        if(joined!=enabled)throw new AssertionError("Joining toggle not causal");
        if(!d.isEnglish("hello"))throw new AssertionError("English changed");
        System.out.println("PASS isolated joining contract enabled="+enabled);
    }
}
