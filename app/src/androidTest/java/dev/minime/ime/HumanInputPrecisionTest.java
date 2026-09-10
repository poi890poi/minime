package dev.minime.ime;

import android.content.*;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.os.SystemClock;
import android.test.InstrumentationTestCase;
import android.view.*;
import dev.minime.core.*;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Frozen synthetic error envelope. Direct dispatch tests geometry/order, not real-time latency. */
@SuppressWarnings("deprecation")
public final class HumanInputPrecisionTest extends InstrumentationTestCase {
    private KeyboardView keyboard;
    private final Map<Character,Rect> keys=new HashMap<>();
    private final List<String> emitted=new ArrayList<>();
    private JSONArray outcomes=new JSONArray(),layouts=new JSONArray(),events;
    private String config;
    private long downAt,at;
    private int failures;
    private float density;
    private void setup(EditorTestActivity host,boolean english,int orientation,float font) throws JSONException {
        Configuration c=new Configuration(host.getResources().getConfiguration());c.orientation=orientation;c.fontScale=font;
        Context context=host.createConfigurationContext(c);density=context.getResources().getDisplayMetrics().density;
        keyboard=new KeyboardView(context,emitted::add,key->false,(path,caps)->emitted.add("TRACE"));
        CompositionEngine engine=new CompositionEngine(new CompositionEngine.Editor() {
            public void composing(String s) {} public void commit(String s) {} public void delete() {}
            public void enter() {} public void finish() {}
        },Learning.NONE);engine.start(false,false,false,false,english);
        keyboard.render(engine,false,false,false,0,false,english,english,true,true,"Enter","");
        host.setContentView(keyboard);assertTrue("Replay host is active and keyboard attached",!host.isFinishing() && !host.isDestroyed() && keyboard.isAttachedToWindow());
        int width=Math.round((orientation==Configuration.ORIENTATION_PORTRAIT?360:740)*density);
        keyboard.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED));
        keyboard.layout(0,0,width,keyboard.getMeasuredHeight());keys.clear();
        for(char letter='a';letter<='z';letter++) {
            View view=find(keyboard,String.valueOf(letter));assertNotNull(view);
            Rect r=new Rect(0,0,view.getWidth(),view.getHeight());keyboard.offsetDescendantRectToMyCoords(view,r);keys.put(letter,r);
        }
        config=(english?"english":"pinyin")+":"+orientation+":"+font+":"+width;
        JSONObject geometry=new JSONObject();for(char letter='a';letter<='z';letter++)geometry.put(String.valueOf(letter),keys.get(letter).flattenToString());
        layouts.put(new JSONObject().put("id",config).put("density",density).put("height",keyboard.getMeasuredHeight()).put("keys",geometry));
    }
    private static View find(View view,String text) {
        if(text.contentEquals(view.getContentDescription()==null?"":view.getContentDescription()))return view;
        if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++) {View found=find(((ViewGroup)view).getChildAt(i),text);if(found!=null)return found;}
        return null;
    }
    private void begin() {emitted.clear();events=new JSONArray();downAt=at=SystemClock.uptimeMillis();}
    private float[] point(char letter,float x,float y) {Rect r=keys.get(letter);return new float[]{r.left+x*r.width(),r.top+y*r.height()};}
    private void event(int action,int delay,int[] ids,float... xy) throws JSONException {
        at+=delay;
        MotionEvent.PointerProperties[] properties=new MotionEvent.PointerProperties[ids.length];
        MotionEvent.PointerCoords[] coords=new MotionEvent.PointerCoords[ids.length];
        JSONArray pointers=new JSONArray();
        for(int i=0;i<ids.length;i++) {
            properties[i]=new MotionEvent.PointerProperties();properties[i].id=ids[i];properties[i].toolType=MotionEvent.TOOL_TYPE_FINGER;
            coords[i]=new MotionEvent.PointerCoords();coords[i].x=xy[2*i];coords[i].y=xy[2*i+1];coords[i].pressure=.45f+(i*.2f);coords[i].size=.2f;
            pointers.put(new JSONArray().put(ids[i]).put(xy[2*i]).put(xy[2*i+1]));
        }
        events.put(new JSONObject().put("action",action).put("ms",at-downAt).put("pointers",pointers));
        // Raw coordinates must be screen coordinates for the real trace recognizer.
        int[] screen=new int[2];keyboard.getLocationOnScreen(screen);
        for(MotionEvent.PointerCoords c:coords){c.x+=screen[0];c.y+=screen[1];}
        MotionEvent e=MotionEvent.obtain(downAt,at,action,ids.length,properties,coords,0,0,1,1,0,0,InputDevice.SOURCE_TOUCHSCREEN,0);
        e.offsetLocation(-screen[0],-screen[1]);
        try {keyboard.dispatchTouchEvent(e);}finally{e.recycle();}
    }
    private void record(String id,String expected,boolean gate) throws JSONException {
        String actual=String.join("",emitted);boolean pass=expected.equals(actual);
        JSONObject row=new JSONObject().put("config",config).put("id",id).put("expected",expected).put("actual",actual)
            .put("commands",new JSONArray(emitted)).put("gate",gate).put("pass",pass).put("eventCount",events.length());
        if(!pass)row.put("events",events);
        outcomes.put(row);if(gate && !pass)failures++;
    }
    private void singles() throws JSONException {
        float[][] origins={{.12f,.12f},{.88f,.12f},{.12f,.88f},{.88f,.88f},{.02f,.5f},{.98f,.5f},{.5f,.02f},{.5f,.98f}};
        for(char letter='a';letter<='z';letter++)for(int p=0;p<origins.length;p++) {
            begin();float[] a=point(letter,origins[p][0],origins[p][1]);
            float dx=(p==4?-2:p==5?2:p%2==0?1:-1)*density,dy=(p==6?-2:p==7?2:1)*density;
            event(MotionEvent.ACTION_DOWN,0,new int[]{3},a[0],a[1]);
            event(MotionEvent.ACTION_MOVE,17+p*3,new int[]{3},a[0]+dx/2,a[1]+dy/2);
            event(MotionEvent.ACTION_MOVE,11,new int[]{3},a[0]+dx,a[1]+dy);
            event(MotionEvent.ACTION_UP,23+p*7,new int[]{3},a[0]+dx,a[1]+dy);
            record("drift:"+letter+":"+p,String.valueOf(letter),true);
        }
    }
    private void pairs() throws JSONException {
        Random random=new Random(20260908L);
        for(char first='a';first<='z';first++)for(char second='a';second<='z';second++)for(boolean reverse:new boolean[]{false,true}) {
            begin();float[] a=point(first,.25f+random.nextFloat()*.5f,.25f+random.nextFloat()*.5f),b=point(second,.25f+random.nextFloat()*.5f,.25f+random.nextFloat()*.5f);
            event(MotionEvent.ACTION_DOWN,0,new int[]{3},a[0],a[1]);
            event(MotionEvent.ACTION_POINTER_DOWN|(1<<MotionEvent.ACTION_POINTER_INDEX_SHIFT),8+random.nextInt(65),new int[]{3,17},a[0],a[1],b[0],b[1]);
            // Deliberately reorder the pointer array: identity is the ID, not its array index.
            event(MotionEvent.ACTION_MOVE,5+random.nextInt(30),new int[]{17,3},b[0]+density,b[1]-density,a[0]-density,a[1]+density);
            event(MotionEvent.ACTION_POINTER_UP|((reverse?0:1)<<MotionEvent.ACTION_POINTER_INDEX_SHIFT),9+random.nextInt(31),new int[]{17,3},b[0]+density,b[1]-density,a[0]-density,a[1]+density);
            float[] last=reverse?a:b;
            event(MotionEvent.ACTION_UP,7+random.nextInt(70),new int[]{reverse?3:17},last[0],last[1]);
            record("overlap:"+first+second+":"+reverse,""+first+second,true);
        }
    }
    private void controls() throws JSONException {
        for(char letter='a';letter<='z';letter++) {
            float[] a=point(letter,.5f,.5f);
            begin();event(MotionEvent.ACTION_DOWN,0,new int[]{17},a[0],a[1]);event(MotionEvent.ACTION_CANCEL,80,new int[]{17},a[0],a[1]);
            event(MotionEvent.ACTION_UP,15,new int[]{17},a[0],a[1]);record("cancel:"+letter,"",true);
            begin();event(MotionEvent.ACTION_DOWN,0,new int[]{3},a[0],a[1]);
            event(MotionEvent.ACTION_MOVE,35,new int[]{3},a[0]+density,a[1]-32*density);
            event(MotionEvent.ACTION_UP,40,new int[]{3},a[0]+density,a[1]-32*density);
            record("intentional-up:"+letter,"LITERAL:"+Character.toUpperCase(letter),true);
            // Outside DOWN is intentionally ambiguous: measure, do not require magical recovery.
            begin();Rect r=keys.get(letter);float x=r.right+2*density;
            event(MotionEvent.ACTION_DOWN,0,new int[]{3},x,a[1]);event(MotionEvent.ACTION_UP,70,new int[]{3},x,a[1]);
            record("outside-right:"+letter,String.valueOf(letter),false);
        }
    }
    private void outerMargins() throws JSONException {
        float width=keyboard.getWidth();
        for(char edge:new char[]{'a','l'}) {
            View view=find(keyboard,String.valueOf(edge));Rect r=keys.get(edge);
            float visual=r.left+(r.width()+view.getPaddingLeft()-view.getPaddingRight())/2f;
            assertEquals("Original glyph center "+edge,width*(edge=='a'?.1f:.9f),visual,2f);
            for(float f:new float[]{.1f,.5f,.9f}) {
                float x=width*(edge=='a'?f*.05f:1-f*.05f),y=r.exactCenterY();
                for(int gesture=0;gesture<4;gesture++) {
                    begin();event(MotionEvent.ACTION_DOWN,0,new int[]{3},x,y);
                    if(gesture>=2)event(MotionEvent.ACTION_MOVE,30,new int[]{3},x,y+(gesture==2?-32:32)*density);
                    event(gesture==1?MotionEvent.ACTION_CANCEL:MotionEvent.ACTION_UP,55,new int[]{3},x,y+(gesture>=2?(gesture==2?-32:32)*density:0));
                    record("outer-margin:"+edge+":"+f+":"+gesture,
                        gesture==0?String.valueOf(edge):gesture==1?"":"LITERAL:"+(gesture==2?String.valueOf(Character.toUpperCase(edge)):(edge=='a'?"@":config.startsWith("english")?")":"）")),true);
                }
            }
            for(char other='a';other<='z';other++)for(boolean marginFirst:new boolean[]{false,true})for(boolean reverse:new boolean[]{false,true}) {
                float[] margin={width*(edge=='a'?.025f:.975f),r.exactCenterY()},center=point(other,.5f,.5f);
                float[] a=marginFirst?margin:center,b=marginFirst?center:margin;
                begin();event(MotionEvent.ACTION_DOWN,0,new int[]{3},a[0],a[1]);
                event(MotionEvent.ACTION_POINTER_DOWN|(1<<MotionEvent.ACTION_POINTER_INDEX_SHIFT),30,new int[]{3,17},a[0],a[1],b[0],b[1]);
                event(MotionEvent.ACTION_MOVE,15,new int[]{17,3},b[0],b[1],a[0],a[1]);
                event(MotionEvent.ACTION_POINTER_UP|((reverse?0:1)<<MotionEvent.ACTION_POINTER_INDEX_SHIFT),20,new int[]{17,3},b[0],b[1],a[0],a[1]);
                float[] last=reverse?a:b;event(MotionEvent.ACTION_UP,20,new int[]{reverse?3:17},last[0],last[1]);
                record("outer-overlap:"+edge+other+":"+marginFirst+":"+reverse,marginFirst?""+edge+other:""+other+edge,true);
            }
        }
    }
    public void testOuterMarginsStartEnglishTrace() throws Throwable {
        for(int orientation:new int[]{Configuration.ORIENTATION_PORTRAIT,Configuration.ORIENTATION_LANDSCAPE}) {
            Intent intent=new Intent(getInstrumentation().getTargetContext(),EditorTestActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);
            EditorTestActivity host=(EditorTestActivity)getInstrumentation().startActivitySync(intent);
            try {runTestOnUiThread(()-> {try {
                setup(host,true,orientation,1f);
                for(String path:new String[]{"asd","lkj"}) {
                    float x=keyboard.getWidth()*(path.charAt(0)=='a'?.025f:.975f),y=keys.get(path.charAt(0)).exactCenterY();
                    begin();event(MotionEvent.ACTION_DOWN,0,new int[]{3},x,y);
                    for(char c:path.toCharArray()) {float[] p=point(c,.5f,.5f);x=p[0];y=p[1];event(MotionEvent.ACTION_MOVE,30,new int[]{3},x,y);}
                    event(MotionEvent.ACTION_UP,30,new int[]{3},x,y);
                    assertEquals("Margin trace owns the stream "+path,Collections.singletonList("TRACE"),emitted);
                }
            }catch(JSONException failure){throw new RuntimeException(failure);} });}
            finally {runTestOnUiThread(host::finish);getInstrumentation().waitForIdleSync();}
        }
    }
    public void testPortraitImprecisionMatrix() throws Throwable {matrix(Configuration.ORIENTATION_PORTRAIT,"portrait");}
    public void testLandscapeImprecisionMatrix() throws Throwable {matrix(Configuration.ORIENTATION_LANDSCAPE,"landscape");}
    private void matrix(int orientation,String name) throws Throwable {
        boolean complete=false;
        try {
            for(boolean english:new boolean[]{false,true})for(float font:new float[]{1f,1.3f}) {
                Intent intent=new Intent(getInstrumentation().getTargetContext(),EditorTestActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);
                EditorTestActivity host=(EditorTestActivity)getInstrumentation().startActivitySync(intent);
                getInstrumentation().waitForIdleSync();
                try {runTestOnUiThread(()-> {
                    try {setup(host,english,orientation,font);singles();pairs();controls();outerMargins();}
                    catch(JSONException failure) {throw new RuntimeException(failure);}
                });} finally {runTestOnUiThread(host::finish);getInstrumentation().waitForIdleSync();}
            }
            complete=true;
        } finally {
            JSONObject report=new JSONObject().put("complete",complete).put("seed",20260908).put("virtualEventTime",true)
                .put("device",android.os.Build.MODEL).put("sdk",android.os.Build.VERSION.SDK_INT).put("layouts",layouts).put("failures",failures).put("cases",outcomes);
            writeReport(getInstrumentation().getTargetContext(),"human-input-"+name+".json",report);
        }
        assertEquals("See per-case human-input-"+name+".json for full failing pointer streams",0,failures);
        assertEquals("Every configuration and profile must run",7480,outcomes.length());
    }
    static void writeReport(Context context,String name,JSONObject report) throws Exception {
        File runId=new File(context.getExternalFilesDir(null),"human-input-run-id.txt");
        String id=runId.exists()?new String(java.nio.file.Files.readAllBytes(runId.toPath()),StandardCharsets.UTF_8).trim():"manual";
        report.put("runId",id);
        try(Writer writer=new OutputStreamWriter(new FileOutputStream(new File(context.getExternalFilesDir(null),name)),StandardCharsets.UTF_8)){writer.write(report.toString());}
    }
}
