package dev.minime.core;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import static dev.minime.core.Regression.*;

final class JapaneseBasicsRegression {
    static void run()throws Exception {
        Path source=Paths.get("app/src/main/assets/japanese-basic.tsv");
        JapaneseBasics basics=JapaneseBasics.read(Files.newBufferedReader(source));
        int kanaRows=0;
        for(String line:Files.readAllLines(source)) {
            if(line.startsWith("#"))continue;
            String[] p=line.split("\t");
            List<Candidate> result=basics.lookup(p[1]);
            yes(result.size()<=8,"character search bounded");
            if(!p[0].equals("kanji")) {
                kanaRows++;
                yes(result.stream().limit(2).anyMatch(c->c.text.equals(p[2]) && !c.incomplete),"every source kana alias is a leading exact choice");
            }
            yes(result.stream().allMatch(c->c.languageCharacter && c.pack.equals("japanese")),"source character identity preserved");
        }
        yes(kanaRows>0,"nonempty library mappings audited");
        // Independent fixtures force a collision with reversed Mandarin frequency.
        JapaneseBasics fixture=JapaneseBasics.read(new StringReader("kanji\tka\t甲\t2\tkanjidic:U7532\nkanji\tka\t乙\t1\tkanjidic:U4E59\n"));
        AddonDictionary words=AddonDictionary.read(new StringReader("japanese\tka\tことば\tfixture\tfixture\n"));
        AddonDictionary combined=AddonDictionary.combine(AddonDictionary.withJapaneseBasics(words,fixture),AddonDictionary.read(new StringReader("geography\tka\t地理\tfixture\tfixture\n")));
        Editor e=new Editor();CompositionEngine c=engine(e,Learning.NONE,false);c.switchMode(InputMode.JAPANESE,false);
        c.decoder((d,r,b,context,done)->done.accept(Arrays.asList(new Candidate("甲",false,200),new Candidate("乙",false,100))),()->{});
        c.addons(combined,new HashSet<>(Arrays.asList("japanese","geography")));type(c,"ka");
        equal("乙",c.candidates().get(1).text,"Japanese source frequency overrides Mandarin glyph order in Japanese mode");
        equal(1L,c.candidates().stream().filter(v->v.text.equals("乙")).count(),"source collision deduplicated");
        yes(c.preferred()>=0,"replaced default retains a valid highlight");
        Candidate choice=c.candidates().get(c.preferred());c.space();equal(choice.text+(choice.literal?" ":""),e.text,"character collision Space agrees with highlight");
        c.switchMode(InputMode.CHINESE,false);type(c,"ka");equal("甲",c.candidates().get(1).text,"Chinese mode retains Mandarin order");
        equal(words.lookup("ka",Collections.singleton("japanese")).toString(),AddonDictionary.withJapaneseBasics(words,JapaneseBasics.EMPTY).lookup("ka",Collections.singleton("japanese")).toString(),"empty/failing optional source preserves words");
        yes(combined.lookup("ka",Collections.singleton("geography")).stream().noneMatch(v->v.languageCharacter),"character source respects pack disable");
        StringBuilder expanded=new StringBuilder("japanese\tabcdef\t定型句\tfixture\teveryday_expressions\n");
        for(int i=0;i<32;i++)expanded.append("japanese\tab").append((char)('a'+i/26)).append((char)('a'+i%26)).append("\t語彙").append(i).append("\tfixture\teveryday_vocabulary\n");
        AddonDictionary separate=AddonDictionary.read(new StringReader(expanded.toString()));
        List<Candidate> matches=separate.lookup("ab",Collections.singleton("japanese"));
        yes(matches.stream().anyMatch(v->v.text.equals("定型句")),"vocabulary growth cannot evict an expression from its lookup budget");
        yes(matches.stream().anyMatch(v->v.text.startsWith("語彙")),"ordinary vocabulary gets its own bounded partial results");
        yes(matches.size()<=48,"two word budgets remain bounded");
        for(String bad:Arrays.asList("kanji\tka\t甲\t501\tkanjidic:U7532\n","kanji\tka\t甲\t1\tfixture\n","hiragana\tka\t甲\t0\twanakana:5.3.1\n")) {
            try {JapaneseBasics.read(new StringReader(bad));throw new AssertionError("invalid character source accepted");}catch(IOException expected){}
        }
        System.out.println("PASS Japanese characters: "+kanaRows+" kana source aliases, bounded lookup, source frequency and mode isolation");
    }
}
