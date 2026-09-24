package dev.minime.ime;

import android.content.Context;
import android.graphics.Rect;
import android.os.*;
import android.test.ActivityInstrumentationTestCase2;
import android.text.*;
import android.view.*;
import android.view.inputmethod.*;
import android.view.inspector.WindowInspector;
import dev.minime.core.CompositionEngine;
import dev.minime.testing.CommitObservation;
import dev.minime.testing.LanguageTimingInputs;
import dev.minime.testing.LanguageTimingInputs.Query;
import java.io.*;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.TimeUnit;

/** OS-injected event to frame submission. Never a physical contact/presentation metric. */
@SuppressWarnings("deprecation")
public final class TouchLatencyTest extends ActivityInstrumentationTestCase2<EditorTestActivity> {
    public TouchLatencyTest(){super(EditorTestActivity.class);}
    private EditorTestActivity activity;
    private KeyboardView keyboard;
    private CompositionEngine engine;
    private CandidateTimingProbe timing;
    private volatile Sample active;
    private volatile Sample editorActive;
    private volatile Sample candidateActive;
    private final List<Sample> samples=new ArrayList<>();
    private static final class Sample {
        String mode,query,expected,action;int interval;Query labelled;
        volatile long down,up,callback,rawDraw,rawSubmit,candidateDraw,candidateSubmit,pressedSubmit;
        View key;float x,y;
    }
    private static final class Target {
        final View key;final float x,y;
        Target(View key) {
            this.key=key;int[] xy=new int[2];key.getLocationOnScreen(xy);
            x=xy[0]+key.getWidth()/2f;y=xy[1]+key.getHeight()/2f;
        }
    }
    private static View find(View v,String description) {
        if(description.contentEquals(v.getContentDescription()==null?"":v.getContentDescription()) && v.isShown())return v;
        if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++) {
            View result=find(((ViewGroup)v).getChildAt(i),description);if(result!=null)return result;
        }
        return null;
    }
    private static KeyboardView keyboard(View v) {
        if(v instanceof KeyboardView)return (KeyboardView)v;
        if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++) {
            KeyboardView result=keyboard(((ViewGroup)v).getChildAt(i));if(result!=null)return result;
        }
        return null;
    }
    private void focus(android.widget.EditText field) {
        getInstrumentation().runOnMainSync(()-> {
            field.requestFocus();((InputMethodManager)activity.getSystemService(Context.INPUT_METHOD_SERVICE)).showSoftInput(field,InputMethodManager.SHOW_IMPLICIT);
        });
        SystemClock.sleep(180);
    }
    private boolean matches(Sample s) {
        if(s.action.equals("space"))return CommitObservation.finished(BaseInputConnection.getComposingSpanEnd(activity.text.getText()),activity.text.getText().toString(),s.expected,engine.raw());
        return activity.text.getText().toString().equals(s.expected);
    }
    private void inject(Sample s,int action) {
        long now=SystemClock.uptimeMillis();
        if(action==MotionEvent.ACTION_DOWN)s.down=now*1000000;
        else {s.up=now*1000000;editorActive=s;candidateActive=s.action.equals("key")?s:null;}
        MotionEvent event=MotionEvent.obtain(s.down/1000000,now,action,s.x,s.y,0);
        event.setSource(InputDevice.SOURCE_TOUCHSCREEN);
        try {assertTrue(getInstrumentation().getUiAutomation().injectInputEvent(event,false));}finally{event.recycle();}
    }
    public void testTouchToSubmittedFrames()throws Exception {runReplay(true);}
    public void testTouchWithoutStageHooks()throws Exception {runReplay(false);}
    public void testChineseStagesShard0()throws Exception {runReplay(true,"chinese",0);}
    public void testTaiwaneseStagesShard0()throws Exception {runReplay(true,"taiwanese_english",0);}
    public void testJapaneseStagesShard0()throws Exception {runReplay(true,"japanese_english",0);}
    public void testChineseShard0()throws Exception {runReplay(false,"chinese",0);}
    public void testChineseShard1()throws Exception {runReplay(false,"chinese",1);}
    public void testChineseShard2()throws Exception {runReplay(false,"chinese",2);}
    public void testChineseShard3()throws Exception {runReplay(false,"chinese",3);}
    public void testEnglishShard0()throws Exception {runReplay(false,"english",0);}
    public void testEnglishShard1()throws Exception {runReplay(false,"english",1);}
    public void testEnglishShard2()throws Exception {runReplay(false,"english",2);}
    public void testEnglishShard3()throws Exception {runReplay(false,"english",3);}
    public void testTaiwaneseShard0()throws Exception {runReplay(false,"taiwanese_english",0);}
    public void testTaiwaneseShard1()throws Exception {runReplay(false,"taiwanese_english",1);}
    public void testTaiwaneseShard2()throws Exception {runReplay(false,"taiwanese_english",2);}
    public void testTaiwaneseShard3()throws Exception {runReplay(false,"taiwanese_english",3);}
    public void testJapaneseShard0()throws Exception {runReplay(false,"japanese_english",0);}
    public void testJapaneseShard1()throws Exception {runReplay(false,"japanese_english",1);}
    public void testJapaneseShard2()throws Exception {runReplay(false,"japanese_english",2);}
    public void testJapaneseShard3()throws Exception {runReplay(false,"japanese_english",3);}
    private void runReplay(boolean stageHooks)throws Exception {runReplay(stageHooks,null,0);}
    private void runReplay(boolean stageHooks,String selectedMode,int shard)throws Exception {
        List<Query> queries=new ArrayList<>();
        if(selectedMode!=null)queries.addAll(LanguageTimingInputs.shard(LanguageTimingInputs.read(getInstrumentation().getContext().getAssets().open("language-timing-inputs.tsv")),selectedMode,shard));
        else {
            try(BufferedReader in=new BufferedReader(new InputStreamReader(getInstrumentation().getContext().getAssets().open("latency-inputs.tsv"),"UTF-8"))) {
                String line;int row=0;while((line=in.readLine())!=null){String[] p=line.split("\t");if(row++%144==0 && p[2].matches("[a-z]{3,16}"))queries.add(Query.legacy(p[2]));}
            }
            assertTrue("Diverse frozen corpus sample",queries.size()>=8);queries=new ArrayList<>(queries.subList(0,8));
        }
        Context context=getInstrumentation().getTargetContext();
        context.getSharedPreferences("settings",0).edit().clear().putBoolean("addon_poj",true).putBoolean("addon_japanese",true).commit();
        activity=getActivity();
        DictionaryRepository.load(context).get(60,TimeUnit.SECONDS);
        AddonRepository.load(context).get(60,TimeUnit.SECONDS);
        assertTrue(RimeBackend.load(context).get(60,TimeUnit.SECONDS));
        try(ParcelFileDescriptor fd=getInstrumentation().getUiAutomation().executeShellCommand("ime set app.minime.keyboard/dev.minime.ime.MiniMeService");InputStream in=new ParcelFileDescriptor.AutoCloseInputStream(fd)){while(in.read()!=-1){}}
        focus(activity.text);
        getInstrumentation().runOnMainSync(()-> {
            for(View root:WindowInspector.getGlobalWindowViews()){keyboard=keyboard(root);if(keyboard!=null)break;}
            assertNotNull("Visible installed keyboard",keyboard);
            try {Field field=KeyboardView.class.getDeclaredField("snapshotEngine");field.setAccessible(true);engine=(CompositionEngine)field.get(keyboard);}catch(Exception e){throw new RuntimeException(e);}
            if(stageHooks)try {timing=new CandidateTimingProbe(engine,keyboard);}catch(Exception e){throw new RuntimeException(e);}
            assertTrue("Hardware accelerated editor",activity.text.isHardwareAccelerated());
            assertTrue("Hardware accelerated IME",keyboard.isHardwareAccelerated());
            activity.text.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int a,int c,int f){}public void onTextChanged(CharSequence s,int a,int b,int c){}public void afterTextChanged(Editable text){Sample s=editorActive;if(s!=null && s.up>0 && s.callback==0 && matches(s))s.callback=System.nanoTime();}});
            activity.text.getViewTreeObserver().addOnPreDrawListener(()-> {
                Sample s=editorActive;if(s!=null && s.up>0 && s.rawDraw==0 && matches(s)) {
                    s.rawDraw=System.nanoTime();activity.text.getViewTreeObserver().registerFrameCommitCallback(()->s.rawSubmit=System.nanoTime());
                }
                return true;
            });
            keyboard.getViewTreeObserver().addOnPreDrawListener(()-> {
                Sample s=active;if(s==null)return true;
                if(s.pressedSubmit==0 && s.up==0 && s.key.isPressed())keyboard.getViewTreeObserver().registerFrameCommitCallback(()->{if(s.pressedSubmit==0)s.pressedSubmit=System.nanoTime();});
                // Finger-down on the next key does not supersede the current
                // spelling. Observe it until the next key is released.
                Sample candidate=candidateActive;
                if(candidate!=null && candidate.candidateDraw==0 && engine.raw().equals(candidate.expected) && !engine.predictionPending()) {
                    candidate.candidateDraw=System.nanoTime();keyboard.getViewTreeObserver().registerFrameCommitCallback(()->candidate.candidateSubmit=System.nanoTime());
                }
                return true;
            });
        });
        String[] modes=selectedMode==null?new String[]{"chinese","english","taiwanese_english","japanese_english"}:new String[]{selectedMode};
        try {for(String mode:modes) {
            active=null;editorActive=null;candidateActive=null;
            context.getSharedPreferences("settings",0).edit().putString("mixed_mode",mode.equals("english")?"chinese":mode).putBoolean("english_mode",mode.equals("english")).commit();
            focus(activity.url);focus(activity.text);
            for(int interval:new int[]{150,60})for(Query labelled:queries) {
                String query=labelled.raw;
                active=null;editorActive=null;candidateActive=null;getInstrumentation().runOnMainSync(()->activity.text.setText(""));focus(activity.url);focus(activity.text);
                Map<Character,Target> targets=new HashMap<>();
                // Resolve the stable board geometry before timing the query.
                // A per-letter main-thread lookup slows input when the app is busy,
                // giving the slower build more time to finish its candidates.
                getInstrumentation().runOnMainSync(()->{
                    for(char c:(query+" ").toCharArray())if(!targets.containsKey(c)) {
                        String label=c==' '?"Space":String.valueOf(c);View key=find(keyboard,label);
                        assertNotNull(label,key);targets.put(c,new Target(key));
                    }
                });
                String expected="";
                for(char c:(query+" ").toCharArray()) {
                    Sample s=new Sample();s.mode=mode;s.query=query;s.interval=interval;s.action=c==' '?"space":"key";s.labelled=labelled;
                    if(c!=' ')expected+=c;s.expected=expected;
                    Target target=targets.get(c);s.key=target.key;s.x=target.x;s.y=target.y;
                    samples.add(s);active=s;long start=SystemClock.uptimeMillis();inject(s,MotionEvent.ACTION_DOWN);SystemClock.sleep(25);inject(s,MotionEvent.ACTION_UP);
                    long rest=interval-(SystemClock.uptimeMillis()-start);if(rest>0)SystemClock.sleep(rest);
                }
                SystemClock.sleep(160);
            }
        }
        } finally {active=null;editorActive=null;candidateActive=null;
        if(timing!=null)getInstrumentation().runOnMainSync(()->{try {timing.write(context.getExternalFilesDir(null));}catch(Exception e){throw new RuntimeException(e);}finally{try{timing.close();}catch(Exception e){throw new RuntimeException(e);}}});
        // Existing aggregate counters, read after the replay; no per-key hook.
        getInstrumentation().runOnMainSync(()-> {
            try {
                Field field=CompositionEngine.class.getDeclaredField("decoder");field.setAccessible(true);
                Object decoder=field.get(engine);assertTrue(decoder instanceof AsyncDecoder);
                dev.minime.core.DecodePipeline.Stats stats=((AsyncDecoder)decoder).stats;
                try(PrintWriter out=new PrintWriter(new File(context.getExternalFilesDir(null),"candidate-work.tsv"),"UTF-8")) {
                    out.println("metric\tcount\twork_ns");
                    out.println("requests\t"+stats.requests.get()+"\t0");
                    out.println("delivered\t"+stats.delivered.get()+"\t0");
                    out.println("cancelled\t"+stats.cancelled.get()+"\t0");
                    out.println("stale_delivery\t"+stats.staleDelivery.get()+"\t0");
                    for(int i=0;i<3;i++)out.println(new String[]{"base","rime","addons"}[i]+"\t"+stats.calls[i].get()+"\t"+stats.nanos[i].get());
                }
            } catch(Exception e){throw new RuntimeException(e);}
        });
        try(PrintWriter out=new PrintWriter(new File(context.getExternalFilesDir(null),"touch-latency.tsv"),"UTF-8")) {
            out.println("mode\tinterval_ms\tquery\texpected\taction\tdown_ns\tup_ns\teditor_callback_ns\teditor_pre_draw_ns\teditor_submit_ns\tcandidate_pre_draw_ns\tcandidate_submit_ns\tpressed_submit_ns\tquery_id\tsource\tgenre\tcondition");
            for(Sample s:samples)out.println(s.mode+"\t"+s.interval+"\t"+s.query+"\t"+s.expected+"\t"+s.action+"\t"+s.down+"\t"+s.up+"\t"+s.callback+"\t"+s.rawDraw+"\t"+s.rawSubmit+"\t"+s.candidateDraw+"\t"+s.candidateSubmit+"\t"+s.pressedSubmit+"\t"+s.labelled.id+"\t"+s.labelled.source+"\t"+s.labelled.genre+"\t"+s.labelled.condition);
        }}
    }
}
