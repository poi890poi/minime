package dev.minime.core;

import java.io.*;
import java.nio.file.*;
import java.lang.reflect.*;
import java.util.*;

/** Interleaved one-variable comparison; reflection is confined to this benchmark. */
public final class BranchEfficiencyAudit {
    static final Map<Object,Object[]> indexes=new IdentityHashMap<>();
    static Field branch;
    static void indexes(AddonDictionary addon)throws Exception {
        Field units=AddonDictionary.class.getDeclaredField("units");units.setAccessible(true);
        for(Object index:((Map<?,?>)units.get(addon)).values()) {
            int[][] original=(int[][])branch.get(index);
            indexes.put(index,new Object[]{new int[original.length][],original});
        }
        for(String name:Arrays.asList("first","second")) {
            Field field=AddonDictionary.class.getDeclaredField(name);field.setAccessible(true);
            AddonDictionary child=(AddonDictionary)field.get(addon);if(child!=null)indexes(child);
        }
    }
    static void strategy(int choice)throws Exception {for(Map.Entry<Object,Object[]> entry:indexes.entrySet())branch.set(entry.getKey(),entry.getValue()[choice]);}
    static List<String> output(List<Candidate> found) {
        List<String> result=new ArrayList<>();
        for(Candidate c:found)result.add(c.text+"\t"+c.score+"\t"+c.pack+"\t"+c.incomplete+"\t"+c.abbreviated+"\t"+c.consumed+"\t"+c.alternateText());
        return result;
    }
    public static void main(String[] args)throws Exception {
        branch=ReadingUnitIndex.class.getDeclaredField("branches");branch.setAccessible(true);
        Path assets=Paths.get("app/src/main/assets");
        AddonDictionary addon=AddonDictionary.combine(AddonDictionary.read(Files.newBufferedReader(assets.resolve("addons.tsv"))),AddonDictionary.read(Files.newBufferedReader(assets.resolve("geography.tsv"))));
        addon=AddonDictionary.withJapaneseBasics(addon,JapaneseBasics.read(Files.newBufferedReader(assets.resolve("japanese-basic.tsv"))));
        indexes(addon);
        Set<String> all=new HashSet<>(Arrays.asList("taiwan","geography","poj","japanese"));
        List<String[]> inputs=new ArrayList<>();int row=0;
        for(String line:Files.readAllLines(Paths.get("app/src/androidTest/assets/latency-inputs.tsv")))if(row++%48<3)inputs.add(line.split("\t"));
        try(PrintWriter out=new PrintWriter(args[0],"UTF-8")) {
            out.println("round\tmode\tstrategy\tgroup\tcondition\tquery\telapsed_ns");
            for(int round=-2;round<4;round++)for(InputMode mode:InputMode.values()) {
                row=0;
                for(String[] input:inputs) {
                    List<String> reference=null;
                    for(int pass=0;pass<2;pass++) {
                        int chosen=Math.floorMod(round+row+pass,2);strategy(chosen);
                        long start=System.nanoTime();List<Candidate> found=addon.lookup(input[2],mode.packs(all));long elapsed=System.nanoTime()-start;
                        List<String> values=output(found);
                        if(reference==null)reference=values;else if(!reference.equals(values))throw new AssertionError("Branch parity: "+mode+" / "+input[2]);
                        if(round>=0)out.println(round+"\t"+mode.id+"\t"+(chosen==0?"linear":"indexed")+"\t"+String.join("\t",input)+"\t"+elapsed);
                    }
                    row++;
                }
            }
        } finally {strategy(1);}
        System.out.println("PASS interleaved branch/linear output parity, 372 measured queries per strategy per mode");
    }
}
