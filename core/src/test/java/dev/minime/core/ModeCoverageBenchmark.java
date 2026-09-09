package dev.minime.core;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.function.Consumer;

/** Paired output audit. Reference targets are never passed to the engine/decoder. */
public final class ModeCoverageBenchmark {
    static final Set<String> ALL=new HashSet<>(Arrays.asList("taiwan","geography","poj","japanese"));
    static String json(String s){return SpeculationBenchmark.json(s);}
    static Set<String> packs(String mode) {
        if(mode.startsWith("legacy-"))return ALL;
        if(mode.equals("english"))return Collections.emptySet();
        Set<String> selected=new HashSet<>(Arrays.asList("taiwan","geography"));
        if(mode.equals("taiwanese"))selected.add("poj");if(mode.equals("japanese"))selected.add("japanese");return selected;
    }
    static final class Editor implements CompositionEngine.Editor {
        String text="";public void composing(String s){}public void commit(String s){text+=s;}
        public void delete(){}public void enter(){}public void finish(){}
    }
    static final class Decoder implements CompositionEngine.Decoder {
        final SpeculationBenchmark.Native nativeRime;
        final Map<String,List<Candidate>> core=new HashMap<>(),nativeCache=new HashMap<>(),optional=new HashMap<>();
        Runnable pending;
        Decoder(SpeculationBenchmark.Native n){nativeRime=n;}
        public void convert(PhoneticDictionary d,String r,boolean z,String c,Consumer<List<Candidate>> result){throw new AssertionError("combined query");}
        public void query(PhoneticDictionary d,String raw,boolean z,String context,boolean phonetic,AddonDictionary a,Set<String> enabled,Consumer<List<Candidate>> result) {
            pending=()-> {
                List<Candidate> found=new ArrayList<>();
                if(phonetic) {
                    List<Candidate> base=core.computeIfAbsent(context+"\t"+raw,k->d.convert(raw,z,context));
                    List<Candidate> nativeChoices=nativeCache.computeIfAbsent(raw,k->{try{return nativeRime.query(raw);}catch(IOException e){throw new UncheckedIOException(e);}});
                    found=CandidateMerge.merge(nativeChoices,base);
                }
                found.addAll(optional.computeIfAbsent(new TreeSet<>(enabled)+"\t"+raw,k->a.lookup(raw,enabled)));
                result.accept(found);
            };
        }
        void flush(){if(pending!=null){Runnable work=pending;pending=null;work.run();}}
    }
    @SuppressWarnings({"unchecked","rawtypes"})
    static void mode(CompositionEngine engine,String id)throws Exception {
        try {
            Class type=Class.forName("dev.minime.core.InputMode");
            Object value=Enum.valueOf(type,id.toUpperCase(Locale.ROOT));
            CompositionEngine.class.getMethod("switchMode",type,boolean.class).invoke(engine,value,false);
        }catch(ClassNotFoundException legacy) {/* Immutable pre-mode baseline classes. */}
    }
    public static void main(String[] args)throws Exception {
        Path assets=Paths.get("app/src/main/assets");
        PhoneticDictionary dictionary=PhoneticDictionary.readBinary(Files.newInputStream(Paths.get("app/build/generated/minimeAssets/model.bin")));
        dictionary.englishSpelling(Files.newBufferedReader(assets.resolve("en_spelling.tsv")));
        AddonDictionary addons=AddonDictionary.combine(AddonDictionary.read(Files.newBufferedReader(assets.resolve("addons.tsv"))),AddonDictionary.read(Files.newBufferedReader(assets.resolve("geography.tsv"))));
        boolean baseline=args[2].equals("baseline");
        List<String> modes=baseline?Arrays.asList("legacy-mixed","legacy-english","chinese","english","taiwanese","japanese"):Arrays.asList("chinese","english","taiwanese","japanese");
        try(SpeculationBenchmark.Native nativeRime=new SpeculationBenchmark.Native(91);BufferedWriter out=SpeculationBenchmark.writer(args[1])) {
            Decoder decoder=new Decoder(nativeRime);int row=0;
            for(String line:Files.readAllLines(Paths.get(args[0]))) {
                String[] p=line.split("\t",-1);String raw=p[4];
                for(String mode:modes) {
                    boolean english=mode.equals("english") || mode.equals("legacy-english");
                    Editor editor=new Editor();CompositionEngine c=new CompositionEngine(editor,Learning.NONE);c.dictionary(dictionary);c.start(false,false,false,false,english);
                    c.decoder(decoder,()->{});c.addons(addons,packs(mode));
                    if(!mode.startsWith("legacy-"))mode(c,mode);
                    // Accepted source-prefix words, same in every run; no target-word oracle.
                    if(!p[6].isEmpty())for(String previous:p[6].split(" ")) {
                        previous.codePoints().forEach(c::type);c.select(0);c.space();
                    }
                    raw.codePoints().forEach(c::type);decoder.flush();
                    List<String> values=new ArrayList<>();
                    for(Candidate v:c.candidates())values.add("["+json(v.text)+","+v.literal+","+v.consumed+","+v.incomplete+","+v.supplemental+"]");
                    out.write("{\"row\":"+row+",\"mode\":"+json(mode)+",\"preferred\":"+c.preferred()+",\"candidates\":["+String.join(",",values)+"]}\n");
                }
                if(++row%500==0){out.flush();System.out.println("rows="+row);}
            }
            System.out.println("PASS rows="+row+" native unique="+decoder.nativeCache.size()+"; caches serve output audit only, not latency measurements");
        }
    }
}
