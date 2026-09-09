package dev.minime.core;
import java.nio.file.*;
import java.util.*;
import java.io.*;
/** Frozen beginner inventory. Retrieval and annotation availability, not language accuracy. */
public final class TaiwaneseHanAudit {
    public static void main(String[] args)throws Exception {
        PairedForms pairs=PairedForms.read(Files.newBufferedReader(Paths.get(args[0])));
        AddonDictionary addon=AddonDictionary.read(Files.newBufferedReader(Paths.get("app/src/main/assets/addons.tsv")),pairs);
        try(BufferedWriter out=Files.newBufferedWriter(Paths.get(args[2]))) {
            out.write("source\tcondition\tquery\toutput\trank\than\telapsed_ns\n");
            for(String line:Files.readAllLines(Paths.get(args[1]))) {
                String[] p=line.split("\t",-1);long at=System.nanoTime();List<Candidate> found=addon.lookup(p[2],Collections.singleton("poj"));long elapsed=System.nanoTime()-at;
                int rank=-1;String han="";
                for(int i=0;i<found.size();i++)if(found.get(i).text.equals(p[3])) {rank=i+1;han=found.get(i).alternateText();break;}
                out.write(line+"\t"+rank+"\t"+han+"\t"+elapsed+"\n");
            }
        }
    }
}
