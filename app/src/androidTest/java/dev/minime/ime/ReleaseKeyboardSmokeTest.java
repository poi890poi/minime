package dev.minime.ime;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.graphics.Rect;
import android.os.*;
import android.test.ActivityInstrumentationTestCase2;
import android.view.*;
import android.view.accessibility.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import dev.minime.core.InputMode;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.json.*;

/** Exercises the production Settings editor on a non-debuggable release payload.
 * Requires a matching test certificate; does not certify the Play signing chain,
 * physical touch accuracy or presentation latency. Outer lease restores preferences. */
@SuppressWarnings("deprecation")
public final class ReleaseKeyboardSmokeTest extends ActivityInstrumentationTestCase2<SettingsActivity> {
    public ReleaseKeyboardSmokeTest(){super(SettingsActivity.class);}
    private EditText editor(View view) {
        if(view instanceof EditText && "這個 pronunciation 不對".contentEquals(((EditText)view).getHint()))return (EditText)view;
        if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++){EditText result=editor(((ViewGroup)view).getChildAt(i));if(result!=null)return result;}
        return null;
    }
    private AccessibilityNodeInfo find(AccessibilityNodeInfo n,String label) {
        if(n==null)return null;
        if(n.isVisibleToUser() && label.contentEquals(n.getContentDescription()==null?"":n.getContentDescription()))return n;
        for(int i=0;i<n.getChildCount();i++){AccessibilityNodeInfo found=find(n.getChild(i),label);if(found!=null){n.recycle();return found;}}
        n.recycle();return null;
    }
    private AccessibilityNodeInfo node(String label) {
        for(AccessibilityWindowInfo w:getInstrumentation().getUiAutomation().getWindows())if(w.getType()==AccessibilityWindowInfo.TYPE_INPUT_METHOD) {
            AccessibilityNodeInfo n=find(w.getRoot(),label);if(n!=null)return n;
        }return null;
    }
    private void tap(String label)throws Exception {
        long until=SystemClock.uptimeMillis()+6000;AccessibilityNodeInfo n;
        do {n=node(label);if(n==null)n=node(label.toUpperCase(Locale.ROOT));if(n!=null)break;SystemClock.sleep(30);}while(SystemClock.uptimeMillis()<until);
        assertNotNull("Visible key "+label,n);Rect r=new Rect();n.getBoundsInScreen(r);n.recycle();tap(r);
    }
    private void tap(Rect r) {
        long down=SystemClock.uptimeMillis();
        for(int action:new int[]{MotionEvent.ACTION_DOWN,MotionEvent.ACTION_UP}) {
            MotionEvent e=MotionEvent.obtain(down,SystemClock.uptimeMillis(),action,r.exactCenterX(),r.exactCenterY(),0);e.setSource(InputDevice.SOURCE_TOUCHSCREEN);
            try {assertTrue(getInstrumentation().getUiAutomation().injectInputEvent(e,true));}finally{e.recycle();}SystemClock.sleep(40);
        }
    }
    private void candidates(AccessibilityNodeInfo n,List<String> result) {
        if(n==null)return;CharSequence d=n.getContentDescription();
        if(n.isVisibleToUser() && d!=null && d.toString().startsWith("Candidate ") && !d.toString().equals("Candidate list"))result.add(d.toString().substring(10));
        for(int i=0;i<n.getChildCount();i++)candidates(n.getChild(i),result);n.recycle();
    }
    private List<String> candidates() {
        List<String> values=new ArrayList<>();for(AccessibilityWindowInfo w:getInstrumentation().getUiAutomation().getWindows())if(w.getType()==AccessibilityWindowInfo.TYPE_INPUT_METHOD)candidates(w.getRoot(),values);return values;
    }
    public void testExplicitLatinSourceCompletion()throws Exception {
        Context context=getInstrumentation().getTargetContext();
        assertEquals("Non-debuggable payload",0,context.getApplicationInfo().flags&ApplicationInfo.FLAG_DEBUGGABLE);
        context.getSharedPreferences("settings",0).edit().putBoolean("rime_pinyin",false)
            .putBoolean("english_mode",false).putBoolean("joined_kalq",false).commit();
        AccessibilityServiceInfo info=getInstrumentation().getUiAutomation().getServiceInfo();info.flags|=AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS;getInstrumentation().getUiAutomation().setServiceInfo(info);
        SettingsActivity activity=getActivity();EditText text=editor(activity.getWindow().getDecorView());assertNotNull(text);
        DictionaryRepository.load(activity).get(60,java.util.concurrent.TimeUnit.SECONDS);
        try(ParcelFileDescriptor fd=getInstrumentation().getUiAutomation().executeShellCommand("ime set app.minime.keyboard/dev.minime.ime.MiniMeService");InputStream in=new ParcelFileDescriptor.AutoCloseInputStream(fd)){while(in.read()!=-1){}}
        InputMethodManager imm=(InputMethodManager)activity.getSystemService(Context.INPUT_METHOD_SERVICE);
        JSONArray results=new JSONArray();
        for(boolean select:new boolean[]{true,false}) {
            getInstrumentation().runOnMainSync(()->{text.setText("");text.requestFocus();text.requestRectangleOnScreen(new Rect(0,0,text.getWidth(),text.getHeight()),true);imm.restartInput(text);imm.showSoftInput(text,InputMethodManager.SHOW_IMPLICIT);});
            getInstrumentation().waitForIdleSync();SystemClock.sleep(500);
            Rect field=new Rect();getInstrumentation().runOnMainSync(()->assertTrue(text.getGlobalVisibleRect(field)));tap(field);SystemClock.sleep(400);
            String badge=null;
            for(InputMode mode:new InputMode[]{InputMode.CHINESE,InputMode.ENGLISH,InputMode.TAIWANESE,InputMode.JAPANESE}) {
                AccessibilityNodeInfo n=node("Choose language mode: "+mode.id);
                if(n!=null){n.recycle();badge="Choose language mode: "+mode.id;break;}
            }
            assertNotNull("Visible idle mode",badge);tap(badge);tap("Choose chinese mode");
            tap("⇧");tap("L");tap("o");tap("n");
            String[] actual={""};getInstrumentation().runOnMainSync(()->actual[0]=text.getText().toString());assertEquals("Lon",actual[0]);
            if(select) {
                tap("Expand candidates");AccessibilityNodeInfo choice=node("Candidate London");assertNotNull("Capitalized source completion is visible",choice);choice.recycle();
                android.graphics.Bitmap screenshot=getInstrumentation().getUiAutomation().takeScreenshot();assertNotNull(screenshot);
                try(FileOutputStream out=new FileOutputStream(new File(activity.getExternalFilesDir(null),"explicit-latin-completion.png"))){assertTrue(screenshot.compress(android.graphics.Bitmap.CompressFormat.PNG,100,out));}finally{screenshot.recycle();}
                tap("Candidate London");
            } else tap("Space");
            String expected=select?"London":"Lon ";long until=SystemClock.uptimeMillis()+3000;int[] composing={0};
            do {
                getInstrumentation().runOnMainSync(()->{actual[0]=text.getText().toString();composing[0]=android.view.inputmethod.BaseInputConnection.getComposingSpanStart(text.getText());});
                if(expected.equals(actual[0]) && composing[0]<0)break;SystemClock.sleep(25);
            }while(SystemClock.uptimeMillis()<until);
            assertEquals(expected,actual[0]);assertEquals(-1,composing[0]);
            results.put(new JSONObject().put("raw","Lon").put("action",select?"tap London":"Space").put("committed",actual[0]).put("composingStart",composing[0]));
        }
        try(Writer out=new OutputStreamWriter(new FileOutputStream(new File(activity.getExternalFilesDir(null),"explicit-latin-completion.json")),StandardCharsets.UTF_8)){out.write(results.toString(2));}
    }
    public void testFourModesOnNonDebuggablePayload()throws Exception {
        Context context=getInstrumentation().getTargetContext();
        assertEquals("Release debuggable flag must be absent",0,context.getApplicationInfo().flags&ApplicationInfo.FLAG_DEBUGGABLE);
        context.getSharedPreferences("settings",0).edit().putBoolean("rime_pinyin",false).putBoolean("addon_poj",true).putBoolean("addon_japanese",true).putBoolean("joined_kalq",false).commit();
        AccessibilityServiceInfo info=getInstrumentation().getUiAutomation().getServiceInfo();info.flags|=AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS;getInstrumentation().getUiAutomation().setServiceInfo(info);
        SettingsActivity activity=getActivity();EditText text=editor(activity.getWindow().getDecorView());assertNotNull(text);
        DictionaryRepository.load(activity).get(60,java.util.concurrent.TimeUnit.SECONDS);AddonRepository.load(activity).get(60,java.util.concurrent.TimeUnit.SECONDS);
        try(ParcelFileDescriptor fd=getInstrumentation().getUiAutomation().executeShellCommand("ime set app.minime.keyboard/dev.minime.ime.MiniMeService");InputStream in=new ParcelFileDescriptor.AutoCloseInputStream(fd)){while(in.read()!=-1){}}
        JSONArray rows=new JSONArray();InputMethodManager imm=(InputMethodManager)activity.getSystemService(Context.INPUT_METHOD_SERVICE);
        try {
            for(InputMode mode:new InputMode[]{InputMode.CHINESE,InputMode.ENGLISH,InputMode.TAIWANESE_ENGLISH,InputMode.JAPANESE_ENGLISH}) {
                String raw=mode==InputMode.CHINESE?"nihao":mode==InputMode.ENGLISH?"tomorr":AddonTestData.probe(activity,mode.taiwanese()?"poj":"japanese")[0];
                getInstrumentation().runOnMainSync(()->{text.setText("");text.requestFocus();text.requestRectangleOnScreen(new Rect(0,0,text.getWidth(),text.getHeight()),true);imm.restartInput(text);imm.showSoftInput(text,InputMethodManager.SHOW_IMPLICIT);});
                getInstrumentation().waitForIdleSync();SystemClock.sleep(300);
                Rect field=new Rect();getInstrumentation().runOnMainSync(()->assertTrue("Try-it field visible",text.getGlobalVisibleRect(field)));
                tap(field);SystemClock.sleep(600);
                // A same-field restart intentionally preserves its active language.
                // Select through the visible control, not a preference write that
                // would mislabel Chinese output as an English-mode observation.
                String badge=null;
                for(InputMode choice:new InputMode[]{InputMode.CHINESE,InputMode.ENGLISH,InputMode.TAIWANESE,InputMode.JAPANESE}) {
                    AccessibilityNodeInfo n=node("Choose language mode: "+choice.id);
                    if(n!=null){n.recycle();badge="Choose language mode: "+choice.id;break;}
                }
                assertNotNull("Visible active mode",badge);tap(badge);tap("Choose "+mode.family().id+" mode");
                AccessibilityNodeInfo active=node("Choose language mode: "+mode.family().id);
                assertNotNull("Requested language is visibly active",active);active.recycle();String prefix="";
                for(char letter:raw.toCharArray()) {tap(String.valueOf(letter));prefix+=letter;getInstrumentation().waitForIdleSync();assertEquals("Each injected key reaches the release editor",prefix,text.getText().toString().toLowerCase(Locale.ROOT));}
                long until=SystemClock.uptimeMillis()+6000;List<String> values;
                do{values=candidates();if(!values.isEmpty())break;SystemClock.sleep(30);}while(SystemClock.uptimeMillis()<until);
                assertFalse("Visible suggestions in "+mode,values.isEmpty());
                if(mode.english())for(String value:values)assertFalse("English candidates exclude Han and kana",value.codePoints().anyMatch(cp->Character.UnicodeScript.of(cp)==Character.UnicodeScript.HAN || Character.UnicodeScript.of(cp)==Character.UnicodeScript.HIRAGANA || Character.UnicodeScript.of(cp)==Character.UnicodeScript.KATAKANA));
                String chosen=values.get(0);tap("Candidate "+chosen);getInstrumentation().waitForIdleSync();
                assertTrue("Explicit candidate reaches editor",text.getText().toString().startsWith(chosen));
                rows.put(new JSONObject().put("mode",mode.id).put("raw",raw).put("candidates",new JSONArray(values)).put("accepted",text.getText().toString()));
            }
        } finally {
            android.graphics.Bitmap shot=getInstrumentation().getUiAutomation().takeScreenshot();
            if(shot!=null)try(FileOutputStream out=new FileOutputStream(new File(activity.getExternalFilesDir(null),"release-payload-smoke.png"))){shot.compress(android.graphics.Bitmap.CompressFormat.PNG,100,out);}finally{shot.recycle();}
            getInstrumentation().runOnMainSync(()->text.setText(""));
            try(FileOutputStream out=new FileOutputStream(new File(activity.getExternalFilesDir(null),"release-payload-smoke.json"))){out.write(rows.toString(2).getBytes(StandardCharsets.UTF_8));}
        }
    }
}
