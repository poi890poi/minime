package dev.minime.core;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Source-reading coverage diagnosis; no target text enters runtime lookup. */
public final class ReadingPriorAudit {
    static Map<String,Integer> inventory(List<Candidate> values) {
        Map<String,Integer> result=new LinkedHashMap<>();
        for(int i=0;i<values.size();i++) if(values.get(i).consumed==0)
            result.putIfAbsent(values.get(i).text,i+1);
        return result;
    }
    static PhoneticDictionary load(Path chinese)throws Exception {
        Path assets=Paths.get("app/src/main/assets");
        return PhoneticDictionary.load(Files.newBufferedReader(chinese),
            new StringReader(""),Files.newBufferedReader(assets.resolve("syllables.tsv")));
    }
    static boolean han(String text) {
        return text.codePointCount(0,text.length())==1
            && Character.UnicodeScript.of(text.codePointAt(0))==Character.UnicodeScript.HAN;
    }
    public static void main(String[] args)throws Exception {
        if(args.length!=4)throw new IllegalArgumentException("before.tsv after.tsv inputs.txt output.tsv");
        PhoneticDictionary before=load(Paths.get(args[0])),after=load(Paths.get(args[1]));
        Map<String,Set<String>> readings=new TreeMap<>();
        for(String row:Files.readAllLines(Paths.get(args[0]),StandardCharsets.UTF_8)) {
            String[] p=row.split("\t");
            readings.computeIfAbsent(p[2],k->new TreeSet<>()).add(p[0]);
        }
        Map<String,Map<String,Integer>> oldCache=new HashMap<>(),newCache=new HashMap<>();
        Set<String> queries=new TreeSet<>(Files.readAllLines(Paths.get(args[2]),StandardCharsets.UTF_8));
        for(Map.Entry<String,Set<String>> e:readings.entrySet())if(han(e.getKey()))queries.addAll(e.getValue());
        int glyphPairs=0,missingBefore=0,missingAfter=0,lostWhole=0,lostGlyph=0,unreachable=0,queriesChanged=0;
        try(BufferedWriter out=Files.newBufferedWriter(Paths.get(args[3]),StandardCharsets.UTF_8)) {
            out.write("query\tkind\ttext\tbefore_rank\tafter_rank\tcomplete_readings\tcomplete_before\tcomplete_after\n");
            for(String q:queries) {
                Map<String,Integer> x=oldCache.computeIfAbsent(q,k->inventory(before.convert(k,false))),
                    y=newCache.computeIfAbsent(q,k->inventory(after.convert(k,false)));
                if(!x.equals(y))queriesChanged++;
                Set<String> identities=new TreeSet<>(x.keySet());identities.addAll(y.keySet());
                for(String text:identities) {
                    int a=x.getOrDefault(text,0),b=y.getOrDefault(text,0);
                    if(a==b)continue;
                    int completeBefore=0,completeAfter=0;
                    Set<String> spellings=readings.getOrDefault(text,Collections.emptySet());
                    if(a>0&&b==0) {
                        lostWhole++;if(han(text))lostGlyph++;
                        for(String spelling:spellings) {
                            if(oldCache.computeIfAbsent(spelling,k->inventory(before.convert(k,false))).containsKey(text))completeBefore++;
                            if(newCache.computeIfAbsent(spelling,k->inventory(after.convert(k,false))).containsKey(text))completeAfter++;
                        }
                        if(completeAfter==0)unreachable++;
                    }
                    out.write(q+"\t"+(han(text)?"glyph":"phrase")+"\t"+text+"\t"+a+"\t"+b+"\t"+spellings.size()+"\t"+completeBefore+"\t"+completeAfter+"\n");
                }
            }
            for(Map.Entry<String,Set<String>> e:readings.entrySet())if(han(e.getKey()))
                for(String spelling:e.getValue()) {
                    glyphPairs++;
                    if(!oldCache.computeIfAbsent(spelling,k->inventory(before.convert(k,false))).containsKey(e.getKey()))missingBefore++;
                    if(!newCache.computeIfAbsent(spelling,k->inventory(after.convert(k,false))).containsKey(e.getKey()))missingAfter++;
                }
        }
        System.out.println("{\"queries\":"+queries.size()+",\"changed_queries\":"+queriesChanged+
            ",\"single_han_reading_pairs\":"+glyphPairs+",\"complete_missing_before\":"+missingBefore+
            ",\"complete_missing_after\":"+missingAfter+",\"lost_query_identities\":"+lostWhole+
            ",\"lost_glyph_identities\":"+lostGlyph+",\"lost_identities_without_complete_access\":"+unreachable+"}");
    }
}
