package dev.minime.core;

import java.nio.file.*;

/** Build-time tool: freezes attributed TSV data and precomputed indexes. */
public final class CompileModel {
    public static void main(String[] args)throws Exception {
        Path source=Paths.get("app/src/main/assets");
        PhoneticDictionary dictionary=PhoneticDictionary.load(Files.newBufferedReader(source.resolve("zh_tw.tsv")),Files.newBufferedReader(source.resolve("en_us.tsv")),Files.newBufferedReader(source.resolve("syllables.tsv")),Files.newBufferedReader(source.resolve("context.tsv")));
        Path output=Paths.get(args[0]);Files.createDirectories(output.toAbsolutePath().getParent());dictionary.writeBinary(Files.newOutputStream(output));
        long start=System.nanoTime();PhoneticDictionary binary=PhoneticDictionary.readBinary(Files.newInputStream(output));
        System.out.println("Binary load ms="+(System.nanoTime()-start)/1000000+" bytes="+Files.size(output));
        for(String raw:new String[]{"nh","srufa","wxsrf","mingtian","womenxyaoxuexi","s".repeat(32),"ㄋㄧˇㄏㄠˇ"}) {
            boolean zh=raw.codePointAt(0)>127;
            for(String context:new String[]{"","今天"}) {
                var before=dictionary.convert(raw,zh,context);var after=binary.convert(raw,zh,context);
                if(before.size()!=after.size())throw new AssertionError("Binary count "+raw);
                for(int i=0;i<before.size();i++)if(!before.get(i).text.equals(after.get(i).text) || before.get(i).score!=after.get(i).score)throw new AssertionError("Binary parity "+raw);
            }
        }
        System.out.println("Binary query parity PASS");
        java.security.MessageDigest digest=java.security.MessageDigest.getInstance("SHA-256");
        StringBuilder manifest=new StringBuilder("{\n  \"format\": 1,\n  \"bytes\": "+Files.size(output)+",\n  \"sha256\": \""+java.util.HexFormat.of().formatHex(digest.digest(Files.readAllBytes(output)))+"\",\n  \"sources\": {");
        int i=0;for(String name:new String[]{"zh_tw.tsv","en_us.tsv","syllables.tsv","context.tsv"}) {
            if(i++>0)manifest.append(',');manifest.append("\n    \"").append(name).append("\": \"").append(java.util.HexFormat.of().formatHex(digest.digest(Files.readAllBytes(source.resolve(name))))).append('"');
        }
        manifest.append("\n  }\n}\n");Files.writeString(output.resolveSibling("model-report.json"),manifest.toString());
        CompileAddonPacks.partition(source.resolve("addons.tsv"),output.toAbsolutePath().getParent());
    }
}
