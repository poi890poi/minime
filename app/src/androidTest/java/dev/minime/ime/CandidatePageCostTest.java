package dev.minime.ime;

import android.test.InstrumentationTestCase;
import android.view.*;
import android.widget.TextView;
import android.graphics.*;
import dev.minime.core.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.security.MessageDigest;

/** Isolated UI work, not touch-to-presented-frame latency. Same APK runs on A/B. */
@SuppressWarnings("deprecation")
public final class CandidatePageCostTest extends InstrumentationTestCase {
    private static final class Deferred implements CompositionEngine.Decoder {
        Consumer<List<Candidate>> result;
        public void convert(PhoneticDictionary d,String r,boolean z,String c,Consumer<List<Candidate>> done){result=done;}
        public void query(PhoneticDictionary d,String r,boolean z,String c,boolean p,AddonDictionary a,Set<String> e,Consumer<List<Candidate>> done){result=done;}
    }
    private View find(View view,String description) {
        if(description.contentEquals(view.getContentDescription()==null?"":view.getContentDescription()))return view;
        if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++) {
            View found=find(((ViewGroup)view).getChildAt(i),description);if(found!=null)return found;
        }
        return null;
    }
    private void draw(KeyboardView view,Bitmap bitmap) {
        view.measure(View.MeasureSpec.makeMeasureSpec(bitmap.getWidth(),View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED));
        view.layout(0,0,bitmap.getWidth(),view.getMeasuredHeight());view.draw(new Canvas(bitmap));
    }
    public void testStripExpansionAndDeepSelectionCosts()throws Exception {
        android.content.Context context=getInstrumentation().getTargetContext();
        context.getSharedPreferences("settings",0).edit().clear().commit();context.getSharedPreferences("learning",0).edit().clear().commit();
        PhoneticDictionary dictionary=DictionaryRepository.load(context).get(60,TimeUnit.SECONDS);
        Set<String> packs=new HashSet<>(Arrays.asList("taiwan","geography"));
        AddonDictionary addons=AddonRepository.load(context,packs).get(60,TimeUnit.SECONDS);
        Set<String> prefixes=new LinkedHashSet<>();
        try(BufferedReader in=new BufferedReader(new InputStreamReader(getInstrumentation().getContext().getAssets().open("latency-inputs.tsv"),"UTF-8"))) {
            String line;int row=0,queries=0;
            while((line=in.readLine())!=null && queries<8) {
                String raw=line.split("\t")[2];if(row++%144!=0 || !raw.matches("[a-z]{3,16}"))continue;
                queries++;for(int i=1;i<=raw.length();i++)prefixes.add(raw.substring(0,i));
            }
        }
        CandidateGlyphs[] glyphs={null};Bitmap[] bitmap={null};
        getInstrumentation().runOnMainSync(()-> {
            glyphs[0]=new CandidateGlyphs(new TextView(context).getPaint());
            bitmap[0]=Bitmap.createBitmap(context.getResources().getDisplayMetrics().widthPixels,context.getResources().getDisplayMetrics().heightPixels,Bitmap.Config.ARGB_8888);
        });
        MessageDigest digest=MessageDigest.getInstance("SHA-256");
        try(PrintWriter out=new PrintWriter(new File(context.getExternalFilesDir(null),"candidate-page-cost.tsv"),"UTF-8")) {
            out.println("round\tquery\tapply_ns\tstrip_ns\texpand_ns\tselect_ns\tapply_probes\tstrip_probes\texpand_probes\tcandidates\tsha256");
            for(int round=0;round<2;round++)for(String raw:prefixes) {
                List<Candidate> choices=new ArrayList<>(dictionary.convert(raw,false));choices.addAll(addons.lookup(raw,packs));
                long[] elapsed=new long[4];int[] probes=new int[4],count={0};String[] signature={""};
                getInstrumentation().runOnMainSync(()-> {
                    StringBuilder accepted=new StringBuilder();
                    CompositionEngine engine=new CompositionEngine(new CompositionEngine.Editor() {
                        public void composing(String s){}public void commit(String s){accepted.append(s);}public void delete(){}public void enter(){}public void finish(){}
                    },new LocalLearning(context));
                    Deferred decoder=new Deferred();engine.dictionary(dictionary);engine.start(false,false,false,false);
                    engine.candidateDisplay(s->{probes[0]++;return glyphs[0].test(s);});engine.addons(addons,packs);engine.phraseLearning(true);engine.decoder(decoder,()->{});
                    raw.codePoints().forEach(engine::type);assertNotNull(decoder.result);probes[0]=0;
                    KeyboardView keyboard=new KeyboardView(context,key->{},key->false,(points,caps)->{});
                    long at=System.nanoTime();decoder.result.accept(new ArrayList<>(choices));elapsed[0]=System.nanoTime()-at;probes[1]=probes[0];
                    at=System.nanoTime();keyboard.render(engine,false,false,false,0,false,false,false,true,false,"Enter","");draw(keyboard,bitmap[0]);elapsed[1]=System.nanoTime()-at;probes[2]=probes[0]-probes[1];
                    View expand=find(keyboard,"Expand candidates");assertNotNull(expand);
                    at=System.nanoTime();expand.performClick();draw(keyboard,bitmap[0]);elapsed[2]=System.nanoTime()-at;probes[3]=probes[0]-probes[1]-probes[2];
                    List<Candidate> complete=engine.candidates();count[0]=complete.size();
                    StringBuilder identity=new StringBuilder().append(engine.preferred()).append('\n');
                    for(Candidate c:complete)identity.append(c.text).append('\t').append(c.literal).append('\t').append(c.consumed).append('\t').append(c.alternateText()).append('\n');
                    byte[] hash=digest.digest(identity.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));StringBuilder hex=new StringBuilder();for(byte b:hash)hex.append(String.format(Locale.ROOT,"%02x",b&255));signature[0]=hex.toString();
                    Candidate last=complete.get(complete.size()-1);
                    at=System.nanoTime();engine.selectCandidate(last,engine.compositionId());elapsed[3]=System.nanoTime()-at;
                    assertEquals("Deep choice retains output identity",last.text,accepted.toString());
                });
                out.println(round+"\t"+raw+"\t"+elapsed[0]+"\t"+elapsed[1]+"\t"+elapsed[2]+"\t"+elapsed[3]+"\t"+probes[1]+"\t"+probes[2]+"\t"+probes[3]+"\t"+count[0]+"\t"+signature[0]);out.flush();
            }
        } finally {bitmap[0].recycle();}
    }
}
