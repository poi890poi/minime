package dev.minime.ime;

import android.content.Context;
import android.test.InstrumentationTestCase;
import dev.minime.core.*;
import org.json.JSONObject;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.TimeUnit;

/** Direct Java lookup timing; no keyboard/touch latency claim. */
@SuppressWarnings("deprecation")
public final class JavaLookupPhoneTest extends InstrumentationTestCase {
    public void testFrozenLookupReplay()throws Exception {
        Context app=getInstrumentation().getTargetContext(),tests=getInstrumentation().getContext();
        Map<String,AddonDictionary> dictionaries=new HashMap<>();
        for(String pack:Arrays.asList("japanese","poj","taiwan"))
            dictionaries.put(pack,AddonRepository.load(app,Collections.singleton(pack)).get(60,TimeUnit.SECONDS));
        List<String[]> rows=new ArrayList<>();
        try(BufferedReader in=new BufferedReader(new InputStreamReader(tests.getAssets().open("japanese-evaluation/java-lookup.tsv"),StandardCharsets.UTF_8))) {
            String line;while((line=in.readLine())!=null)rows.add(line.split("\t",-1));
        }
        MessageDigest digest=MessageDigest.getInstance("SHA-256");
        try(PrintWriter out=new PrintWriter(new File(app.getExternalFilesDir(null),"java-lookup-phone.jsonl"),"UTF-8")) {
            for(int pass=0;pass<3;pass++)for(int i=0;i<rows.size();i++) {
                String[] row=rows.get(i);Set<String> enabled=Collections.singleton(row[0]);AddonDictionary dictionary=dictionaries.get(row[0]);
                long at=System.nanoTime();List<Candidate> values=dictionary.lookup(row[4],enabled);long ns=System.nanoTime()-at;
                ByteArrayOutputStream bytes=new ByteArrayOutputStream();DataOutputStream data=new DataOutputStream(bytes);
                for(Candidate c:values) {
                    data.writeUTF(c.text);data.writeDouble(c.score);data.writeInt(c.consumed);data.writeUTF(c.pack);
                    data.writeBoolean(c.literal);data.writeBoolean(c.incomplete);data.writeBoolean(c.supplemental);
                    data.writeBoolean(c.abbreviated);data.writeBoolean(c.composed);data.writeBoolean(c.languageCharacter);data.writeBoolean(c.transliteration);
                    data.writeUTF(c.pair==null?"":c.pair.phonetic);data.writeUTF(c.pair==null?"":c.pair.han);data.writeUTF(c.pair==null?"":c.pair.source);
                }
                out.println(new JSONObject().put("pass",pass).put("row",i).put("pack",row[0]).put("group",row[1]).put("condition",row[2])
                    .put("ns",ns).put("count",values.size()).put("digest",Base64.getEncoder().encodeToString(digest.digest(bytes.toByteArray()))));
            }
        }
    }
}
