package dev.minime.core;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.zip.GZIPInputStream;

/** Frozen source-supported queries; labels are only used after dictionary lookup. */
public final class RudyBoundaryEvaluation {
    public static void main(String[] args)throws Exception {
        System.gc();long heapBefore=Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory(),loadStart=System.nanoTime();
        AddonDictionary dictionary=AddonDictionary.read(Files.newBufferedReader(Paths.get(args[0])));
        long load=System.nanoTime()-loadStart;System.gc();long heap=Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory()-heapBefore;
        Files.writeString(Paths.get(args[2]+".load.json"),"{\"load_ms\":"+load/1e6+",\"approx_retained_heap_bytes\":"+heap+",\"asset_bytes\":"+Files.size(Paths.get(args[0]))+"}\n");
        Map<String,List<Candidate>> cache=new HashMap<>();Map<String,Long> times=new HashMap<>();
        try(BufferedReader in=new BufferedReader(new InputStreamReader(new GZIPInputStream(Files.newInputStream(Paths.get(args[1]))),StandardCharsets.UTF_8));
            BufferedWriter out=Files.newBufferedWriter(Paths.get(args[2]))) {
            out.write("group\tcondition\traw\ttarget\treading\trank\tlookup_ns\toutputs\n");in.readLine();String line;
            while((line=in.readLine())!=null) {
                String[] p=line.split("\t",-1);String raw=p[2];
                if(!cache.containsKey(raw)) {long start=System.nanoTime();cache.put(raw,dictionary.lookup(raw,Collections.singleton("geography")));times.put(raw,System.nanoTime()-start);}
                List<Candidate> values=cache.get(raw);int rank=0;List<String> text=new ArrayList<>();
                for(int i=0;i<values.size();i++){Candidate c=values.get(i);if(c.text.equals(p[3]))rank=i+1;text.add(c.text+":"+c.consumed);}
                out.write(line+"\t"+rank+"\t"+times.get(raw)+"\t"+String.join("|",text)+"\n");
            }
        }
    }
}
