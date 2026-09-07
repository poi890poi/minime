package dev.minime.ime;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.graphics.Bitmap;
import android.os.SystemClock;
import android.provider.Settings;
import android.test.ActivityInstrumentationTestCase2;
import android.view.*;
import android.view.accessibility.*;
import android.view.inputmethod.*;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;

/** Explicitly invoked reference study. Coordinates must come from a current screenshot. */
@SuppressWarnings("deprecation")
public final class LegacyStudyTest extends ActivityInstrumentationTestCase2<EditorTestActivity> {
    public LegacyStudyTest() { super(EditorTestActivity.class); }
    private EditorTestActivity activity;
    private AccessibilityNodeInfo find(AccessibilityNodeInfo n,String label) {
        if(n==null) return null;
        if(n.isVisibleToUser() && (label.contentEquals(n.getContentDescription()==null?"":n.getContentDescription()) || label.contentEquals(n.getText()==null?"":n.getText()))) return n;
        for(int i=0;i<n.getChildCount();i++) { AccessibilityNodeInfo found=find(n.getChild(i),label); if(found!=null) { n.recycle(); return found; } }
        n.recycle(); return null;
    }
    private AccessibilityNodeInfo node(String label) {
        for(AccessibilityWindowInfo w:getInstrumentation().getUiAutomation().getWindows()) {
            if(w.getType()!=AccessibilityWindowInfo.TYPE_INPUT_METHOD) continue;
            AccessibilityNodeInfo found=find(w.getRoot(),label); if(found!=null) return found;
        }
        return null;
    }
    private void key(String label,float dy) {
        AccessibilityNodeInfo n=node(label);
        if(n==null && label.equals("阴平")) n=node("空格");
        assertNotNull("Visible legacy key: "+label,n);
        android.graphics.Rect r=new android.graphics.Rect(); n.getBoundsInScreen(r); n.recycle();
        touch(r.exactCenterX(),r.exactCenterY(),dy);
    }
    private void layout(String name) {
        if(name.equals("current")) return;
        AccessibilityNodeInfo letters=node("字母鍵盤");
        if(letters!=null) { letters.recycle(); key("字母鍵盤",0); SystemClock.sleep(250); }
        String probe=name.equals("pinyin")?"q":"玻";
        AccessibilityNodeInfo present=node(probe); if(present!=null) { present.recycle(); return; }
        AccessibilityNodeInfo chooser=node("選取中文鍵盤");
        if(chooser!=null) { chooser.recycle(); key("選取中文鍵盤",0); }
        SystemClock.sleep(300); key(name.equals("pinyin")?"拼音鍵盤":"注音鍵盤",0); SystemClock.sleep(400);
        present=node(probe); assertNotNull("Reference layout ready: "+name,present); present.recycle();
    }
    private void touch(float x,float y,float dy) {
        long start=SystemClock.uptimeMillis(); event(start,MotionEvent.ACTION_DOWN,x,y);
        for(int i=1;i<=5;i++) { SystemClock.sleep(20); event(start,MotionEvent.ACTION_MOVE,x,y+dy*i/5); }
        event(start,MotionEvent.ACTION_UP,x,y+dy); SystemClock.sleep(120);
    }
    private void event(long start,int action,float x,float y) {
        MotionEvent e=MotionEvent.obtain(start,SystemClock.uptimeMillis(),action,x,y,0); e.setSource(InputDevice.SOURCE_TOUCHSCREEN);
        assertTrue(getInstrumentation().getUiAutomation().injectInputEvent(e,true)); e.recycle();
    }
    private void labels(AccessibilityNodeInfo n,JSONArray out) throws JSONException {
        if(n==null) return;
        if(n.isVisibleToUser()) {
            if(n.getText()!=null) out.put(n.getText().toString());
            if(n.getContentDescription()!=null) out.put(n.getContentDescription().toString());
            for(int i=0;i<n.getChildCount();i++) labels(n.getChild(i),out);
        }
        n.recycle();
    }
    private JSONObject snapshot(String label) throws Exception {
        JSONObject result=new JSONObject(); result.put("step",label);
        getInstrumentation().runOnMainSync(()-> {
            try {
                result.put("editor",activity.text.getText().toString());
                result.put("composingStart",BaseInputConnection.getComposingSpanStart(activity.text.getText()));
                result.put("composingEnd",BaseInputConnection.getComposingSpanEnd(activity.text.getText()));
                result.put("selection",activity.text.getSelectionStart());
            } catch(JSONException e) { throw new AssertionError(e); }
        });
        JSONArray visible=new JSONArray();
        for(AccessibilityWindowInfo w:getInstrumentation().getUiAutomation().getWindows())
            if(w.getType()==AccessibilityWindowInfo.TYPE_INPUT_METHOD) labels(w.getRoot(),visible);
        result.put("keyboard",visible); return result;
    }
    public void testObservedSequences() throws Exception {
        assertEquals("com.google.android.apps.inputmethod.zhuyin/.ZhuyinInputMethodService",
            Settings.Secure.getString(getInstrumentation().getTargetContext().getContentResolver(),Settings.Secure.DEFAULT_INPUT_METHOD));
        activity=getActivity();
        AccessibilityServiceInfo service=getInstrumentation().getUiAutomation().getServiceInfo();
        service.flags|=AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS; getInstrumentation().getUiAutomation().setServiceInfo(service);
        File dir=activity.getExternalFilesDir(null);
        String source=new String(java.nio.file.Files.readAllBytes(new File(dir,"legacy-plan.json").toPath()),StandardCharsets.UTF_8);
        JSONArray cases=new JSONArray(source), records=new JSONArray();
        for(int c=0;c<cases.length();c++) {
            JSONObject test=cases.getJSONObject(c);
            getInstrumentation().runOnMainSync(()-> {
                activity.text.setText(""); activity.text.requestFocus();
                InputMethodManager imm=(InputMethodManager)activity.getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
                imm.restartInput(activity.text); imm.showSoftInput(activity.text,InputMethodManager.SHOW_IMPLICIT);
            });
            SystemClock.sleep(600);
            layout(test.optString("layout","zhuyin"));
            JSONArray steps=new JSONArray(),actions=test.getJSONArray("actions");
            records.put(new JSONObject().put("name",test.getString("name")).put("steps",steps));
            for(int i=0;i<actions.length();i++) {
                JSONObject action=actions.getJSONObject(i);
                if(action.optBoolean("snapshot",false)) { /* observation only */ }
                else if(action.optBoolean("doubleTap",false) || action.optBoolean("hold",false)) {
                    AccessibilityNodeInfo target=node(action.getString("description")); assertNotNull(target);
                    android.graphics.Rect r=new android.graphics.Rect(); target.getBoundsInScreen(r); target.recycle();
                    int taps=action.optBoolean("doubleTap",false)?2:1;
                    for(int tap=0;tap<taps;tap++) {
                        long down=SystemClock.uptimeMillis(); event(down,MotionEvent.ACTION_DOWN,r.exactCenterX(),r.exactCenterY());
                        SystemClock.sleep(action.optBoolean("hold",false)?700:35);
                        event(down,MotionEvent.ACTION_UP,r.exactCenterX(),r.exactCenterY()); SystemClock.sleep(65);
                    }
                    SystemClock.sleep(150);
                }
                else if(action.has("cursor")) getInstrumentation().runOnMainSync(()->activity.text.setSelection(action.optInt("cursor")));
                else if(action.has("description")) key(action.getString("description"),(float)action.optDouble("dy",0));
                else touch((float)action.getDouble("x"),(float)action.getDouble("y"),(float)action.optDouble("dy",0));
                steps.put(snapshot(action.optString("label","touch")));
                try(FileOutputStream out=new FileOutputStream(new File(dir,"legacy-observations.json"))) { out.write(records.toString(2).getBytes(StandardCharsets.UTF_8)); }
                if(action.optBoolean("capture",false)) {
                    Bitmap bitmap=getInstrumentation().getUiAutomation().takeScreenshot();
                    try(FileOutputStream out=new FileOutputStream(new File(dir,"legacy-"+test.getString("name")+"-"+i+".png"))) { bitmap.compress(Bitmap.CompressFormat.PNG,100,out); }
                    finally { bitmap.recycle(); }
                }
            }
        }
    }
}
