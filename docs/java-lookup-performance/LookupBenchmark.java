package dev.minime.core;

import java.io.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;
import java.lang.management.ManagementFactory;

/** Only raw input and active pack reach lookup; labels remain outside the timer. */
public final class LookupBenchmark {
    static final Path ASSETS=Paths.get("app/build/generated/minimeAssets");
    static String digest(List<Candidate> values)throws Exception {
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();
        DataOutputStream out=new DataOutputStream(bytes);
        for(Candidate c:values) {
            out.writeUTF(c.text);out.writeDouble(c.score);out.writeUTF(c.reading);out.writeInt(c.consumed);
            out.writeBoolean(c.literal);out.writeBoolean(c.supplemental);out.writeBoolean(c.abbreviated);
            out.writeBoolean(c.composed);out.writeBoolean(c.incomplete);out.writeUTF(c.pack);
            out.writeBoolean(c.languageCharacter);out.writeBoolean(c.transliteration);
            out.writeUTF(c.pair==null?"":c.pair.phonetic);out.writeUTF(c.pair==null?"":c.pair.han);
            out.writeUTF(c.pair==null?"":c.pair.source);
        }
        return Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA-256").digest(bytes.toByteArray()));
    }
    public static void main(String[] args)throws Exception {
        Map<String,AddonDictionary> dictionaries=new HashMap<>();
        for(String pack:Arrays.asList("japanese","poj","taiwan")) {
            AddonDictionary d=AddonDictionary.readBinary(Files.newInputStream(ASSETS.resolve("addon-"+pack+".bin")));
            if(pack.equals("japanese"))d=AddonDictionary.withJapaneseBasics(d,JapaneseBasics.read(Files.newBufferedReader(ASSETS.resolve("japanese-basic.tsv"))));
            dictionaries.put(pack,d);
        }
        List<String[]> rows=new ArrayList<>();
        for(String line:Files.readAllLines(Paths.get(args[0]),StandardCharsets.UTF_8))rows.add(line.split("\t",-1));
        com.sun.management.ThreadMXBean bean=(com.sun.management.ThreadMXBean)ManagementFactory.getThreadMXBean();
        bean.setThreadAllocatedMemoryEnabled(true);long thread=Thread.currentThread().getId();
        try(PrintWriter out=new PrintWriter(args[1],"UTF-8")) {
            out.println("pass\trow\tpack\tgroup\tcondition\tns\tallocated_bytes\tcount\tdigest");
            for(int pass=0;pass<3;pass++)for(int i=0;i<rows.size();i++) {
                String[] row=rows.get(i);Set<String> enabled=Collections.singleton(row[0]);
                AddonDictionary d=dictionaries.get(row[0]);long before=bean.getThreadAllocatedBytes(thread),at=System.nanoTime();
                List<Candidate> values=d.lookup(row[4],enabled);
                long ns=System.nanoTime()-at,bytes=bean.getThreadAllocatedBytes(thread)-before;
                out.println(pass+"\t"+i+"\t"+row[0]+"\t"+row[1]+"\t"+row[2]+"\t"+ns+"\t"+bytes+"\t"+values.size()+"\t"+digest(values));
            }
        }
    }
}
