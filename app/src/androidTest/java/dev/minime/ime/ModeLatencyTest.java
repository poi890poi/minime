package dev.minime.ime;

import android.test.InstrumentationTestCase;
import android.content.Context;
import android.os.*;
import dev.minime.core.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;

/** Last-key dispatch through real core/worker/main callback, without display refresh. */
@SuppressWarnings({"deprecation","unchecked","rawtypes"})
public final class ModeLatencyTest extends InstrumentationTestCase {
    public void testPerModeTypingLatency()throws Exception {
        Context context=getInstrumentation().getTargetContext();
        PhoneticDictionary dictionary=DictionaryRepository.load(context).get(60,TimeUnit.SECONDS);
        AddonDictionary addons=AddonDictionary.combine(AddonRepository.load(context).get(60,TimeUnit.SECONDS),AddonRepository.geography(context).get(60,TimeUnit.SECONDS));
        assertTrue(RimeBackend.load(context).get(60,TimeUnit.SECONDS));
        Class modeType;try{modeType=Class.forName("dev.minime.core.InputMode");}catch(ClassNotFoundException old){modeType=null;}
        final Class type=modeType;
        List<String> modes=type==null?Arrays.asList("legacy-mixed","legacy-english"):Arrays.asList("chinese","english","taiwanese","japanese");
        List<String[]> inputs=new ArrayList<>();
        try(BufferedReader in=new BufferedReader(new InputStreamReader(getInstrumentation().getContext().getAssets().open("latency-inputs.tsv"),StandardCharsets.UTF_8))){String line;int i=0;while((line=in.readLine())!=null)if(i++%48<3)inputs.add(line.split("\t"));}
        Handler main=new Handler(Looper.getMainLooper());
        try(AsyncDecoder decoder=new AsyncDecoder(main);PrintWriter out=new PrintWriter(new File(context.getExternalFilesDir(null),"mode-latency.tsv"),"UTF-8")) {
            decoder.rime(true);out.println("round\tmode\tgroup\tcondition\tquery\tlast_key_us\tcandidates\tpreferred");
            for(int round=-1;round<2;round++)for(String mode:modes)for(String[] p:inputs) {
                CountDownLatch done=new CountDownLatch(1);long[] elapsed={0};int[] counts={0};String[] preferred={""};
                getInstrumentation().runOnMainSync(()-> {
                    try {
                        CompositionEngine engine=new CompositionEngine(new CompositionEngine.Editor(){public void composing(String s){}public void commit(String s){}public void delete(){}public void enter(){}public void finish(){}},Learning.NONE);
                        engine.dictionary(dictionary);engine.start(false,false,false,false,mode.endsWith("english"));
                        engine.addons(addons,new HashSet<>(Arrays.asList("taiwan","geography","poj","japanese")));
                        if(type!=null)CompositionEngine.class.getMethod("switchMode",type,boolean.class).invoke(engine,Enum.valueOf(type,mode.toUpperCase(Locale.ROOT)),false);
                        long[] at={0};Runnable settled=()-> {
                            if(at[0]!=0 && !engine.predictionPending() && engine.raw().equals(p[2]) && done.getCount()!=0) {
                                elapsed[0]=(System.nanoTime()-at[0])/1000;counts[0]=engine.candidates().size();preferred[0]=engine.candidates().get(engine.preferred()).text;done.countDown();
                            }
                        };
                        engine.decoder(decoder,settled);
                        for(int i=0;i<p[2].length();i++){if(i==p[2].length()-1)at[0]=System.nanoTime();engine.type(p[2].charAt(i));}
                        settled.run();
                    }catch(Exception e){throw new RuntimeException(e);}
                });
                assertTrue("Mode prediction timeout",done.await(10,TimeUnit.SECONDS));
                if(round>=0)out.println(round+"\t"+mode+"\t"+String.join("\t",p)+"\t"+elapsed[0]+"\t"+counts[0]+"\t"+preferred[0]);
            }
        }
    }
}
