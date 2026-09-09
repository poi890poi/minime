package dev.minime.core;

import java.nio.file.*;
import java.io.*;
import java.util.*;

/** Cached query merge/application timing, isolated from native/dictionary lookup and typing. */
public final class ModePriorityBenchmark {
    public static void main(String[] args) throws Exception {
        Path assets=Paths.get("app/src/main/assets");
        PhoneticDictionary dictionary=PhoneticDictionary.readBinary(Files.newInputStream(Paths.get("app/build/generated/minimeAssets/model.bin")));
        dictionary.englishSpelling(Files.newBufferedReader(assets.resolve("en_spelling.tsv")));
        AddonDictionary addons=AddonDictionary.combine(AddonDictionary.read(Files.newBufferedReader(assets.resolve("addons.tsv"))),AddonDictionary.read(Files.newBufferedReader(assets.resolve("geography.tsv"))));
        List<String> inputs=Files.readAllLines(Paths.get("docs/input-modes/coverage-inputs.tsv"));
        try(SpeculationBenchmark.Native nativeRime=new SpeculationBenchmark.Native(92);
            BufferedWriter out=Files.newBufferedWriter(Paths.get(args[0]))) {
            ModeCoverageBenchmark.Decoder decoder=new ModeCoverageBenchmark.Decoder(nativeRime);
            out.write("pass\trow\tmode\tgroup\tcondition\tapply_ns\n");long checksum=0;
            // Full warmup fills every lookup cache. Measured passes include merge,
            // candidate ordering and callback dispatch, not input or native lookup.
            for(int pass=-1;pass<3;pass++) {
                int row=0;
                for(String line:inputs) {
                    String[] p=line.split("\t",-1);
                    for(String mode:Arrays.asList("taiwanese","japanese")) {
                        CompositionEngine c=new CompositionEngine(new ModeCoverageBenchmark.Editor(),Learning.NONE);
                        c.dictionary(dictionary);c.start(false,false,false,false);
                        c.decoder(decoder,()->{});c.addons(addons,ModeCoverageBenchmark.packs(mode));ModeCoverageBenchmark.mode(c,mode);
                        if(!p[6].isEmpty())for(String previous:p[6].split(" ")) {previous.codePoints().forEach(c::type);c.select(0);c.space();}
                        p[4].codePoints().forEach(c::type);
                        long at=System.nanoTime();decoder.flush();long elapsed=System.nanoTime()-at;
                        checksum+=c.candidates().size();
                        if(pass>=0)out.write(pass+"\t"+row+"\t"+mode+"\t"+p[0]+"\t"+p[3]+"\t"+elapsed+"\n");
                    }
                    row++;
                }
                out.flush();System.out.println("pass="+pass+" rows="+row+" checksum="+checksum);
            }
        }
    }
}
