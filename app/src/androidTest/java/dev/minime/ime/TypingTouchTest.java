package dev.minime.ime;

import android.graphics.Rect;
import android.os.SystemClock;
import android.test.InstrumentationTestCase;
import android.view.*;
import dev.minime.core.*;
import java.util.*;

/** Sends pointer streams through the real keyboard ViewGroups, rather than accessibility clicks. */
@SuppressWarnings("deprecation")
public final class TypingTouchTest extends InstrumentationTestCase {
    private KeyboardView keyboard;
    private final List<String> emitted=new ArrayList<>();
    private void onMain(Runnable test) throws Throwable {
        java.util.concurrent.atomic.AtomicReference<Throwable> failure=new java.util.concurrent.atomic.AtomicReference<>();
        getInstrumentation().runOnMainSync(()-> {try {
            keyboard=new KeyboardView(getInstrumentation().getTargetContext(),emitted::add,key->false,(path,caps)->{});
            CompositionEngine engine=new CompositionEngine(new CompositionEngine.Editor() {
                public void composing(String s) {} public void commit(String s) {} public void delete() {}
                public void enter() {} public void finish() {}
            },Learning.NONE);
            engine.start(false,false,false,false);
            keyboard.render(engine,false,false,false,0,false,false,false,true,true,"Enter","");
            int width=getInstrumentation().getTargetContext().getResources().getDisplayMetrics().widthPixels;
            keyboard.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED));
            keyboard.layout(0,0,width,keyboard.getMeasuredHeight());test.run();
        }catch(Throwable t){failure.set(t);} });
        if(failure.get()!=null)throw failure.get();
    }
    private View find(View v,String label) {
        if(label.contentEquals(v.getContentDescription()==null?"":v.getContentDescription()))return v;
        if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++){View r=find(((ViewGroup)v).getChildAt(i),label);if(r!=null)return r;}
        return null;
    }
    private Rect key(String label) {
        View v=find(keyboard,label);assertNotNull(label,v);Rect r=new Rect(0,0,v.getWidth(),v.getHeight());keyboard.offsetDescendantRectToMyCoords(v,r);return r;
    }
    private void event(int action,int[] ids,float... xy) {
        MotionEvent.PointerProperties[] properties=new MotionEvent.PointerProperties[ids.length];
        MotionEvent.PointerCoords[] coords=new MotionEvent.PointerCoords[ids.length];
        for(int i=0;i<ids.length;i++){properties[i]=new MotionEvent.PointerProperties();properties[i].id=ids[i];properties[i].toolType=MotionEvent.TOOL_TYPE_FINGER;
            coords[i]=new MotionEvent.PointerCoords();coords[i].x=xy[2*i];coords[i].y=xy[2*i+1];coords[i].pressure=1;coords[i].size=1;}
        long now=SystemClock.uptimeMillis();MotionEvent e=MotionEvent.obtain(now,now,action,ids.length,properties,coords,0,0,1,1,0,0,InputDevice.SOURCE_TOUCHSCREEN,0);
        try{keyboard.dispatchTouchEvent(e);}finally{e.recycle();}
    }
    public void testOverlappingThumbsAcrossRows() throws Throwable {onMain(()-> {
        for(String[] pair:new String[][]{{"n","i"},{"w","o"},{"m","e"},{"a","l"}}) {
            emitted.clear();Rect a=key(pair[0]),b=key(pair[1]);
            event(MotionEvent.ACTION_DOWN,new int[]{3},a.exactCenterX(),a.exactCenterY());
            event(MotionEvent.ACTION_POINTER_DOWN|(1<<MotionEvent.ACTION_POINTER_INDEX_SHIFT),new int[]{3,7},a.exactCenterX(),a.exactCenterY(),b.exactCenterX(),b.exactCenterY());
            event(MotionEvent.ACTION_POINTER_UP,new int[]{3,7},a.exactCenterX(),a.exactCenterY(),b.exactCenterX(),b.exactCenterY());
            event(MotionEvent.ACTION_UP,new int[]{7},b.exactCenterX(),b.exactCenterY());
            assertEquals("Overlapping keys "+Arrays.toString(pair),Arrays.asList(pair),emitted);
        }
    });}
    public void testNearEdgeReleaseWithinPlatformSlop() throws Throwable {onMain(()-> {
        float slop=ViewConfiguration.get(keyboard.getContext()).getScaledTouchSlop();
        for(String label:new String[]{"n","i","a","p"}) {
            emitted.clear();Rect r=key(label);float x=r.right-1,y=r.exactCenterY();
            event(MotionEvent.ACTION_DOWN,new int[]{0},x,y);
            event(MotionEvent.ACTION_MOVE,new int[]{0},x+slop/2,y);
            event(MotionEvent.ACTION_UP,new int[]{0},x+slop/2,y);
            assertEquals("Small edge drift "+label,Collections.singletonList(label),emitted);
        }
    });}
    public void testReverseReleaseAndCancellation() throws Throwable {onMain(()-> {
        Rect a=key("w"),b=key("o");
        event(MotionEvent.ACTION_DOWN,new int[]{3},a.exactCenterX(),a.exactCenterY());
        event(MotionEvent.ACTION_POINTER_DOWN|(1<<MotionEvent.ACTION_POINTER_INDEX_SHIFT),new int[]{3,7},a.exactCenterX(),a.exactCenterY(),b.exactCenterX(),b.exactCenterY());
        event(MotionEvent.ACTION_POINTER_UP|(1<<MotionEvent.ACTION_POINTER_INDEX_SHIFT),new int[]{3,7},a.exactCenterX(),a.exactCenterY(),b.exactCenterX(),b.exactCenterY());
        event(MotionEvent.ACTION_UP,new int[]{3},a.exactCenterX(),a.exactCenterY());
        assertEquals("Google retains finger-down order",Arrays.asList("w","o"),emitted);emitted.clear();
        event(MotionEvent.ACTION_DOWN,new int[]{3},a.exactCenterX(),a.exactCenterY());
        event(MotionEvent.ACTION_CANCEL,new int[]{3},a.exactCenterX(),a.exactCenterY());
        event(MotionEvent.ACTION_UP,new int[]{3},a.exactCenterX(),a.exactCenterY());
        assertTrue("Cancellation emits nothing",emitted.isEmpty());
        event(MotionEvent.ACTION_DOWN,new int[]{3},a.exactCenterX(),a.exactCenterY());
        event(MotionEvent.ACTION_POINTER_DOWN|(1<<MotionEvent.ACTION_POINTER_INDEX_SHIFT),new int[]{3,7},a.exactCenterX(),a.exactCenterY(),a.exactCenterX(),a.exactCenterY());
        event(MotionEvent.ACTION_POINTER_UP,new int[]{3,7},a.exactCenterX(),a.exactCenterY(),a.exactCenterX(),a.exactCenterY());
        event(MotionEvent.ACTION_UP,new int[]{7},a.exactCenterX(),a.exactCenterY());
        assertEquals("Google preserves overlapping repeated letters",Arrays.asList("w","w"),emitted);
    });}

    /** A separate diagnostic: blank row margins, not dictionary acceptance. */
    public void testOuterRowContacts() throws Throwable {onMain(()-> {
        int contacts=0,misses=0;
        // Check every letter first, at center and both near-edge positions.
        for(char c='a';c<='z';c++)for(float f:new float[]{.02f,.5f,.98f}) {
            String label=String.valueOf(c);Rect r=key(label);float x=r.left+r.width()*f,y=r.exactCenterY();
            emitted.clear();event(MotionEvent.ACTION_DOWN,new int[]{3},x,y);event(MotionEvent.ACTION_UP,new int[]{3},x,y);
            assertEquals("In-key contact "+label+" "+f,Collections.singletonList(label),emitted);
        }
        // The two symmetric row gutters are defined by layout, never by word examples.
        for(String label:new String[]{"a","l"})for(float f:new float[]{.1f,.5f,.9f}) {
            Rect r=key(label);float x=(label.equals("a")?f*.05f:1-f*.05f)*keyboard.getWidth(),y=r.exactCenterY();
            emitted.clear();event(MotionEvent.ACTION_DOWN,new int[]{3},x,y);event(MotionEvent.ACTION_UP,new int[]{3},x,y);
            contacts++;if(!emitted.equals(Collections.singletonList(label)))misses++;
        }
        android.util.Log.i("MinIME-EdgeProbe","in-key=78/78; outer-row="+(contacts-misses)+"/"+contacts);
        assertEquals("All-letter controls pass; blank outer-row contacts lost",0,misses);
    });}


}
