package dev.minime.core;

import java.nio.file.*;
import java.util.*;

/** Every proposed flag, casing and mode; engine contracts, not language accuracy. */
public final class GrammarSourceEvaluation {
    public static void main(String[] args)throws Exception {
        Path assets=Paths.get("app/src/main/assets"),metadata=Paths.get(args[0]);
        PhoneticDictionary d=PhoneticDictionary.load(Files.newBufferedReader(assets.resolve("zh_tw.tsv")),
            Files.newBufferedReader(assets.resolve("en_us.tsv")),Files.newBufferedReader(assets.resolve("syllables.tsv")),
            Files.newBufferedReader(assets.resolve("context.tsv")));
        d.englishSpelling(Files.newBufferedReader(metadata));
        int flags=0,episodes=0,restorations=0,protectedBare=0;
        for(String line:Files.readAllLines(metadata)) {
            if(!line.startsWith("contraction\t"))continue;
            String word=line.split("\t")[1],key=word.replace("'","");flags++;
            for(String raw:new String[]{key,Character.toUpperCase(key.charAt(0))+key.substring(1),key.toUpperCase(Locale.ROOT)}) {
                for(boolean english:new boolean[]{false,true}) {
                    Regression.Editor editor=new Regression.Editor();
                    CompositionEngine c=new CompositionEngine(editor,Learning.NONE);c.dictionary(d);
                    c.start(false,false,false,false,english);c.englishOptions(false,false);
                    raw.codePoints().forEach(c::type);
                    if(!c.candidates().get(0).text.equals(raw))throw new AssertionError("Raw recovery unavailable: "+raw);
                    List<Candidate> matches=d.englishApostrophes(raw,english);
                    if(matches.isEmpty())throw new AssertionError("Flag has no indexed spelling: "+word);
                    String expected=d.validEnglishSpelling(raw)?raw:matches.get(0).text;
                    c.space();
                    if(!editor.text.equals(expected+" "))throw new AssertionError("Space contract: "+raw+" => "+editor.text+" expected "+expected);
                    if(expected.equals(raw))protectedBare++;else {
                        restorations++;
                        c.backspace();
                        if(!c.raw().equals(raw))throw new AssertionError("Undo did not restore input: "+raw);
                        c.select(0);
                        if(!editor.text.equals(raw))throw new AssertionError("Raw selection changed input: "+raw);
                    }
                    episodes++;
                }
            }
        }
        System.out.println("{\"flags\":"+flags+",\"episodes\":"+episodes+",\"restorations\":"+restorations+",\"protected_bare\":"+protectedBare+"}");
    }
}
