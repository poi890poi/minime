package dev.minime.core;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Decoder sees only the syllable input list, never independent glyph frequencies. */
public final class GlyphRankingProbe {
    static void write(BufferedWriter out,String raw,String mode,List<Candidate> values)throws Exception {
        List<String> rows=new ArrayList<>();
        for(Candidate c:values)rows.add("["+SpeculationBenchmark.json(c.text)+","+c.score+","+c.consumed+","+SpeculationBenchmark.composed(c)+","+c.supplemental+"]");
        out.write("{\"input\":"+SpeculationBenchmark.json(raw)+",\"mode\":"+SpeculationBenchmark.json(mode)+",\"candidates\":["+String.join(",",rows)+"]}\n");
    }
    @SuppressWarnings("unchecked")
    static List<Candidate> merge(List<Candidate> primary,List<Candidate> fallback)throws Exception {
        try {return (List<Candidate>)Class.forName("dev.minime.core.CandidateMerge").getMethod("merge",List.class,List.class).invoke(null,primary,fallback);}
        catch(ClassNotFoundException baseline) {
            List<Candidate> result=new ArrayList<>(primary);Set<String> seen=new HashSet<>();for(Candidate c:result)seen.add(c.text);
            for(Candidate c:fallback)if(seen.add(c.text))result.add(c);return result;
        }
    }
    public static void main(String[] args)throws Exception {
        PhoneticDictionary dictionary=PhoneticDictionary.readBinary(Files.newInputStream(Paths.get("app/build/generated/minimeAssets/model.bin")));
        dictionary.englishSpelling(Files.newBufferedReader(Paths.get("app/src/main/assets/en_spelling.tsv")));
        AddonDictionary addons=AddonDictionary.combine(AddonDictionary.read(Files.newBufferedReader(Paths.get("app/src/main/assets/addons.tsv"))),AddonDictionary.read(Files.newBufferedReader(Paths.get("app/src/main/assets/geography.tsv"))));
        CompositionEngine.Editor editor=new CompositionEngine.Editor(){public void composing(String s){}public void commit(String s){}public void delete(){}public void enter(){}public void finish(){}};
        try(SpeculationBenchmark.Native nativeRime=new SpeculationBenchmark.Native(4);BufferedWriter out=Files.newBufferedWriter(Paths.get(args[0]))) {
            for(String raw:Files.readAllLines(Paths.get("docs/glyph-ranking/inputs.txt"))) {
                List<Candidate> core=dictionary.convert(raw,false),nativeValues=nativeRime.query(raw),merged=merge(nativeValues,core);
                write(out,raw,"core",core);write(out,raw,"native",nativeValues);write(out,raw,"merged",merged);
                for(boolean packs:new boolean[]{false,true}) {
                    CompositionEngine engine=new CompositionEngine(editor,Learning.NONE);engine.dictionary(dictionary);engine.start(false,false,false,false);
                    if(packs)engine.addons(addons,new HashSet<>(Arrays.asList("taiwan","geography","poj","japanese")));
                    engine.decoder((d,r,b,c,done)->done.accept(merged),()->{});raw.codePoints().forEach(engine::type);
                    write(out,raw,packs?"all-packs":"no-packs",engine.candidates().subList(1,engine.candidates().size()));
                }
            }
        }
    }
}
