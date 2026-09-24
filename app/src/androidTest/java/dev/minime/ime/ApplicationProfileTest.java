package dev.minime.ime;

import android.test.InstrumentationTestCase;
import android.os.Debug;
import dev.minime.core.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/** Diagnostic sampling only: no UI/font work, no latency acceptance claims. */
@SuppressWarnings("deprecation")
public final class ApplicationProfileTest extends InstrumentationTestCase {
    private static final class Deferred implements CompositionEngine.Decoder {
        Consumer<List<Candidate>> result;
        public void convert(PhoneticDictionary d,String r,boolean z,String c,Consumer<List<Candidate>> done){result=done;}
        public void query(PhoneticDictionary d,String r,boolean z,String c,boolean p,AddonDictionary a,Set<String> e,Consumer<List<Candidate>> done){result=done;}
    }
    public void testCandidateApplicationProfile()throws Exception {runProfile(true);}
    public void testCandidateApplicationWithoutTracing()throws Exception {runProfile(false);}
    private void runProfile(boolean tracing)throws Exception {
        android.content.Context context=getInstrumentation().getTargetContext();
        context.getSharedPreferences("settings",0).edit().clear().commit();
        context.getSharedPreferences("learning",0).edit().clear().commit();
        PhoneticDictionary dictionary=DictionaryRepository.load(context).get(60,TimeUnit.SECONDS);
        Set<String> packs=new HashSet<>(Arrays.asList("taiwan","geography"));
        AddonDictionary addons=AddonRepository.load(context,packs).get(60,TimeUnit.SECONDS);
        Set<String> prefixes=new LinkedHashSet<>();
        try(BufferedReader in=new BufferedReader(new InputStreamReader(getInstrumentation().getContext().getAssets().open("latency-inputs.tsv"),"UTF-8"))) {
            String line;int row=0,queries=0;
            while((line=in.readLine())!=null && queries<8) {
                String raw=line.split("\t")[2];if(row++%144!=0 || !raw.matches("[a-z]{3,16}"))continue;
                queries++;for(int i=1;i<=raw.length();i++)prefixes.add(raw.substring(0,i));
            }
        }
        File directory=context.getExternalFilesDir(null);
        if(tracing)Debug.startMethodTracingSampling(new File(directory,"application.trace").toString(),32*1024*1024,1000);
        try(PrintWriter out=new PrintWriter(new File(directory,tracing?"application-cost.tsv":"application-untraced.tsv"),"UTF-8")) {
            out.println("query\tround\tapply_ns\tenglish_completion_ns\tcandidates");
            for(String raw:prefixes) {
                List<Candidate> choices=new ArrayList<>(dictionary.convert(raw,false));choices.addAll(addons.lookup(raw,packs));
                for(int round=-1;round<3;round++) {
                    long[] elapsed=new long[2];int[] count={0};
                    getInstrumentation().runOnMainSync(()->{
                        CompositionEngine engine=new CompositionEngine(new CompositionEngine.Editor(){public void composing(String s){}public void commit(String s){}public void delete(){}public void enter(){}public void finish(){}},new LocalLearning(context));
                        Deferred decoder=new Deferred();engine.dictionary(dictionary);engine.start(false,false,false,false);
                        engine.addons(addons,packs);engine.phraseLearning(true);engine.decoder(decoder,()->{});
                        raw.codePoints().forEach(engine::type);assertNotNull(decoder.result);
                        long at=System.nanoTime();decoder.result.accept(new ArrayList<>(choices));elapsed[0]=System.nanoTime()-at;
                        count[0]=engine.candidates().size();
                        at=System.nanoTime();dictionary.englishCompletions(raw,false);elapsed[1]=System.nanoTime()-at;
                    });
                    if(round>=0)out.println(raw+"\t"+round+"\t"+elapsed[0]+"\t"+elapsed[1]+"\t"+count[0]);
                }
            }
        } finally {if(tracing)Debug.stopMethodTracing();}
    }
}
