package dev.minime.core;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Identical source for old/new classes; uncached dictionary work, not UI latency. */
public final class DictionaryEfficiencyAudit {
    public static void main(String[] args)throws Exception {
        Path assets=Paths.get(args[0]);long start=System.nanoTime();
        PairedForms pairs=PairedForms.read(Files.newBufferedReader(assets.resolve("paired-forms.tsv")));
        AddonDictionary addons=AddonDictionary.combine(AddonDictionary.read(Files.newBufferedReader(assets.resolve("addons.tsv")),pairs),AddonDictionary.read(Files.newBufferedReader(assets.resolve("geography.tsv"))));
        addons=AddonDictionary.withJapaneseBasics(addons,JapaneseBasics.read(Files.newBufferedReader(assets.resolve("japanese-basic.tsv"))));
        long load=System.nanoTime()-start;
        System.gc();Thread.sleep(100);
        long heap=Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory();
        Set<String> all=new HashSet<>(Arrays.asList("taiwan","geography","poj","japanese"));
        List<String[]> inputs=new ArrayList<>();int row=0;
        for(String line:Files.readAllLines(Paths.get("app/src/androidTest/assets/latency-inputs.tsv")))if(row++%48<3)inputs.add(line.split("\t"));
        try(PrintWriter out=new PrintWriter(args[1],"UTF-8")) {
            out.println("round\tmode\tgroup\tcondition\tquery\telapsed_ns\tcandidates");
            for(int round=-1;round<2;round++)for(String id:args[2].split(",")) {
                Set<String> scope=InputMode.fromId(id).packs(all);
                for(String[] p:inputs) {
                    start=System.nanoTime();List<Candidate> found=addons.lookup(p[2],scope);long elapsed=System.nanoTime()-start;
                    if(round>=0)out.println(round+"\t"+id+"\t"+String.join("\t",p)+"\t"+elapsed+"\t"+found.size());
                }
            }
        }
        System.out.println("load_ms="+load/1e6+" retained_heap_bytes="+heap+" queries_per_mode="+inputs.size()*2);
    }
}
