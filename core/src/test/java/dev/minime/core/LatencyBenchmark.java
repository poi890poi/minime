package dev.minime.core;

import java.io.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

/** Serial, no query cache: components and candidate fingerprints, not UI latency. */
public final class LatencyBenchmark {
    static final Set<String> PACKS=new HashSet<>(Arrays.asList("taiwan","geography","poj","japanese"));
    static String signature(List<Candidate> values) throws Exception {
        MessageDigest digest=MessageDigest.getInstance("SHA-256");
        for(Candidate c:values)digest.update((c.text+"\t"+Double.toHexString(c.score)+"\t"+c.consumed+"\t"+c.composed+"\t"+c.incomplete+"\t"+c.literal+"\t"+c.abbreviated+"\t"+c.supplemental+"\t"+c.reading+"\n").getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(digest.digest());
    }
    public static void main(String[] args)throws Exception {
        Path root=Paths.get(""),assets=root.resolve("app/src/main/assets");
        List<String> inputs=Files.readAllLines(Paths.get(args[0]));int rounds=Integer.parseInt(args[2]);
        long start=System.nanoTime();
        PhoneticDictionary dictionary=PhoneticDictionary.readBinary(Files.newInputStream(root.resolve("app/build/generated/minimeAssets/model.bin")));
        AddonDictionary addons=AddonDictionary.combine(AddonDictionary.read(Files.newBufferedReader(assets.resolve("addons.tsv"))),AddonDictionary.read(Files.newBufferedReader(assets.resolve("geography.tsv"))));
        System.out.println("load_ms="+(System.nanoTime()-start)/1e6);
        try(BufferedWriter out=Files.newBufferedWriter(Paths.get(args[1]))) {
            out.write("round\tgroup\tcondition\tquery\tcore_us\taddons_us\tcore_n\taddons_n\tcore_signature\taddons_signature\n");
            for(int round=-1;round<rounds;round++) {
                for(String line:inputs) {
                    String[] p=line.split("\t");String raw=p[2];
                    long at=System.nanoTime();List<Candidate> core=dictionary.convert(raw,false);long coreUs=(System.nanoTime()-at)/1000;
                    at=System.nanoTime();List<Candidate> optional=addons.lookup(raw,PACKS);long addonUs=(System.nanoTime()-at)/1000;
                    if(round>=0)out.write(round+"\t"+line+"\t"+coreUs+"\t"+addonUs+"\t"+core.size()+"\t"+optional.size()+"\t"+signature(core)+"\t"+signature(optional)+"\n");
                }
                out.flush();System.out.println("round="+round);
            }
        }
    }
}
