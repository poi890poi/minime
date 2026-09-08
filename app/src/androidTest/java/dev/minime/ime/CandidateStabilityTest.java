package dev.minime.ime;

import android.test.ActivityInstrumentationTestCase2;
import android.view.*;
import dev.minime.core.*;
import java.util.*;
import java.util.function.Consumer;

/** Deterministic decoder timing through the real Android candidate presentation. */
@SuppressWarnings("deprecation")
public final class CandidateStabilityTest extends ActivityInstrumentationTestCase2<EditorTestActivity> {
    public CandidateStabilityTest() {super(EditorTestActivity.class);}
    private PhoneticDictionary dictionary;
    @Override protected void setUp() throws Exception {
        super.setUp();dictionary=DictionaryRepository.load(getInstrumentation().getTargetContext()).get(30,java.util.concurrent.TimeUnit.SECONDS);
    }
    private final List<Consumer<List<Candidate>>> replies=new ArrayList<>();
    private CompositionEngine engine;
    private KeyboardView view;
    private String committed="";
    private void setup() {
        engine=new CompositionEngine(new CompositionEngine.Editor() {
            public void composing(String s) {} public void commit(String s) {committed+=s;} public void delete() {}
            public void enter() {} public void finish() {}
        },Learning.NONE);
        engine.dictionary(dictionary);engine.start(false,false,false,false);
        view=new KeyboardView(getInstrumentation().getTargetContext(),key->{},key->false,(path,caps)->{},action->{action.run();render();});
        engine.decoder((dict,raw,zh,context,done)->replies.add(done),this::render);
        engine.type('n');reply();render();
    }
    private void reply() {replies.get(replies.size()-1).accept(Collections.singletonList(new Candidate("一",false,1000)));}
    private void render() {view.render(engine,false,false,false,0,false,false,false,true,true,"Enter","");}
    public void testBufferSurvivesLiteralAndUnavailablePredictions() throws Throwable {
        runTestOnUiThread(()-> {
            setup();android.widget.FrameLayout buffer=(android.widget.FrameLayout)view.compositionAnnotation();
            View raw=buffer.getChildAt(1);assertEquals(View.VISIBLE,raw.getVisibility());
            engine.type('i');render();assertEquals(View.VISIBLE,raw.getVisibility());
            replies.get(replies.size()-1).accept(Collections.emptyList());
            assertEquals("Literal default must not hide nonempty composition",View.VISIBLE,raw.getVisibility());
            for(char c:"haotime".toCharArray()) {
                engine.type(c);render();assertEquals(View.VISIBLE,raw.getVisibility());reply();
                assertEquals("Completed prediction must not remove buffer",View.VISIBLE,raw.getVisibility());
            }
            engine.abandon();render();assertEquals("No stale buffer after composition ends",View.GONE,raw.getVisibility());
        });
    }
    public void testPendingQueryKeepsVisibleCandidates() throws Throwable {
        runTestOnUiThread(()-> {
            setup();View word=find(view,"Candidate 一"),scroll=find(view,"Candidate list");assertNotNull(word);
            engine.type('i');render();
            assertNotNull("Pending prediction must not erase the Chinese row",find(view,"Candidate 一"));
            assertNotNull("Raw spelling advances immediately",find(view,"Exact input ni"));
            assertSame("Pending query retains the actual word view",word,find(view,"Candidate 一"));
            assertSame(scroll,find(view,"Candidate list"));
            reply();assertNotNull(find(view,"Candidate 一"));
        });
    }
    public void testExpandedPendingAndSelection() throws Throwable {
        runTestOnUiThread(()-> {
            setup();find(view,"Expand candidates").performClick();
            View grid=find(view,"Expanded candidate list");assertNotNull(grid);
            engine.type('i');render();assertSame(grid,find(view,"Expanded candidate list"));
            find(grid,"Candidate 一").performClick();assertEquals("",committed);
            reply();assertEquals("一",committed);assertEquals("",engine.raw());
            assertNull(find(view,"Expanded candidate list"));
        });
    }
    public void testOldChoiceCannotCrossEditorOrPrivateField() throws Throwable {
        runTestOnUiThread(()-> {
            setup();View old=find(view,"Candidate 一");
            engine.start(false,true,true,true,true);view.render(engine,false,false,false,0,false,true,true,false,false,"Enter","");
            assertNull(find(view,"Candidate 一"));old.performClick();assertEquals("",committed);
            engine.start(false,false,false,false);engine.type('n');render();
            assertNull("New composition cannot retain old results",find(view,"Candidate 一"));
            reply();old.performClick();assertEquals("",committed);assertEquals("n",engine.raw());
        });
    }
    public void testCandidateTouchSurvivesResultReplacement() throws Throwable {
        gesture(false);
    }
    public void testCandidateTouchCancellationAppliesLatestRow() throws Throwable {
        gesture(true);
    }
    private void gesture(boolean cancel) throws Throwable {
        EditorTestActivity host=getActivity();
        runTestOnUiThread(()-> {
            setup();host.setContentView(view);assertTrue("Touch test owns an attached view",view.isAttachedToWindow());
            int width=Math.round(400*view.getResources().getDisplayMetrics().density);
            view.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED));
            view.layout(0,0,width,view.getMeasuredHeight());
            View word=find(view,"Candidate 一"),scroll=find(view,"Candidate list");
            android.graphics.Rect bounds=new android.graphics.Rect(0,0,word.getWidth(),word.getHeight());view.offsetDescendantRectToMyCoords(word,bounds);
            long now=android.os.SystemClock.uptimeMillis();
            MotionEvent down=MotionEvent.obtain(now,now,MotionEvent.ACTION_DOWN,bounds.exactCenterX(),bounds.exactCenterY(),0);
            view.dispatchTouchEvent(down);down.recycle();
            engine.type('i');render();
            replies.get(replies.size()-1).accept(Arrays.asList(new Candidate("二",false,2000),new Candidate("一",false,1000)));
            assertSame("Prediction cannot remove the touched row",scroll,find(view,"Candidate list"));
            assertNull("Replacement waits until gesture ends",find(view,"Candidate 二"));
            MotionEvent up=MotionEvent.obtain(now,now+40,cancel?MotionEvent.ACTION_CANCEL:MotionEvent.ACTION_UP,bounds.exactCenterX(),bounds.exactCenterY(),0);
            view.dispatchTouchEvent(up);up.recycle();
        });
        // TextView posts its click after UP; observe it after the main queue drains.
        getInstrumentation().waitForIdleSync();
        runTestOnUiThread(()-> {
            assertEquals(cancel?"":"一",committed);
            assertEquals(cancel?"ni":"",engine.raw());
            if(cancel)assertNotNull(find(view,"Candidate 二"));
        });
    }
    public void testUnchangedRenderPreservesCandidateViews() throws Throwable {
        runTestOnUiThread(()-> {
            setup();View scroll=find(view,"Candidate list"),word=find(view,"Candidate 一");
            render();
            assertSame("Unchanged renders must preserve the scroller",scroll,find(view,"Candidate list"));
            assertSame("Unchanged renders must preserve the word",word,find(view,"Candidate 一"));
        });
    }
    private static View find(View v,String description) {
        if(v instanceof KeyboardView) {View found=find(((KeyboardView)v).compositionAnnotation(),description);if(found!=null)return found;}
        if(description.contentEquals(v.getContentDescription()==null?"":v.getContentDescription()))return v;
        if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++){View found=find(((ViewGroup)v).getChildAt(i),description);if(found!=null)return found;}
        return null;
    }
}
