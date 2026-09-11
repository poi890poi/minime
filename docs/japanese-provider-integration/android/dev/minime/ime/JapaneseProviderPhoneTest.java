package dev.minime.ime;

import android.content.Context;
import android.os.*;
import android.test.InstrumentationTestCase;
import dev.minime.core.*;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/** Instrumented core/worker/JNI admission, not a synthetic language accuracy score. */
@SuppressWarnings("deprecation")
public final class JapaneseProviderPhoneTest extends InstrumentationTestCase {
    public void testNativeParityAndCompositionAdmission()throws Exception {
        Context app=getInstrumentation().getTargetContext(),tests=getInstrumentation().getContext();
        List<JSONObject> stream=new ArrayList<>();LinkedHashMap<String,String> clauses=new LinkedHashMap<>();
        try(BufferedReader in=new BufferedReader(new InputStreamReader(tests.getAssets().open("japanese-evaluation/stream.jsonl"),StandardCharsets.UTF_8))) {
            String line;while((line=in.readLine())!=null){JSONObject row=new JSONObject(line);stream.add(row);clauses.put(row.getString("id"),row.getString("raw"));}
        }
        File report=new File(app.getExternalFilesDir(null),"japanese-provider-phone.jsonl");
        List<Long> warm=new ArrayList<>(),pipeline=new ArrayList<>();int differences=0;
        try(PrintWriter out=new PrintWriter(report,"UTF-8");JapaneseNativeTestProvider nativeProvider=new JapaneseNativeTestProvider(tests,app)) {
            out.println(new JSONObject().put("kind","load").put("native_ns",nativeProvider.loadNs).put("pss_kib",Debug.getPss())
                .put("model",Build.MODEL).put("sdk",Build.VERSION.SDK_INT));out.flush();
            for(int cycle=0;cycle<3;cycle++)for(JSONObject row:stream) {
                long at=System.nanoTime();List<String> choices=nativeProvider.convert(row.getString("kana"));long wall=System.nanoTime()-at,elapsed=nativeProvider.nativeNs();
                List<String> expected=new ArrayList<>();JSONArray a=row.getJSONArray("expected");for(int i=0;i<a.length();i++)expected.add(a.getString(i));
                boolean same=expected.equals(choices);if(!same)differences++;
                if(cycle>0)warm.add(elapsed);
                out.println(new JSONObject().put("kind","native").put("cycle",cycle).put("id",row.getString("id")).put("key",row.getInt("key"))
                    .put("native_ns",elapsed).put("jni_ns",wall).put("equal",same).put("choices",new JSONArray(choices)));
            }
            out.flush();
            PhoneticDictionary dictionary=DictionaryRepository.load(app).get(60,TimeUnit.SECONDS);
            AddonDictionary addons=AddonRepository.load(app,Collections.singleton("japanese")).get(60,TimeUnit.SECONDS);
            Handler main=new Handler(Looper.getMainLooper());AsyncDecoder decoder=new AsyncDecoder(main);
            try {
                getInstrumentation().runOnMainSync(()->decoder.japanese(nativeProvider));
                for(int cycle=0;cycle<3;cycle++)for(Map.Entry<String,String> clause:clauses.entrySet()) {
                    CountDownLatch done=new CountDownLatch(1);AtomicReference<CompositionEngine> active=new AtomicReference<>();
                    AtomicReference<Throwable> error=new AtomicReference<>();long[] at={0},elapsed={0},lastKeyAt={0},lastKeyElapsed={0},dispatch={0};
                    long stageBefore=decoder.stats.nanos[2].get();String[] expected={""},committed={""};int[] count={0};
                    getInstrumentation().runOnMainSync(()-> {
                        CompositionEngine engine=new CompositionEngine(new CompositionEngine.Editor(){
                            public void composing(String s){}public void commit(String s){committed[0]+=s;}public void delete(){}public void enter(){}public void finish(){}
                        },Learning.NONE);
                        active.set(engine);engine.dictionary(dictionary);engine.start(false,false,false,false);
                        engine.addons(addons,Collections.singleton("japanese"));engine.switchMode(InputMode.JAPANESE_ENGLISH,false);
                        engine.decoder(decoder,()-> {
                            if(at[0]!=0 && !engine.predictionPending() && engine.raw().equals(clause.getValue()) && done.getCount()!=0) {
                                long now=System.nanoTime();elapsed[0]=now-at[0];lastKeyElapsed[0]=now-lastKeyAt[0];count[0]=engine.candidates().size();
                                Candidate selected=engine.candidates().get(engine.preferred());expected[0]=selected.text+(selected.literal?" ":"");
                                // Clear guard before Space triggers a render callback.
                                at[0]=0;
                                try{engine.space();if(!expected[0].equals(committed[0]))throw new AssertionError("Space disagrees with highlighted choice");}
                                catch(Throwable failure){error.set(failure);}finally{done.countDown();}
                            }
                        });
                        at[0]=System.nanoTime();for(int cp:clause.getValue().codePoints().toArray()){lastKeyAt[0]=System.nanoTime();engine.type(cp);}dispatch[0]=System.nanoTime()-at[0];
                    });
                    assertTrue("Pipeline deadline",done.await(10,TimeUnit.SECONDS));if(error.get()!=null)throw new AssertionError(error.get());
                    if(cycle>0)pipeline.add(elapsed[0]);out.println(new JSONObject().put("kind","pipeline").put("cycle",cycle).put("id",clause.getKey())
                        .put("elapsed_ns",elapsed[0]).put("last_key_ns",lastKeyElapsed[0]).put("dispatch_ns",dispatch[0])
                        .put("addon_and_conversion_ns",decoder.stats.nanos[2].get()-stageBefore).put("candidates",count[0]).put("committed",committed[0]));
                    getInstrumentation().runOnMainSync(()->active.get().abandon());
                }
                // Supersede an actual native query and then switch the provider off.
                AtomicInteger stale=new AtomicInteger();CountDownLatch last=new CountDownLatch(1);
                getInstrumentation().runOnMainSync(()-> {
                    decoder.query(dictionary,clauses.values().iterator().next(),false,"",false,addons,Collections.singleton("japanese"),r->stale.incrementAndGet());
                    decoder.japanese(null);
                    decoder.query(dictionary,"english",false,"",false,addons,Collections.emptySet(),r->{
                        if(r.stream().anyMatch(c->c.pack.equals("japanese")))stale.incrementAndGet();last.countDown();
                    });
                });
                assertTrue(last.await(5,TimeUnit.SECONDS));getInstrumentation().waitForIdleSync();assertEquals(0,stale.get());
            } finally {getInstrumentation().runOnMainSync(decoder::close);}
            Collections.sort(warm);Collections.sort(pipeline);
            long p95=warm.get((int)(warm.size()*.95)),p99=warm.get((int)(warm.size()*.99)),pipeline95=pipeline.get((int)(pipeline.size()*.95));
            out.println(new JSONObject().put("kind","summary").put("native_p95_ns",p95).put("native_p99_ns",p99).put("pipeline_p95_ns",pipeline95)
                .put("ordered_differences",differences).put("pss_kib",Debug.getPss()).put("native_gate",p95<=10000000 && p99<=20000000).put("pipeline_gate",pipeline95<=50000000));
            out.flush();
            assertEquals("Desktop/phone ordered native parity",0,differences);
            // Timing is recorded as an admission decision, not hidden by an assertion
            // that would prevent remaining editor/keyboard contract tests from running.
        }
    }
}
