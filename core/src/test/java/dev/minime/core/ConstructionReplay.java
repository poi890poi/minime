package dev.minime.core;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.function.Consumer;
import java.util.zip.GZIPInputStream;

/** Replay frozen native telemetry through the real Java merge/engine. Labels
 * stay in this evaluator and never enter the decoder or candidate policy. */
public final class ConstructionReplay {
    static final class Editor implements CompositionEngine.Editor {
        String output="";
        public void composing(String s){} public void commit(String s){output+=s;}
        public void delete(){} public void enter(){} public void finish(){}
    }
    static final class Decoder implements CompositionEngine.Decoder {
        Consumer<List<Candidate>> pending;String raw,context;boolean bpmf;PhoneticDictionary dictionary;
        public void convert(PhoneticDictionary d,String r,boolean b,String c,Consumer<List<Candidate>> result) {
            dictionary=d;raw=r;bpmf=b;context=c;pending=result;
        }
        void flush(List<Candidate> nativeChoices) {
            if(pending!=null)pending.accept(CandidateMerge.merge(nativeChoices,dictionary.convert(raw,bpmf,context)));
        }
    }
    public static void main(String[] args) throws Exception {
        Path assets=Paths.get("app/src/main/assets");
        PhoneticDictionary d=PhoneticDictionary.load(Files.newBufferedReader(assets.resolve("zh_tw.tsv")),Files.newBufferedReader(assets.resolve("en_us.tsv")),Files.newBufferedReader(assets.resolve("syllables.tsv")),Files.newBufferedReader(assets.resolve("context.tsv")));
        d.englishSpelling(Files.newBufferedReader(assets.resolve("en_spelling.tsv")));
        AddonDictionary addons=AddonDictionary.combine(AddonDictionary.read(Files.newBufferedReader(assets.resolve("addons.tsv"))),AddonDictionary.read(Files.newBufferedReader(assets.resolve("geography.tsv"))));
        Set<String> packs=new HashSet<>(Arrays.asList("taiwan","geography"));int count=0;
        try(BufferedReader in=new BufferedReader(new InputStreamReader(new GZIPInputStream(Files.newInputStream(Paths.get(args[0]))),StandardCharsets.UTF_8));BufferedWriter out=Files.newBufferedWriter(Paths.get(args[1]),StandardCharsets.UTF_8)) {
            String row;
            while((row=in.readLine())!=null) {
                String[] fields=row.split("\t",-1);String raw=fields[2],target=fields[3];List<Candidate> nativeChoices=new ArrayList<>();
                while(!(row=in.readLine()).equals("END")) {
                    Candidate candidate=NativeCandidateCodec.decode(raw,row,nativeChoices.size());
                    if(candidate==null)throw new IOException("Invalid frozen native record");
                    // Reproduce the old Android metadata loss for baseline only.
                    if(Boolean.getBoolean("minime.replayLegacyOrigin"))candidate=new Candidate(candidate.text,false,candidate.score,candidate.consumed);
                    nativeChoices.add(candidate);
                }
                for(boolean optional:new boolean[]{false,true}) {
                    Editor editor=new Editor();Decoder decoder=new Decoder();CompositionEngine engine=new CompositionEngine(editor,Learning.NONE);
                    engine.dictionary(d);engine.start(false,false,false,false);engine.decoder(decoder,()->{});
                    if(optional)engine.addons(addons,packs);
                    raw.codePoints().forEach(engine::type);long before=System.nanoTime();decoder.flush(nativeChoices);long ns=System.nanoTime()-before;
                    int rank=0;for(int i=1;i<engine.candidates().size();i++)if(engine.candidates().get(i).text.equals(target)){rank=i;break;}
                    Candidate chosen=engine.candidates().get(engine.preferred());engine.space();
                    out.write(fields[0]+"\t"+fields[1]+"\t"+optional+"\t"+rank+"\t"+chosen.literal+"\t"+chosen.composed+"\t"+editor.output.equals(target)+"\t"+ns+"\t"+chosen.text);out.newLine();
                }
                if(++count%1000==0){out.flush();System.out.println("Replayed "+count);}
            }
        }
    }
}
