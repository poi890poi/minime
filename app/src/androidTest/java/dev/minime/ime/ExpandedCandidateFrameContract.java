package dev.minime.ime;

import android.app.Activity;
import android.graphics.Rect;
import android.test.ActivityInstrumentationTestCase2;
import android.view.*;
import android.widget.*;
import dev.minime.core.*;
import java.io.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

/** Attached GPU frame submission, not physical touch or panel presentation. */
@SuppressWarnings("deprecation")
public abstract class ExpandedCandidateFrameContract<T extends Activity> extends ActivityInstrumentationTestCase2<T> {
    protected ExpandedCandidateFrameContract(Class<T> activity){super(activity);}
    private KeyboardView board;private CompositionEngine engine;
    private final StringBuilder accepted=new StringBuilder();
    private volatile Sample active;
    private static final class Sample {
        final CountDownLatch submitted=new CountDownLatch(1);
        long start,work,draw;volatile long submit;
        int before,after,yBefore,yAfter;
    }
    private void main(Runnable action){getInstrumentation().runOnMainSync(action);}
    private View find(View v,String description) {
        if(description.contentEquals(v.getContentDescription()==null?"":v.getContentDescription()))return v;
        if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++) {
            View found=find(((ViewGroup)v).getChildAt(i),description);if(found!=null)return found;
        }
        return null;
    }
    private ScrollView scroll(){return (ScrollView)find(board,"Expanded candidate list");}
    private int count(){ScrollView s=scroll();return s==null?0:((ViewGroup)s.getChildAt(0)).getChildCount();}
    private int offset(){ScrollView s=scroll();return s==null?0:s.getScrollY();}
    private void render(){board.render(engine,false,false,false,0,false,false,false,true,false,"Enter","");}
    private Sample measure(Runnable action)throws Exception {
        Sample s=new Sample();
        main(()->{s.before=count();s.yBefore=offset();active=s;s.start=System.nanoTime();action.run();s.work=System.nanoTime()-s.start;board.invalidate();});
        boolean observed=s.submitted.await(3,TimeUnit.SECONDS);
        main(()->{s.after=count();s.yAfter=offset();active=null;});
        assertTrue("A submitted frame must be observed after each action",observed);
        return s;
    }
    private String signature()throws Exception {
        StringBuilder identity=new StringBuilder().append(engine.preferred()).append('\n');
        for(Candidate c:engine.candidates())identity.append(c.text).append('\t').append(c.literal).append('\t').append(c.consumed).append('\t').append(c.alternateText()).append('\n');
        StringBuilder hex=new StringBuilder();for(byte b:MessageDigest.getInstance("SHA-256").digest(identity.toString().getBytes("UTF-8")))hex.append(String.format(Locale.ROOT,"%02x",b&255));return hex.toString();
    }
    private void record(PrintWriter out,int round,String raw,String action,int step,Sample s,int total,String hash) {
        out.println(round+"\t"+raw+"\t"+action+"\t"+step+"\t"+s.work+"\t"+(s.draw-s.start)+"\t"+(s.submit-s.start)+"\t"+s.before+"\t"+s.after+"\t"+s.yBefore+"\t"+s.yAfter+"\t"+total+"\t"+hash);out.flush();
    }
    public void testExpansionScrollAndSelectionFrames()throws Exception {
        Activity activity=getActivity();
        PhoneticDictionary dictionary=DictionaryRepository.load(activity).get(60,TimeUnit.SECONDS);
        Set<String> packs=new HashSet<>(Arrays.asList("taiwan","geography"));
        AddonDictionary addons=AddonRepository.load(activity,packs).get(60,TimeUnit.SECONDS);
        Set<String> prefixes=new LinkedHashSet<>();
        try(BufferedReader in=new BufferedReader(new InputStreamReader(getInstrumentation().getContext().getAssets().open("latency-inputs.tsv"),"UTF-8"))) {
            String line;int row=0,queries=0;
            while((line=in.readLine())!=null && queries<8) {
                String raw=line.split("\t")[2];if(row++%144!=0 || !raw.matches("[a-z]{3,16}"))continue;
                queries++;for(int i=1;i<=raw.length();i++)prefixes.add(raw.substring(0,i));
            }
        }
        assertEquals("Pinned prefix workload",46,prefixes.size());
        List<String> inputs=new ArrayList<>();int index=0;for(String raw:prefixes)if(index++%4==0)inputs.add(raw);
        String orientation=getClass().getSimpleName().contains("Landscape")?"landscape":"portrait";
        try(PrintWriter out=new PrintWriter(new File(activity.getExternalFilesDir(null),"expanded-frames-"+orientation+".tsv"),"UTF-8")) {
            out.println("round\tquery\taction\tstep\twork_ns\tdraw_ns\tsubmit_ns\tbefore_views\tafter_views\tbefore_y\tafter_y\tcandidates\tsha256");
            for(int round=0;round<2;round++)for(String raw:inputs) {
                List<Candidate> choices=new ArrayList<>(dictionary.convert(raw,false));choices.addAll(addons.lookup(raw,packs));
                main(()-> {
                    accepted.setLength(0);
                    engine=new CompositionEngine(new CompositionEngine.Editor() {
                        public void composing(String s){}public void commit(String s){accepted.append(s);}public void delete(){}public void enter(){}public void finish(){}
                    },Learning.NONE);
                    engine.dictionary(dictionary);engine.start(false,false,false,false);engine.addons(addons,packs);
                    board=new KeyboardView(activity,key->{},key->false,(points,caps)->{},action->{action.run();render();});
                    engine.candidateDisplay(new CandidateGlyphs(new TextView(activity).getPaint()));
                    engine.decoder(new CompositionEngine.Decoder() {
                        public void convert(PhoneticDictionary d,String r,boolean z,String c,Consumer<List<Candidate>> done){done.accept(new ArrayList<>(choices));}
                        public void query(PhoneticDictionary d,String r,boolean z,String c,boolean p,AddonDictionary a,Set<String> enabled,Consumer<List<Candidate>> done){done.accept(new ArrayList<>(choices));}
                    },()->{});
                    raw.codePoints().forEach(engine::type);activity.setContentView(board);render();
                    board.getViewTreeObserver().addOnPreDrawListener(()-> {
                        Sample s=active;if(s!=null && s.draw==0) {
                            s.draw=System.nanoTime();board.getViewTreeObserver().registerFrameCommitCallback(()->{s.submit=System.nanoTime();s.submitted.countDown();});
                        }
                        return true;
                    });
                });getInstrumentation().waitForIdleSync();
                main(()->assertTrue("Hardware accelerated visible keyboard",board.isHardwareAccelerated()));
                String hash=signature();int total=engine.candidates().size();Candidate tail=engine.candidates().get(total-1);
                View[] open={null};main(()->{open[0]=find(board,"Expand candidates");assertNotNull(open[0]);});
                record(out,round,raw,"expand",0,measure(()->assertTrue(open[0].performClick())),total,hash);
                boolean[] visible={false};int step=0;
                while(true) {
                    main(()->{View last=find(board,"Candidate "+tail.text);visible[0]=last!=null && last.getGlobalVisibleRect(new Rect());});
                    if(visible[0])break;
                    assertTrue("Bounded scroll must reach the tail",step<1000);
                    Sample sample=measure(()->{ScrollView s=scroll();assertNotNull(s);s.scrollTo(0,s.getScrollY()+Math.max(1,3*s.getHeight()/4));});
                    record(out,round,raw,"scroll",++step,sample,total,hash);
                    assertTrue("Scroll or allocation must advance",sample.yAfter>sample.yBefore || sample.after>sample.before);
                }
                assertEquals("Complete ordered list stays unchanged",hash,signature());
                View[] last={null};main(()->{last[0]=find(board,"Candidate "+tail.text);assertNotNull(last[0]);});
                record(out,round,raw,"select",step,measure(()->assertTrue(last[0].performClick())),total,hash);
                assertEquals("Deep visible choice keeps its own identity",tail.text,accepted.toString());
            }
        } finally {active=null;}
    }
}
