package dev.minime.core;

import java.io.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

/** Source-wide eligible words, one fixed prefix position; dispatch parity, not accuracy. */
public final class ExplicitLatinSourceAudit {
    private static final class Result {
        final Map<String,Double> values=new TreeMap<>();String space;
    }
    private static Result completions(PhoneticDictionary d,String raw,InputMode mode) {
        Regression.Editor e=new Regression.Editor();CompositionEngine c=new CompositionEngine(e,Learning.NONE);
        c.dictionary(d);c.start(false,false,false,false,mode.english());c.switchMode(mode,false);
        raw.codePoints().forEach(c::type);Result result=new Result();
        for(Candidate value:c.candidates())if(value.literal && value.incomplete)result.values.put(value.text,value.score);
        c.space();result.space=e.text;
        return result;
    }
    public static void main(String[] args)throws Exception {
        Path source=Paths.get("app/src/main/assets/en_us.tsv");Set<String> queries=new TreeSet<>();int eligible=0;
        for(String line:Files.readAllLines(source)) {
            String word=line.split("\t")[0];
            if(word.length()<3 || !word.matches("(?:[A-Z][a-z]+|[A-Z]{3,})"))continue;
            eligible++;String prefix=word.substring(0,Math.max(2,(word.length()+1)/2)).toLowerCase(Locale.ROOT);
            queries.add(Character.toUpperCase(prefix.charAt(0))+prefix.substring(1));queries.add(prefix.toUpperCase(Locale.ROOT));
        }
        PhoneticDictionary d=PhoneticDictionary.load(new StringReader(""),Files.newBufferedReader(source),new StringReader(""));
        int episodes=0,words=0,mismatches=0,nonliteralSpaces=0;long start=System.nanoTime();
        MessageDigest spaces=MessageDigest.getInstance("SHA-256"),inputs=MessageDigest.getInstance("SHA-256");
        for(String raw:queries) {
            inputs.update((raw+"\n").getBytes(StandardCharsets.UTF_8));
            Result reference=completions(d,raw,InputMode.ENGLISH);words+=reference.values.size();
            if(reference.values.isEmpty())throw new AssertionError("Eligible source prefix has no completion");
            for(InputMode mode:InputMode.values())if(mode.englishEnabled()) {
                Result observed=mode.english()?reference:completions(d,raw,mode);episodes++;
                if(!reference.values.equals(observed.values))mismatches++;
                if(!observed.space.equals(raw+" "))nonliteralSpaces++;
                spaces.update((mode.id+"\t"+raw+"\t"+observed.space+"\n").getBytes(StandardCharsets.UTF_8));
            }
        }
        String report="{\"eligible_source_entries\":"+eligible+",\"unique_cased_prefixes\":"+queries.size()+",\"mode_episodes\":"+episodes+",\"reference_completion_identities\":"+words+",\"mode_inventory_mismatches\":"+mismatches+",\"nonliteral_space_results\":"+nonliteralSpaces+",\"input_sha256\":\""+HexFormat.of().formatHex(inputs.digest())+"\",\"space_sha256\":\""+HexFormat.of().formatHex(spaces.digest())+"\",\"elapsed_ms\":"+(System.nanoTime()-start)/1000000+",\"scope\":\"One half-prefix per eligible source entry, title and caps; engine inventory/score comparison across English-enabled modes, baseline/trial Space hash; not language accuracy or device latency.\"}\n";
        Files.writeString(Paths.get(args[0]),report);System.out.print(report);
    }
}
