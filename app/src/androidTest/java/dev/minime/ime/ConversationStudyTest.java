package dev.minime.ime;

import android.test.AndroidTestCase;
import dev.minime.core.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;
import org.json.*;

/** Evaluation only: expected answers are written to the report, never passed to a decoder. */
@SuppressWarnings("deprecation")
public final class ConversationStudyTest extends AndroidTestCase {
    private static final class Editor implements CompositionEngine.Editor {
        String text="";
        public void composing(String s) {}
        public void commit(String s) {text+=s;}
        public void delete() {}
        public void enter() {text+="\n";}
        public void finish() {}
    }
    public void testEverydayInputs() throws Exception {
        PhoneticDictionary dictionary=DictionaryRepository.load(getContext()).get(60,TimeUnit.SECONDS);
        assertTrue(RimeBackend.load(getContext()).get(60,TimeUnit.SECONDS));
        JSONArray rows=new JSONArray();
        try(BufferedReader reader=new BufferedReader(new InputStreamReader(getContext().createPackageContext("app.minime.keyboard.test",0).getAssets().open("conversation-probes.tsv"),StandardCharsets.UTF_8))) {
            String line;while((line=reader.readLine())!=null) {
                if(line.isEmpty() || line.startsWith("#"))continue;
                String[] p=line.split("\t",-1);
                for(boolean english:new boolean[]{false,true}) {
                    if(english && !p[0].equals("english"))continue;
                    Editor editor=new Editor();CompositionEngine engine=new CompositionEngine(editor,Learning.NONE);
                    engine.dictionary(dictionary);engine.start(false,false,false,false,english);
                    // Same source merge as AsyncDecoder, synchronous solely for deterministic telemetry.
                    engine.decoder((d,raw,bpmf,context,done)->{
                        List<Candidate> nativeChoices=RimeBackend.candidates(raw),fallback=d.convert(raw,bpmf,context);
                        Set<String> seen=new HashSet<>();for(Candidate c:nativeChoices)seen.add(c.text);
                        for(Candidate c:fallback)if(seen.add(c.text))nativeChoices.add(c);
                        done.accept(nativeChoices);
                    },()->{});
                    JSONArray tokens=new JSONArray();
                    for(String token:p[1].split(" ")) {
                        for(int cp:token.codePoints().toArray())engine.type(cp);
                        JSONArray candidates=new JSONArray();int rank=0,index=0;
                        for(Candidate c:engine.candidates()) {
                            if(c.text.equals(p[2]))rank=index;
                            if(index<25)candidates.put(new JSONObject().put("text",c.text).put("consumed",c.consumed).put("score",c.score));
                            index++;
                        }
                        tokens.put(new JSONObject().put("raw",token).put("intent",engine.intent().name()).put("englishKnown",dictionary.isEnglish(token))
                            .put("preferred",engine.preferred()).put("rank",rank).put("candidates",candidates));
                        engine.space();
                    }
                    rows.put(new JSONObject().put("group",p[0]).put("input",p[1]).put("expected",p[2]).put("mode",english?"english":"pinyin").put("output",editor.text).put("tokens",tokens));
                }
            }
        }
        try(OutputStream out=new FileOutputStream(new File(getContext().getExternalFilesDir(null),"conversation-study.json"))) {out.write(rows.toString(2).getBytes(StandardCharsets.UTF_8));}
        assertTrue(rows.length()>60);
    }
}
