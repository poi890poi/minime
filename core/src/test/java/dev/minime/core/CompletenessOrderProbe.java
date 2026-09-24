package dev.minime.core;

import java.io.*;
import java.util.*;

/** Generated mechanism probe; these artificial counts are not linguistic labels. */
public final class CompletenessOrderProbe {
    public static void main(String[] args)throws Exception {
        boolean trial=args.length==1 && args[0].equals("complete");
        int episodes=0;
        for(String stem:Arrays.asList("ka","ba","pa","ma","na","la")) {
            String full="\u4e00",completion="\u4e01",duplicate="\u4e02";
            String rows=stem+"\tㄚ\t"+full+"\t10\tㄚ\n"
                +stem+"n\tㄢ\t"+completion+"\t100000000\tㄢ\n"
                +stem+"\tㄚ\t"+duplicate+"\t1\tㄚ\n"
                +stem+"n\tㄢ\t"+duplicate+"\t1000000\tㄢ\n";
            PhoneticDictionary d=PhoneticDictionary.load(new StringReader(rows),new StringReader(""),new StringReader(stem+"\n"+stem+"n\n"));
            List<Candidate> direct=d.convert(stem,false);
            Regression.equal(trial?full:completion,direct.get(0).text,"dictionary ordering");
            Candidate dup=direct.stream().filter(c->c.text.equals(duplicate)).findFirst().get();
            Regression.equal(!trial,dup.incomplete,"dedup retains correct reading path");
            Regression.equal(new HashSet<>(Arrays.asList(full,completion,duplicate)),texts(direct),"all identities survive");
            Regression.Editor editor=new Regression.Editor();
            CompositionEngine c=new CompositionEngine(editor,Learning.NONE);c.dictionary(d);c.start(false,false,false,false);
            for(int ch:stem.codePoints().toArray())c.type(ch);
            Regression.equal(trial?full:completion,c.candidates().get(c.preferred()).text,"composition preserves dictionary evidence");
            c.space();Regression.equal(trial?full:completion,editor.text,"Space matches visible default");
            // An initial with no complete source spelling keeps frequency order.
            Regression.equal(completion,d.convert(stem.substring(0,1),false).get(0).text,"initial-only frequency control");
            Learning learned=new Learning() {
                public int count(String context,String raw,String text) {return text.equals(completion)?3:0;}
                public void choose(String context,String raw,String text) {}
            };
            editor=new Regression.Editor();c=new CompositionEngine(editor,learned);c.dictionary(d);c.start(false,false,false,false);for(int ch:stem.codePoints().toArray())c.type(ch);
            Regression.equal(completion,c.candidates().get(c.preferred()).text,"explicit learned completion remains preferred");
            episodes++;
        }
        System.out.println("PASS "+episodes+" generated completeness mechanisms; policy="+(trial?"complete":"frequency")+" (not language accuracy)");
    }
    private static Set<String> texts(List<Candidate> values) {
        Set<String> result=new HashSet<>();for(Candidate c:values)result.add(c.text);return result;
    }
}
