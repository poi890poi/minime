package dev.minime.ime;

import android.os.*;
import android.test.InstrumentationTestCase;
import dev.minime.core.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/** Controlled worker stress, not physical input or a frame-latency benchmark. */
@SuppressWarnings("deprecation")
public final class DecoderBurstTest extends InstrumentationTestCase {
    public void testRealProviderBurstsRejectSupersededCallbacks()throws Exception {
        android.content.Context context=getInstrumentation().getTargetContext();
        PhoneticDictionary dictionary=DictionaryRepository.load(context).get(60,TimeUnit.SECONDS);
        Set<String> packs=new HashSet<>(Arrays.asList("taiwan","geography"));
        AddonDictionary addons=AddonRepository.load(context,packs).get(60,TimeUnit.SECONDS);
        List<String> prefixes=new ArrayList<>();
        try(BufferedReader in=new BufferedReader(new InputStreamReader(getInstrumentation().getContext().getAssets().open("latency-inputs.tsv"),"UTF-8"))) {
            String line;int row=0,queries=0;
            while((line=in.readLine())!=null && queries<8) {
                String raw=line.split("\t")[2];if(row++%144!=0 || !raw.matches("[a-z]{3,16}"))continue;
                queries++;for(int i=1;i<=raw.length();i++)prefixes.add(raw.substring(0,i));
            }
        }
        assertEquals(50,prefixes.size());
        try(PrintWriter out=new PrintWriter(new File(context.getExternalFilesDir(null),"decoder-burst.tsv"),"UTF-8")) {
            out.println("round\tdispatch_pattern\trequests\tdelivered\tobsolete_callbacks\tbase_calls\tbase_ns\taddon_calls\taddon_ns\tcancelled\tstale_delivery");
            for(int round=-1;round<2;round++)for(int gap:new int[]{0,4,12,150}) {
                AsyncDecoder decoder=new AsyncDecoder(new Handler(Looper.getMainLooper()));
                CountDownLatch done=new CountDownLatch(1);AtomicInteger obsolete=new AtomicInteger();
                try {
                    getInstrumentation().runOnMainSync(()-> {
                        for(int i=0;i<prefixes.size();i++) {
                            final boolean last=i==prefixes.size()-1;
                            decoder.query(dictionary,prefixes.get(i),false,"",true,addons,packs,values->{
                                if(last)done.countDown();else obsolete.incrementAndGet();
                            });
                            int pause=gap==150?(i%2==0?4:150):gap;
                            if(pause>0 && !last)SystemClock.sleep(pause);
                        }
                    });
                    assertTrue("Latest result delivered",done.await(10,TimeUnit.SECONDS));getInstrumentation().waitForIdleSync();
                    assertEquals(0,obsolete.get());assertEquals(1L,decoder.stats.delivered.get());
                    assertEquals((long)prefixes.size(),decoder.stats.requests.get());
                    DecodePipeline.Stats s=decoder.stats;
                    if(round>=0)out.println(round+"\t"+(gap==150?"paired_4_150":gap)+"\t"+s.requests.get()+"\t"+s.delivered.get()+"\t"+obsolete.get()+"\t"+s.calls[0].get()+"\t"+s.nanos[0].get()+"\t"+s.calls[2].get()+"\t"+s.nanos[2].get()+"\t"+s.cancelled.get()+"\t"+s.staleDelivery.get());
                } finally {getInstrumentation().runOnMainSync(decoder::close);}
            }
        }
    }
}
