package dev.minime.ime;

import android.content.Context;
import android.graphics.Paint;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.text.PositionedGlyphs;
import android.graphics.text.TextRunShaper;
import android.icu.text.BreakIterator;
import android.test.InstrumentationTestCase;
import android.view.*;
import android.widget.TextView;
import dev.minime.core.*;
import dev.minime.testing.TextIntegrity;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;
import org.json.*;

/** Fails on unreadable displayed candidates; source-only font gaps are reported separately. */
@SuppressWarnings("deprecation")
public final class CandidateTextAuditTest extends InstrumentationTestCase {
    private final JSONArray failures=new JSONArray(),queries=new JSONArray(),sourceGaps=new JSONArray();
    private final Map<String,List<String>> glyphs=new HashMap<>();
    private int views,sourceRows;
    private Context target;
    private static final CompositionEngine.Editor EDITOR=new CompositionEngine.Editor() {
        public void composing(String text){} public void commit(String text){} public void delete(){}
        public void enter(){} public void finish(){}
    };
    private void main(Runnable work) throws Throwable {
        java.util.concurrent.atomic.AtomicReference<Throwable> failure=new java.util.concurrent.atomic.AtomicReference<>();
        getInstrumentation().runOnMainSync(()->{try {work.run();}catch(Throwable t){failure.set(t);}});
        if(failure.get()!=null)throw failure.get();
    }
    private List<String> missing(Paint paint,String text) {
        assertTrue("Shaped-glyph audit requires Android 12/API 31 or newer; older phones are not certified by this test",android.os.Build.VERSION.SDK_INT>=31);
        String key=paint.getTypeface()+"|"+paint.getTextSize()+"|"+paint.getTextLocales()+"|"+text;
        if(glyphs.containsKey(key))return glyphs.get(key);
        List<String> result=new ArrayList<>();
        BreakIterator clusters=BreakIterator.getCharacterInstance(Locale.ROOT);clusters.setText(text);
        for(int start=clusters.first(),end=clusters.next();end!=BreakIterator.DONE;start=end,end=clusters.next()) {
            String cluster=text.substring(start,end);
            if(cluster.codePoints().allMatch(Character::isWhitespace))continue;
            // Use grapheme clusters, not UTF-16 units: combining POJ and emoji
            // joiners/selectors must be tested together with their base glyphs.
            // hasGlyph(multi-character text) asks for a SINGLE ligature and
            // falsely rejects valid base+mark rendering. Inspect shaped .notdef
            // glyph IDs instead, using this TextView's paint and full context.
            PositionedGlyphs shaped=TextRunShaper.shapeTextRun(text,start,end-start,0,text.length(),0,0,false,paint);
            for(int i=0;i<shaped.glyphCount();i++)if(shaped.getGlyphId(i)==0) {
                result.add(TextIntegrity.codePoints(cluster));break;
            }
        }
        glyphs.put(key,result);
        return result;
    }
    public void testSentinelDistinguishesMalformedTextAndMissingFonts() throws Throwable {
        main(()->{
            TextView word=new TextView(getInstrumentation().getTargetContext());word.setTextSize(20);
            word.setText("\ud800");assertFalse(TextIntegrity.problems(word.getText().toString()).isEmpty());
            assertFalse(missing(word.getPaint(),new String(Character.toChars(0x10ffff))).isEmpty());
            String valid="台灣 かな カナ chhiu\u0301 o\u0358";word.setText(valid);
            assertEquals(valid,word.getText().toString());assertTrue(TextIntegrity.problems(valid).isEmpty());
            assertTrue("Common Han, kana and combining POJ must render: "+missing(word.getPaint(),valid),missing(word.getPaint(),valid).isEmpty());
            CandidateGlyphs policy=new CandidateGlyphs(word.getPaint());
            assertTrue("Runtime policy preserves readable combined text",policy.test(valid));
            assertTrue("Runtime policy preserves emoji joiners and selectors",policy.test("\ud83d\udc69\u200d\ud83d\udcbb\u2764\ufe0f"));
            assertFalse("Runtime policy rejects unsupported text",policy.test(new String(Character.toChars(0x10ffff))));
            String supplementary="\ud840\udc00";word.setText(supplementary);
            assertEquals("A valid surrogate pair must survive TextView binding",supplementary,word.getText().toString());
        });
    }
    private void checkViews(View view,String raw,String provider,String panel,List<String> rendered) {
        if(view instanceof TextView && view.getContentDescription()!=null && view.getContentDescription().toString().startsWith("Candidate ")) {
            TextView word=(TextView)view;String text=word.getText().toString();rendered.add(text);views++;
            List<String> invalid=TextIntegrity.problems(text),absent=missing(word.getPaint(),text);
            if(!invalid.isEmpty() || !absent.isEmpty())try {
                failures.put(new JSONObject().put("input",raw).put("provider",provider).put("panel",panel)
                    .put("text",text).put("codePoints",TextIntegrity.codePoints(text))
                    .put("malformed",new JSONArray(invalid)).put("missingGlyphs",new JSONArray(absent)));
            }catch(JSONException e){throw new RuntimeException(e);}
        }
        if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++)checkViews(((ViewGroup)view).getChildAt(i),raw,provider,panel,rendered);
    }
    private View expand(View view) {
        if("Expand candidates".contentEquals(view.getContentDescription()==null?"":view.getContentDescription()))return view;
        if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++){View found=expand(((ViewGroup)view).getChildAt(i));if(found!=null)return found;}
        return null;
    }
    private void layout(KeyboardView keyboard) {
        int width=target.getResources().getDisplayMetrics().widthPixels;
        keyboard.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED));
        keyboard.layout(0,0,width,keyboard.getMeasuredHeight());
    }
    private void render(String raw,String provider,PhoneticDictionary dictionary,List<Candidate> nativeChoices) throws Throwable {
        main(()->{
            CompositionEngine engine=new CompositionEngine(EDITOR,Learning.NONE);engine.dictionary(dictionary);engine.start(false,false,false,false);
            CandidateGlyphs capability=new CandidateGlyphs(new TextView(target).getPaint());long[] cost={0,0};
            engine.candidateDisplay(text->{long begin=System.nanoTime();boolean allowed=capability.test(text);cost[0]+=System.nanoTime()-begin;cost[1]++;return allowed;});
            if(nativeChoices!=null)engine.decoder((d,r,b,c,done)->done.accept(r.equals(raw)?CandidateMerge.merge(nativeChoices,d.convert(r,b,c)):Collections.emptyList()),()->{});
            raw.codePoints().forEach(engine::type);
            KeyboardView keyboard=new KeyboardView(target,k->{},k->false,(p,c)->{});
            keyboard.render(engine,false,false,false,0,false,false,false,true,false,"Enter","");layout(keyboard);
            List<String> strip=new ArrayList<>(),grid=new ArrayList<>();checkViews(keyboard,raw,provider,"strip",strip);
            View expand=expand(keyboard);assertNotNull("Candidate expansion control",expand);expand.performClick();layout(keyboard);
            checkViews(keyboard,raw,provider,"expanded",grid);
            if(raw.equals("rime"))try {
                Bitmap bitmap=Bitmap.createBitmap(keyboard.getWidth(),keyboard.getHeight(),Bitmap.Config.ARGB_8888);
                try(OutputStream out=new FileOutputStream(new File(target.getExternalFilesDir(null),"candidate-text-rime-"+(nativeChoices==null?"java":"native")+".png"))) {
                    keyboard.draw(new Canvas(bitmap));bitmap.compress(Bitmap.CompressFormat.PNG,100,out);
                }finally{bitmap.recycle();}
            }catch(IOException e){throw new UncheckedIOException(e);}
            for(Candidate c:engine.candidates())if(!c.literal)assertTrue("Expanded panel lost exact text "+TextIntegrity.codePoints(c.text),grid.contains(c.text));
            try {queries.put(new JSONObject().put("input",raw).put("provider",provider).put("strip",new JSONArray(strip)).put("expanded",new JSONArray(grid))
                .put("glyphCheckMicros",cost[0]/1000).put("glyphChecks",cost[1]));}
            catch(JSONException e){throw new RuntimeException(e);}
        });
    }
    public void testSourceRepertoireAndLiveCandidatePanels() throws Throwable {
        target=getInstrumentation().getTargetContext();Context source=target.createPackageContext("app.minime.keyboard.test",0);
        Paint paint=new Paint();paint.setTextSize(20*target.getResources().getDisplayMetrics().scaledDensity);
        Set<Integer> seen=new HashSet<>();Set<String> inputs=new TreeSet<>();
        try {
            // Every output glyph in every source, not a curated list of uncommon Han.
            // These census gaps do not fail unless an actual displayed candidate uses them.
            for(String file:new String[]{"zh_tw.tsv","en_us.tsv","addons.tsv","geography.tsv","paired-forms.tsv","japanese-basic.tsv"}) {
                try(BufferedReader reader=new BufferedReader(TextIntegrity.utf8(source.getAssets().open(file)))) {
                    String line;int row=0;while((line=reader.readLine())!=null) {
                        row++;if(line.startsWith("#") || line.isEmpty())continue;sourceRows++;
                        TextIntegrity.require(line,file+":"+row);String[] fields=line.split("\t",-1);
                        String text=fields[file.equals("en_us.tsv")?0:2];
                        for(int cp:text.codePoints().toArray())if(seen.add(cp) && !Character.isWhitespace(cp) && Character.getType(cp)!=Character.NON_SPACING_MARK && Character.getType(cp)!=Character.FORMAT) {
                            String glyph=new String(Character.toChars(cp));
                            if(!paint.hasGlyph(glyph))sourceGaps.put(new JSONObject().put("source",file).put("line",row).put("text",text).put("codePoint",TextIntegrity.codePoints(glyph)));
                        }
                    }
                }
            }
            try(BufferedReader reader=new BufferedReader(TextIntegrity.utf8(source.getAssets().open("syllables.tsv")))) {
                String line;while((line=reader.readLine())!=null)inputs.add(line.split("\t")[0]);
            }
            // Explicit screenshot reproduction plus every prefix, separate from broad source probes.
            for(int i=1;i<=4;i++)inputs.add("rime".substring(0,i));
            PhoneticDictionary dictionary=DictionaryRepository.load(target).get(90,TimeUnit.SECONDS);
            assertTrue("Native decoder must load",RimeBackend.load(target).get(90,TimeUnit.SECONDS));
            for(String raw:inputs) {
                render(raw,"java",dictionary,null);
                List<Candidate> candidates=RimeBackend.candidates(raw);
                for(Candidate c:candidates)TextIntegrity.require(c.text,"JNI input="+raw);
                render(raw,"rime+java",dictionary,candidates);
            }
        } finally {
            JSONObject report=new JSONObject().put("device",android.os.Build.MODEL).put("sdk",android.os.Build.VERSION.SDK_INT)
                .put("sourceRows",sourceRows).put("sourceCodePoints",seen.size()).put("sourceFontGaps",sourceGaps)
                .put("renderedViews",views).put("queries",queries).put("displayFailures",failures);
            try(OutputStream out=new FileOutputStream(new File(target.getExternalFilesDir(null),"candidate-text-audit.json"))) {out.write(report.toString(2).getBytes(StandardCharsets.UTF_8));}
        }
        assertEquals("Unreadable displayed candidates; see candidate-text-audit.json (encoding and missing-font failures are separate)",0,failures.length());
    }
}
