package dev.minime.ime;

import android.content.Context;
import android.content.res.Configuration;
import android.test.InstrumentationTestCase;
import android.view.*;
import dev.minime.core.*;

/** Measures the real Android layout across content states, without driving a decoder benchmark. */
@SuppressWarnings("deprecation")
public final class KeyboardHeightTest extends InstrumentationTestCase {
    public void testStableBoundsAcrossModesAndConfigurations() throws Throwable {
        PhoneticDictionary dictionary=DictionaryRepository.load(getInstrumentation().getTargetContext()).get(30,java.util.concurrent.TimeUnit.SECONDS);
        java.util.concurrent.atomic.AtomicReference<Throwable> failure=new java.util.concurrent.atomic.AtomicReference<>();
        getInstrumentation().runOnMainSync(()-> {try {
            for(int orientation:new int[]{Configuration.ORIENTATION_PORTRAIT,Configuration.ORIENTATION_LANDSCAPE}) {
                for(float font:new float[]{1f,1.3f}) {
                    Configuration config=new Configuration(getInstrumentation().getTargetContext().getResources().getConfiguration());
                    config.orientation=orientation;config.fontScale=font;
                    Context context=getInstrumentation().getTargetContext().createConfigurationContext(config);
                    KeyboardView view=new KeyboardView(context,key->{},key->false,(path,caps)->{});
                    CompositionEngine engine=new CompositionEngine(new CompositionEngine.Editor() {
                        public void composing(String s) {} public void commit(String s) {} public void delete() {}
                        public void enter() {} public void finish() {}
                    },Learning.NONE);
                    engine.dictionary(dictionary);engine.start(false,false,false,false);
                    render(view,engine,false,false,false,0,"");int expected=measure(view,orientation);
                    for(char c:"nihao".toCharArray()) {engine.type(c);render(view,engine,false,false,false,0,"");same(view,orientation,expected,"composing "+engine.raw());}
                    assertTrue("Chinese composition has candidates",engine.candidates().size()>1);
                    find(view,"Expand candidates").performClick();same(view,orientation,expected,"expanded");
                    find(view,"Collapse candidates").performClick();same(view,orientation,expected,"collapsed");
                    engine.confirm();render(view,engine,false,false,false,0,"");same(view,orientation,expected,"committed");
                    for(boolean english:new boolean[]{false,true}) for(boolean zhuyin:new boolean[]{false,true}) {
                        engine.start(zhuyin,false,false,false,english);
                        for(int panel=0;panel<=3;panel++) {
                            render(view,engine,zhuyin,english,false,panel,"");same(view,orientation,expected,"panel "+panel);
                            contained(view);
                        }
                    }
                    engine.start(false,true,true,true,true);render(view,engine,false,true,true,0,"");same(view,orientation,expected,"private numeric");contained(view);
                    engine.start(false,false,false,false);render(view,engine,false,false,false,0,"Loading dictionary");same(view,orientation,expected,"loading");
                    // A system navigation inset changes the usable window, not the content budget.
                    view.setPadding(0,0,0,37);assertEquals(expected+37,measure(view,orientation));
                }
            }
        } catch(Throwable t) {failure.set(t);} });
        if(failure.get()!=null)throw failure.get();
    }
    private static void render(KeyboardView v,CompositionEngine e,boolean zh,boolean en,boolean numeric,int panel,String loading) {
        v.render(e,zh,false,false,panel,numeric,en,en,!numeric,"Enter",loading);
    }
    private static int measure(View v,int orientation) {
        int width=Math.round((orientation==Configuration.ORIENTATION_PORTRAIT?400:800)*v.getResources().getDisplayMetrics().density);
        v.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED));
        v.layout(0,0,width,v.getMeasuredHeight());return v.getMeasuredHeight();
    }
    private static void same(View v,int orientation,int expected,String state) {assertEquals(state,expected,measure(v,orientation));}
    private static View find(View v,String description) {
        if(description.contentEquals(v.getContentDescription()==null?"":v.getContentDescription()))return v;
        if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++){View found=find(((ViewGroup)v).getChildAt(i),description);if(found!=null)return found;}
        return null;
    }
    private static void contained(View v) {
        if(!(v instanceof ViewGroup) || v instanceof android.widget.ScrollView || v instanceof android.widget.HorizontalScrollView)return;
        ViewGroup group=(ViewGroup)v;
        for(int i=0;i<group.getChildCount();i++) {View child=group.getChildAt(i);if(child.getVisibility()!=View.VISIBLE)continue;
            assertTrue("Visible child has height",child.getHeight()>0);
            assertTrue("Panel child fits its parent: "+child.getClass().getSimpleName()+" "+child.getContentDescription()+" "+child.getTop()+".."+child.getBottom()+" / "+group.getHeight(),child.getTop()>=0 && child.getBottom()<=group.getHeight());contained(child);
        }
    }
}
