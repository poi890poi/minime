package dev.minime.core;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.function.Consumer;

/** Actual core dispatch/delivery; deferred worker avoids conflating those stages. */
public final class PipelineBenchmark {
    static final class Deferred implements CompositionEngine.Decoder {
        Runnable pending;
        public void convert(PhoneticDictionary d,String raw,boolean z,String context,Consumer<List<Candidate>> done){throw new AssertionError();}
        public void query(PhoneticDictionary d,String raw,boolean z,String context,boolean phonetic,AddonDictionary a,Set<String> packs,Consumer<List<Candidate>> done) {
            pending=()->done.accept(a.lookup(raw,packs));
        }
        public void cancel(){pending=null;}
    }
    public static void main(String[] args)throws Exception {
        Path assets=Paths.get("app/build/generated/minimeAssets");
        PhoneticDictionary dictionary=PhoneticDictionary.readBinary(Files.newInputStream(assets.resolve("model.bin")));
        dictionary.englishSpelling(Files.newBufferedReader(assets.resolve("en_spelling.tsv")));
        AddonDictionary addons=AddonDictionary.withJapaneseBasics(AddonDictionary.readBinary(Files.newInputStream(assets.resolve("addon-japanese.bin"))),JapaneseBasics.read(Files.newBufferedReader(assets.resolve("japanese-basic.tsv"))));
        List<String> clauses=Files.readAllLines(Paths.get(args[0]));
        try(PrintWriter out=new PrintWriter(args[1],"UTF-8")) {
            out.println("cycle\tclause\tdispatch_ns\tlookup_delivery_ns\tdigest\tpreferred");
            for(int cycle=0;cycle<Integer.parseInt(args[2]);cycle++)for(int i=0;i<clauses.size();i++) {
                CompositionEngine engine=new CompositionEngine(new CompositionEngine.Editor(){
                    public void composing(String s){}public void commit(String s){}public void delete(){}public void enter(){}public void finish(){}
                },Learning.NONE);
                engine.dictionary(dictionary);engine.start(false,false,false,false);
                engine.addons(addons,Collections.singleton("japanese"));engine.switchMode(InputMode.JAPANESE_ENGLISH,false);
                Deferred decoder=new Deferred();engine.decoder(decoder,()->{});
                long at=System.nanoTime();for(int cp:clauses.get(i).codePoints().toArray())engine.type(cp);long dispatch=System.nanoTime()-at;
                at=System.nanoTime();decoder.pending.run();long delivery=System.nanoTime()-at;
                out.println(cycle+"\t"+i+"\t"+dispatch+"\t"+delivery+"\t"+LookupBenchmark.digest(engine.candidates())+"\t"+engine.preferred());
            }
        }
    }
}
