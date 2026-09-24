package dev.minime.core;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Exact tone-bearing source access, not frequency or language-quality labels. */
public final class ZhuyinReadingAudit {
    private static Set<String> exact(PhoneticDictionary dictionary,String reading) {
        Set<String> values=new HashSet<>();
        for(Candidate candidate:dictionary.convert(reading,true))
            if(candidate.consumed==0)values.add(candidate.text);
        return values;
    }
    public static void main(String[] args)throws Exception {
        if(args.length!=2)throw new IllegalArgumentException("before.tsv after.tsv");
        PhoneticDictionary before=ReadingPriorAudit.load(Paths.get(args[0]));
        PhoneticDictionary after=ReadingPriorAudit.load(Paths.get(args[1]));
        Map<String,Set<String>> reference=new TreeMap<>();
        for(String line:Files.readAllLines(Paths.get(args[0]),StandardCharsets.UTF_8)) {
            String[] cells=line.split("\t");
            if(ReadingPriorAudit.han(cells[2]))reference.computeIfAbsent(cells[4],k->new TreeSet<>()).add(cells[2]);
        }
        int pairs=0,missingBefore=0,missingAfter=0,lost=0;
        for(Map.Entry<String,Set<String>> entry:reference.entrySet()) {
            Set<String> a=exact(before,entry.getKey()),b=exact(after,entry.getKey());
            for(String glyph:entry.getValue()) {
                pairs++;
                if(!a.contains(glyph))missingBefore++;
                if(!b.contains(glyph))missingAfter++;
                if(a.contains(glyph)&&!b.contains(glyph))lost++;
            }
        }
        System.out.println("{\"tone_bearing_queries\":"+reference.size()+",\"source_pairs\":"+pairs
            +",\"missing_before\":"+missingBefore+",\"missing_after\":"+missingAfter+",\"lost_exact_pairs\":"+lost+"}");
        if(lost!=0)throw new AssertionError("Exact tone-bearing readings disappeared");
    }
}
