package dev.minime.core;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.security.MessageDigest;

/** Frozen genre/source probes; only asset loading changes between variants. */
public final class PackLoadingAudit {
    private static Reader read(Path p)throws IOException {return Files.newBufferedReader(p);}
    public static void main(String[] args)throws Exception {
        Path source=Paths.get("app/src/main/assets"),split=Paths.get("artifacts/mode-mechanism-review/packs");
        String variant=args[0],scope=args[1];
        System.gc();Thread.sleep(100);
        long before=Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory(),start=System.nanoTime();
        PairedForms pairs=!variant.equals("binary") && (variant.equals("combined") || scope.equals("poj"))?PairedForms.read(read(source.resolve("paired-forms.tsv"))):PairedForms.EMPTY;
        AddonDictionary dictionary=variant.equals("binary")?AddonDictionary.readBinary(Files.newInputStream(split.resolve("addon-"+scope+".bin")))
            :AddonDictionary.read(read(variant.equals("combined")?source.resolve("addons.tsv"):split.resolve("addon-"+scope+".tsv")),pairs);
        if(variant.equals("combined") || scope.equals("japanese"))dictionary=AddonDictionary.withJapaneseBasics(dictionary,JapaneseBasics.read(read(source.resolve("japanese-basic.tsv"))));
        long load=System.nanoTime()-start;pairs=null;
        System.gc();Thread.sleep(100);
        long retained=Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory()-before;
        System.out.println("variant="+variant+" scope="+scope+" load_ms="+load/1e6+" retained_bytes="+retained);
        List<String> inputs=Files.readAllLines(Paths.get("docs/input-modes/coverage-inputs.tsv"));
        Set<String> packs=Collections.singleton(scope);
        for(int i=0;i<inputs.size();i+=81)dictionary.lookup(inputs.get(i).split("\t",-1)[4],packs);
        MessageDigest digest=MessageDigest.getInstance("SHA-256");
        try(BufferedWriter output=SpeculationBenchmark.writer(args[2])) {
            for(int row=0;row<inputs.size();row++) {
                String[] fields=inputs.get(row).split("\t",-1);start=System.nanoTime();
                List<Candidate> result=dictionary.lookup(fields[4],packs);long elapsed=System.nanoTime()-start;
                StringBuilder identity=new StringBuilder();
                for(Candidate c:result)identity.append(c.text).append('\t').append(c.score).append('\t').append(c.pack).append('\t')
                    .append(c.incomplete).append('\t').append(c.abbreviated).append('\t').append(c.languageCharacter).append('\t')
                    .append(c.pair==null?"":c.pair.phonetic+"|"+c.pair.han+"|"+c.pair.source).append('\n');
                String signature=HexFormat.of().formatHex(digest.digest(identity.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8)));
                output.write(row+"\t"+fields[0]+"\t"+fields[3]+"\t"+elapsed+"\t"+result.size()+"\t"+signature+"\n");
            }
        }
    }
}
