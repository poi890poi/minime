package dev.minime.ime;

import android.test.AndroidTestCase;
import android.os.SystemClock;
import dev.minime.core.Candidate;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;
import org.json.*;

@SuppressWarnings("deprecation")
public final class RimeIntegrationTest extends AndroidTestCase {
    public void testBoundedNoiseAndIsolation() throws Exception {
        assertTrue(RimeBackend.load(getContext()).get(60,TimeUnit.SECONDS));
        JSONArray samples=new JSONArray();
        for(String raw:new String[]{"s".repeat(32),"w".repeat(96),"women".repeat(18)}) {
            long begin=System.nanoTime();List<Candidate> choices=RimeBackend.convert(raw);
            long micros=(System.nanoTime()-begin)/1000;
            samples.put(new JSONObject().put("input",raw).put("microseconds",micros).put("candidates",choices.size()));
            assertTrue("Bounded query: "+raw.length()+" characters took "+micros+" us",micros<1000000);
            assertEquals("Query isolation after noise","我們明天見",RimeBackend.convert("womenmingtianjian").get(0).text);
        }
        try(OutputStream out=new FileOutputStream(new File(getContext().getExternalFilesDir(null),"rime-stress.json"))) {out.write(samples.toString(2).getBytes(StandardCharsets.UTF_8));}
    }
    public void testPackagedModelAndCompleteCandidates() throws Exception {
        long begin=SystemClock.elapsedRealtime();
        assertTrue("Native Rime must load; fallback cannot pass this test",RimeBackend.load(getContext()).get(60,TimeUnit.SECONDS));
        long loadMs=SystemClock.elapsedRealtime()-begin;
        JSONArray samples=new JSONArray();Map<String,Integer> hits=new HashMap<>();
        try(BufferedReader in=new BufferedReader(new InputStreamReader(getContext().createPackageContext("dev.minime.ime.test",0).getAssets().open("rime-probes.tsv"),StandardCharsets.UTF_8))) {
            String line;while((line=in.readLine())!=null) {
                String[] p=line.split("\t",-1);long at=System.nanoTime();
                List<Candidate> choices=RimeBackend.convert(p[3]);long micros=(System.nanoTime()-at)/1000;
                String first=choices.isEmpty()?"":choices.get(0).text;
                if(first.equals(p[4]))hits.merge(p[0],1,Integer::sum);
                JSONArray words=new JSONArray();for(Candidate c:choices)words.put(c.text);
                samples.put(new JSONObject().put("group",p[0]).put("case",p[1]).put("input",p[3]).put("expected",p[4]).put("first",first).put("microseconds",micros).put("candidates",words));
            }
        }
        JSONObject report=new JSONObject().put("loadMs",loadMs).put("firstChoiceHits",new JSONObject(hits)).put("samples",samples);
        try(OutputStream out=new FileOutputStream(new File(getContext().getExternalFilesDir(null),"rime-phone.json"))) {out.write(report.toString(2).getBytes(StandardCharsets.UTF_8));}
        assertEquals(14,(int)hits.getOrDefault("reference",0));
        assertEquals(20,(int)hits.getOrDefault("development",0));
        assertEquals(15,(int)hits.getOrDefault("fresh",0));
        assertTrue(RimeBackend.convert("ssh://host").isEmpty());
        assertTrue(RimeBackend.convert("a".repeat(97)).isEmpty());
        assertFalse(RimeBackend.convert("womenmingtianjian").stream().anyMatch(c->c.text.equals("我們")));
        assertNoUserData(new File(getContext().getNoBackupFilesDir(),"rime"));
    }
    private void assertNoUserData(File root) {
        assertFalse("No learned native database",root.getName().contains("userdb"));
        assertFalse("No native input logs",root.getName().endsWith(".log"));
        File[] children=root.listFiles();if(children!=null)for(File child:children)assertNoUserData(child);
    }
}
