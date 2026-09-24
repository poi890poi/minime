package dev.minime.core;

import java.io.*;
import java.lang.reflect.Field;
import java.nio.file.*;
import java.util.*;
import static dev.minime.core.Regression.*;

/** The unchanged full-list implementation is the oracle, not the new predicate. */
final class CompletionExistenceRegression {
    @SuppressWarnings("unchecked")
    static SortedSet<String> queries(PhoneticDictionary d)throws Exception {
        Field field=PhoneticDictionary.class.getDeclaredField("english");field.setAccessible(true);
        Set<String> words=((Map<String,Integer>)field.get(d)).keySet();
        SortedSet<String> prefixes=new TreeSet<>();
        for(String word:words)for(int i=1;i<=word.length();i++)prefixes.add(word.substring(0,i).toLowerCase(Locale.ROOT));
        SortedSet<String> inputs=new TreeSet<>(Arrays.asList("","'","''","a'","A'","1","漢字","a b","a-b","a_b","a.b","a''b","a'b'c","a\ud800","a\udfff"));
        for(String prefix:prefixes) {
            inputs.add(prefix);inputs.add(prefix.toUpperCase(Locale.ROOT));
            inputs.add(Character.toUpperCase(prefix.charAt(0))+prefix.substring(1));
            if(prefix.length()>1)inputs.add(prefix.charAt(0)+prefix.substring(1).toUpperCase(Locale.ROOT));
            // Generated extensions exercise exact-leaf and absent-prefix paths.
            if(prefix.hashCode()%31==0)inputs.add(prefix+"zz");
        }
        return inputs;
    }
    static void run()throws Exception {
        SortedSet<String> inputs=queries(dictionary);int positives=0;
        for(String raw:inputs)for(boolean latin:new boolean[]{false,true}) {
            boolean expected=!dictionary.englishCompletions(raw,latin).isEmpty();
            equal(expected,dictionary.hasEnglishCompletion(raw,latin),"completion existence: "+raw+" / "+latin);
            if(expected)positives++;
        }
        PhoneticDictionary empty=PhoneticDictionary.load(new StringReader(""),new StringReader(""),new StringReader(""));
        for(String raw:Arrays.asList("","a","ab","AB","Ab","aB","ab'","ab'c"))for(boolean latin:new boolean[]{false,true})
            equal(false,empty.hasEnglishCompletion(raw,latin),"empty dictionary has no completion");
        System.out.println("PASS completion existence: "+inputs.size()+" distinct source-derived/case/boundary inputs x 2 contexts; "+positives+" positive decisions; not language accuracy");
    }
    /** Paired warmed JVM microbenchmark; no claim about Android frames. */
    public static void main(String[] args)throws Exception {
        Path assets=Paths.get("app/src/main/assets"),out=Paths.get(args[0]);Files.createDirectories(out);
        PhoneticDictionary d=PhoneticDictionary.load(Files.newBufferedReader(assets.resolve("zh_tw.tsv")),Files.newBufferedReader(assets.resolve("en_us.tsv")),Files.newBufferedReader(assets.resolve("syllables.tsv")),Files.newBufferedReader(assets.resolve("context.tsv")));
        List<String> inputs=new ArrayList<>();
        for(String raw:queries(d))if(raw.matches("[a-z]{2,16}") && Math.floorMod(raw.hashCode(),113)==0)inputs.add(raw);
        Files.write(out.resolve("inputs.txt"),inputs);
        try(PrintWriter log=new PrintWriter(Files.newBufferedWriter(out.resolve("microbenchmark.tsv")))) {
            log.println("round\tquery\tlatin_context\tlist_ns\texists_ns\tresult");
            for(int round=-2;round<5;round++)for(String raw:inputs)for(boolean latin:new boolean[]{false,true}) {
                long a,b,at;boolean x,y;
                if((round&1)==0) {
                    at=System.nanoTime();x=!d.englishCompletions(raw,latin).isEmpty();a=System.nanoTime()-at;
                    at=System.nanoTime();y=d.hasEnglishCompletion(raw,latin);b=System.nanoTime()-at;
                } else {
                    at=System.nanoTime();y=d.hasEnglishCompletion(raw,latin);b=System.nanoTime()-at;
                    at=System.nanoTime();x=!d.englishCompletions(raw,latin).isEmpty();a=System.nanoTime()-at;
                }
                if(x!=y)throw new AssertionError(raw);
                if(round>=0)log.println(round+"\t"+raw+"\t"+latin+"\t"+a+"\t"+b+"\t"+x);
            }
        }
        System.out.println("Measured "+inputs.size()+" inputs x 2 contexts x 5 rounds after 2 warm-ups");
    }
}
