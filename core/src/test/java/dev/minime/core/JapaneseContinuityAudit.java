package dev.minime.core;

import java.nio.file.*;
import java.util.*;

/** Source retrieval and synthetic concatenation availability, never sentence accuracy. */
public final class JapaneseContinuityAudit {
    public static void main(String[] args)throws Exception {
        AddonDictionary words=AddonDictionary.withJapaneseBasics(AddonDictionary.read(Files.newBufferedReader(Paths.get("app/src/main/assets/addons.tsv"))),
            JapaneseBasics.read(Files.newBufferedReader(Paths.get("app/src/main/assets/japanese-basic.tsv"))));
        List<String[]> rows=new ArrayList<>();
        for(String line:Files.readAllLines(Paths.get("docs/japanese-coverage/inputs.tsv"))) {
            String[] p=line.split("\t",-1);if(p[0].equals("full"))rows.add(p);
        }
        Map<String,int[]> counts=new TreeMap<>();List<String> output=new ArrayList<>();
        output.add("condition\tinput\ttarget\tchoices\thit\tlookup_ns");
        for(int i=0;i<rows.size();i++) {
            String[] p=rows.get(i);probe(words,"source-"+(p[4].length()<=3?"1-3":"4+"),p[3],p[4],counts,output);
            if(i%23==0) {String raw=p[3]+rows.get((i+7919)%rows.size())[3];if(raw.length()<=96)probe(words,"joined",raw,"",counts,output);}
        }
        for(Map.Entry<String,int[]> e:counts.entrySet())System.out.println(e.getKey()+" queries/empty/hit="+Arrays.toString(e.getValue()));
        Files.write(Paths.get(args[0]),output);
    }
    private static void probe(AddonDictionary words,String kind,String raw,String target,Map<String,int[]> counts,List<String> output) {
        long at=System.nanoTime();List<Candidate> result=words.lookup(raw,Collections.singleton("japanese"));long ns=System.nanoTime()-at;
        boolean hit=result.stream().anyMatch(c->c.text.equals(target));int[] n=counts.computeIfAbsent(kind,k->new int[3]);n[0]++;if(result.isEmpty())n[1]++;if(hit)n[2]++;
        output.add(kind+"\t"+raw+"\t"+target+"\t"+result.size()+"\t"+hit+"\t"+ns);
    }
}
