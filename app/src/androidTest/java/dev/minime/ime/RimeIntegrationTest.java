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
    public void testPrefixCandidateConsumption() throws Exception {
        assertTrue(RimeBackend.load(getContext()).get(60,TimeUnit.SECONDS));
        for(String input:new String[]{"nihao","ni'hao","womenmingtianjian","womenmtjian"}) {
            List<Candidate> complete=RimeBackend.convert(input),choices=RimeBackend.candidates(input);
            if(!complete.isEmpty())assertEquals("Stored whole-phrase default preserved",complete.get(0).text,choices.get(0).text);
            assertTrue("Native decoder never joins separate entries",choices.stream().noneMatch(c->c.composed));
            assertTrue("Every native consumption offset is bounded",choices.stream().allMatch(c->c.consumed>=0 && c.consumed<input.length()));
        }
        assertTrue("Prefix 你 consumes ni",RimeBackend.candidates("nihao").stream().anyMatch(c->c.text.equals("你") && c.consumed==2));
        assertTrue("Apostrophe spelling retains a prefix choice",RimeBackend.candidates("ni'hao").stream().anyMatch(c->c.text.equals("你") && c.consumed>=2 && c.consumed<4));
        assertTrue(RimeBackend.candidates("ssh://host").isEmpty());
    }
    public void testBoundedNoiseAndIsolation() throws Exception {
        assertTrue(RimeBackend.load(getContext()).get(60,TimeUnit.SECONDS));
        JSONArray samples=new JSONArray();
        for(String raw:new String[]{"s".repeat(32),"w".repeat(96),"women".repeat(18)}) {
            long begin=System.nanoTime();List<Candidate> choices=RimeBackend.candidates(raw);
            long micros=(System.nanoTime()-begin)/1000;
            samples.put(new JSONObject().put("input",raw).put("microseconds",micros).put("candidates",choices.size()));
            assertTrue("Bounded query: "+raw.length()+" characters took "+micros+" us",micros<1000000);
            assertEquals("Stored query isolation after noise","你好",RimeBackend.convert("nihao").get(0).text);
        }
        try(OutputStream out=new FileOutputStream(new File(getContext().getExternalFilesDir(null),"rime-stress.json"))) {out.write(samples.toString(2).getBytes(StandardCharsets.UTF_8));}
    }
    public void testPackagedModelAndCompleteCandidates() throws Exception {
        long begin=SystemClock.elapsedRealtime();
        assertTrue("Native Rime must load; fallback cannot pass this test",RimeBackend.load(getContext()).get(60,TimeUnit.SECONDS));
        long loadMs=SystemClock.elapsedRealtime()-begin;
        JSONArray samples=new JSONArray();Map<String,Integer> hits=new HashMap<>();
        try(BufferedReader in=new BufferedReader(new InputStreamReader(getContext().createPackageContext("app.minime.keyboard.test",0).getAssets().open("rime-probes.tsv"),StandardCharsets.UTF_8))) {
            String line;while((line=in.readLine())!=null) {
                String[] p=line.split("\t",-1);long at=System.nanoTime();
                List<Candidate> choices=RimeBackend.convert(p[3]);long micros=(System.nanoTime()-at)/1000;
                long uiAt=System.nanoTime();List<Candidate> uiChoices=RimeBackend.candidates(p[3]);long uiMicros=(System.nanoTime()-uiAt)/1000;
                assertTrue("No generated native sequence for "+p[3],uiChoices.stream().noneMatch(c->c.composed));
                if(!choices.isEmpty())assertEquals("UI retains complete-phrase default for "+p[3],choices.get(0).text,uiChoices.get(0).text);
                String first=choices.isEmpty()?"":choices.get(0).text;
                if(first.equals(p[4]))hits.merge(p[0],1,Integer::sum);
                JSONArray words=new JSONArray();for(Candidate c:choices)words.put(c.text);
                samples.put(new JSONObject().put("group",p[0]).put("case",p[1]).put("input",p[3]).put("expected",p[4]).put("first",first).put("microseconds",micros).put("uiMicroseconds",uiMicros).put("candidates",words));
            }
        }
        JSONObject report=new JSONObject().put("loadMs",loadMs).put("firstChoiceHits",new JSONObject(hits)).put("samples",samples);
        try(OutputStream out=new FileOutputStream(new File(getContext().getExternalFilesDir(null),"rime-phone.json"))) {out.write(report.toString(2).getBytes(StandardCharsets.UTF_8));}
        // Keep every original target in the report, including missing constructed
        // clauses. The old construction hit floors do not define the null contract.
        assertEquals("Stored phrase remains available","你好",RimeBackend.convert("nihao").get(0).text);
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
