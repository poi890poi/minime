package dev.minime.ime;

import android.test.AndroidTestCase;
import java.util.concurrent.TimeUnit;
import java.io.*;
import java.nio.charset.StandardCharsets;
import org.json.*;

@SuppressWarnings("deprecation")
public final class TaiwanConversionTest extends AndroidTestCase {
    public void testTaiwanVariantsAndFreshPhrases() throws Exception {
        assertTrue(RimeBackend.load(getContext()).get(60,TimeUnit.SECONDS));
        // First three reproduce the baseline; the rest were frozen after proposing
        // the upstream filter and before running it. No entries feed production data.
        String[][] cases={{"chifan","吃飯"},{"weishenme","為什麼"},{"nizainali","你在哪裡"},
            {"qunzhong","群眾"},{"miantiao","麵條"},{"taishou","抬手"},
            {"shanfeng","山峰"},{"qichuang","起床"},{"qifa","啟發"},{"qizhong","其中"}};
        JSONArray report=new JSONArray();
        for(String[] row:cases) {
            java.util.List<dev.minime.core.Candidate> choices=RimeBackend.convert(row[0]);int rank=0;
            for(int i=0;i<choices.size();i++)if(choices.get(i).text.equals(row[1]))rank=i+1;
            report.put(new JSONObject().put("input",row[0]).put("expected",row[1]).put("first",choices.get(0).text).put("rank",rank));
        }
        try(OutputStream out=new FileOutputStream(new File(getContext().getExternalFilesDir(null),"taiwan-holdout.json"))) {out.write(report.toString(2).getBytes(StandardCharsets.UTF_8));}
        // Variant conversion is the contract. Ambiguous homophones need not all
        // rank first: e.g. 太守 currently precedes 抬手; retain that negative result.
        for(int i=0;i<report.length();i++)assertTrue(report.getJSONObject(i).toString(),report.getJSONObject(i).getInt("rank")>0);
        for(int i=0;i<3;i++)assertEquals(1,report.getJSONObject(i).getInt("rank"));
        assertTrue(RimeBackend.candidates("chifan").stream().anyMatch(c->c.text.equals("吃") && c.consumed==3));
        assertTrue(RimeBackend.candidates("nihao").stream().anyMatch(c->c.text.equals("你") && c.consumed==2));
    }
}
