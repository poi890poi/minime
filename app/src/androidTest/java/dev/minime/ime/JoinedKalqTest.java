package dev.minime.ime;

import android.content.*;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.os.SystemClock;
import android.test.InstrumentationTestCase;
import android.view.*;
import java.util.*;
import dev.minime.core.*;

/** Exercise the production layout and SlideKey dispatch, including shared-language use. */
@SuppressWarnings("deprecation")
public final class JoinedKalqTest extends InstrumentationTestCase {
    private KeyboardView keyboard;
    private CompositionEngine engine;
    private final List<String> emitted=new ArrayList<>();
    private final List<float[]> traces=new ArrayList<>();
    private SharedPreferences settings;
    private boolean existed,old;
    @Override protected void setUp() throws Exception {
        super.setUp();settings=getInstrumentation().getTargetContext().getSharedPreferences("settings",Context.MODE_PRIVATE);
        existed=settings.contains("joined_kalq");old=settings.getBoolean("joined_kalq",false);
        settings.edit().putBoolean("joined_kalq",true).commit();
    }
    @Override protected void tearDown() throws Exception {
        try {SharedPreferences.Editor e=settings.edit();if(existed)e.putBoolean("joined_kalq",old);else e.remove("joined_kalq");e.commit();}
        finally {super.tearDown();}
    }
    private void main(Runnable test) throws Throwable {
        java.util.concurrent.atomic.AtomicReference<Throwable> failure=new java.util.concurrent.atomic.AtomicReference<>();
        getInstrumentation().runOnMainSync(()->{try{test.run();}catch(Throwable error){failure.set(error);}});
        if(failure.get()!=null)throw failure.get();
    }
    private void create(Context context,InputMode mode) {
        keyboard=new KeyboardView(context,emitted::add,k->false,(p,c)->traces.add(p));
        engine=new CompositionEngine(new CompositionEngine.Editor() {
            public void composing(String s){} public void commit(String s){} public void delete(){}
            public void enter(){} public void finish(){}
        },Learning.NONE);
        engine.start(false,false,false,false,mode.english());engine.switchMode(mode,false);
    }
    private int render(InputMode mode,boolean zh,boolean numeric,int panel,boolean shifted,boolean ascii) {
        keyboard.render(engine,zh,shifted,shifted,panel,numeric,ascii,mode.english(),!numeric,true,"Enter","");
        int width=Math.round((keyboard.getResources().getConfiguration().orientation==Configuration.ORIENTATION_LANDSCAPE?760:360)*keyboard.getResources().getDisplayMetrics().density);
        keyboard.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED));
        keyboard.layout(0,0,width,keyboard.getMeasuredHeight());return keyboard.getMeasuredHeight();
    }
    private List<View> find(String label) {List<View> result=new ArrayList<>();collect(keyboard,label,result);return result;}
    private void collect(View view,String label,List<View> result) {
        if(label.contentEquals(view.getContentDescription()==null?"":view.getContentDescription()))result.add(view);
        if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++)collect(((ViewGroup)view).getChildAt(i),label,result);
    }
    private Rect bounds(View view) {Rect r=new Rect(0,0,view.getWidth(),view.getHeight());keyboard.offsetDescendantRectToMyCoords(view,r);return r;}
    private View key(String label) {List<View> found=find(label);assertFalse("Missing "+label,found.isEmpty());return found.get(0);}
    private void event(int action,int[] ids,float... xy) {
        MotionEvent.PointerProperties[] properties=new MotionEvent.PointerProperties[ids.length];
        MotionEvent.PointerCoords[] coords=new MotionEvent.PointerCoords[ids.length];
        for(int i=0;i<ids.length;i++) {properties[i]=new MotionEvent.PointerProperties();properties[i].id=ids[i];properties[i].toolType=MotionEvent.TOOL_TYPE_FINGER;
            coords[i]=new MotionEvent.PointerCoords();coords[i].x=xy[2*i];coords[i].y=xy[2*i+1];coords[i].pressure=1;coords[i].size=1;}
        long now=SystemClock.uptimeMillis();MotionEvent e=MotionEvent.obtain(now,now,action,ids.length,properties,coords,0,0,1,1,0,0,InputDevice.SOURCE_TOUCHSCREEN,0);
        try {keyboard.dispatchTouchEvent(e);}finally{e.recycle();}
    }
    private void tap(View view,int direction) {
        Rect r=bounds(view);float x=r.exactCenterX(),y=r.exactCenterY();
        event(MotionEvent.ACTION_DOWN,new int[]{3},x,y);
        if(direction!=0){y+=direction*25*keyboard.getResources().getDisplayMetrics().density;event(MotionEvent.ACTION_MOVE,new int[]{3},x,y);}
        event(MotionEvent.ACTION_UP,new int[]{3},x,y);
    }
    public void testAlphabetSlidesModesAndStableHeight() throws Throwable {main(()-> {
        Context base=getInstrumentation().getTargetContext();
        for(int orientation:new int[]{Configuration.ORIENTATION_PORTRAIT,Configuration.ORIENTATION_LANDSCAPE})for(float scale:new float[]{1f,1.3f}) {
            Configuration config=new Configuration(base.getResources().getConfiguration());config.orientation=orientation;config.fontScale=scale;
            for(InputMode mode:InputMode.values()) {
                create(base.createConfigurationContext(config),mode);
                settings.edit().putBoolean("joined_kalq",false).commit();int normal=render(mode,false,false,0,false,mode.english());
                assertTrue("QWERTY fallback",bounds(key("q")).top<bounds(key("m")).top);
                settings.edit().putBoolean("joined_kalq",true).commit();assertEquals(normal,render(mode,false,false,0,false,mode.english()));
                assertTrue("Joined applies to "+mode,bounds(key("m")).top<bounds(key("q")).top);
                // The four-letter group is centered between equal-width outer controls.
                Rect d=bounds(key("d")),v=bounds(key("v")),bottom=bounds((View)key("d").getParent());
                assertEquals("Bottom letters centered",bottom.exactCenterX(),(d.left+v.right)/2f,1f);
                assertEquals("Letter touch width retained",bounds(key("m")).width(),d.width(),1f);
                assertEquals("Symmetric outer controls",bounds(key("⇧")).width(),bounds(key("⌫")).width(),1f);
                assertTrue("Shift precedes letters",bounds(key("⇧")).right<=d.left);
                assertTrue("Delete follows letters",bounds(key("⌫")).left>=v.right);
                for(int r=0;r<JoinedKalq.rows();r++)for(int c=0;c<JoinedKalq.row(r).length();c++) {
                    char letter=JoinedKalq.row(r).charAt(c);if(letter==' ')continue;
                    assertEquals("Unique letter",1,find(String.valueOf(letter)).size());
                    for(int slide:new int[]{0,-1,1}) {
                        emitted.clear();tap(key(String.valueOf(letter)),slide);
                        String expected=slide==0?String.valueOf(letter):"LITERAL:"+(slide<0?Character.toUpperCase(letter):JoinedKalq.symbol(r,c,mode.english()));
                        assertEquals(mode+" letter/slide "+letter+"/"+slide,Collections.singletonList(expected),emitted);
                    }
                }
                assertTrue("KALQ never uses QWERTY trace templates",traces.isEmpty());
                assertEquals(3,find("Space").size());
                for(View space:find("Space")){emitted.clear();tap(space,0);assertEquals(Collections.singletonList("SPACE"),emitted);}
                render(mode,false,false,0,true,mode.english());emitted.clear();tap(key("M"),0);assertEquals(Collections.singletonList("M"),emitted);
                for(int panel=1;panel<=3;panel++)assertEquals("Panel height",normal,render(mode,false,false,panel,false,mode.english()));
                assertEquals(normal,render(mode,false,true,0,false,true));assertTrue("Numeric fallback",find("m").isEmpty());
                assertEquals(normal,render(mode,true,false,0,false,false));assertTrue("Zhuyin fallback",find("m").isEmpty());assertFalse(find("ㄅ").isEmpty());
            }
        }
    });}
    public void testOverlapEveryLetterWithAllSpacesAndDelete() throws Throwable {main(()-> {
        create(getInstrumentation().getTargetContext(),InputMode.ENGLISH);render(InputMode.ENGLISH,false,false,0,false,true);
        List<View> controls=find("Space");controls.add(key("⌫"));
        for(char letter='a';letter<='z';letter++)for(View control:controls)for(boolean controlFirst:new boolean[]{false,true})for(boolean releaseFirst:new boolean[]{false,true}) {
            String ctl="⌫".contentEquals(control.getContentDescription())?"DELETE":"SPACE";
            Rect a=bounds(controlFirst?control:key(String.valueOf(letter))),b=bounds(controlFirst?key(String.valueOf(letter)):control);
            emitted.clear();event(MotionEvent.ACTION_DOWN,new int[]{3},a.exactCenterX(),a.exactCenterY());
            event(MotionEvent.ACTION_POINTER_DOWN|(1<<MotionEvent.ACTION_POINTER_INDEX_SHIFT),new int[]{3,7},a.exactCenterX(),a.exactCenterY(),b.exactCenterX(),b.exactCenterY());
            event(MotionEvent.ACTION_POINTER_UP|((releaseFirst?0:1)<<MotionEvent.ACTION_POINTER_INDEX_SHIFT),new int[]{3,7},a.exactCenterX(),a.exactCenterY(),b.exactCenterX(),b.exactCenterY());
            Rect remaining=releaseFirst?b:a;event(MotionEvent.ACTION_UP,new int[]{releaseFirst?7:3},remaining.exactCenterX(),remaining.exactCenterY());
            assertEquals(Arrays.asList(controlFirst?ctl:String.valueOf(letter),controlFirst?String.valueOf(letter):ctl),emitted);
        }
        emitted.clear();Rect m=bounds(key("m"));event(MotionEvent.ACTION_DOWN,new int[]{3},m.exactCenterX(),m.exactCenterY());event(MotionEvent.ACTION_CANCEL,new int[]{3},m.exactCenterX(),m.exactCenterY());event(MotionEvent.ACTION_UP,new int[]{3},m.exactCenterX(),m.exactCenterY());assertTrue(emitted.isEmpty());
    });}
    public void testSymbolInventoryAndBackupType() throws Exception {
        for(boolean ascii:new boolean[]{false,true})assertEquals(KeyboardView.mainBoardSymbols(false,ascii,false),KeyboardView.mainBoardSymbols(false,ascii,false,true,ascii));
        String document="{\"format\":\"MinIME preferences\",\"version\":1,\"settings\":{\"joined_kalq\":true},\"learning\":{}}";
        PreferenceBackup.Snapshot snapshot=PreferenceBackup.decode(new java.io.ByteArrayInputStream(document.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        assertEquals(true,snapshot.settings.get("joined_kalq"));
        try {PreferenceBackup.decode(new java.io.ByteArrayInputStream(document.replace(":true",":\"true\"").getBytes(java.nio.charset.StandardCharsets.UTF_8)));fail("Wrong preference type accepted");}
        catch(java.io.IOException expected){}
    }
    public void testPrivateFieldsAndHorizontalCancellation() throws Throwable {main(()-> {
        create(getInstrumentation().getTargetContext(),InputMode.ENGLISH);engine.start(false,true,true,true,true);
        render(InputMode.ENGLISH,false,false,0,false,true);assertTrue(engine.privateField());
        assertTrue("Private letter field keeps chosen layout",bounds(key("m")).top<bounds(key("q")).top);
        emitted.clear();tap(key("a"),0);assertEquals(Collections.singletonList("a"),emitted);
        emitted.clear();Rect m=bounds(key("m")),j=bounds(key("j"));
        event(MotionEvent.ACTION_DOWN,new int[]{3},m.exactCenterX(),m.exactCenterY());
        event(MotionEvent.ACTION_MOVE,new int[]{3},j.exactCenterX(),j.exactCenterY());
        event(MotionEvent.ACTION_UP,new int[]{3},j.exactCenterX(),j.exactCenterY());
        assertTrue("Horizontal drag cancels instead of decoding a QWERTY trace",emitted.isEmpty());assertTrue(traces.isEmpty());
    });}
    public void testLayoutTogglePreservesComposition() throws Throwable {
        PhoneticDictionary dictionary=DictionaryRepository.load(getInstrumentation().getTargetContext()).get(30,java.util.concurrent.TimeUnit.SECONDS);
        main(()-> {
            create(getInstrumentation().getTargetContext(),InputMode.CHINESE);engine.dictionary(dictionary);
            for(char letter:"nihao".toCharArray())engine.type(letter);
            List<String> before=new ArrayList<>();for(Candidate c:engine.candidates())before.add(c.text);
            long composition=engine.compositionId();
            for(boolean joined:new boolean[]{false,true,false,true}) {
                settings.edit().putBoolean("joined_kalq",joined).commit();render(InputMode.CHINESE,false,false,0,false,false);
                assertEquals("nihao",engine.raw());assertEquals(composition,engine.compositionId());
                List<String> after=new ArrayList<>();for(Candidate c:engine.candidates())after.add(c.text);assertEquals(before,after);
                assertEquals(joined,bounds(key("m")).top<bounds(key("q")).top);
            }
        });
    }
}
