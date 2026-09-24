package dev.minime.core;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;

/** Serialization parity on all source single-glyph spellings and frozen prefixes. */
public final class ReadingModelBinaryAudit {
    private static String hash(Path path)throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path)));
    }
    private static void equal(List<Candidate> a,List<Candidate> b,String query) {
        if(a.size()!=b.size())throw new AssertionError("Binary count: "+query);
        for(int i=0;i<a.size();i++) {
            Candidate x=a.get(i),y=b.get(i);
            if(!x.text.equals(y.text)||!x.reading.equals(y.reading)||x.score!=y.score||x.consumed!=y.consumed
                ||x.literal!=y.literal||x.incomplete!=y.incomplete||x.abbreviated!=y.abbreviated||x.composed!=y.composed)
                throw new AssertionError("Binary candidate metadata: "+query+" / "+i);
        }
    }
    public static void main(String[] args)throws Exception {
        if(args.length!=3)throw new IllegalArgumentException("before.tsv after.tsv output-directory");
        Path assets=Paths.get("app/src/main/assets"),output=Paths.get(args[2]);Files.createDirectories(output);
        Set<String> queries=new TreeSet<>(Files.readAllLines(Paths.get("docs/glyph-ranking/inputs.txt")));
        for(String line:Files.readAllLines(Paths.get(args[0]),StandardCharsets.UTF_8)) {
            String[] row=line.split("\t");
            if(ReadingPriorAudit.han(row[2])) {queries.add(row[0]);queries.add(row[4]);}
        }
        for(int side=0;side<2;side++) {
            Path source=Paths.get(args[side]);
            PhoneticDictionary dictionary=PhoneticDictionary.load(Files.newBufferedReader(source),
                Files.newBufferedReader(assets.resolve("en_us.tsv")),Files.newBufferedReader(assets.resolve("syllables.tsv")),
                Files.newBufferedReader(assets.resolve("context.tsv")));
            Path binary=output.resolve(side==0?"before.bin":"after.bin");
            dictionary.writeBinary(Files.newOutputStream(binary));
            long started=System.nanoTime();
            PhoneticDictionary restored=PhoneticDictionary.readBinary(Files.newInputStream(binary));
            long load=System.nanoTime()-started;
            for(String raw:queries) {
                boolean zhuyin=raw.codePointAt(0)>127;
                equal(dictionary.convert(raw,zhuyin),restored.convert(raw,zhuyin),raw);
            }
            System.out.println("{\"side\":\""+(side==0?"before":"after")+"\",\"queries\":"+queries.size()
                +",\"bytes\":"+Files.size(binary)+",\"source_sha256\":\""+hash(source)+"\",\"binary_sha256\":\""
                +hash(binary)+"\",\"load_ns_observation_only\":"+load+"}");
        }
    }
}
