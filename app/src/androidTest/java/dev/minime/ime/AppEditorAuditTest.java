package dev.minime.ime;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Context;
import android.os.SystemClock;
import android.view.accessibility.*;
import java.util.*;

/** Explicitly selected app audits; excluded from the normal local editor suite. */
@SuppressWarnings("deprecation")
public final class AppEditorAuditTest extends KeyboardInteractionTest {
    private AccessibilityNodeInfo webEditor(AccessibilityNodeInfo n) {
        if(n==null)return null;
        if(n.isEditable() && n.isVisibleToUser() && "com.android.chrome".contentEquals(n.getPackageName()))return n;
        for(int i=0;i<n.getChildCount();i++){AccessibilityNodeInfo found=webEditor(n.getChild(i));if(found!=null){n.recycle();return found;}}
        n.recycle();return null;
    }
    private AccessibilityNodeInfo externalField(String id) {
        long end=SystemClock.uptimeMillis()+6000;
        do {
            for(AccessibilityWindowInfo w:getInstrumentation().getUiAutomation().getWindows()) {
                AccessibilityNodeInfo root=w.getRoot();if(root==null)continue;
                if(id.equals("chrome-web")){AccessibilityNodeInfo found=webEditor(root);if(found!=null)return found;continue;}
                List<AccessibilityNodeInfo> nodes=root.findAccessibilityNodeInfosByViewId(id);root.recycle();
                if(!nodes.isEmpty())return nodes.get(0);
            }
            SystemClock.sleep(50);
        }while(SystemClock.uptimeMillis()<end);
        throw new AssertionError("Missing external editor: "+id);
    }
    private void clearExternal(String id) {
        AccessibilityNodeInfo n=externalField(id);android.os.Bundle args=new android.os.Bundle();
        args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,"");
        assertTrue(n.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT,args));
        if(id.equals("chrome-web")) {
            android.graphics.Rect r=new android.graphics.Rect();n.getBoundsInScreen(r);n.recycle();
            long at=SystemClock.uptimeMillis();
            for(int action:new int[]{android.view.MotionEvent.ACTION_DOWN,android.view.MotionEvent.ACTION_UP}) {
                android.view.MotionEvent e=android.view.MotionEvent.obtain(at,SystemClock.uptimeMillis(),action,r.exactCenterX(),r.exactCenterY(),0);
                e.setSource(android.view.InputDevice.SOURCE_TOUCHSCREEN);assertTrue(getInstrumentation().getUiAutomation().injectInputEvent(e,true));e.recycle();SystemClock.sleep(35);
            }
        } else {
            // ACTION_FOCUS may return false when the editor already has focus.
            // Verify delivery through the editor's actual text, not that return value.
            n.performAction(AccessibilityNodeInfo.ACTION_FOCUS);n.performAction(AccessibilityNodeInfo.ACTION_CLICK);n.recycle();
        }
        node("Space").recycle();SystemClock.sleep(350);
    }
    private void externalMode(dev.minime.core.InputMode mode) {
        boolean opened=false;
        for(String id:new String[]{"chinese","english","taiwanese","japanese"}) {
            for(AccessibilityWindowInfo w:getInstrumentation().getUiAutomation().getWindows()) {
                AccessibilityNodeInfo n=find(w.getRoot(),"Choose language mode: "+id);
                if(n!=null){assertTrue(n.performAction(AccessibilityNodeInfo.ACTION_CLICK));n.recycle();opened=true;break;}
            }
            if(opened)break;
        }
        assertTrue("Idle mode chooser visible",opened);click("Choose "+mode.id+" mode");
        node("Choose language mode: "+mode.id).recycle();
    }
    private void externalType(String id,String raw) {
        String expected="";
        for(char c:raw.toCharArray()) {
            String key=c==' '?"Space":String.valueOf(c);boolean lower=false;
            for(AccessibilityWindowInfo w:getInstrumentation().getUiAutomation().getWindows()) {
                AccessibilityNodeInfo n=find(w.getRoot(),key);if(n!=null){lower=true;n.recycle();break;}
            }
            click(lower?key:key.toUpperCase(Locale.ROOT));expected+=c;
            String actual="";long end=SystemClock.uptimeMillis()+3000;
            do {AccessibilityNodeInfo n=externalField(id);actual=String.valueOf(n.getText());n.recycle();if(expected.equalsIgnoreCase(actual))break;SystemClock.sleep(25);}while(SystemClock.uptimeMillis()<end);
            assertEquals("Typed external field",expected.toLowerCase(Locale.ROOT),actual.toLowerCase(Locale.ROOT));
        }
    }
    /** Runs only when explicitly selected: operates a new, empty Keep note. */
    public void testKeepSuggestionAudit()throws Exception {
        AccessibilityServiceInfo service=getInstrumentation().getUiAutomation().getServiceInfo();
        service.flags|=AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS;getInstrumentation().getUiAutomation().setServiceInfo(service);
        activity.getSharedPreferences("settings",Context.MODE_PRIVATE).edit().putBoolean("addon_poj",true).putBoolean("addon_japanese",true).commit();
        AddonRepository.load(activity).get(60,java.util.concurrent.TimeUnit.SECONDS);
        activity.startActivity(new android.content.Intent("android.intent.action.CREATE_NOTE").setPackage("com.google.android.keep"));
        String title="com.google.android.keep:id/editable_title",body="com.google.android.keep:id/edit_note_text";
        org.json.JSONArray rows=new org.json.JSONArray();
        try {
            for(String field:new String[]{title,body})for(dev.minime.core.InputMode mode:new dev.minime.core.InputMode[]{dev.minime.core.InputMode.ENGLISH,dev.minime.core.InputMode.CHINESE,dev.minime.core.InputMode.TAIWANESE,dev.minime.core.InputMode.JAPANESE}) {
                String nativeQuery=mode.english()?"tomorr":mode==dev.minime.core.InputMode.CHINESE?"nihao":mode.taiwanese()?AddonTestData.probe(activity,"poj","everyday_")[0]:"konnichi";
                for(String raw:mode.english()?new String[]{"pronun",nativeQuery,"thank "}:new String[]{"pronun",nativeQuery}) {
                    clearExternal(field);externalMode(mode);externalType(field,raw);
                    rows.put(new org.json.JSONObject().put("field",field).put("mode",mode.id).put("raw",raw).put("candidates",new org.json.JSONArray(auditCandidates())));
                }
                clearExternal(field);
            }
        } finally {
            try {clearExternal(title);clearExternal(body);} finally {
                getInstrumentation().getUiAutomation().performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK);
                SystemClock.sleep(300);
                getInstrumentation().getUiAutomation().performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK);
                try(java.io.FileOutputStream out=new java.io.FileOutputStream(new java.io.File(activity.getExternalFilesDir(null),"keep-suggestion-audit.json"))){out.write(rows.toString(2).getBytes(java.nio.charset.StandardCharsets.UTF_8));}
            }
        }
    }
    /** Actual Chrome, serving a disposable editor on device loopback only. */
    public void testChromeSuggestionAudit()throws Exception {
        java.net.ServerSocket server=new java.net.ServerSocket(0,4,java.net.InetAddress.getByName("127.0.0.1"));
        Thread serving=new Thread(()-> {
            while(!server.isClosed())try(java.net.Socket client=server.accept()) {
                client.setSoTimeout(3000);
                java.io.BufferedReader in=new java.io.BufferedReader(new java.io.InputStreamReader(client.getInputStream(),java.nio.charset.StandardCharsets.US_ASCII));
                for(int lines=0;lines<50;lines++){String line=in.readLine();if(line==null || line.isEmpty())break;}
                byte[] page=("<!doctype html><meta name='viewport' content='width=device-width,initial-scale=1'><title>MinIME local editor audit</title><h2>MinIME local editor audit</h2><textarea aria-label='MinIME audit editor' style='width:95%;height:200px'></textarea>").getBytes(java.nio.charset.StandardCharsets.UTF_8);
                java.io.OutputStream out=client.getOutputStream();out.write(("HTTP/1.1 200 OK\r\nContent-Type: text/html; charset=utf-8\r\nContent-Length: "+page.length+"\r\nConnection: close\r\n\r\n").getBytes(java.nio.charset.StandardCharsets.US_ASCII));out.write(page);out.flush();
            }catch(java.io.IOException ignored){if(server.isClosed())return;}
        },"local-editor-audit");serving.start();
        org.json.JSONArray rows=new org.json.JSONArray();
        try {
            activity.getSharedPreferences("settings",Context.MODE_PRIVATE).edit().putBoolean("addon_poj",true).putBoolean("addon_japanese",true).commit();
            AddonRepository.load(activity).get(60,java.util.concurrent.TimeUnit.SECONDS);
            android.content.Intent intent=new android.content.Intent(android.content.Intent.ACTION_VIEW,android.net.Uri.parse("http://127.0.0.1:"+server.getLocalPort()+"/")).setPackage("com.android.chrome");
            activity.startActivity(intent);SystemClock.sleep(1500);
            for(dev.minime.core.InputMode mode:new dev.minime.core.InputMode[]{dev.minime.core.InputMode.ENGLISH,dev.minime.core.InputMode.CHINESE,dev.minime.core.InputMode.TAIWANESE,dev.minime.core.InputMode.JAPANESE}) {
                String nativeQuery=mode.english()?"tomorr":mode==dev.minime.core.InputMode.CHINESE?"nihao":mode.taiwanese()?AddonTestData.probe(activity,"poj","everyday_")[0]:"konnichi";
                for(String raw:mode.english()?new String[]{"pronun",nativeQuery,"thank "}:new String[]{"pronun",nativeQuery}) {
                    clearExternal("chrome-web");externalMode(mode);externalType("chrome-web",raw);
                    rows.put(new org.json.JSONObject().put("field","Chrome textarea").put("mode",mode.id).put("raw",raw).put("candidates",new org.json.JSONArray(auditCandidates())));
                }
            }
            clearExternal("chrome-web");
        } finally {
            try {clearExternal("chrome-web");} finally {
                getInstrumentation().getUiAutomation().performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK);
                SystemClock.sleep(300);getInstrumentation().getUiAutomation().performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK);
                server.close();serving.join(4000);
                try(java.io.FileOutputStream out=new java.io.FileOutputStream(new java.io.File(activity.getExternalFilesDir(null),"chrome-suggestion-audit.json"))){out.write(rows.toString(2).getBytes(java.nio.charset.StandardCharsets.UTF_8));}
            }
        }
    }
}
