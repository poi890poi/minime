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
import java.util.*;
import org.json.*;

/** Authored store examples, never language-quality benchmarks or training inputs. */
@SuppressWarnings("deprecation")
public final class StoreCaptureTest extends ActivityInstrumentationTestCase2<EditorTestActivity> {
    public StoreCaptureTest(){super(EditorTestActivity.class);}
    private EditorTestActivity host;
    private final JSONArray capturedCandidates=new JSONArray();
    private String currentMode,currentNote,typedInput="";
    private boolean expanded;
    private String completedInteractions="";
    private void collectCandidates(AccessibilityNodeInfo node,Set<String> labels,Set<String> exactInputs) {
        if(node==null)return;
        CharSequence description=node.getContentDescription();
        // Inspect actionable word leaves, not the scroll container named
        // "Candidate list". Exact literal input has its own accessibility role.
        if(node.isVisibleToUser() && node.isClickable() && node.getChildCount()==0 && description!=null) {
            String label=description.toString();
            if(label.startsWith("Candidate "))labels.add(label.substring("Candidate ".length()));
            if(label.startsWith("Exact input "))exactInputs.add(label.substring("Exact input ".length()));
        }
        for(int i=0;i<node.getChildCount();i++)collectCandidates(node.getChild(i),labels,exactInputs);
        node.recycle();
    }
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
        press(label,45);
    }
    private void press(String label,long holdMs)throws Exception {
        ownIme();
        AccessibilityNodeInfo n=key(label);if(n==null)n=key(label.toUpperCase(java.util.Locale.ROOT));assertNotNull("Visible IME key "+label,n);
        Rect r=new Rect();n.getBoundsInScreen(r);n.recycle();long t=SystemClock.uptimeMillis();
        for(int action:new int[]{MotionEvent.ACTION_DOWN,MotionEvent.ACTION_UP}){
            MotionEvent e=MotionEvent.obtain(t,SystemClock.uptimeMillis(),action,r.exactCenterX(),r.exactCenterY(),0);e.setSource(InputDevice.SOURCE_TOUCHSCREEN);
            assertTrue(getInstrumentation().getUiAutomation().injectInputEvent(e,true));e.recycle();SystemClock.sleep(action==MotionEvent.ACTION_DOWN?holdMs:45);
        }
    }
    private void show(String mode,String note)throws Exception {
        currentMode=mode;currentNote=note;typedInput="";expanded=false;completedInteractions="";
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
            TextView subtitle=new TextView(host);subtitle.setText("多語日常 · 範例文字");subtitle.setTextSize(13);subtitle.setTextColor(Color.rgb(90,111,108));body.addView(subtitle);
            host.text=new EditText(host);host.text.setTextSize(20);host.text.setGravity(Gravity.TOP);host.text.setBackgroundColor(Color.TRANSPARENT);host.text.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_MULTI_LINE);host.text.setText(note);host.text.setSelection(note.length());
            body.addView(host.text,new LinearLayout.LayoutParams(-1,0,1));host.setContentView(body);host.text.requestFocus();
            host.text.post(()->((InputMethodManager)host.getSystemService(Context.INPUT_METHOD_SERVICE)).showSoftInput(host.text,InputMethodManager.SHOW_IMPLICIT));
        });
        long focusUntil=SystemClock.uptimeMillis()+5000;
        while(!host.hasWindowFocus()&&SystemClock.uptimeMillis()<focusUntil)SystemClock.sleep(50);
        assertTrue("Capture editor owns window focus",host.hasWindowFocus());
        getInstrumentation().waitForIdleSync();
        // The replacement editor must be attached and served before requesting IME visibility.
        getInstrumentation().runOnMainSync(()->((InputMethodManager)host.getSystemService(Context.INPUT_METHOD_SERVICE)).restartInput(host.text));
        int[] position=new int[2];getInstrumentation().runOnMainSync(()->host.text.getLocationOnScreen(position));
        long tap=SystemClock.uptimeMillis();
        for(int action:new int[]{MotionEvent.ACTION_DOWN,MotionEvent.ACTION_UP}) {
            MotionEvent e=MotionEvent.obtain(tap,SystemClock.uptimeMillis(),action,position[0]+40,position[1]+40,0);e.setSource(InputDevice.SOURCE_TOUCHSCREEN);
            assertTrue(getInstrumentation().getUiAutomation().injectInputEvent(e,true));e.recycle();
        }
        getInstrumentation().runOnMainSync(()->host.text.setSelection(host.text.length()));
        // Samsung may restore its IME while instrumentation's target starts.
        // Select after the replacement editor has completed its first interaction.
        try(android.os.ParcelFileDescriptor fd=getInstrumentation().getUiAutomation().executeShellCommand("ime set app.minime.keyboard/dev.minime.ime.MiniMeService");InputStream in=new android.os.ParcelFileDescriptor.AutoCloseInputStream(fd)){while(in.read()!=-1){}}
        getInstrumentation().runOnMainSync(()->((InputMethodManager)host.getSystemService(Context.INPUT_METHOD_SERVICE)).showSoftInput(host.text,InputMethodManager.SHOW_IMPLICIT));
        long until=SystemClock.uptimeMillis()+8000;
        while(key("q")==null&&key("Q")==null&&SystemClock.uptimeMillis()<until)SystemClock.sleep(100);
        SystemClock.sleep(1500);
        capture("diagnostic");
    }
    private void capture(String name)throws Exception {
        ownIme();
        getInstrumentation().waitForIdleSync();SystemClock.sleep(1200);
        Set<String> labels=new LinkedHashSet<>(),exactInputs=new LinkedHashSet<>();
        for(AccessibilityWindowInfo w:getInstrumentation().getUiAutomation().getWindows())if(w.getType()==AccessibilityWindowInfo.TYPE_INPUT_METHOD)collectCandidates(w.getRoot(),labels,exactInputs);
        if(!name.equals("diagnostic")) {
            capturedCandidates.put(new JSONObject().put("screenshot",name).put("mode",currentMode).put("prefilledExampleContext",currentNote).put("typedInput",typedInput).put("completedInteractions",completedInteractions).put("expanded",expanded).put("visibleCandidateLabels",new JSONArray(labels)).put("visibleExactInputs",new JSONArray(exactInputs)));
            try(OutputStream out=new FileOutputStream(new File(host.getExternalFilesDir(null),"store-candidates.json"))) {
                out.write(capturedCandidates.toString(2).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            }
            assertTrue("Store screenshot needs a visible word choice",!labels.isEmpty() || !exactInputs.isEmpty());
            if(name.equals("05-geography")) {
                Set<String> attested=new HashSet<>();
                for(dev.minime.core.Candidate c:AddonRepository.geography(host).get(60,java.util.concurrent.TimeUnit.SECONDS).lookup("jianianduan",Collections.singleton("geography")))attested.add(c.text);
                assertTrue("Same geography input must visibly include the requested place",labels.contains("加年端社"));
                assertTrue("Every visible geography candidate must be backed by the matching source entries",attested.containsAll(labels));
            }
        }
        Bitmap b=getInstrumentation().getUiAutomation().takeScreenshot();assertNotNull(b);b.setHasAlpha(false);
        try(FileOutputStream out=new FileOutputStream(new File(host.getExternalFilesDir(null),"store-"+name+".png"))){assertTrue(b.compress(Bitmap.CompressFormat.PNG,100,out));}finally{b.recycle();}
    }
    private void example(String name,String mode,String note,String input,boolean expand)throws Exception {
        show(mode,note);typedInput=input;
        for(char c:input.toCharArray())press(String.valueOf(c));
        if(expand){SystemClock.sleep(1200);press("Expand candidates");expanded=true;}
        capture(name);
    }
    public void testCaptureStoreExamples()throws Exception {
        host=getActivity();DictionaryRepository.load(host).get(60,java.util.concurrent.TimeUnit.SECONDS);AccessibilityServiceInfo info=getInstrumentation().getUiAutomation().getServiceInfo();info.flags|=AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS;getInstrumentation().getUiAutomation().setServiceInfo(info);
        example("01-chinese","chinese","週末一起去走走？\n好啊！先喝咖啡，再散步。\nSee you tomorrow!\n時間、地點，一次說清楚。\n","mingtian",false);
        example("02-english","english","Thanks for your help!\nI can't wait to see you.\nLet's meet after work.\nHave a wonderful weekend.\n","thank",false);
        example("03-taiwanese","taiwanese_english","lí hó　你好\nto-siā　多謝\nchài-hōe　再會\nchia̍h-pá--bōe　食飽未\n","liho",false);
        example("04-japanese","japanese_english","おはようございます。\nこんにちは。Hello!\nありがとうございます。\nまた明日。See you tomorrow!\n","arigatou",false);
        example("05-geography","chinese","山林裡，也有值得記住的名字。\n加年端社・加年端部落舊址\n八通關古道・八通關駐在所\nTaiwan trails & history\n","jianianduan",false);
        example("06-trails","chinese","下次想認識的山林地名\n八通關古道・八通關山\n八通關駐在所・八通關山西峰\n舊路線，也有新的發現。\n","batongguan",true);
        show("taiwanese_english","同一個候選，兩種輸出。\n點選白話字，長按輸出漢字。\n");
        for(char c:"liho".toCharArray())press(String.valueOf(c));SystemClock.sleep(1200);press("Candidate lí hó");press("↵");
        for(char c:"liho".toCharArray())press(String.valueOf(c));SystemClock.sleep(1200);press("Candidate lí hó",ViewConfiguration.getLongPressTimeout()+120);press("↵");
        getInstrumentation().runOnMainSync(()->assertEquals("Both forms are actual keyboard output",currentNote+"lí hó\n你好\n",host.text.getText().toString()));
        completedInteractions="Typed liho, tapped lí hó, Enter; typed liho, held lí hó to insert 你好, Enter. Both outputs asserted against the actual editor.";
        typedInput="tosia";for(char c:typedInput.toCharArray())press(String.valueOf(c));capture("07-taiwanese-choices");
        example("08-japanese-choices","japanese_english","気持ちを、ことばに。\nきもち・キモチ・気持ち\n日常のひとことを日本語で。\nA few words, every day.\n","kimochi",true);
    }
}
