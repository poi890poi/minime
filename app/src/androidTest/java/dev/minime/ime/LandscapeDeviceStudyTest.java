package dev.minime.ime;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.*;
import android.graphics.*;
import android.os.*;
import android.provider.Settings;
import android.test.ActivityInstrumentationTestCase2;
import android.view.*;
import android.view.accessibility.*;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** On-screen injected touch trial. Not a human typing benchmark. */
@SuppressWarnings("deprecation")
public final class LandscapeDeviceStudyTest extends ActivityInstrumentationTestCase2<LandscapeStudyActivity> {
    public LandscapeDeviceStudyTest(){super(LandscapeStudyActivity.class);}
    private LandscapeStudyActivity activity;
    private File dir;
    private final JSONArray records=new JSONArray();
    private void main(Runnable task){getInstrumentation().runOnMainSync(task);getInstrumentation().waitForIdleSync();}
    private Rect bounds(View v){int[] xy=new int[2];v.getLocationOnScreen(xy);return new Rect(xy[0],xy[1],xy[0]+v.getWidth(),xy[1]+v.getHeight());}
    private void event(long start,int action,float x,float y){
        MotionEvent e=MotionEvent.obtain(start,SystemClock.uptimeMillis(),action,x,y,0);e.setSource(InputDevice.SOURCE_TOUCHSCREEN);
        try{assertTrue("Android accepted injection",getInstrumentation().getUiAutomation().injectInputEvent(e,true));}finally{e.recycle();}
    }
    private void touch(Rect r,int direction){
        float x=r.exactCenterX(),y=r.exactCenterY(),travel=24*activity.getResources().getDisplayMetrics().density*direction;
        long start=SystemClock.uptimeMillis();event(start,MotionEvent.ACTION_DOWN,x,y);
        try{
            if(direction!=0)for(int n=1;n<=4;n++){SystemClock.sleep(12);event(start,MotionEvent.ACTION_MOVE,x,y+travel*n/4);}
            else SystemClock.sleep(25);
        }finally{event(start,MotionEvent.ACTION_UP,x,y+travel);}
        getInstrumentation().waitForIdleSync();
    }
    private void screenshot(String name)throws Exception{
        Bitmap b=getInstrumentation().getUiAutomation().takeScreenshot();assertNotNull(b);
        try(FileOutputStream out=new FileOutputStream(new File(dir,"landscape-"+name+".png"))){assertTrue(b.compress(Bitmap.CompressFormat.PNG,100,out));}finally{b.recycle();}
    }
    private AccessibilityNodeInfo find(AccessibilityNodeInfo n,String text){
        if(n==null)return null;
        try{
            if(n.isVisibleToUser()&&(text.contentEquals(n.getText()==null?"":n.getText())||text.contentEquals(n.getContentDescription()==null?"":n.getContentDescription())))return AccessibilityNodeInfo.obtain(n);
            for(int i=0;i<n.getChildCount();i++){AccessibilityNodeInfo found=find(n.getChild(i),text);if(found!=null)return found;}
            return null;
        }finally{n.recycle();}
    }
    private AccessibilityNodeInfo imeKey(String letter){
        for(AccessibilityWindowInfo w:getInstrumentation().getUiAutomation().getWindows())try{
            if(w.getType()==AccessibilityWindowInfo.TYPE_INPUT_METHOD){AccessibilityNodeInfo n=find(w.getRoot(),letter);if(n!=null)return n;}
        }finally{w.recycle();}return null;
    }
    public void testNativeLandscapeLayouts()throws Exception{
        Context context=getInstrumentation().getTargetContext();
        SharedPreferences prefs=context.getSharedPreferences("settings",Context.MODE_PRIVATE);
        Map<String,?> prior=new HashMap<>(prefs.getAll());
        String auto=Settings.System.getString(context.getContentResolver(),Settings.System.ACCELEROMETER_ROTATION);
        String rotation=Settings.System.getString(context.getContentResolver(),Settings.System.USER_ROTATION);
        int errors=0;boolean integrationPassed=false;
        dir=context.getExternalFilesDir(null);
        // Never export a previous run's screenshot after a failed current run.
        for(String name:new String[]{"compact","split-low","split-tall","side-panels","actual-ime"}){
            File file=new File(dir,"landscape-"+name+".png");if(file.exists()&&!file.delete())throw new IOException("Cannot clear old trial screenshot: "+name);
        }
        try{
            prefs.edit().putBoolean("joined_kalq",true).commit();
            activity=getActivity();getInstrumentation().waitForIdleSync();
            // An orientation launch can recreate the fixture after getActivity.
            // Operate only on the current, attached, focused Activity instance.
            for(int n=0;n<40;n++){
                SystemClock.sleep(100);LandscapeStudyActivity current=LandscapeStudyActivity.current;
                if(current!=null&&!current.isDestroyed()&&current.hasWindowFocus()&&current.root.getWidth()>current.root.getHeight()){
                    activity=current;break;
                }
            }
            assertSame("Current Activity instance",LandscapeStudyActivity.current,activity);
            assertTrue("Fixture owns the visible window",activity.hasWindowFocus());
            dir=activity.getExternalFilesDir(null);
            assertTrue(activity.root.getWidth()>activity.root.getHeight());
            for(String name:new String[]{"compact","split-low","split-tall","side-panels"}){
                assertSame("Fixture has not been replaced",LandscapeStudyActivity.current,activity);
                assertTrue("Fixture remains attached",activity.root.isAttachedToWindow());
                main(()->activity.show(name));SystemClock.sleep(200);screenshot(name);
                float density=activity.getResources().getDisplayMetrics().density;
                JSONObject row=new JSONObject().put("layout",name).put("width_dp",activity.root.getWidth()/density).put("height_dp",activity.root.getHeight()/density)
                    .put("row_dp",activity.rowHeight).put("block_dp",activity.blockWidth).put("clear_above_dp",activity.clearHeight);
                JSONArray attempts=new JSONArray(),rects=new JSONArray();
                for(Map.Entry<String,SlideKey> entry:activity.letters.entrySet()){
                    String letter=entry.getKey();SlideKey key=entry.getValue();Rect r=bounds(key);
                    rects.put(new JSONObject().put("key",letter).put("left",r.left).put("top",r.top).put("right",r.right).put("bottom",r.bottom));
                    String symbol="";AccessibilityNodeInfo info=key.createAccessibilityNodeInfo();
                    for(AccessibilityNodeInfo.AccessibilityAction a:info.getActionList())if(a.getLabel()!=null&&a.getLabel().toString().startsWith("Slide down: "))symbol=a.getLabel().toString().substring(12);
                    info.recycle();assertFalse(symbol.isEmpty());
                    for(int direction:new int[]{0,-1,1}){
                        main(()->{activity.emitted.clear();activity.editor.setText("");});
                        touch(r,direction);
                        String expected=direction==0?letter:direction<0?"LITERAL:"+letter.toUpperCase(Locale.ROOT):"LITERAL:"+symbol;
                        List<String> actual=new ArrayList<>(activity.emitted);boolean pass=actual.equals(Collections.singletonList(expected));
                        if(!pass)errors++;
                        attempts.put(new JSONObject().put("key",letter).put("direction",direction).put("expected",expected).put("actual",new JSONArray(actual)).put("pass",pass));
                    }
                }
                // Real repeated touchscreen sequences, with no dictionary/autocorrection.
                main(()->{activity.emitted.clear();activity.editor.setText("");});
                String phrase="thequickbrownfoxjumpsoverthelazydog";
                for(char c:(phrase+phrase).toCharArray())touch(bounds(activity.letters.get(String.valueOf(c))),0);
                String actual=activity.editor.getText().toString();boolean repeated=actual.equals(phrase+phrase);if(!repeated)errors++;
                row.put("attempts",attempts).put("key_bounds_px",rects).put("repeated_expected",phrase+phrase).put("repeated_actual",actual).put("repeated_pass",repeated);records.put(row);
            }
            // Separately observe the shipped compact keyboard in a real IME window.
            AccessibilityServiceInfo service=getInstrumentation().getUiAutomation().getServiceInfo();service.flags|=AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS;
            getInstrumentation().getUiAutomation().setServiceInfo(service);
            main(()->activity.realIme());
            AccessibilityNodeInfo m=null;
            for(int n=0;n<30&&m==null;n++){SystemClock.sleep(200);m=imeKey("m");if(m==null)m=imeKey("M");}
            assertNotNull("Actual MinIME window is available",m);m.recycle();
            // Visibility can precede the IME opening animation's final position.
            // Require stable screen bounds before injecting the first contact.
            Rect priorBounds=null;int stable=0;
            for(int n=0;n<30&&stable<3;n++){
                SystemClock.sleep(150);AccessibilityNodeInfo key=imeKey("h");if(key==null)key=imeKey("H");assertNotNull(key);
                Rect now=new Rect();key.getBoundsInScreen(now);key.recycle();
                stable=now.equals(priorBounds)?stable+1:0;priorBounds=now;
            }
            assertEquals("IME geometry settled",3,stable);
            JSONArray realTouches=new JSONArray();
            for(char c:"hello".toCharArray()){
                AccessibilityNodeInfo key=imeKey(String.valueOf(c));if(key==null)key=imeKey(String.valueOf(c).toUpperCase(Locale.ROOT));assertNotNull(key);
                Rect r=new Rect();key.getBoundsInScreen(r);key.recycle();touch(r,0);
                realTouches.put(new JSONObject().put("intended",String.valueOf(c)).put("bounds",r.toShortString()).put("editor_after",activity.editor.getText().toString()));
            }
            screenshot("actual-ime");
            records.put(new JSONObject().put("layout","actual-ime").put("editor_text",activity.editor.getText().toString()).put("touches",realTouches).put("editor_height_dp",activity.editor.getHeight()/activity.getResources().getDisplayMetrics().density));
            assertEquals("Real IME retains every typed letter", "hello", activity.editor.getText().toString());
            integrationPassed=true;
        }finally{
            if(dir!=null)try(FileOutputStream out=new FileOutputStream(new File(dir,"landscape-device.json"))){
                JSONObject result=new JSONObject().put("device",Build.MODEL).put("api",Build.VERSION.SDK_INT).put("injected_failures",errors).put("integration_passed",integrationPassed).put("records",records);
                out.write(result.toString(2).getBytes(StandardCharsets.UTF_8));
            }
            if(activity!=null)main(()->activity.finish());
            SharedPreferences.Editor edit=prefs.edit().clear();for(Map.Entry<String,?> e:prior.entrySet()){
                Object v=e.getValue();if(v instanceof Boolean)edit.putBoolean(e.getKey(),(Boolean)v);else if(v instanceof String)edit.putString(e.getKey(),(String)v);
                else if(v instanceof Integer)edit.putInt(e.getKey(),(Integer)v);else if(v instanceof Long)edit.putLong(e.getKey(),(Long)v);else if(v instanceof Float)edit.putFloat(e.getKey(),(Float)v);
            }edit.commit();
            assertEquals("Auto rotation unchanged",auto,Settings.System.getString(context.getContentResolver(),Settings.System.ACCELEROMETER_ROTATION));
            assertEquals("User rotation unchanged",rotation,Settings.System.getString(context.getContentResolver(),Settings.System.USER_ROTATION));
        }
        assertEquals("Injected trial failures; inspect JSON",0,errors);
    }
}
