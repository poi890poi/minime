package dev.minime.core;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Same frozen queries on old/new assets, including legacy cross-pack collisions. */
public final class EntityLookupBenchmark {
    public static void main(String[] args)throws Exception {
        long start=System.nanoTime();AddonDictionary d=AddonDictionary.combine(AddonDictionary.read(Files.newBufferedReader(Paths.get(args[0]))),AddonDictionary.read(Files.newBufferedReader(Paths.get("app/src/main/assets/geography.tsv"))));long load=System.nanoTime()-start;
        Map<String,List<String[]>> queries=new TreeMap<>();for(String line:Files.readAllLines(Paths.get("docs/taiwan-quality/entity-lookup-inputs.tsv"))) {String[] p=line.split("\t");queries.computeIfAbsent(p[2],k->new ArrayList<>()).add(p);}
        if(args.length>2) {int modulus=Integer.parseInt(args[2]);queries.keySet().removeIf(key->Math.floorMod(key.hashCode(),modulus)!=0);}
        Set<String> all=new HashSet<>(Arrays.asList("taiwan","geography","poj","japanese"));Map<String,int[]> stats=new TreeMap<>();List<Long> micros=new ArrayList<>();int count=0;
        try(BufferedWriter out=Files.newBufferedWriter(Paths.get(args[1]+"-ranks.tsv"))) {
            for(Map.Entry<String,List<String[]>> q:queries.entrySet()) {
                long at=System.nanoTime();List<Candidate> found=d.lookup(q.getKey(),all);micros.add((System.nanoTime()-at)/1000);
                Map<String,List<Candidate>> isolated=new HashMap<>();
                for(String[] p:q.getValue()) {
                    int rank=AddonMatchingBenchmark.rank(found,p[3]),single=AddonMatchingBenchmark.rank(isolated.computeIfAbsent(p[0],pack->d.lookup(q.getKey(),Collections.singleton(pack))),p[3]);
                    int[] s=stats.computeIfAbsent(p[0]+"/"+p[1],k->new int[4]);s[0]++;if(rank==1)s[1]++;if(rank>0)s[2]++;if(single>0)s[3]++;
                    out.write(String.join("\t",p)+"\t"+rank+"\t"+single+"\n");
                }
                if(++count%5000==0){out.flush();System.out.println(count+"/"+queries.size());}
            }
        }
        Collections.sort(micros);List<String> groups=new ArrayList<>();stats.forEach((k,v)->groups.add(SpeculationBenchmark.json(k)+":{\"n\":"+v[0]+",\"top1\":"+v[1]+",\"top8\":"+v[2]+",\"isolated_top8\":"+v[3]+"}"));
        String report="{\"queries\":"+count+",\"load_ms\":"+load/1e6+",\"p50_us\":"+micros.get(count/2)+",\"p95_us\":"+micros.get(count*95/100)+",\"max_us\":"+micros.get(count-1)+",\"groups\":{"+String.join(",",groups)+"}}\n";
        Files.writeString(Paths.get(args[1]+"-summary.json"),report);System.out.println(report);
    }
}
