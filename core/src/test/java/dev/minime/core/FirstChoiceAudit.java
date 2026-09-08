package dev.minime.core;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.function.Consumer;
import java.util.zip.GZIPOutputStream;

/** Fresh editor, final revision flush, real acceptance. No reference labels in decoder. */
public final class FirstChoiceAudit {
    static class Editor implements CompositionEngine.Editor {
        String text=""; public void composing(String s){}public void commit(String s){text+=s;}
        public void delete(){}public void enter(){}public void finish(){}
    }
    static String json(String s){return SpeculationBenchmark.json(s);}
    static void write(BufferedWriter out,String input,String mode,List<Candidate> values,String preferred,String space)throws Exception {
        List<String> rows=new ArrayList<>();
        for(Candidate c:values)rows.add("["+json(c.text)+","+c.score+","+c.consumed+","+c.composed+","+c.supplemental+","+c.incomplete+"]");
        out.write("{\"input\":"+json(input)+",\"mode\":"+json(mode)+",\"preferred\":"+json(preferred)+",\"space\":"+json(space)+",\"candidates\":["+String.join(",",rows)+"]}\n");
    }
    static class Decoder implements CompositionEngine.Decoder {
        Consumer<List<Candidate>> result;AddonDictionary addon;Set<String> packs;String raw;
        public void convert(PhoneticDictionary d,String r,boolean b,String c,Consumer<List<Candidate>> done){throw new AssertionError("combined query required");}
        public void query(PhoneticDictionary d,String r,boolean b,String c,boolean phonetic,AddonDictionary a,Set<String> p,Consumer<List<Candidate>> done){result=done;addon=a;packs=p;raw=r;}
        void flush(List<Candidate> merged,List<Candidate> optional){List<Candidate> found=new ArrayList<>(merged);found.addAll(optional);result.accept(found);}
    }
    public static void main(String[] args)throws Exception {
        PhoneticDictionary dictionary=PhoneticDictionary.readBinary(Files.newInputStream(Paths.get("app/build/generated/minimeAssets/model.bin")));
        dictionary.englishSpelling(Files.newBufferedReader(Paths.get("app/src/main/assets/en_spelling.tsv")));
        AddonDictionary language=AddonDictionary.read(Files.newBufferedReader(Paths.get(args.length>2?args[2]:"app/src/main/assets/addons.tsv")));
        AddonDictionary addons=AddonDictionary.combine(language,AddonDictionary.read(Files.newBufferedReader(Paths.get("app/src/main/assets/geography.tsv"))));
        Set<String> all=new HashSet<>(Arrays.asList("taiwan","geography","poj","japanese")),taiwan=Collections.singleton("taiwan");
        try(SpeculationBenchmark.Native nativeRime=new SpeculationBenchmark.Native(64);BufferedWriter out=new BufferedWriter(new OutputStreamWriter(new GZIPOutputStream(Files.newOutputStream(Paths.get(args[1]))),StandardCharsets.UTF_8))) {
            int count=0;
            for(String line:Files.readAllLines(Paths.get(args[0]))) {
                String raw=line.split("\t")[0];List<Candidate> core=dictionary.convert(raw,false),nativeValues=nativeRime.query(raw),merged=CandidateMerge.merge(nativeValues,core);
                write(out,raw,"core",core,"","");write(out,raw,"native",nativeValues,"","");write(out,raw,"merged",merged,"","");
                List<Candidate> tw=addons.lookup(raw,taiwan),every=addons.lookup(raw,all);
                for(String mode:Arrays.asList("no-packs","taiwan","all-packs")) {
                    Editor editor=new Editor();CompositionEngine engine=new CompositionEngine(editor,Learning.NONE);engine.dictionary(dictionary);engine.start(false,false,false,false);
                    Set<String> packs=mode.equals("taiwan")?taiwan:mode.equals("all-packs")?all:Collections.emptySet();engine.addons(addons,packs);
                    Decoder decoder=new Decoder();engine.decoder(decoder,()->{});raw.codePoints().forEach(engine::type);
                    decoder.flush(merged,mode.equals("taiwan")?tw:mode.equals("all-packs")?every:Collections.emptyList());
                    List<Candidate> candidates=new ArrayList<>(engine.candidates().subList(1,engine.candidates().size()));String preferred=engine.candidates().get(engine.preferred()).text;
                    engine.space();write(out,raw,mode,candidates,preferred,editor.text);
                }
                if(++count%1000==0){out.flush();System.out.println(count+" inputs");}
            }
        }
    }
}
