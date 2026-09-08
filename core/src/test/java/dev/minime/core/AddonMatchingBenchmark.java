package dev.minime.core;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;

/** Exhaustive retrieval and lookup cost; no targets influence runtime queries. */
public final class AddonMatchingBenchmark {
    static final class Stat {
        int n,top1,top8;void add(int rank){n++;if(rank==1)top1++;if(rank>0)top8++;}
        String json(){return "{\"n\":"+n+",\"top1\":"+top1+",\"top8\":"+top8+"}";}
    }
    static int rank(List<Candidate> list,String target){for(int i=0;i<list.size();i++)if(list.get(i).text.equals(target))return i+1;return 0;}
    public static void main(String[] args)throws Exception {
        TreeMap<String,List<String[]>> queries=new TreeMap<>();
        try(BufferedReader in=new BufferedReader(new InputStreamReader(new GZIPInputStream(Files.newInputStream(Paths.get("docs/speculation/addon-inputs.tsv.gz"))),StandardCharsets.UTF_8))) {
            String line;while((line=in.readLine())!=null){String[] p=line.split("\t");queries.computeIfAbsent(p[2],k->new ArrayList<>()).add(p);}
        }
        System.gc();long before=Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory(),start=System.nanoTime();
        AddonDictionary dictionary=AddonDictionary.combine(AddonDictionary.read(Files.newBufferedReader(Paths.get("app/src/main/assets/addons.tsv"))),AddonDictionary.read(Files.newBufferedReader(Paths.get("app/src/main/assets/geography.tsv"))));
        long load=System.nanoTime()-start;System.gc();long retained=Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory()-before;
        Set<String> all=new TreeSet<>(Arrays.asList("taiwan","poj","japanese","geography"));
        Map<String,Stat> stats=new TreeMap<>();List<Long> times=new ArrayList<>();int done=0;
        try(BufferedWriter out=new BufferedWriter(new OutputStreamWriter(new GZIPOutputStream(Files.newOutputStream(Paths.get(args[0]+"-targets.tsv.gz"))),StandardCharsets.UTF_8))) {
            for(Map.Entry<String,List<String[]>> query:queries.entrySet()) {
                long at=System.nanoTime();List<Candidate> matches=dictionary.lookup(query.getKey(),all);times.add((System.nanoTime()-at)/1000);
                Map<String,List<Candidate>> isolated=new HashMap<>();
                for(String[] p:query.getValue()) {
                    int rank=rank(matches,p[3]),single=rank(isolated.computeIfAbsent(p[0],pack->dictionary.lookup(p[2],Collections.singleton(pack))),p[3]);
                    stats.computeIfAbsent(p[0]+"/"+p[1],k->new Stat()).add(rank);
                    stats.computeIfAbsent("isolated/"+p[0]+"/"+p[1],k->new Stat()).add(single);
                    out.write(String.join("\t",p)+"\t"+rank+"\t"+single+"\n");
                }
                if(++done%10000==0)System.out.println(done+"/"+queries.size());
            }
        }
        Collections.sort(times);List<String> groups=new ArrayList<>();stats.forEach((key,value)->groups.add("\""+key+"\":"+value.json()));
        String json="{\"queries\":"+done+",\"load_ms\":"+load/1e6+",\"retained_heap_bytes\":"+retained+",\"query_p50_us\":"+times.get(times.size()/2)+",\"query_p95_us\":"+times.get(times.size()*95/100)+",\"query_max_us\":"+times.get(times.size()-1)+",\"groups\":{"+String.join(",",groups)+"}}\n";
        Files.writeString(Paths.get(args[0]+"-summary.json"),json);System.out.print(json);
    }
}
