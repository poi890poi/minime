package dev.minime.core;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.function.Consumer;
import java.util.zip.GZIPInputStream;
/** Labels enter scoring only, never the decoder, ranking or acceptance. */
public final class NullConstructionReplay {
    static String q(String s){return "\""+s.replace("\\","\\\\").replace("\"","\\\"").replace("\n","\\n").replace("\r","\\r").replace("\t","\\t")+"\"";}
    static final class Editor implements CompositionEngine.Editor {
        String output="";public void composing(String s){} public void commit(String s){output+=s;}
        public void delete(){} public void enter(){} public void finish(){}
    }
    static final class Decoder implements CompositionEngine.Decoder {
        Consumer<List<Candidate>> pending;String raw,context;boolean bpmf;PhoneticDictionary d;
        public void convert(PhoneticDictionary a,String r,boolean b,String c,Consumer<List<Candidate>> result){d=a;raw=r;bpmf=b;context=c;pending=result;}
        void flush(List<Candidate> nativeChoices){if(pending!=null){Consumer<List<Candidate>> done=pending;pending=null;done.accept(CandidateMerge.merge(nativeChoices,d.convert(raw,bpmf,context)));}}
    }
    public static void main(String[] args)throws Exception {
        Path assets=Paths.get("app/src/main/assets");
        PhoneticDictionary d=PhoneticDictionary.load(Files.newBufferedReader(assets.resolve("zh_tw.tsv")),Files.newBufferedReader(assets.resolve("en_us.tsv")),Files.newBufferedReader(assets.resolve("syllables.tsv")),Files.newBufferedReader(assets.resolve("context.tsv")));
        d.englishSpelling(Files.newBufferedReader(assets.resolve("en_spelling.tsv")));
        AddonDictionary addons=AddonDictionary.combine(AddonDictionary.read(Files.newBufferedReader(assets.resolve("addons.tsv"))),AddonDictionary.read(Files.newBufferedReader(assets.resolve("geography.tsv"))));
        Set<String> packs=new HashSet<>(Arrays.asList("taiwan","geography"));int count=0;
        try(BufferedReader in=new BufferedReader(new InputStreamReader(new GZIPInputStream(Files.newInputStream(Paths.get(args[0]))),StandardCharsets.UTF_8));BufferedWriter out=Files.newBufferedWriter(Paths.get(args[1]),StandardCharsets.UTF_8)) {
            String row;while((row=in.readLine())!=null){
                String[] f=row.split("\t",-1);String raw=f[2],target=f[3];List<Candidate> nativeChoices=new ArrayList<>();
                while(!(row=in.readLine()).equals("END")){Candidate c=NativeCandidateCodec.decode(raw,row,nativeChoices.size());if(c==null)throw new IOException("Invalid native record");nativeChoices.add(c);}
                for(boolean optional:new boolean[]{false,true}) {
                    Editor editor=new Editor();Decoder decoder=new Decoder();CompositionEngine e=new CompositionEngine(editor,Learning.NONE);
                    e.dictionary(d);e.start(false,false,false,false);e.decoder(decoder,()->{});if(optional)e.addons(addons,packs);
                    raw.codePoints().forEach(e::type);long before=System.nanoTime();decoder.flush(nativeChoices);long ns=System.nanoTime()-before;
                    int rank=0,constructed=0,han=0;List<String> candidates=new ArrayList<>();
                    for(int i=1;i<e.candidates().size();i++){
                        Candidate c=e.candidates().get(i);if(c.text.equals(target) && rank==0)rank=i;if(c.composed)constructed++;
                        if(c.text.codePoints().anyMatch(cp->Character.UnicodeScript.of(cp)==Character.UnicodeScript.HAN))han++;
                        candidates.add("{\"text\":"+q(c.text)+",\"consumed\":"+c.consumed+",\"constructed\":"+c.composed+",\"literal\":"+c.literal+"}");
                    }
                    if(args[2].equals("null") && constructed>0)throw new AssertionError("Null emitted construction: "+raw);
                    int preferred=e.preferred();Candidate chosen=e.candidates().get(preferred);e.space();
                    out.write("{\"condition\":"+q(f[0])+",\"index\":"+f[1]+",\"addons\":"+optional+",\"rank\":"+rank+",\"preferred\":"+preferred+",\"chosen\":"+q(chosen.text)+",\"space\":"+q(editor.output)+",\"remaining\":"+q(e.raw())+",\"hit\":"+editor.output.equals(target)+",\"core_ns\":"+ns+",\"constructed\":"+constructed+",\"han\":"+han+",\"candidates\":["+String.join(",",candidates)+"]}\n");
                }
                if(++count%1000==0){out.flush();System.out.println(args[2]+" replayed "+count);}
            }
        }
    }
}
