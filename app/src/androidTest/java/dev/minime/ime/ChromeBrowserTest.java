package dev.minime.ime;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Intent;
import android.net.Uri;
import android.os.*;
import android.test.ActivityInstrumentationTestCase2;
import android.view.accessibility.*;
import java.io.InputStream;

/** Explicit Chrome-only smoke test; excluded from the generic device test list. */
@SuppressWarnings("deprecation")
public final class ChromeBrowserTest extends ActivityInstrumentationTestCase2<EditorTestActivity> {
    public ChromeBrowserTest() {super(EditorTestActivity.class);}
    private void shell(String command) throws Exception {
        try(ParcelFileDescriptor fd=getInstrumentation().getUiAutomation().executeShellCommand(command);
                InputStream in=new ParcelFileDescriptor.AutoCloseInputStream(fd)) {while(in.read()!=-1) {}}
    }
    private AccessibilityNodeInfo find(AccessibilityNodeInfo root,String value,boolean id) {
        if(root==null)return null;
        if(root.isVisibleToUser() && value.equals(id?root.getViewIdResourceName():String.valueOf(root.getContentDescription())))return root;
        for(int i=0;i<root.getChildCount();i++) {
            AccessibilityNodeInfo child=find(root.getChild(i),value,id);
            if(child!=null) {root.recycle();return child;}
        }
        root.recycle();return null;
    }
    private AccessibilityNodeInfo node(String value,boolean id) {
        long deadline=SystemClock.uptimeMillis()+7000;
        do {
            for(AccessibilityWindowInfo window:getInstrumentation().getUiAutomation().getWindows()) {
                if(!id && window.getType()!=AccessibilityWindowInfo.TYPE_INPUT_METHOD)continue;
                AccessibilityNodeInfo found=find(window.getRoot(),value,id);if(found!=null)return found;
            }
            SystemClock.sleep(50);
        }while(SystemClock.uptimeMillis()<deadline);
        android.graphics.Bitmap bitmap=getInstrumentation().getUiAutomation().takeScreenshot();
        try(java.io.FileOutputStream out=new java.io.FileOutputStream(new java.io.File(getActivity().getExternalFilesDir(null),"chrome-control-failure.png"))) {
            bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,out);
        }catch(java.io.IOException e){throw new AssertionError(e);}finally{bitmap.recycle();}
        throw new AssertionError("Visible Chrome test control missing: "+value);
    }
    private void tap(String value,boolean id) {
        AccessibilityNodeInfo n=node(value,id);
        try {assertTrue("Activate visible control: "+value,n.performAction(AccessibilityNodeInfo.ACTION_CLICK));}finally{n.recycle();}
        SystemClock.sleep(200);
    }
    public void testOmniboxChineseConversion() throws Exception {
        EditorTestActivity activity=getActivity();
        AccessibilityServiceInfo service=getInstrumentation().getUiAutomation().getServiceInfo();
        service.flags|=AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS|AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS;
        getInstrumentation().getUiAutomation().setServiceInfo(service);
        Intent chrome=new Intent(Intent.ACTION_VIEW,Uri.parse("about:blank")).setPackage("com.android.chrome").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            shell("ime set dev.minime.ime/.MiniMeService");
            activity.startActivity(chrome);
            node("com.android.chrome:id/url_bar",true).recycle();
            getInstrumentation().getUiAutomation().waitForIdle(700,5000);
            tap("com.android.chrome:id/url_bar",true);
            assertEquals("dev.minime.ime/.MiniMeService",android.provider.Settings.Secure.getString(activity.getContentResolver(),android.provider.Settings.Secure.DEFAULT_INPUT_METHOD));
            tap("Switch to Chinese",false);node("Switch to English",false).recycle();
            for(char letter:"nihao".toCharArray())tap(String.valueOf(letter),false);
            tap("Space",false);
            long deadline=SystemClock.uptimeMillis()+5000;String text;
            do {
                AccessibilityNodeInfo address=node("com.android.chrome:id/url_bar",true);
                text=String.valueOf(address.getText());address.recycle();
                if(text.equals("你好"))break;SystemClock.sleep(50);
            }while(SystemClock.uptimeMillis()<deadline);
            assertEquals("Chinese committed in Chrome without submitting a search","你好",text);
        } finally {
            shell("input keyevent KEYCODE_BACK");shell("input keyevent KEYCODE_BACK");
        }
    }
}
