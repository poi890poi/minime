package dev.minime.core;
import java.nio.file.*;
import java.util.*;
import java.util.function.Consumer;
/** Reference-guided selections model an ideal user, not human time or corrections. */
public final class NullConstructionEffort {
    static final class Decoder implements CompositionEngine.Decoder {
        DesktopEvaluation.Native nativeRime;PhoneticDictionary d;String raw,context;boolean bpmf;Consumer<List<Candidate>> pending;
        Decoder(DesktopEvaluation.Native n){nativeRime=n;}
        public void convert(PhoneticDictionary a,String r,boolean b,String c,Consumer<List<Candidate>> callback){d=a;raw=r;bpmf=b;context=c;pending=callback;}
        void flush(){if(pending!=null){Consumer<List<Candidate>> done=pending;pending=null;done.accept(CandidateMerge.merge(nativeRime.query(raw),d.convert(raw,bpmf,context)));}}
    }
    public static void main(String[] args)throws Exception {
        Path assets=Paths.get("app/src/main/assets");
        PhoneticDictionary d=PhoneticDictionary.load(Files.newBufferedReader(assets.resolve("zh_tw.tsv")),Files.newBufferedReader(assets.resolve("en_us.tsv")),Files.newBufferedReader(assets.resolve("syllables.tsv")),Files.newBufferedReader(assets.resolve("context.tsv")));
        d.englishSpelling(Files.newBufferedReader(assets.resolve("en_spelling.tsv")));
        AddonDictionary addons=AddonDictionary.combine(AddonDictionary.read(Files.newBufferedReader(assets.resolve("addons.tsv"))),AddonDictionary.read(Files.newBufferedReader(assets.resolve("geography.tsv"))));
        Set<String> packs=new HashSet<>(Arrays.asList("taiwan","geography"));
        try(DesktopEvaluation.Native n=new DesktopEvaluation.Native(args[2],args[3],args[4],args[5]);java.io.BufferedWriter out=Files.newBufferedWriter(Paths.get(args[1]))) {
            for(String row:Files.readAllLines(Paths.get(args[0]))) {
                String[] f=row.split("\t");String target=f[3];
                NullConstructionReplay.Editor editor=new NullConstructionReplay.Editor();Decoder decoder=new Decoder(n);CompositionEngine e=new CompositionEngine(editor,Learning.NONE);
                e.dictionary(d);e.start(false,false,false,false);e.decoder(decoder,()->{});e.addons(addons,packs);f[2].codePoints().forEach(e::type);decoder.flush();
                int selections=0;List<String> trace=new ArrayList<>();
                // Expected text is used only by this ideal-user controller to choose
                // an already visible candidate; it is never passed to a query.
                while(!editor.output.equals(target) && !e.raw().isEmpty() && selections<target.length()+1) {
                    if(!target.startsWith(editor.output))throw new AssertionError("Reference controller diverged");
                    String rest=target.substring(editor.output.length());int index=-1,length=0;
                    for(int i=0;i<e.candidates().size() && i<=8;i++) {
                        Candidate c=e.candidates().get(i);
                        if(rest.startsWith(c.text) && c.text.length()>length && (c.consumed>0 || c.text.equals(rest))){index=i;length=c.text.length();}
                    }
                    if(index<0)break;
                    Candidate chosen=e.candidates().get(index);trace.add(NullConstructionReplay.q(chosen.text));e.select(index);selections++;decoder.flush();
                }
                out.write("{\"role\":"+NullConstructionReplay.q(f[0])+",\"condition\":"+NullConstructionReplay.q(f[1])+",\"raw\":"+NullConstructionReplay.q(f[2])+",\"target\":"+NullConstructionReplay.q(target)+",\"document\":"+NullConstructionReplay.q(f[4])+",\"success\":"+editor.output.equals(target)+",\"selections\":"+selections+",\"remaining\":"+NullConstructionReplay.q(e.raw())+",\"output\":"+NullConstructionReplay.q(editor.output)+",\"trace\":["+String.join(",",trace)+"]}\n");
            }
        }
    }
}
