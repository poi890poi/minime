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
    private volatile Sample active;
    private final List<Sample> samples=new ArrayList<>();
    private static final class Sample {
        String mode,query,expected,action;int interval;
        volatile long down,up,callback,rawDraw,rawSubmit,candidateDraw,candidateSubmit,pressedSubmit;
        View key;float x,y;
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
        if(s.action.equals("space"))return BaseInputConnection.getComposingSpanEnd(activity.text.getText())<0 && !activity.text.getText().toString().equals(s.expected);
        return activity.text.getText().toString().equals(s.expected);
    }
    private void inject(Sample s,int action) {
        long now=SystemClock.uptimeMillis();
        if(action==MotionEvent.ACTION_DOWN)s.down=now*1000000;else s.up=now*1000000;
        MotionEvent event=MotionEvent.obtain(s.down/1000000,now,action,s.x,s.y,0);
        event.setSource(InputDevice.SOURCE_TOUCHSCREEN);
        try {assertTrue(getInstrumentation().getUiAutomation().injectInputEvent(event,false));}finally{event.recycle();}
    }
    public void testTouchToSubmittedFrames()throws Exception {
        Context context=getInstrumentation().getTargetContext();
        context.getSharedPreferences("settings",0).edit().clear().putBoolean("addon_poj",true).putBoolean("addon_japanese",true).commit();
        activity=getActivity();
        DictionaryRepository.load(context).get(60,TimeUnit.SECONDS);
        AddonRepository.load(context).get(60,TimeUnit.SECONDS);
        assertTrue(RimeBackend.load(context).get(60,TimeUnit.SECONDS));
        try(ParcelFileDescriptor fd=getInstrumentation().getUiAutomation().executeShellCommand("ime set dev.minime.ime/.MiniMeService");InputStream in=new ParcelFileDescriptor.AutoCloseInputStream(fd)){while(in.read()!=-1){}}
        focus(activity.text);
        getInstrumentation().runOnMainSync(()-> {
            for(View root:WindowInspector.getGlobalWindowViews()){keyboard=keyboard(root);if(keyboard!=null)break;}
            assertNotNull("Visible installed keyboard",keyboard);
            try {Field field=KeyboardView.class.getDeclaredField("snapshotEngine");field.setAccessible(true);engine=(CompositionEngine)field.get(keyboard);}catch(Exception e){throw new RuntimeException(e);}
            assertTrue("Hardware accelerated editor",activity.text.isHardwareAccelerated());
            assertTrue("Hardware accelerated IME",keyboard.isHardwareAccelerated());
            activity.text.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int a,int c,int f){}public void onTextChanged(CharSequence s,int a,int b,int c){}public void afterTextChanged(Editable text){Sample s=active;if(s!=null && s.up>0 && s.callback==0 && matches(s))s.callback=System.nanoTime();}});
            activity.text.getViewTreeObserver().addOnPreDrawListener(()-> {
                Sample s=active;if(s!=null && s.up>0 && s.rawDraw==0 && matches(s)) {
                    s.rawDraw=System.nanoTime();activity.text.getViewTreeObserver().registerFrameCommitCallback(()->s.rawSubmit=System.nanoTime());
                }
                return true;
            });
            keyboard.getViewTreeObserver().addOnPreDrawListener(()-> {
                Sample s=active;if(s==null)return true;
                if(s.pressedSubmit==0 && s.up==0 && s.key.isPressed())keyboard.getViewTreeObserver().registerFrameCommitCallback(()->{if(s.pressedSubmit==0)s.pressedSubmit=System.nanoTime();});
                if(s.up>0 && s.candidateDraw==0 && !s.action.equals("space") && engine.raw().equals(s.expected) && !engine.predictionPending()) {
                    s.candidateDraw=System.nanoTime();keyboard.getViewTreeObserver().registerFrameCommitCallback(()->s.candidateSubmit=System.nanoTime());
                }
                return true;
            });
        });
        List<String> queries=new ArrayList<>();
        try(BufferedReader in=new BufferedReader(new InputStreamReader(getInstrumentation().getContext().getAssets().open("latency-inputs.tsv"),"UTF-8"))) {
            String line;int row=0;while((line=in.readLine())!=null){String[] p=line.split("\t");if(row++%144==0 && p[2].matches("[a-z]{3,16}"))queries.add(p[2]);}
        }
        assertTrue("Diverse frozen corpus sample",queries.size()>=8);
        try {for(String mode:new String[]{"chinese","english","taiwanese_english","japanese_english"}) {
            active=null;
            context.getSharedPreferences("settings",0).edit().putString("mixed_mode",mode.equals("english")?"chinese":mode).putBoolean("english_mode",mode.equals("english")).commit();
            focus(activity.url);focus(activity.text);
            for(int interval:new int[]{150,60})for(String query:queries.subList(0,8)) {
                active=null;getInstrumentation().runOnMainSync(()->activity.text.setText(""));focus(activity.url);focus(activity.text);
                String expected="";
                for(char c:(query+" ").toCharArray()) {
                    Sample s=new Sample();s.mode=mode;s.query=query;s.interval=interval;s.action=c==' '?"space":"key";
                    if(c!=' ')expected+=c;s.expected=expected;
                    String label=c==' '?"Space":String.valueOf(c);
                    getInstrumentation().runOnMainSync(()->{s.key=find(keyboard,label);if(s.key!=null){int[] xy=new int[2];s.key.getLocationOnScreen(xy);s.x=xy[0]+s.key.getWidth()/2f;s.y=xy[1]+s.key.getHeight()/2f;}});assertNotNull(label,s.key);
                    samples.add(s);active=s;long start=SystemClock.uptimeMillis();inject(s,MotionEvent.ACTION_DOWN);SystemClock.sleep(25);inject(s,MotionEvent.ACTION_UP);
                    long rest=interval-(SystemClock.uptimeMillis()-start);if(rest>0)SystemClock.sleep(rest);
                }
                SystemClock.sleep(160);
            }
        }
        } finally {active=null;
        try(PrintWriter out=new PrintWriter(new File(context.getExternalFilesDir(null),"touch-latency.tsv"),"UTF-8")) {
            out.println("mode\tinterval_ms\tquery\texpected\taction\tdown_ns\tup_ns\teditor_callback_ns\teditor_pre_draw_ns\teditor_submit_ns\tcandidate_pre_draw_ns\tcandidate_submit_ns\tpressed_submit_ns");
            for(Sample s:samples)out.println(s.mode+"\t"+s.interval+"\t"+s.query+"\t"+s.expected+"\t"+s.action+"\t"+s.down+"\t"+s.up+"\t"+s.callback+"\t"+s.rawDraw+"\t"+s.rawSubmit+"\t"+s.candidateDraw+"\t"+s.candidateSubmit+"\t"+s.pressedSubmit);
        }}
    }
}
