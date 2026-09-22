package dev.minime.ime;

import android.test.ActivityInstrumentationTestCase2;
import android.view.*;
import android.widget.*;
import dev.minime.core.*;
import java.util.*;

/** Allocation bounds must not become a vocabulary or acceptance cap. */
@SuppressWarnings("deprecation")
public final class CandidatePagingTest extends ActivityInstrumentationTestCase2<EditorTestActivity> {
    public CandidatePagingTest(){super(EditorTestActivity.class);}
    private KeyboardView keyboard;
    private CompositionEngine engine;
    private String accepted="";
    private View find(View view,String description) {
        if(description.contentEquals(view.getContentDescription()==null?"":view.getContentDescription()))return view;
        if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++) {
            View result=find(((ViewGroup)view).getChildAt(i),description);if(result!=null)return result;
        }
        return null;
    }
    private void render(){keyboard.render(engine,false,false,false,0,false,false,false,true,true,"Enter","");}
    public void testEveryCandidateRemainsReachableAndNewInputResetsAllocation()throws Exception {
        EditorTestActivity activity=getActivity();
        PhoneticDictionary dictionary=DictionaryRepository.load(activity).get(30,java.util.concurrent.TimeUnit.SECONDS);
        List<Candidate> choices=new ArrayList<>();
        for(int i=0;i<256;i++)choices.add(new Candidate("測試選項"+String.format(Locale.ROOT,"%03d",i),false,1000-i));
        getInstrumentation().runOnMainSync(()-> {
            keyboard=new KeyboardView(activity,key->{},key->false,(points,caps)->{});
            engine=new CompositionEngine(new CompositionEngine.Editor() {
                public void composing(String s){} public void commit(String s){accepted+=s;}
                public void delete(){} public void enter(){} public void finish(){}
            },Learning.NONE);
            engine.dictionary(dictionary);
            engine.decoder((dict,raw,zh,context,done)->done.accept(new ArrayList<>(choices)),this::render);
            engine.start(false,false,false,false);activity.setContentView(keyboard);engine.type('a');render();
        });
        getInstrumentation().waitForIdleSync();
        String last="Candidate "+choices.get(choices.size()-1).text;
        getInstrumentation().runOnMainSync(()->assertNull("Off-screen tail is not allocated on a keystroke",find(keyboard,last)));
        final boolean[] found={false};
        for(int page=0;page<40 && !found[0];page++) {
            getInstrumentation().runOnMainSync(()-> {
                HorizontalScrollView scroll=(HorizontalScrollView)find(keyboard,"Candidate list");assertNotNull(scroll);
                scroll.fullScroll(View.FOCUS_RIGHT);
            });
            getInstrumentation().waitForIdleSync();android.os.SystemClock.sleep(50);
            getInstrumentation().runOnMainSync(()->found[0]=find(keyboard,last)!=null);
        }
        assertTrue("Scrolling reaches the original final candidate",found[0]);
        getInstrumentation().runOnMainSync(()->((HorizontalScrollView)find(keyboard,"Candidate list")).fullScroll(View.FOCUS_RIGHT));
        getInstrumentation().waitForIdleSync();
        getInstrumentation().runOnMainSync(()-> {
            assertTrue("Final candidate is actually inside the visible viewport",find(keyboard,last).getGlobalVisibleRect(new android.graphics.Rect()));
            assertEquals("Paging preserves every candidate and its order",choices.get(255).text,engine.candidates().get(engine.candidates().size()-1).text);
            assertTrue(find(keyboard,last).performClick());
            assertEquals("Deep selection commits its own text",choices.get(255).text,accepted);
            engine.type('b');render();
            assertNull("A new input returns to bounded initial allocation",find(keyboard,last));
        });
    }
}
