package dev.minime.core;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Packaging only: preserve every admitted row, including duplicates and order. */
public final class CompileAddonPacks {
    public static void main(String[] args)throws Exception {partition(Paths.get(args[0]),Paths.get(args[1]));}
    static void partition(Path source,Path output)throws IOException {
        Files.createDirectories(output);
        Map<String,BufferedWriter> writers=new LinkedHashMap<>();Map<String,Integer> counts=new LinkedHashMap<>();
        try {
            for(String pack:Arrays.asList("taiwan","poj","japanese")) {
                writers.put(pack,Files.newBufferedWriter(output.resolve("addon-"+pack+".tsv")));counts.put(pack,0);
            }
            try(BufferedReader in=Files.newBufferedReader(source)) {
                String row;while((row=in.readLine())!=null) {
                    if(row.isEmpty() || row.startsWith("#"))continue;
                    int tab=row.indexOf('\t');String pack=tab<0?"":row.substring(0,tab);
                    BufferedWriter writer=writers.get(pack);
                    if(writer==null)throw new IOException("Unregistered packaged language: "+pack);
                    writer.write(row);writer.newLine();counts.put(pack,counts.get(pack)+1);
                }
            }
        } finally {for(BufferedWriter writer:writers.values())writer.close();}
        System.out.println("Verbatim optional dictionary partition: "+counts);
        for(String pack:counts.keySet()) {
            PairedForms pairs=pack.equals("poj")?PairedForms.read(Files.newBufferedReader(source.resolveSibling("paired-forms.tsv"))):PairedForms.EMPTY;
            AddonDictionary words=AddonDictionary.read(Files.newBufferedReader(output.resolve("addon-"+pack+".tsv")),pairs);
            Path binary=output.resolve("addon-"+pack+".bin");words.writeBinary(Files.newOutputStream(binary));
            System.out.println("Prebuilt "+pack+" index bytes="+Files.size(binary));
        }
    }
}
