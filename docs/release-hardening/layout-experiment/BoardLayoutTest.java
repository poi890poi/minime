package dev.minime.ime;

import android.content.res.Configuration;
import android.test.ActivityInstrumentationTestCase2;
import android.view.*;
import dev.minime.core.*;
import java.lang.reflect.Field;
import java.util.*;
import java.util.function.Consumer;

/** Candidate refreshes must not request layout of an unchanged letter board. */
@SuppressWarnings("deprecation")
public final class BoardLayoutTest extends ActivityInstrumentationTestCase2<EditorTestActivity> {
    public BoardLayoutTest(){super(EditorTestActivity.class);}
    private KeyboardView keyboard;
    private CompositionEngine engine;
    private ViewGroup keys;
    private Consumer<List<Candidate>> reply;
    private void render(int panel,boolean numeric) {
        keyboard.render(engine,false,false,false,panel,numeric,false,false,true,true,"Enter","");
    }
    private void settle() {
        int width=Math.round(400*keyboard.getResources().getDisplayMetrics().density);
        keyboard.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED));
        keyboard.layout(0,0,width,keyboard.getMeasuredHeight());
        assertFalse("Measured board starts settled",keys.isLayoutRequested());
    }
    public void testPortraitRefreshAndTransitions()throws Throwable {check(Configuration.ORIENTATION_PORTRAIT);}
    public void testLandscapeRefreshAndTransitions()throws Throwable {check(Configuration.ORIENTATION_LANDSCAPE);}
    private void check(int orientation)throws Throwable {
        PhoneticDictionary dictionary=DictionaryRepository.load(getInstrumentation().getTargetContext()).get(30,java.util.concurrent.TimeUnit.SECONDS);
        EditorTestActivity activity=getActivity();
        runTestOnUiThread(()-> {
            // Own the configuration rather than changing the phone's orientation.
            Configuration config=new Configuration(activity.getResources().getConfiguration());
            config.orientation=orientation;
            android.content.Context context=activity.createConfigurationContext(config);
            keyboard=new KeyboardView(context,key->{},key->false,(path,caps)->{});
            engine=new CompositionEngine(new CompositionEngine.Editor(){
                public void composing(String s){}public void commit(String s){}public void delete(){}public void enter(){}public void finish(){}
            },Learning.NONE);
            engine.dictionary(dictionary);engine.start(false,false,false,false);
            engine.decoder((d,raw,z,c,done)->reply=done,()->render(0,false));
            try {Field f=KeyboardView.class.getDeclaredField("keys");f.setAccessible(true);keys=(ViewGroup)f.get(keyboard);}
            catch(Exception e){throw new RuntimeException(e);}
            activity.setContentView(keyboard);engine.type('n');render(0,false);settle();
            View firstRow=keys.getChildAt(0);int mainHeight=keys.getLayoutParams().height;
            reply.accept(Arrays.asList(new Candidate("一",false,1000),new Candidate("二",false,900)));
            assertFalse("Candidate result must not request layout of unchanged board",keys.isLayoutRequested());
            assertSame("Existing touch targets survive candidate refresh",firstRow,keys.getChildAt(0));
            settle();engine.type('i');render(0,false);
            assertFalse("Pending raw input must not request unchanged board layout",keys.isLayoutRequested());
            assertEquals(mainHeight,keys.getLayoutParams().height);
            render(1,false);assertTrue("Symbol transition still requests layout",keys.isLayoutRequested());
            assertTrue("Symbol panel retains its extra toolbar height",keys.getLayoutParams().height>mainHeight);
            settle();render(0,true);assertEquals("Numeric returns to main height",mainHeight,keys.getLayoutParams().height);
            settle();render(0,false);assertEquals(mainHeight,keys.getLayoutParams().height);
            assertNotSame("Actual layout changes replace keys",firstRow,keys.getChildAt(0));
        });
    }
}
