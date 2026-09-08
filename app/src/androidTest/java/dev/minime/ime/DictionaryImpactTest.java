package dev.minime.ime;

import android.test.AndroidTestCase;
import android.content.*;
import dev.minime.core.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.json.*;

/** Explicit synthetic microbenchmarks; no editor content or production logging. */
@SuppressWarnings("deprecation")
public final class DictionaryImpactTest extends AndroidTestCase {
    private long heap() {Runtime r=Runtime.getRuntime();return r.totalMemory()-r.freeMemory();}
    private Reader asset(String name)throws IOException {return new InputStreamReader(getContext().getAssets().open(name),StandardCharsets.UTF_8);}
    private JSONObject measure(Runnable action,int count)throws Exception {
        for(int i=0;i<15;i++)action.run();long[] times=new long[count];
        for(int i=0;i<count;i++) {long t=System.nanoTime();action.run();times[i]=(System.nanoTime()-t)/1000;}
        Arrays.sort(times);return new JSONObject().put("n",count).put("p50_us",times[count/2]).put("p95_us",times[count*95/100]).put("max_us",times[count-1]);
    }
    public void testDictionaryCosts()throws Exception {
        System.gc();long initial=heap(),at=System.nanoTime();
        AddonDictionary language=AddonDictionary.read(asset("addons.tsv"));long languageUs=(System.nanoTime()-at)/1000;
        at=System.nanoTime();AddonDictionary geo=AddonDictionary.read(asset("geography.tsv"));long geographyUs=(System.nanoTime()-at)/1000;
        System.gc();long retained=heap()-initial;
        JSONObject report=new JSONObject().put("language_load_us",languageUs).put("geography_load_us",geographyUs).put("approx_retained_heap_bytes",retained);
        report.put("combine",measure(()->AddonDictionary.combine(language,geo),30));
        AddonDictionary both=AddonDictionary.combine(language,geo);Set<String> all=new HashSet<>(Arrays.asList("poj","japanese","taiwan","geography"));
        List<String> keys=new ArrayList<>();try(BufferedReader reader=new BufferedReader(asset("geography.tsv"))) {
            String row;while((row=reader.readLine())!=null && keys.size()<512) {String[] p=row.split("\t");if(p.length==5 && p[1].matches("[a-z']{4,24}"))keys.add(p[1]);}
        }
        final int[] position={0};report.put("lookup_all",measure(()->both.lookup(keys.get(position[0]++%keys.size()),all),1000));
        SharedPreferences settings=getContext().getSharedPreferences("settings",Context.MODE_PRIVATE),learning=getContext().getSharedPreferences("learning",Context.MODE_PRIVATE);
        StringBuilder custom=new StringBuilder(),phrases=new StringBuilder();
        for(int i=0;i<512;i++) {custom.append(keys.get(i)).append('\t').append("測試").append(i).append('\n');phrases.append(keys.get(i)).append('\t').appendCodePoint(0x4e00+i).append("山\t3\n");}
        learning.edit().putString("custom",custom.toString()).putString("phrases_v1",phrases.toString()).commit();settings.edit().putBoolean("phrase_learning",true).commit();
        LocalLearning local=new LocalLearning(getContext());
        // Alternating order reduces one-sided warmup bias. Repeat the same saved data.
        JSONArray trials=new JSONArray();
        for(int trial=0;trial<4;trial++) {
            JSONObject result=new JSONObject();
            if(trial%2==0)result.put("custom",measure(()->local.custom(keys.get(position[0]++%keys.size())),100));
            result.put("learned",measure(()->local.phrases(keys.get(position[0]++%keys.size())),100));
            if(trial%2!=0)result.put("custom",measure(()->local.custom(keys.get(position[0]++%keys.size())),100));
            trials.put(result);
        }
        report.put("saved_512_trials",trials).put("scope","Android component costs; no Rime, touch dispatch or rendering. Heap is approximate; timings are not end-to-end frame latency.");
        try(FileOutputStream out=new FileOutputStream(new File(getContext().getExternalFilesDir(null),"dictionary-impact.json"))) {out.write(report.toString(2).getBytes(StandardCharsets.UTF_8));}
        System.out.println("DICTIONARY_IMPACT "+report);
        assertFalse(local.phrases(keys.get(0)).isEmpty());
    }
}
