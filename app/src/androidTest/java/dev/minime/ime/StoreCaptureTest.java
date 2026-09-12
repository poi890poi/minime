package dev.minime.ime;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.*;
import android.graphics.*;
import android.os.SystemClock;
import android.test.ActivityInstrumentationTestCase2;
import android.text.InputType;
import android.view.*;
import android.view.accessibility.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import java.io.*;

/** Authored store examples, never language-quality benchmarks or training inputs. */
@SuppressWarnings("deprecation")
public final class StoreCaptureTest extends ActivityInstrumentationTestCase2<EditorTestActivity> {
    public StoreCaptureTest(){super(EditorTestActivity.class);}
    private EditorTestActivity host;
    private AccessibilityNodeInfo find(AccessibilityNodeInfo node,String label) {
        if(node==null)return null;
        AccessibilityNodeInfo found=null;
        if(node.isVisibleToUser()&&(label.contentEquals(node.getContentDescription()==null?"":node.getContentDescription())||label.contentEquals(node.getText()==null?"":node.getText())))found=AccessibilityNodeInfo.obtain(node);
        for(int i=0;i<node.getChildCount();i++){
            AccessibilityNodeInfo child=find(node.getChild(i),label);
            if(child!=null){Rect a=new Rect(),b=new Rect();child.getBoundsInScreen(a);if(found!=null)found.getBoundsInScreen(b);
                if(found==null||a.centerY()>b.centerY()){if(found!=null)found.recycle();found=child;}else child.recycle();}
        }node.recycle();return found;
    }
    private AccessibilityNodeInfo key(String label) {
        for(AccessibilityWindowInfo w:getInstrumentation().getUiAutomation().getWindows())if(w.getType()==AccessibilityWindowInfo.TYPE_INPUT_METHOD){AccessibilityNodeInfo n=find(w.getRoot(),label);if(n!=null)return n;}return null;
    }
    private void ownIme() {assertEquals("Only MinIME may appear in store captures","app.minime.keyboard/dev.minime.ime.MiniMeService",android.provider.Settings.Secure.getString(host.getContentResolver(),android.provider.Settings.Secure.DEFAULT_INPUT_METHOD));}
    private void press(String label)throws Exception {
        ownIme();
        AccessibilityNodeInfo n=key(label);if(n==null)n=key(label.toUpperCase(java.util.Locale.ROOT));assertNotNull("Visible IME key "+label,n);
        Rect r=new Rect();n.getBoundsInScreen(r);n.recycle();long t=SystemClock.uptimeMillis();
        for(int action:new int[]{MotionEvent.ACTION_DOWN,MotionEvent.ACTION_UP}){
            MotionEvent e=MotionEvent.obtain(t,SystemClock.uptimeMillis(),action,r.exactCenterX(),r.exactCenterY(),0);e.setSource(InputDevice.SOURCE_TOUCHSCREEN);
            assertTrue(getInstrumentation().getUiAutomation().injectInputEvent(e,true));e.recycle();SystemClock.sleep(45);
        }
    }
    private void show(String mode,String note)throws Exception {
        try(android.os.ParcelFileDescriptor fd=getInstrumentation().getUiAutomation().executeShellCommand("ime set app.minime.keyboard/dev.minime.ime.MiniMeService");InputStream in=new android.os.ParcelFileDescriptor.AutoCloseInputStream(fd)){while(in.read()!=-1){}}
        ownIme();
        host.getSharedPreferences("settings",0).edit().clear().putBoolean("addon_poj",true).putBoolean("addon_japanese",true).putBoolean("addon_taiwan",true).putBoolean("addon_geography",true).putString("mixed_mode",mode).putBoolean("english_mode",mode.equals("english")).commit();
        host.getSharedPreferences("learning",0).edit().clear().commit();
        getInstrumentation().runOnMainSync(()->{
            host.getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            host.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
            LinearLayout body=new LinearLayout(host);body.setOrientation(LinearLayout.VERTICAL);body.setBackgroundColor(Color.rgb(249,248,242));
            int p=Math.round(24*host.getResources().getDisplayMetrics().density);body.setPadding(p,p,p,p);
            TextView title=new TextView(host);title.setText("筆記  Notes");title.setTextSize(26);title.setTextColor(Color.rgb(0,103,101));body.addView(title);
            host.text=new EditText(host);host.text.setTextSize(22);host.text.setGravity(Gravity.TOP);host.text.setBackgroundColor(Color.TRANSPARENT);host.text.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_MULTI_LINE);host.text.setText(note);host.text.setSelection(note.length());
            body.addView(host.text,new LinearLayout.LayoutParams(-1,0,1));host.setContentView(body);host.text.requestFocus();
            host.text.post(()->((InputMethodManager)host.getSystemService(Context.INPUT_METHOD_SERVICE)).showSoftInput(host.text,InputMethodManager.SHOW_IMPLICIT));
        });
        long focusUntil=SystemClock.uptimeMillis()+5000;
        while(!host.hasWindowFocus()&&SystemClock.uptimeMillis()<focusUntil)SystemClock.sleep(50);
        assertTrue("Capture editor owns window focus",host.hasWindowFocus());
        getInstrumentation().runOnMainSync(()->((InputMethodManager)host.getSystemService(Context.INPUT_METHOD_SERVICE)).showSoftInput(host.text,InputMethodManager.SHOW_IMPLICIT));
        long until=SystemClock.uptimeMillis()+8000;
        while(key("q")==null&&key("Q")==null&&SystemClock.uptimeMillis()<until)SystemClock.sleep(100);
        SystemClock.sleep(1500);
        capture("diagnostic");
    }
    private void capture(String name)throws Exception {
        ownIme();
        getInstrumentation().waitForIdleSync();SystemClock.sleep(1200);
        Bitmap b=getInstrumentation().getUiAutomation().takeScreenshot();assertNotNull(b);b.setHasAlpha(false);
        try(FileOutputStream out=new FileOutputStream(new File(host.getExternalFilesDir(null),"store-"+name+".png"))){assertTrue(b.compress(Bitmap.CompressFormat.PNG,100,out));}finally{b.recycle();}
    }
    public void testCaptureStoreExamples()throws Exception {
        host=getActivity();AccessibilityServiceInfo info=getInstrumentation().getUiAutomation().getServiceInfo();info.flags|=AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS;getInstrumentation().getUiAutomation().setServiceInfo(info);
        show("chinese","週末想去散步。\n");for(char c:"mingtian".toCharArray())press(String.valueOf(c));capture("01-chinese");
        show("english","A little note for tomorrow.\n");for(char c:"hello".toCharArray())press(String.valueOf(c));capture("02-english");
        show("taiwanese_english","台語白話字\n");for(char c:"liho".toCharArray())press(String.valueOf(c));capture("03-taiwanese");
        show("japanese_english","日本語\n");for(char c:"arigatou".toCharArray())press(String.valueOf(c));capture("04-japanese");
    }
}
