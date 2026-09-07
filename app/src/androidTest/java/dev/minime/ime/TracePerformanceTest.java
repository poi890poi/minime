package dev.minime.ime;

import android.test.AndroidTestCase;
import dev.minime.core.*;
import java.util.*;
import org.json.*;

@SuppressWarnings("deprecation")
public final class TracePerformanceTest extends AndroidTestCase {
    public void testTraceDecodeCost()throws Exception {
        PhoneticDictionary d=PhoneticDictionary.readBinary(getContext().getAssets().open("model.bin"));
        float[] path={6,1,2.5f,0,9,1,9,1,8.5f,0};
        JSONArray times=new JSONArray();String word="";
        for(int i=0;i<6;i++) {long start=System.nanoTime();List<Candidate> found=d.englishTrace(path);times.put((System.nanoTime()-start)/1000000);word=found.isEmpty()?"":found.get(0).text;}
        JSONObject result=new JSONObject().put("milliseconds",times).put("top",word);
        try(java.io.FileOutputStream out=new java.io.FileOutputStream(new java.io.File(getContext().getExternalFilesDir(null),"trace-performance.json"))) {out.write(result.toString(2).getBytes(java.nio.charset.StandardCharsets.UTF_8));}
        assertEquals("hello",word);
    }
}
