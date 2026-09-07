package dev.minime.ime;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Context;
import android.graphics.*;
import android.os.*;
import android.provider.Settings;
import android.test.ActivityInstrumentationTestCase2;
import android.text.InputType;
import android.view.*;
import android.view.accessibility.*;
import android.view.inputmethod.*;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Observation-only paired replay. Normal acceptance tests exclude this class. */
@SuppressWarnings("deprecation")
public final class ParityStudyTest extends ActivityInstrumentationTestCase2<EditorTestActivity> {
    public ParityStudyTest(){super(EditorTestActivity.class);}
    private EditorTestActivity activity;
    private File dir;
    private String provider,mode,caseId;
    private final JSONArray records=new JSONArray();
    private String ime(){return provider.equals("google")?"com.google.android.apps.inputmethod.zhuyin/.ZhuyinInputMethodService":"dev.minime.ime/.MiniMeService";}
    private String selectedIme(){return Settings.Secure.getString(activity.getContentResolver(),Settings.Secure.DEFAULT_INPUT_METHOD);}
    private void shell(String command)throws Exception {
        try(ParcelFileDescriptor fd=getInstrumentation().getUiAutomation().executeShellCommand(command);
            InputStream in=new ParcelFileDescriptor.AutoCloseInputStream(fd)){while(in.read()!=-1){}}
    }
    private AccessibilityNodeInfo find(AccessibilityNodeInfo n,String label){
        if(n==null)return null;
        AccessibilityNodeInfo best=null;
        if(n.isVisibleToUser() && (label.contentEquals(n.getText()==null?"":n.getText()) || label.contentEquals(n.getContentDescription()==null?"":n.getContentDescription())))best=AccessibilityNodeInfo.obtain(n);
        for(int i=0;i<n.getChildCount();i++){
            AccessibilityNodeInfo child=find(n.getChild(i),label);if(child==null)continue;
            Rect a=new Rect(),b=new Rect();child.getBoundsInScreen(a);if(best!=null)best.getBoundsInScreen(b);
            // A raw candidate such as "x" can duplicate the letter key's label.
            // The typing key is below the candidate strip.
            if(best==null || a.centerY()>b.centerY()){if(best!=null)best.recycle();best=child;}else child.recycle();
        }
        n.recycle();return best;
    }
    private AccessibilityNodeInfo node(String... labels){
        for(String label:labels)for(AccessibilityWindowInfo w:getInstrumentation().getUiAutomation().getWindows()){
            if(w.getType()!=AccessibilityWindowInfo.TYPE_INPUT_METHOD)continue;
            AccessibilityNodeInfo n=find(w.getRoot(),label);if(n!=null)return n;
        }return null;
    }
    private boolean has(String label){AccessibilityNodeInfo n=node(label);if(n==null)return false;n.recycle();return true;}
    private String[] labels(String semantic){
        switch(semantic){
            case "SPACE":return new String[]{"Space","空格","阴平"};
            case "ENTER":return new String[]{"Enter 鍵","↵","Search","Go","Done","Next","搜尋","搜索","完成","前往","下一步"};
            case "DELETE":return new String[]{"刪除","⌫"};
            case "SHIFT":return new String[]{"Shift","⇧","⬆","⇪"};
            case "COMMA":return new String[]{",","，","全形逗號"};
            case "PERIOD":return new String[]{".","。","全形句號"};
            case "SYMBOLS":return new String[]{"符號鍵盤","?123"};
            case "EMOJI":return new String[]{"Emoji","表情符號鍵盤"};
            case "LETTERS":return new String[]{"字母鍵盤","ABC"};
            case "EXPAND":return new String[]{"其他候選鍵","Expand candidates","Next candidate page"};
            case "LANGUAGE":return provider.equals("google")?new String[]{mode.equals("english")?"中文鍵盤":"英文鍵盤","下一種語言"}:new String[]{"Switch to Chinese","Switch to English"};
            default:return new String[]{semantic,semantic.toUpperCase(Locale.ROOT)};
        }
    }
    private void event(long t,int action,float x,float y){
        MotionEvent e=MotionEvent.obtain(t,SystemClock.uptimeMillis(),action,x,y,0);e.setSource(InputDevice.SOURCE_TOUCHSCREEN);
        try{assertTrue(getInstrumentation().getUiAutomation().injectInputEvent(e,true));}finally{e.recycle();}
    }
    private void press(String key,int direction,int hold,boolean twice)throws Exception {press(key,direction,hold,twice,.8f);}
    private void press(String key,int direction,int hold,boolean twice,float travel)throws Exception {
        if(!selectedIme().equals(ime()))throw new IllegalStateException("Selected IME changed: "+selectedIme());
        AccessibilityNodeInfo n=node(labels(key));
        if(n==null)throw new IllegalStateException("Visible action unavailable: "+key);
        Rect r=new Rect();n.getBoundsInScreen(r);n.recycle();
        for(int tap=0;tap<(twice?2:1);tap++){
            long t=SystemClock.uptimeMillis();float x=r.exactCenterX(),y=r.exactCenterY();event(t,MotionEvent.ACTION_DOWN,x,y);
            try{
                if(direction!=0)for(int j=1;j<=4;j++){SystemClock.sleep(12);event(t,MotionEvent.ACTION_MOVE,x,y+direction*r.height()*travel*j/4);}
                else SystemClock.sleep(hold>0?hold:35);
                if(hold>0){saveSnapshot("held-"+key);capture("held-"+key);}
            }finally{event(t,MotionEvent.ACTION_UP,x,y+direction*r.height()*travel);}
            SystemClock.sleep(twice?65:50);
        }
    }
    private void pointers(long start,int action,int[] ids,float... xy) {
        MotionEvent.PointerProperties[] properties=new MotionEvent.PointerProperties[ids.length];MotionEvent.PointerCoords[] coords=new MotionEvent.PointerCoords[ids.length];
        for(int i=0;i<ids.length;i++){properties[i]=new MotionEvent.PointerProperties();properties[i].id=ids[i];properties[i].toolType=MotionEvent.TOOL_TYPE_FINGER;
            coords[i]=new MotionEvent.PointerCoords();coords[i].x=xy[2*i];coords[i].y=xy[2*i+1];coords[i].pressure=1;coords[i].size=1;}
        MotionEvent e=MotionEvent.obtain(start,SystemClock.uptimeMillis(),action,ids.length,properties,coords,0,0,1,1,0,0,InputDevice.SOURCE_TOUCHSCREEN,0);
        try{assertTrue(getInstrumentation().getUiAutomation().injectInputEvent(e,true));}finally{e.recycle();}
    }
    private void overlap(String pair,boolean reverse)throws Exception {
        AccessibilityNodeInfo first=node(labels(pair.substring(0,1))),second=node(labels(pair.substring(1,2)));
        if(first==null || second==null)throw new IllegalStateException("Overlap keys missing");
        Rect a=new Rect(),b=new Rect();first.getBoundsInScreen(a);second.getBoundsInScreen(b);first.recycle();second.recycle();
        long start=SystemClock.uptimeMillis();pointers(start,MotionEvent.ACTION_DOWN,new int[]{3},a.exactCenterX(),a.exactCenterY());SystemClock.sleep(20);
        pointers(start,MotionEvent.ACTION_POINTER_DOWN|(1<<MotionEvent.ACTION_POINTER_INDEX_SHIFT),new int[]{3,7},a.exactCenterX(),a.exactCenterY(),b.exactCenterX(),b.exactCenterY());SystemClock.sleep(20);
        pointers(start,MotionEvent.ACTION_POINTER_UP|((reverse?1:0)<<MotionEvent.ACTION_POINTER_INDEX_SHIFT),new int[]{3,7},a.exactCenterX(),a.exactCenterY(),b.exactCenterX(),b.exactCenterY());SystemClock.sleep(20);
        Rect last=reverse?a:b;pointers(start,MotionEvent.ACTION_UP,new int[]{reverse?3:7},last.exactCenterX(),last.exactCenterY());
    }
    private void text(String value)throws Exception {
        for(int cp:value.codePoints().toArray()){
            String c=new String(Character.toChars(cp));
            if(cp==' ')press("SPACE",0,0,false);
            else if(cp=='\n')press("ENTER",0,0,false);
            else if(cp==',')press("COMMA",0,0,false);
            else if(cp=='.')press("PERIOD",0,0,false);
            else press(c,0,0,false);
        }
    }
    private void configure(String field){
        int type=InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_MULTI_LINE,options=EditorInfo.IME_ACTION_NONE;
        if(field.equals("sentences"))type|=InputType.TYPE_TEXT_FLAG_CAP_SENTENCES;
        if(field.equals("words"))type|=InputType.TYPE_TEXT_FLAG_CAP_WORDS;
        if(field.equals("nosuggest"))type|=InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS;
        if(field.equals("private"))options|=EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING;
        if(field.equals("password"))type=InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD;
        if(field.equals("url"))type=InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_URI;
        if(field.equals("email"))type=InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS;
        if(field.equals("number"))type=InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL;
        if(field.equals("search")){type=InputType.TYPE_CLASS_TEXT;options=EditorInfo.IME_ACTION_SEARCH;}
        final int input=type,imeOptions=options;
        getInstrumentation().runOnMainSync(()->{
            android.view.ViewGroup parent=(android.view.ViewGroup)activity.text.getParent();int index=parent.indexOfChild(activity.text);
            android.view.ViewGroup.LayoutParams params=activity.text.getLayoutParams();parent.removeView(activity.text);
            activity.text=new android.widget.EditText(activity);activity.text.setHint("Mixed text");activity.text.setContentDescription("Mixed text");
            activity.text.setInputType(input);activity.text.setImeOptions(imeOptions);activity.text.setMinLines((input&InputType.TYPE_TEXT_FLAG_MULTI_LINE)!=0?2:1);
            activity.text.setOnEditorActionListener((v,id,event)->{if(event!=null || id==EditorInfo.IME_ACTION_NONE || id==EditorInfo.IME_ACTION_UNSPECIFIED)return false;activity.actions.setText("Editor action: "+id);return true;});
            parent.addView(activity.text,index,params);activity.text.requestFocus();activity.actions.setText("No editor action yet");
            InputMethodManager imm=(InputMethodManager)activity.getSystemService(Context.INPUT_METHOD_SERVICE);
            activity.text.post(()->imm.showSoftInput(activity.text,InputMethodManager.SHOW_IMPLICIT));
        });SystemClock.sleep(400);
        boolean[] owned={false};long until=SystemClock.uptimeMillis()+1500;
        do {
            getInstrumentation().runOnMainSync(()->owned[0]=activity.text.isAttachedToWindow() && activity.text.hasWindowFocus() && activity.text.hasFocus()
                && ((InputMethodManager)activity.getSystemService(Context.INPUT_METHOD_SERVICE)).isActive(activity.text));
            if(owned[0])break;SystemClock.sleep(25);
        }while(SystemClock.uptimeMillis()<until);
        assertTrue("Observed editor is attached, focused and owns input",owned[0]);
    }
    private void setupCase(JSONObject test)throws Exception {
        mode=test.getString("mode");caseId=test.getString("id");
        // Own the currently visible editor, including after IME/configuration restarts.
        android.content.Intent fresh=new android.content.Intent(getInstrumentation().getTargetContext(),EditorTestActivity.class);
        fresh.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK|android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK);
        activity=(EditorTestActivity)getInstrumentation().startActivitySync(fresh);
        boolean landscape=test.optBoolean("landscape",false);
        if((activity.getResources().getConfiguration().orientation==android.content.res.Configuration.ORIENTATION_LANDSCAPE)!=landscape){
            android.app.Instrumentation.ActivityMonitor monitor=getInstrumentation().addMonitor(EditorTestActivity.class.getName(),null,false);
            getInstrumentation().runOnMainSync(()->activity.setRequestedOrientation(landscape?android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE:android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT));
            android.app.Activity replacement=getInstrumentation().waitForMonitorWithTimeout(monitor,4000);getInstrumentation().removeMonitor(monitor);
            if(replacement instanceof EditorTestActivity)activity=(EditorTestActivity)replacement;
            SystemClock.sleep(350);
        }
        if(provider.equals("minime")){
            activity.getSharedPreferences("settings",Context.MODE_PRIVATE).edit().clear().putBoolean("english_mode",mode.equals("english")).commit();
            activity.getSharedPreferences("learning",Context.MODE_PRIVATE).edit().clear().commit();
        }
        if(!selectedIme().equals(ime())){shell("ime set "+ime());SystemClock.sleep(450);}
        configure("normal");
        if(!has("Space") && !has("空格")){
            SystemClock.sleep(600);
            getInstrumentation().runOnMainSync(()->((InputMethodManager)activity.getSystemService(Context.INPUT_METHOD_SERVICE)).showSoftInput(activity.text,InputMethodManager.SHOW_IMPLICIT));
            SystemClock.sleep(600);
        }
        if(provider.equals("google")){
            if(has("字母鍵盤"))press("LETTERS",0,0,false);
            if(mode.equals("english")){if(!has(",") || !has("."))press("英文鍵盤",0,0,false);}
            else{
                if(!has("全形句號")){
                    if(!has("中文鍵盤")){press("DELETE",0,0,false);SystemClock.sleep(250);}
                    press("中文鍵盤",0,0,false);SystemClock.sleep(250);
                }
                if(!has("q") || !has("全形句號")){
                    if(has("選取中文鍵盤"))press("選取中文鍵盤",0,0,false);
                    SystemClock.sleep(250);press("拼音鍵盤",0,0,false);
                }
            }
            SystemClock.sleep(250);
            if(!has("q") && has("Q")){press("SHIFT",0,0,false);SystemClock.sleep(150);}
        }
        String field=test.optString("field","normal");if(!field.equals("normal"))configure(field);
    }
    private void visible(AccessibilityNodeInfo n,JSONArray result)throws JSONException {
        if(n==null)return;
        if(n.isVisibleToUser()){
            if(n.getText()!=null)result.put(n.getText().toString());
            if(n.getContentDescription()!=null && !Objects.equals(n.getText(),n.getContentDescription()))result.put(n.getContentDescription().toString());
            for(int i=0;i<n.getChildCount();i++)visible(n.getChild(i),result);
        }n.recycle();
    }
    private JSONObject snapshot(String stage)throws Exception {
        SystemClock.sleep(120);JSONObject s=new JSONObject().put("stage",stage).put("ime",selectedIme()).put("orientation",activity.getResources().getConfiguration().orientation);
        getInstrumentation().runOnMainSync(()->{try{
            s.put("text",activity.text.getText().toString()).put("composingStart",BaseInputConnection.getComposingSpanStart(activity.text.getText()))
                .put("composingEnd",BaseInputConnection.getComposingSpanEnd(activity.text.getText())).put("selectionStart",activity.text.getSelectionStart())
                .put("selectionEnd",activity.text.getSelectionEnd()).put("editorAction",activity.actions.getText().toString());
        }catch(JSONException e){throw new AssertionError(e);}});
        JSONArray ui=new JSONArray();for(AccessibilityWindowInfo w:getInstrumentation().getUiAutomation().getWindows())if(w.getType()==AccessibilityWindowInfo.TYPE_INPUT_METHOD){
            Rect bounds=new Rect();w.getBoundsInScreen(bounds);s.put("keyboardBounds",bounds.flattenToString());visible(w.getRoot(),ui);
        }return s.put("visible",ui);
    }
    private JSONArray currentSteps;
    private void saveSnapshot(String stage)throws Exception{currentSteps.put(snapshot(stage));}
    private void capture(String stage)throws Exception{
        Bitmap b=getInstrumentation().getUiAutomation().takeScreenshot();if(b==null)return;
        try(FileOutputStream out=new FileOutputStream(new File(dir,"parity-"+provider+"-"+caseId+"-"+stage+".png"))){b.compress(Bitmap.CompressFormat.PNG,100,out);}finally{b.recycle();}
    }
    private void action(JSONObject action)throws Exception{
        if(action.has("type"))text(action.getString("type"));
        else if(action.has("trace")){
            ArrayList<PointF> points=new ArrayList<>();for(char letter:action.getString("trace").toCharArray()){
                AccessibilityNodeInfo n=node(labels(String.valueOf(letter)));if(n==null)throw new IllegalStateException("Trace key unavailable: "+letter);
                Rect r=new Rect();n.getBoundsInScreen(r);n.recycle();points.add(new PointF(r.exactCenterX(),r.exactCenterY()));
            }
            long at=SystemClock.uptimeMillis();PointF first=points.get(0),last=points.get(points.size()-1);event(at,MotionEvent.ACTION_DOWN,first.x,first.y);
            try{for(int i=1;i<points.size();i++)for(int j=1;j<=6;j++){PointF a=points.get(i-1),b=points.get(i);SystemClock.sleep(15);event(at,MotionEvent.ACTION_MOVE,a.x+(b.x-a.x)*j/6,a.y+(b.y-a.y)*j/6);}}
            finally{event(at,MotionEvent.ACTION_UP,last.x,last.y);}
        }
        else if(action.has("overlap"))overlap(action.getString("overlap"),action.optBoolean("reverse",false));
        else if(action.has("key"))press(action.getString("key"),action.optInt("slide",0),action.optInt("hold",0),action.optBoolean("double",false),(float)action.optDouble("travel",.8));
        else if(action.has("choose"))press(provider.equals("google")?action.getString("choose"):"Candidate "+action.getString("choose"),0,0,false);
        else if(action.has("raw"))press(provider.equals("google")?action.getString("raw"):"Exact input "+action.getString("raw"),0,0,false);
        else if(action.has("cursor"))getInstrumentation().runOnMainSync(()->activity.text.setSelection(action.optInt("cursor"),action.optInt("end",action.optInt("cursor"))));
        else if(action.optBoolean("restart"))getInstrumentation().runOnMainSync(()->((InputMethodManager)activity.getSystemService(Context.INPUT_METHOD_SERVICE)).restartInput(activity.text));
        else if(action.optBoolean("hideShow")){
            getInstrumentation().runOnMainSync(()->((InputMethodManager)activity.getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(activity.text.getWindowToken(),0));SystemClock.sleep(700);
            getInstrumentation().runOnMainSync(()->((InputMethodManager)activity.getSystemService(Context.INPUT_METHOD_SERVICE)).showSoftInput(activity.text,InputMethodManager.SHOW_IMPLICIT));
            SystemClock.sleep(700);
        }
        SystemClock.sleep(100);
    }
    public void testPairedObservations()throws Exception{
        activity=getActivity();dir=activity.getExternalFilesDir(null);DictionaryRepository.load(activity).get(30,java.util.concurrent.TimeUnit.SECONDS);
        AccessibilityServiceInfo access=getInstrumentation().getUiAutomation().getServiceInfo();access.flags|=AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS;getInstrumentation().getUiAutomation().setServiceInfo(access);
        JSONArray plan=new JSONArray(new String(java.nio.file.Files.readAllBytes(new File(dir,"parity-plan.json").toPath()),StandardCharsets.UTF_8));
        for(String name:new String[]{"google","minime"}){provider=name;
            for(int i=0;i<plan.length();i++){
                JSONObject test=plan.getJSONObject(i),record=new JSONObject().put("provider",provider).put("id",test.getString("id"));currentSteps=new JSONArray();record.put("steps",currentSteps);records.put(record);
                try{
                    setupCase(test);saveSnapshot("ready");
                    JSONArray actions=test.getJSONArray("actions");
                    for(int j=0;j<actions.length();j++){JSONObject a=actions.getJSONObject(j);action(a);saveSnapshot(a.optString("label","step-"+j));if(a.optBoolean("capture"))capture("step-"+j);}
                    record.put("status",selectedIme().equals(ime())?"observed":"left-provider");
                }catch(Exception|AssertionError failure){record.put("status","unavailable").put("error",failure.toString());saveSnapshot("failure");capture("failure");}
                try(FileOutputStream out=new FileOutputStream(new File(dir,"parity-observations.json"))){out.write(records.toString(2).getBytes(StandardCharsets.UTF_8));}
                android.util.Log.i("ParityStudy",provider+" "+test.getString("id")+" "+record.getString("status"));
            }
        }
    }
}
