package dev.minime.core;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.zip.GZIPInputStream;

/** Conditional dictionary ranking: targets are scored only after lookup. */
public final class ContextRankingEvaluation {
    public static void main(String[] args)throws Exception {
        Path assets=Paths.get("app/src/main/assets");
        PhoneticDictionary d=PhoneticDictionary.load(Files.newBufferedReader(assets.resolve("zh_tw.tsv")),
            Files.newBufferedReader(assets.resolve("en_us.tsv")),Files.newBufferedReader(assets.resolve("syllables.tsv")),Files.newBufferedReader(assets.resolve("context.tsv")));
        Map<String,List<Candidate>> cache=new HashMap<>();Map<String,Long> timing=new HashMap<>();
        try(BufferedReader in=new BufferedReader(new InputStreamReader(new GZIPInputStream(Files.newInputStream(Paths.get(args[0]))),StandardCharsets.UTF_8));
            BufferedWriter out=Files.newBufferedWriter(Paths.get(args[1]))) {
            out.write("genre\tid\tcondition\tcontext\traw\ttarget\trank\tlookup_ns\toutputs\n");in.readLine();String line;int n=0;
            while((line=in.readLine())!=null) {
                String[] p=line.split("\t",-1);String context=p[3],raw=p[4],target=p[5],key=context+"\t"+raw;
                if(!cache.containsKey(key)) {
                    long begin=System.nanoTime();cache.put(key,d.convert(raw,false,context));timing.put(key,System.nanoTime()-begin);
                }
                List<Candidate> values=cache.get(key);int rank=0;List<String> outputs=new ArrayList<>();
                for(int i=0;i<values.size();i++) {
                    Candidate c=values.get(i);if(c.text.equals(target)&&c.consumed==0)rank=i+1;
                    outputs.add(c.text+":"+c.consumed);
                }
                out.write(line+"\t"+rank+"\t"+timing.get(key)+"\t"+String.join("|",outputs)+"\n");
                if(++n%5000==0){out.flush();System.out.println("Context episodes "+n+" unique queries "+cache.size());}
            }
        }
    }
}
