package dev.minime.core;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Exhaustive complete source readings and frozen partial probes; no accuracy claim. */
public final class JapaneseCoverageAudit {
    public static void main(String[] args)throws Exception {
        long at=System.nanoTime();AddonDictionary addon=AddonDictionary.read(Files.newBufferedReader(Paths.get(args[0])));
        if(!args[1].equals("-"))addon=AddonDictionary.withJapaneseBasics(addon,JapaneseBasics.read(Files.newBufferedReader(Paths.get(args[1]))));
        System.out.println("load_ms="+(System.nanoTime()-at)/1e6);
        Set<String> packs=Collections.singleton("japanese");int count=0;
        try(BufferedWriter out=Files.newBufferedWriter(Paths.get(args[3]))) {
            out.write("condition\tid\treading\tinput\ttarget\trank\tchoices\tlookup_ns\n");
            for(String row:Files.readAllLines(Paths.get(args[2]))) {
                String[] p=row.split("\t",-1);at=System.nanoTime();List<Candidate> candidates=addon.lookup(p[3],packs);long ns=System.nanoTime()-at;int rank=0;
                for(int i=0;i<candidates.size();i++)if(candidates.get(i).text.equals(p[4])) {rank=i+1;break;}
                out.write(row+"\t"+rank+"\t"+candidates.size()+"\t"+ns+"\n");count++;
            }
        }
        System.out.println("PASS frozen source queries="+count);
    }
}
