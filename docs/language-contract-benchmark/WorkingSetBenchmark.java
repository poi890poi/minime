package dev.minime.core;
import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Desktop loading tradeoff; GC-separated retained heap is not Android PSS. */
public final class WorkingSetBenchmark {
    static final Path ASSET=Paths.get("app/build/generated/minimeAssets"),OUT=Paths.get("docs/language-contract-benchmark");
    static PhoneticDictionary base;
    static Map<String,AddonDictionary> cache=new HashMap<>();
    static long heap()throws Exception {System.gc();Thread.sleep(40);System.gc();return Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory();}
    static AddonDictionary load(String pack)throws Exception {
        AddonDictionary d=AddonDictionary.readBinary(Files.newInputStream(ASSET.resolve("addon-"+pack+".bin")));
        return pack.equals("japanese")?AddonDictionary.withJapaneseBasics(d,JapaneseBasics.read(Files.newBufferedReader(ASSET.resolve("japanese-basic.tsv")))):d;
    }
    public static void main(String[] args)throws Exception {
        String policy=args[0];int pass=Integer.parseInt(args[1]);base=PhoneticDictionary.readBinary(Files.newInputStream(ASSET.resolve("model.bin")));
        long baseBytes=heap();String last="japanese";
        try(PrintWriter out=new PrintWriter(OUT.resolve("working-set-"+policy+"-"+pass+".tsv").toFile(),"UTF-8")){
            out.println("step\tpack\tphase\thit\tload_ns\tretained_addon_heap_bytes\tcached_packs");
            // Two workloads fixed before measurement: normal fast toggle; then focus changes.
            String[] seq={"taiwan","japanese","taiwan","japanese","taiwan","japanese","poj","taiwan","poj","japanese","taiwan","poj","japanese","taiwan","poj"};
            for(int i=0;i<seq.length;i++){
                String p=seq[i];boolean hit=cache.containsKey(p);long at=System.nanoTime();if(!hit)cache.put(p,load(p));long ns=System.nanoTime()-at;
                if(!p.equals("taiwan"))last=p;
                if(policy.equals("warm-pair"))cache.keySet().retainAll(new HashSet<>(Arrays.asList("taiwan",last)));
                out.println(i+"\t"+p+"\t"+(i<6?"fast-toggle":"focus-changes")+"\t"+hit+"\t"+ns+"\t"+(heap()-baseBytes)+"\t"+String.join(",",new TreeSet<>(cache.keySet())));
            }
        }
    }
}
