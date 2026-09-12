package dev.minime.ime;

import android.test.AndroidTestCase;
import android.os.*;
import dev.minime.core.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.*;

/** Frozen regression inputs; component and main-handler delivery times, not display latency. */
@SuppressWarnings("deprecation")
public final class SuggestionLatencyTest extends AndroidTestCase {
    private static final Set<String> ALL=new HashSet<>(Arrays.asList("taiwan","geography","poj","japanese"));
    public void testWarmTypingLatency() throws Exception {
        long start=System.nanoTime();
        PhoneticDictionary dictionary=DictionaryRepository.load(getContext()).get(60,TimeUnit.SECONDS);
        AddonDictionary addons=AddonDictionary.combine(AddonRepository.load(getContext()).get(60,TimeUnit.SECONDS),AddonRepository.geography(getContext()).get(60,TimeUnit.SECONDS));
        assertTrue(RimeBackend.load(getContext()).get(60,TimeUnit.SECONDS));
        long loadUs=(System.nanoTime()-start)/1000;
        List<String[]> inputs=new ArrayList<>();
        try(BufferedReader in=new BufferedReader(new InputStreamReader(getContext().createPackageContext("app.minime.keyboard.test",0).getAssets().open("latency-inputs.tsv"),StandardCharsets.UTF_8))) {
            String line;int index=0;while((line=in.readLine())!=null) {
                // First hash-selected full/half/first triple from each 48-row group.
                if(index++%48<3)inputs.add(line.split("\t"));
            }
        }
        File report=new File(getContext().getExternalFilesDir(null),"suggestion-latency.tsv");
        try(PrintWriter out=new PrintWriter(report,"UTF-8")) {
            out.println("# load_us="+loadUs);
            out.println("stage\tround\tgroup\tcondition\tquery\tcore_us\trime_us\taddons_us\tdelivery_us\tsignature");
            for(int round=-1;round<2;round++)for(String[] p:inputs) {
                long at=System.nanoTime();List<Candidate> core=dictionary.convert(p[2],false);long coreUs=(System.nanoTime()-at)/1000;
                at=System.nanoTime();List<Candidate> nativeChoices=RimeBackend.candidates(p[2]);long rimeUs=(System.nanoTime()-at)/1000;
                at=System.nanoTime();List<Candidate> optional=addons.lookup(p[2],ALL);long addonUs=(System.nanoTime()-at)/1000;
                if(round>=0) {
                    List<Candidate> merged=CandidateMerge.merge(nativeChoices,core);merged.addAll(optional);
                    out.println("components\t"+round+"\t"+String.join("\t",p)+"\t"+coreUs+"\t"+rimeUs+"\t"+addonUs+"\t0\t"+signature(merged));
                }
            }
            Handler main=new Handler(Looper.getMainLooper());
            try(AsyncDecoder decoder=new AsyncDecoder(main)) {
                decoder.rime(true);
                for(boolean all:new boolean[]{false,true})for(String[] p:inputs) {
                    CountDownLatch done=new CountDownLatch(1);long[] elapsed={0};
                    CompletableFuture<List<Candidate>> result=new CompletableFuture<>();
                    main.post(()-> {
                        long at=System.nanoTime();
                        decoder.query(dictionary,p[2],false,"",true,addons,all?ALL:Collections.emptySet(),choices->{
                            elapsed[0]=(System.nanoTime()-at)/1000;result.complete(choices);done.countDown();
                        });
                    });
                    assertTrue("Suggestion callback timeout",done.await(10,TimeUnit.SECONDS));
                    out.println("delivery-"+(all?"all":"none")+"\t0\t"+String.join("\t",p)+"\t0\t0\t0\t"+elapsed[0]+"\t"+signature(result.get()));
                }
            }
        }
    }
    private static String signature(List<Candidate> values)throws Exception {
        MessageDigest digest=MessageDigest.getInstance("SHA-256");
        for(Candidate c:values)digest.update((c.text+"\t"+Double.toHexString(c.score)+"\t"+c.consumed+"\t"+c.composed+"\t"+c.incomplete+"\t"+c.literal+"\t"+c.abbreviated+"\t"+c.supplemental+"\n").getBytes(StandardCharsets.UTF_8));
        StringBuilder hex=new StringBuilder();for(byte b:digest.digest())hex.append(String.format(Locale.ROOT,"%02x",b&255));return hex.toString();
    }
}
