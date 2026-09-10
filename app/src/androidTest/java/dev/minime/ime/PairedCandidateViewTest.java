package dev.minime.ime;

import android.test.ActivityInstrumentationTestCase2;
import android.view.*;
import android.os.SystemClock;
import dev.minime.core.*;
import java.io.*;
import java.util.*;

/** Real Android touch dispatch; mechanical fixtures cannot promote production vocabulary. */
@SuppressWarnings("deprecation")
public final class PairedCandidateViewTest extends ActivityInstrumentationTestCase2<EditorTestActivity> {
    public PairedCandidateViewTest() {super(EditorTestActivity.class);}
    private KeyboardView view;private CompositionEngine engine;private String committed="";
    private float x,y;private long down;
    private void setup()throws Throwable {
        setup("abc-def","abcdef");
    }
    private void setup(String output,String raw)throws Throwable {
        PhoneticDictionary dictionary=DictionaryRepository.load(getInstrumentation().getTargetContext()).get(30,java.util.concurrent.TimeUnit.SECONDS);
        PairedForms pairs=PairedForms.read(new StringReader("poj\t"+output+"\t甲乙\titaigi:1\n"));
        AddonDictionary addon=AddonDictionary.read(new StringReader("poj\t"+raw+"\t"+output+"\titaigi:1\tfixture\n"),pairs);
        EditorTestActivity host=getActivity();
        runTestOnUiThread(()-> {
            engine=new CompositionEngine(new CompositionEngine.Editor() {
                public void composing(String s){}public void commit(String s){committed+=s;}public void delete(){}public void enter(){}public void finish(){}
            },Learning.NONE);
            engine.dictionary(dictionary);engine.start(false,false,false,false);engine.addons(addon,Collections.singleton("poj"));engine.switchMode(InputMode.TAIWANESE,false);
            view=new KeyboardView(host,k->{},k->false,(p,c)->{},action->{action.run();render();});
            raw.codePoints().forEach(engine::type);render();host.setContentView(view);
            int width=Math.round(400*view.getResources().getDisplayMetrics().density);
            view.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED));view.layout(0,0,width,view.getMeasuredHeight());
            View word=find(view,"Candidate "+output);assertNotNull(word);
            android.graphics.Rect b=new android.graphics.Rect(0,0,word.getWidth(),word.getHeight());view.offsetDescendantRectToMyCoords(word,b);x=b.exactCenterX();y=b.exactCenterY();
        });
    }
    private void render(){view.render(engine,false,false,false,0,false,false,false,true,false,"Enter","");}
    private void touch(int action,float atX,float atY)throws Throwable {
        runTestOnUiThread(()-> {if(action==MotionEvent.ACTION_DOWN)down=SystemClock.uptimeMillis();MotionEvent e=MotionEvent.obtain(down,SystemClock.uptimeMillis(),action,atX,atY,0);view.dispatchTouchEvent(e);e.recycle();});
    }
    public void testHoldThenReleaseCommitsExactlyOnce()throws Throwable {
        setup();touch(MotionEvent.ACTION_DOWN,x,y);SystemClock.sleep(ViewConfiguration.getLongPressTimeout()+200);touch(MotionEvent.ACTION_UP,x,y);
        getInstrumentation().waitForIdleSync();assertEquals("甲乙",committed);
    }
    public void testCancellationCannotCommitAlternate()throws Throwable {
        setup();touch(MotionEvent.ACTION_DOWN,x,y);touch(MotionEvent.ACTION_CANCEL,x,y);SystemClock.sleep(ViewConfiguration.getLongPressTimeout()+200);
        assertEquals("",committed);
    }
    public void testDragCannotCommitEitherForm()throws Throwable {
        setup();touch(MotionEvent.ACTION_DOWN,x,y);touch(MotionEvent.ACTION_MOVE,x+150,y);SystemClock.sleep(ViewConfiguration.getLongPressTimeout()+200);touch(MotionEvent.ACTION_UP,x+150,y);
        getInstrumentation().waitForIdleSync();assertEquals("",committed);
    }
    public void testHeldPairCannotCrossModeChange()throws Throwable {
        setup();touch(MotionEvent.ACTION_DOWN,x,y);runTestOnUiThread(()->{engine.switchMode(InputMode.JAPANESE,false);render();});
        SystemClock.sleep(ViewConfiguration.getLongPressTimeout()+200);touch(MotionEvent.ACTION_UP,x,y);getInstrumentation().waitForIdleSync();assertEquals("",committed);
    }
    public void testRawTwinHoldRetainsHanAlternate()throws Throwable {
        setup("zavora","zavora");touch(MotionEvent.ACTION_DOWN,x,y);
        SystemClock.sleep(ViewConfiguration.getLongPressTimeout()+200);touch(MotionEvent.ACTION_UP,x,y);
        getInstrumentation().waitForIdleSync();assertEquals("甲乙",committed);
    }
    public void testRawTwinTapRetainsPhonetics()throws Throwable {
        setup("zavora","zavora");touch(MotionEvent.ACTION_DOWN,x,y);touch(MotionEvent.ACTION_UP,x,y);
        getInstrumentation().waitForIdleSync();assertEquals("zavora",committed);
    }
    public void testRawTwinAccessibilityKeepsHanIdentity()throws Throwable {
        setup("zavora","zavora");
        runTestOnUiThread(()-> {
            engine.pairedTaiwanese(true,true);render();
            View word=find(view,"Candidate 甲乙");assertNotNull(word);word.performClick();
        });
        getInstrumentation().waitForIdleSync();assertEquals("甲乙",committed);
    }
    private static View find(View v,String description) {
        if(description.contentEquals(v.getContentDescription()==null?"":v.getContentDescription()))return v;
        if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++){View found=find(((ViewGroup)v).getChildAt(i),description);if(found!=null)return found;}return null;
    }
}
