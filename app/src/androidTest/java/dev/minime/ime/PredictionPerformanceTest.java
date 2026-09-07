package dev.minime.ime;

import android.os.SystemClock;
import android.test.AndroidTestCase;
import dev.minime.core.PhoneticDictionary;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.json.*;

/** Explicit headless benchmark; synthetic inputs only, no editor text or production telemetry. */
@SuppressWarnings("deprecation")
public final class PredictionPerformanceTest extends AndroidTestCase {
    private Reader asset(String name) throws IOException {
        return new InputStreamReader(getContext().getAssets().open(name),StandardCharsets.UTF_8);
    }
    private long heap() { Runtime r=Runtime.getRuntime();return r.totalMemory()-r.freeMemory(); }
    public void testOfflinePredictionCosts() throws Exception {
        System.gc(); long beforeHeap=heap(),begun=SystemClock.elapsedRealtime();
        PhoneticDictionary dictionary=PhoneticDictionary.load(asset("zh_tw.tsv"),asset("en_us.tsv"),asset("syllables.tsv"));
        long loadMs=SystemClock.elapsedRealtime()-begun;
        System.gc(); long retained=heap()-beforeHeap;
        JSONArray samples=new JSONArray();
        for(String raw:new String[]{"nh","jtian","shrf","srufa","wxsrf","mingtian","womenxyaoxuexi","s".repeat(32)}) {
            for(int warm=0;warm<4;warm++) dictionary.convert(raw,false);
            long[] times=new long[12]; int candidates=0;
            for(int i=0;i<times.length;i++) {
                long at=System.nanoTime(); candidates=dictionary.convert(raw,false).size(); times[i]=(System.nanoTime()-at)/1000;
            }
            Arrays.sort(times);
            samples.put(new JSONObject().put("input",raw).put("candidates",candidates).put("medianUs",times[6]).put("p95Us",times[11]));
        }
        JSONObject report=new JSONObject().put("loadMs",loadMs).put("approxRetainedHeapBytes",retained).put("samples",samples)
            .put("scope","Android offline conversion only; excludes input dispatch, composing span updates and rendering. GC makes heap approximate.");
        try(FileOutputStream out=new FileOutputStream(new File(getContext().getExternalFilesDir(null),"prediction-performance.json"))) {
            out.write(report.toString(2).getBytes(StandardCharsets.UTF_8));
        }
        assertFalse(dictionary.convert("srufa",false).isEmpty());
    }
}
