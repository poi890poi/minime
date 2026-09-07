package dev.minime.ime;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Context;
import android.provider.Settings;
import android.test.ActivityInstrumentationTestCase2;
import android.view.accessibility.*;
import android.view.inputmethod.InputMethodManager;
import android.os.SystemClock;
import android.graphics.Rect;
import android.view.MotionEvent;
import android.view.InputDevice;
import android.content.SharedPreferences;
import java.util.*;

/** Clicks the real visible IME buttons; no broadcasts or test-only input hooks. */
@SuppressWarnings("deprecation")
public final class KeyboardInteractionTest extends ActivityInstrumentationTestCase2<EditorTestActivity> {
    public KeyboardInteractionTest() { super(EditorTestActivity.class); }
    private EditorTestActivity activity;
    private boolean zhuyin;
    private final Map<String,Map<String,?>> saved=new HashMap<>();
    @Override protected void setUp() throws Exception {
        super.setUp();
        for(String name:new String[]{"settings","learning"}) {
            SharedPreferences prefs=getInstrumentation().getTargetContext().getSharedPreferences(name,Context.MODE_PRIVATE);
            saved.put(name,new HashMap<>(prefs.getAll())); prefs.edit().clear().commit();
        }
        // Legacy behavior cases keep their established decoder; defaults have a dedicated test.
        getInstrumentation().getTargetContext().getSharedPreferences("settings",Context.MODE_PRIVATE).edit().putBoolean("rime_pinyin",false).commit();
        zhuyin=false;
        activity=getActivity();
        DictionaryRepository.load(activity).get(30,java.util.concurrent.TimeUnit.SECONDS);
        // Samsung can restore the previous IME while instrumentation restarts its target.
        // Select only after the instrumented activity exists.
        try(android.os.ParcelFileDescriptor command=getInstrumentation().getUiAutomation().executeShellCommand("ime set dev.minime.ime/.MiniMeService");
            java.io.InputStream output=new android.os.ParcelFileDescriptor.AutoCloseInputStream(command)) {
            while(output.read()!=-1) { }
        }
        assertEquals("MinIME selected after instrumentation restart", "dev.minime.ime/.MiniMeService",
            Settings.Secure.getString(activity.getContentResolver(),Settings.Secure.DEFAULT_INPUT_METHOD));
        getInstrumentation().runOnMainSync(()-> {
            activity.text.requestFocus();
            ((InputMethodManager)activity.getSystemService(Context.INPUT_METHOD_SERVICE)).showSoftInput(activity.text,InputMethodManager.SHOW_IMPLICIT);
        });
        AccessibilityServiceInfo service=getInstrumentation().getUiAutomation().getServiceInfo();
        service.flags |= AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS;
        getInstrumentation().getUiAutomation().setServiceInfo(service);
        try {node("Space").recycle();}
        catch(AssertionError failure) {capture("review-setup-failure");throw failure;}
    }
    @Override protected void tearDown() throws Exception {
        try {
            for(Map.Entry<String,Map<String,?>> file:saved.entrySet()) {
                SharedPreferences.Editor edit=getInstrumentation().getTargetContext().getSharedPreferences(file.getKey(),Context.MODE_PRIVATE).edit().clear();
                for(Map.Entry<String,?> e:file.getValue().entrySet()) {
                    Object v=e.getValue();
                    if(v instanceof String) edit.putString(e.getKey(),(String)v);
                    else if(v instanceof Integer) edit.putInt(e.getKey(),(Integer)v);
                    else if(v instanceof Boolean) edit.putBoolean(e.getKey(),(Boolean)v);
                    else if(v instanceof Long) edit.putLong(e.getKey(),(Long)v);
                    else if(v instanceof Float) edit.putFloat(e.getKey(),(Float)v);
                }
                edit.commit();
            }
        } finally { super.tearDown(); }
    }
    private AccessibilityNodeInfo find(AccessibilityNodeInfo n,String description) {
        if(n==null) return null;
        if(n.isVisibleToUser() && description.contentEquals(n.getContentDescription()==null?"":n.getContentDescription())) return n;
        for(int i=0;i<n.getChildCount();i++) {
            AccessibilityNodeInfo result=find(n.getChild(i),description);
            if(result!=null) return result;
        }
        n.recycle(); return null;
    }
    private AccessibilityNodeInfo node(String description) {
        long until=SystemClock.uptimeMillis()+6000;
        do {
            for(AccessibilityWindowInfo window:getInstrumentation().getUiAutomation().getWindows()) {
                AccessibilityNodeInfo match=find(window.getRoot(),description);
                if(match!=null) return match;
            }
            SystemClock.sleep(50);
        } while(SystemClock.uptimeMillis()<until);
        capture("review-control-failure");throw new AssertionError("Visible control missing: "+description);
    }
    private void click(String description) {
        if(description.equals("Emoji")) {
            AccessibilityNodeInfo comma=null;
            for(AccessibilityWindowInfo window:getInstrumentation().getUiAutomation().getWindows()) {
                comma=find(window.getRoot(),"，");if(comma==null)comma=find(window.getRoot(),",");if(comma!=null)break;
            }
            assertNotNull("Comma exposes emoji by hold",comma);Rect r=new Rect();comma.getBoundsInScreen(r);comma.recycle();
            long start=SystemClock.uptimeMillis();event(start,MotionEvent.ACTION_DOWN,r.exactCenterX(),r.exactCenterY());SystemClock.sleep(650);
            event(start,MotionEvent.ACTION_UP,r.exactCenterX(),r.exactCenterY());getInstrumentation().waitForIdleSync();return;
        }
        boolean clicked=false;
        for(int attempt=0;attempt<3 && !clicked;attempt++) {
            AccessibilityNodeInfo n=node(description);clicked=n.performAction(AccessibilityNodeInfo.ACTION_CLICK);n.recycle();
            if(!clicked)SystemClock.sleep(50); // An expansion can replace an accessibility snapshot in flight.
        }
        assertTrue("Click visible "+description,clicked);getInstrumentation().waitForIdleSync();
        if(description.equals("Space") && !zhuyin) {
            long until=SystemClock.uptimeMillis()+3000;
            java.util.concurrent.atomic.AtomicBoolean composing=new java.util.concurrent.atomic.AtomicBoolean();
            do {
                getInstrumentation().runOnMainSync(()-> {
                    android.view.View focused=activity.getCurrentFocus();
                    composing.set(focused instanceof android.widget.EditText && android.view.inputmethod.BaseInputConnection.getComposingSpanEnd(((android.widget.EditText)focused).getText())>=0);
                });
                if(!composing.get())break;SystemClock.sleep(15);
            }while(SystemClock.uptimeMillis()<until);
            assertFalse("Space completes its asynchronous composition",composing.get());
        }
        if(description.endsWith(" layout") || description.startsWith("Switch to ") || description.equals("Emoji") || description.equals("Symbols") || description.equals("ABC") || description.endsWith("category") || description.equals("Emoji group")) {
            try { getInstrumentation().getUiAutomation().waitForIdle(300,3000); }
            catch(java.util.concurrent.TimeoutException e) { throw new AssertionError("Layout did not settle",e); }
        }
    }
    private void type(String text) {
        String latin="qwertyuiopasdfghjklzxcvbnm", bpmf="ㄆㄊㄍㄐㄔㄗㄧㄛㄟㄣㄇㄋㄎㄑㄕㄘㄨㄜㄠㄈㄌㄏㄒㄖㄙㄩ";
        text.codePoints().forEach(c->{
            if(zhuyin && latin.indexOf(Character.toLowerCase(c))>=0) {
                slide(bpmf.substring(latin.indexOf(Character.toLowerCase(c)),latin.indexOf(Character.toLowerCase(c))+1),Character.isUpperCase(c)?-1:1,false);
            } else if(!zhuyin && c>='0' && c<='9') slide("qwertyuiop".substring((c-'0'+9)%10,(c-'0'+9)%10+1),1,false);
            else click(c==' '?"Space":new String(Character.toChars(c)));
        });
    }
    private void event(long start,int action,float x,float y) {
        MotionEvent e=MotionEvent.obtain(start,SystemClock.uptimeMillis(),action,x,y,0); e.setSource(InputDevice.SOURCE_TOUCHSCREEN);
        assertTrue(getInstrumentation().getUiAutomation().injectInputEvent(e,true)); e.recycle();
    }
    private Rect bounds(String label) { AccessibilityNodeInfo n=node(label); Rect r=new Rect(); n.getBoundsInScreen(r); n.recycle(); return r; }
    private void slide(String label,int direction,boolean cancel) {
        Rect r=bounds(label); long start=SystemClock.uptimeMillis(); float x=r.exactCenterX(),y=r.exactCenterY();
        event(start,MotionEvent.ACTION_DOWN,x,y);
        for(int i=1;i<=4;i++) { SystemClock.sleep(12); event(start,MotionEvent.ACTION_MOVE,x,y+direction*r.height()*.8f*i/4); }
        event(start,cancel?MotionEvent.ACTION_CANCEL:MotionEvent.ACTION_UP,x,y+direction*r.height()*.8f);
        getInstrumentation().waitForIdleSync();
        // A one-shot Shift slide rebuilds the keys; allow accessibility to discard old node IDs.
        try { getInstrumentation().getUiAutomation().waitForIdle(100,3000); }
        catch(java.util.concurrent.TimeoutException e) { throw new AssertionError("Slide UI did not settle",e); }
    }
    private void clear() { getInstrumentation().runOnMainSync(()->activity.text.setText("")); getInstrumentation().waitForIdleSync(); }
    private AccessibilityNodeInfo textNode(AccessibilityNodeInfo n,String text,boolean list) {
        if(n==null) return null;
        if(n.isVisibleToUser() && (list?"android.widget.ListView".contentEquals(n.getClassName()):text.contentEquals(n.getText()==null?"":n.getText()))) return n;
        for(int i=0;i<n.getChildCount();i++) {AccessibilityNodeInfo found=textNode(n.getChild(i),text,list);if(found!=null) {n.recycle();return found;}}
        n.recycle();return null;
    }
    private void menuItem(String label) {
        for(int attempt=0;attempt<20;attempt++) {
            for(AccessibilityWindowInfo w:getInstrumentation().getUiAutomation().getWindows()) {
                AccessibilityNodeInfo target=textNode(w.getRoot(),label,false);
                if(target!=null) {
                    if(target.isClickable()) {assertTrue(target.performAction(AccessibilityNodeInfo.ACTION_CLICK));target.recycle();}
                    else {
                        Rect r=new Rect();target.getBoundsInScreen(r);target.recycle();long at=SystemClock.uptimeMillis();
                        event(at,MotionEvent.ACTION_DOWN,r.exactCenterX(),r.exactCenterY());SystemClock.sleep(35);event(at,MotionEvent.ACTION_UP,r.exactCenterX(),r.exactCenterY());
                    }
                    getInstrumentation().waitForIdleSync();SystemClock.sleep(300);return;
                }
            }
            boolean scrolled=false;
            for(AccessibilityWindowInfo w:getInstrumentation().getUiAutomation().getWindows()) {
                AccessibilityNodeInfo list=textNode(w.getRoot(),"",true);
                if(list!=null) {scrolled=list.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD);list.recycle();}
            }
            if(!scrolled) for(AccessibilityWindowInfo w:getInstrumentation().getUiAutomation().getWindows()) {
                AccessibilityNodeInfo next=find(w.getRoot(),"Next palette page");
                if(next!=null) {if(next.isEnabled()) next.performAction(AccessibilityNodeInfo.ACTION_CLICK);next.recycle();}
            }
            SystemClock.sleep(100);
        }
        capture("review-picker-failure");throw new AssertionError("Menu item missing: "+label);
    }
    private void hold(String label) {
        AccessibilityNodeInfo n=node(label);assertTrue(n.performAction(AccessibilityNodeInfo.ACTION_LONG_CLICK));n.recycle();getInstrumentation().waitForIdleSync();
        try {getInstrumentation().getUiAutomation().waitForIdle(300,3000);}
        catch(java.util.concurrent.TimeoutException e) {throw new AssertionError("Held-key choices did not settle",e);}
    }
    private void doubleTap(String label) {
        Rect r=bounds(label);
        for(int i=0;i<2;i++) {
            long at=SystemClock.uptimeMillis();event(at,MotionEvent.ACTION_DOWN,r.exactCenterX(),r.exactCenterY());SystemClock.sleep(35);
            event(at,MotionEvent.ACTION_UP,r.exactCenterX(),r.exactCenterY());SystemClock.sleep(65);
        }
        getInstrumentation().waitForIdleSync();
    }
    private void paletteEntry(String label) {
        for(int page=0;page<16;page++) {
            for(AccessibilityWindowInfo w:getInstrumentation().getUiAutomation().getWindows()) {
                AccessibilityNodeInfo found=find(w.getRoot(),label);
                if(found!=null) {assertTrue(found.performAction(AccessibilityNodeInfo.ACTION_CLICK));found.recycle();getInstrumentation().waitForIdleSync();return;}
            }
            click("Next palette page");
        }
        throw new AssertionError("Palette entry missing: "+label);
    }
    public void testDoubleTapCapsLock() {
        doubleTap("⇧");node("⇪").recycle();type("USB API ");assertEquals("USB API ",activity.text.getText().toString());
        click("⇪");type("x ");assertEquals("USB API x ",activity.text.getText().toString());
        clear();click("⇧");type("A");node("⇧").recycle();type("b ");assertEquals("Ab ",activity.text.getText().toString());
        clear();click("⇧");SystemClock.sleep(400);click("⬆");node("⇧").recycle();type("x ");assertEquals("x ",activity.text.getText().toString());
    }
    public void testOneTapLanguageSwitch() {
        type("nihao");click("Switch to English");assertEquals("你好",activity.text.getText().toString());
        type("nihao");click(",");assertEquals("你好nihao,",activity.text.getText().toString());
        type(" meeting ");click("Switch to Chinese");type("nihao");click("。");
        assertEquals("你好nihao, meeting 你好。",activity.text.getText().toString());
        clear();click("注音 layout");click("Switch to English");type("hello ");click("Switch to Chinese");
        node("ㄅ").recycle();assertEquals("hello ",activity.text.getText().toString());
        click("拼音 layout");
    }
    public void testEnglishPrimaryTyping() {
        click("Switch to English");type("pronun");click("Candidate pronunciation");click("Space");
        expectText("pronunciation ");
        type("nihao ");assertEquals("pronunciation nihao ",activity.text.getText().toString());
        clear();doubleTap("⇧");type("PRONUN");click("Candidate PRONUNCIATION");click("Space");click("⇪");
        type("hello");click("↵");assertEquals("PRONUNCIATION hello\n",activity.text.getText().toString());
        type("teh ");assertEquals("PRONUNCIATION hello\nteh ",activity.text.getText().toString());
        clear();slide("a",-1,false);type("bc");click(",");click("Space");type("don");slide("z",1,false);type("t");click(".");
        assertEquals("Abc, don't.",activity.text.getText().toString());
        clear();slide("z",1,false);slide("k",1,false);slide("l",1,false);slide("c",1,false);
        assertEquals("'()\"",activity.text.getText().toString());
        clear();type("pronun");capture("review-english");
        click("Switch to Chinese");type("nihao");click("。");assertEquals("pronun你好。",activity.text.getText().toString());
        click("Switch to English");focus(activity.url);type("pronun");
        for(AccessibilityWindowInfo w:getInstrumentation().getUiAutomation().getWindows()) assertNull("URL has no English completion",find(w.getRoot(),"Candidate pronunciation"));
        slide("k",1,false);assertEquals("pronun(",activity.url.getText().toString());
        focus(activity.text);node("Switch to Chinese").recycle();type("keybo");node("Candidate keyboard").recycle();
    }
    private void expectText(String expected) {
        long until=SystemClock.uptimeMillis()+3000;
        java.util.concurrent.atomic.AtomicReference<String> actual=new java.util.concurrent.atomic.AtomicReference<>();
        do {
            getInstrumentation().runOnMainSync(()->actual.set(activity.text.getText().toString()));
            if(expected.equals(actual.get())) return;
            SystemClock.sleep(25);
        } while(SystemClock.uptimeMillis()<until);
        capture("review-editor-failure");assertEquals("destroyed="+activity.isDestroyed()+" focused="+activity.hasWindowFocus()+" attached="+activity.text.isAttachedToWindow(),expected,actual.get());
    }
    public void testPunctuationCommitAndWidth() {
        type("nihao");click("，");click("。");assertEquals("你好，。",activity.text.getText().toString());
        assertEquals(-1,android.view.inputmethod.BaseInputConnection.getComposingSpanStart(activity.text.getText()));
        clear();type("meeting");click("，");assertEquals("meeting，",activity.text.getText().toString());
        clear();type("hi");slide("。",-1,false);assertEquals("hi.",activity.text.getText().toString());
        hold("。");menuItem("Use English punctuation");clear();type("meeting");click(".");assertEquals("meeting.",activity.text.getText().toString());
        hold(".");menuItem("Use Chinese punctuation");hold("。");menuItem("？");assertEquals("meeting.？",activity.text.getText().toString());
    }
    public void testEmojiAndSymbolPalettes() {
        type("nihao");click("Emoji");click("Emoji grinning face");assertEquals("你好😀",activity.text.getText().toString());
        click("⌫");assertEquals("你好",activity.text.getText().toString());
        click("Emoji category");menuItem("People & Body");click("Emoji group");menuItem("hand-fingers-closed");
        paletteEntry("Emoji thumbs up: medium skin tone");assertEquals("你好👍🏽",activity.text.getText().toString());
        click("⌫");assertEquals("你好",activity.text.getText().toString());
        click("Emoji group");menuItem("family");paletteEntry("Emoji family: man, woman, girl, boy");
        assertEquals("你好👨‍👩‍👧‍👦",activity.text.getText().toString());click("⌫");assertEquals("你好",activity.text.getText().toString());
        click("Emoji category");menuItem("Flags");click("Emoji group");menuItem("country-flag");
        paletteEntry("Emoji flag: Taiwan");assertEquals("你好🇹🇼",activity.text.getText().toString());
        click("⌫");assertEquals("你好",activity.text.getText().toString());capture("review-emoji");
        click("Symbols");click("Symbol category");menuItem("數學 Math");click("Symbol ≠");assertEquals("你好≠",activity.text.getText().toString());
        click("Symbol category");menuItem("貨幣 Currency");click("Symbol €");capture("review-symbols");
        click("ABC");type("meeting ");assertEquals("你好≠€meeting ",activity.text.getText().toString());
    }
    public void testVisibleMixedPinyinAndRecovery() {
        type("zhege  pronunciation budui ");
        assertEquals("這個 pronunciation 不對",activity.text.getText().toString());
        capture("review-pinyin");
        clear(); type("mingtian  meeting gaidao  3pm ");
        assertEquals("明天 meeting 改到 3pm ",activity.text.getText().toString());
        clear(); type("ming"); click("Exact input ming"); type(" zhongwen ");
        assertEquals("ming 中文",activity.text.getText().toString());
    }
    public void testVisibleZhuyinAndEnglish() {
        click("注音 layout");
        zhuyin=true;
        type("ㄓㄜˋㄍㄜ˙  pronunciation ㄅㄨˊㄉㄨㄟˋ ");
        assertEquals("這個 pronunciation 不對",activity.text.getText().toString());
        capture("review-zhuyin");
    }
    public void testDefaultRimeAndPartialSelection() throws Exception {
        SharedPreferences prefs=getInstrumentation().getTargetContext().getSharedPreferences("settings",Context.MODE_PRIVATE);
        prefs.edit().remove("rime_pinyin").commit();assertTrue("Rime enabled when no preference exists",RimeBackend.enabled(activity));
        focus(activity.url);focus(activity.text);type("womenmtjian ");expectText("我們明天見");
        clear();type("nihao");click("Candidate 你");expectText("你hao");
        node("Exact input hao").recycle();click("Candidate 好");expectText("你好");
        clear();type("nihao");click("Candidate 你");click("⌫");expectText("你ha");type("o ");expectText("你好");
        clear();type("nihao");click("Candidate 你");click("Exact input hao");expectText("你hao");
        clear();type("nihao");click("Candidate 你");click("Switch to English");expectText("你好");type(" hello ");expectText("你好 hello ");
        click("Switch to Chinese");clear();type("womenmingtianjian");
        Rect raw=bounds("Exact input womenmingtianjian"),phrase=bounds("Candidate 我們明天見");
        assertTrue("Raw recovery is above the candidate strip",raw.bottom<=phrase.top);
        click("Expand candidates");capture("first-impression-expanded");click("Candidate 我們明天見");expectText("我們明天見");
        prefs.edit().putBoolean("rime_pinyin",false).commit();assertFalse("Explicit original backend choice retained",RimeBackend.enabled(activity));
    }
    public void testRimePhrasesAndLiteralRecovery() throws Exception {
        assertTrue(RimeBackend.load(activity).get(60,java.util.concurrent.TimeUnit.SECONDS));
        getInstrumentation().getTargetContext().getSharedPreferences("settings",Context.MODE_PRIVATE).edit().putBoolean("rime_pinyin",true).commit();
        focus(activity.url);focus(activity.text);
        type("womenmingtianjian ");expectText("我們明天見");
        getInstrumentation().getUiAutomation().waitForIdle(200,3000);capture("rime-phone-phrase");
        clear();type("womenmtjian ");expectText("我們明天見");
        clear();type("woyaohekafei");click("。");expectText("我要喝咖啡。");
        clear();type("qingbangwokanyixia");click("Switch to English");expectText("請幫我看一下");
        type(" hello ");expectText("請幫我看一下 hello ");click("Switch to Chinese");
        clear();type("ssh ");expectText("ssh ");
        clear();type("womenmtjian");click("Exact input womenmtjian");expectText("womenmtjian");
        getInstrumentation().getTargetContext().getSharedPreferences("settings",Context.MODE_PRIVATE).edit().putBoolean("rime_pinyin",false).commit();
        focus(activity.url);focus(activity.text);clear();type("bkq ");expectText("不客氣");
    }
    public void testPinyinInitialsMixedSyllablesAndEditing() {
        type("jt"); click("Candidate 今天"); assertEquals("今天",activity.text.getText().toString());
        clear(); type("srufa"); click("Candidate 輸入法"); assertEquals("輸入法",activity.text.getText().toString());
        clear(); type("shrf"); click("⌫"); type("fa");
        node("Exact input shrfa").recycle(); click("Candidate 輸入法"); assertEquals("輸入法",activity.text.getText().toString());
        clear(); type("wxsrf"); capture("review-pinyin-abbreviations");
        click("Candidate 我想輸入法"); assertEquals("我想輸入法",activity.text.getText().toString());
        clear(); type("nh"); click("Exact input nh"); type(" meeting "); assertEquals("nh meeting ",activity.text.getText().toString());
    }
    public void testSlidesCaseNumbersAndCancellation() {
        slide("g",-1,false); type("it"); slide("h",-1,false); type("ub ");
        assertEquals("GitHub ",activity.text.getText().toString());
        clear(); slide("q",1,false); type("pm "); assertEquals("1pm ",activity.text.getText().toString());
        clear(); slide("a",-1,true); assertEquals("",activity.text.getText().toString());
        click("a"); click("Exact input a"); assertEquals("a",activity.text.getText().toString());
        clear(); type("nihao"); slide("v",1,false); assertEquals("你好？",activity.text.getText().toString());
        clear(); for(String key:new String[]{"m","i","n","g"}) slide(key,-1,false);
        type(" zhongwen "); assertEquals("MING 中文",activity.text.getText().toString());
        clear(); click("⇧"); slide("Q",-1,false); click("Space");
        assertEquals("Q ",activity.text.getText().toString());
    }
    public void testCandidateStripScrollsWithoutCommitting() {
        type("ming"); node("Candidate 明").recycle();
        try { getInstrumentation().getUiAutomation().waitForIdle(300,3000); }
        catch(java.util.concurrent.TimeoutException e) { throw new AssertionError(e); }
        AccessibilityNodeInfo word=node("Candidate 明");
        Rect before=new Rect(); word.getBoundsInScreen(before);
        float x=before.exactCenterX(),y=before.exactCenterY(); long start=SystemClock.uptimeMillis();
        event(start,MotionEvent.ACTION_DOWN,x,y);
        for(int i=1;i<=6;i++) { SystemClock.sleep(20); event(start,MotionEvent.ACTION_MOVE,x-i*25,y); }
        event(start,MotionEvent.ACTION_UP,x-150,y); getInstrumentation().waitForIdleSync();
        assertEquals("ming",activity.text.getText().toString());
        assertTrue(word.refresh()); Rect after=new Rect(); word.getBoundsInScreen(after); word.recycle();
        capture("review-candidates");
        assertTrue("Candidate moved out of the clipped viewport: "+before+" to "+after,after.right<before.right);
        click("Exact input ming"); assertEquals("ming",activity.text.getText().toString());
    }
    private void capture(String name) {
        android.graphics.Bitmap bitmap=getInstrumentation().getUiAutomation().takeScreenshot();
        try(java.io.FileOutputStream out=new java.io.FileOutputStream(new java.io.File(activity.getExternalFilesDir(null),name+".png"))) {
            assertTrue(bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,out));
        } catch(java.io.IOException e) { throw new AssertionError(e); }
        finally { bitmap.recycle(); }
    }
    public void testZhuyinCapitalAndPunctuationSlides() {
        click("注音 layout"); zhuyin=true;
        capture("layout-zhuyin-before");
        type("U");capture("layout-zhuyin-first-slide");expectText("U");
        type("SB ㄓㄨˋㄧㄣ  "); assertEquals("USB 注音",activity.text.getText().toString());
        clear(); slide("ㄅ",1,false); slide("ㄉ",-1,false); type(" "); assertEquals("1@ ",activity.text.getText().toString());
        clear(); type("ㄋㄧˇㄏㄠˇ"); slide("ㄥ",1,false); assertEquals("你好？",activity.text.getText().toString());
        // ㄦ belongs to the bottom row, below the forty-key grid.
        assertTrue(bounds("ㄦ").top>bounds("ㄥ").top);
    }
    public void testCursorMoveClearsCommittedContext() {
        type("zhongwen ");
        getInstrumentation().runOnMainSync(()->activity.text.setSelection(0)); getInstrumentation().waitForIdleSync();
        slide("，",-1,false);type(" "); assertEquals(", 中文",activity.text.getText().toString());
        clear(); type("ming");
        getInstrumentation().runOnMainSync(()->activity.text.setSelection(0)); getInstrumentation().waitForIdleSync();
        type("x"); click("Exact input x"); assertEquals("xming",activity.text.getText().toString());
    }
    public void testEnterConfirmsBeforeNewline() {
        type("nihao"); click("↵"); assertEquals("你好",activity.text.getText().toString());
        click("↵"); assertEquals("你好\n",activity.text.getText().toString());
        focus(activity.search); type("nihao"); click("Search");
        assertEquals("你好",activity.search.getText().toString());
        click("Search");
        assertEquals("Editor action: 3",activity.actions.getText().toString());
    }
    public void testEditorSentenceCapitalization() {
        click("Switch to English");
        getInstrumentation().runOnMainSync(()-> {
            android.widget.EditText text=new android.widget.EditText(activity);
            text.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE|android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
            activity.setContentView(text);activity.text=text;
        });
        focus(activity.text);click("H");type("ello");click(".");click("Space");click("W");type("orld");click("Space");
        assertEquals("Hello. World ",activity.text.getText().toString());
    }
    public void testExpandedCandidatesInBothLanguages() {
        type("ming");click("Expand candidates");node("Collapse candidates").recycle();
        click("Candidate 明");assertEquals("明",activity.text.getText().toString());
        clear();click("Switch to English");type("pronun");click("Expand candidates");
        click("Candidate pronunciation");type("test ");assertEquals("pronunciation test ",activity.text.getText().toString());
    }
    public void testPunctuationHoldDragRelease() {
        type("nihao");Rect origin=bounds("。");long start=SystemClock.uptimeMillis();
        event(start,MotionEvent.ACTION_DOWN,origin.exactCenterX(),origin.exactCenterY());SystemClock.sleep(700);
        Rect target=bounds("Punctuation ？");capture("implementation-punctuation-popup");
        event(start,MotionEvent.ACTION_MOVE,target.exactCenterX(),target.exactCenterY());SystemClock.sleep(80);
        event(start,MotionEvent.ACTION_UP,target.exactCenterX(),target.exactCenterY());getInstrumentation().waitForIdleSync();
        expectText("你好？");
    }
    public void testEnglishCorrectionUndoAndDoubleSpace() {
        getInstrumentation().getTargetContext().getSharedPreferences("settings",Context.MODE_PRIVATE).edit().putBoolean("english_correction",true).commit();
        click("Switch to English");focus(activity.search);focus(activity.text);
        type("teh ");expectText("the ");click("⌫");expectText("teh");click("Exact input teh");click("Space");
        expectText("teh ");clear();type("hello");click("Space");click("Space");expectText("hello. ");
        clear();type("im ");expectText("I'm ");clear();type("cant ");expectText("cant ");
        clear();type("pronun");click("Candidate pronunciation");click("Space");click("Space");expectText("pronunciation. ");
    }
    public void testCompositionSurvivesHideAndRestart() {
        type("nihao");
        getInstrumentation().runOnMainSync(()->((InputMethodManager)activity.getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(activity.text.getWindowToken(),0));
        SystemClock.sleep(700);focus(activity.text);SystemClock.sleep(500);click("Space");expectText("你好");
        clear();type("nihao");getInstrumentation().runOnMainSync(()->((InputMethodManager)activity.getSystemService(Context.INPUT_METHOD_SERVICE)).restartInput(activity.text));
        SystemClock.sleep(500);click("Space");expectText("你好");
    }
    public void testEnglishWordTraceAndVerticalSlide() {
        click("Switch to English");String word="hello";Rect first=bounds("h");long start=SystemClock.uptimeMillis();
        float x=first.exactCenterX(),y=first.exactCenterY();event(start,MotionEvent.ACTION_DOWN,x,y);
        for(int i=1;i<word.length();i++) {Rect r=bounds(word.substring(i,i+1));float nx=r.exactCenterX(),ny=r.exactCenterY();
            for(int step=1;step<=8;step++){event(start,MotionEvent.ACTION_MOVE,x+(nx-x)*step/8,y+(ny-y)*step/8);SystemClock.sleep(12);}x=nx;y=ny;
        }
        event(start,MotionEvent.ACTION_UP,x,y);node("Exact input hello").recycle();click("Space");expectText("hello ");
        slide("g",-1,false);type("ood ");expectText("hello Good ");
    }
    public void testOptionalEmojiRecentsRespectPrivateInput() {
        SharedPreferences settings=getInstrumentation().getTargetContext().getSharedPreferences("settings",Context.MODE_PRIVATE);
        SharedPreferences learning=getInstrumentation().getTargetContext().getSharedPreferences("learning",Context.MODE_PRIVATE);
        click("Emoji");click("Emoji grinning face");assertFalse(learning.contains("recent_emoji"));click("ABC");
        settings.edit().putBoolean("emoji_recents",true).commit();click("Emoji");click("Emoji grinning face");
        String recent=learning.getString("recent_emoji","");assertEquals("😀",recent);click("ABC");
        getInstrumentation().runOnMainSync(()-> {
            android.widget.EditText text=new android.widget.EditText(activity);
            text.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE);
            text.setImeOptions(android.view.inputmethod.EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING);activity.setContentView(text);activity.text=text;
        });focus(activity.text);click("Emoji");click("Emoji grinning face with big eyes");
        assertEquals(recent,learning.getString("recent_emoji",""));
        click("Emoji category");for(AccessibilityWindowInfo w:getInstrumentation().getUiAutomation().getWindows())assertNull("Private field hides recents",find(w.getRoot(),"Recent"));
    }
    public void testHeldDeleteStopsOnRelease() {
        type("abcdefghijkl");
        Rect r=bounds("⌫"); long start=SystemClock.uptimeMillis();
        event(start,MotionEvent.ACTION_DOWN,r.exactCenterX(),r.exactCenterY());
        SystemClock.sleep(850); event(start,MotionEvent.ACTION_UP,r.exactCenterX(),r.exactCenterY());
        getInstrumentation().waitForIdleSync(); String remaining=activity.text.getText().toString();
        assertTrue(remaining.length()<11); SystemClock.sleep(180);
        assertEquals(remaining,activity.text.getText().toString());
    }
    public void testVisibleUrlPasswordAndNumericFields() {
        focus(activity.url); type("ming"); click("Space");
        assertEquals("ming ",activity.url.getText().toString());
        focus(activity.password); type("ming");
        assertEquals("ming",activity.password.getText().toString());
        assertTrue(activity.password.getTransformationMethod() instanceof android.text.method.PasswordTransformationMethod);
        focus(activity.number); click("1"); click("2"); click("."); click("3");
        assertEquals("12.3",activity.number.getText().toString());
    }
    private void focus(android.view.View view) {
        getInstrumentation().runOnMainSync(()-> {
            view.requestFocus(); ((InputMethodManager)activity.getSystemService(Context.INPUT_METHOD_SERVICE)).showSoftInput(view,InputMethodManager.SHOW_IMPLICIT);
        });
        getInstrumentation().waitForIdleSync();
        try { getInstrumentation().getUiAutomation().waitForIdle(300,3000); }
        catch(java.util.concurrent.TimeoutException e) { throw new AssertionError(e); }
    }
    public void testMixedEnglishDefaultVisible() throws Exception {
        getInstrumentation().getTargetContext().getSharedPreferences("settings",Context.MODE_PRIVATE).edit().remove("rime_pinyin").commit();
        assertTrue(RimeBackend.load(activity).get(60,java.util.concurrent.TimeUnit.SECONDS));focus(activity.url);focus(activity.text);
        for(String raw:new String[]{"hello","time","thanks","morning"}) {
            clear();type(raw);
            Rect literal=bounds("Exact input "+raw),strip=bounds("Candidate list");
            assertTrue("Literal Space default is inside the candidate strip",strip.contains(literal.centerX(),literal.centerY()));
            if(raw.equals("time"))node("Candidate times").recycle();
            click("Expand candidates");
            AccessibilityNodeInfo grid=node("Expanded candidate list"),exact=find(grid,"Exact input "+raw);
            assertNotNull("Expansion retains the literal choice",exact);exact.recycle();
            click("Collapse candidates");click("Space");expectText(raw+" ");
        }
        clear();type("nihao");click("Candidate 你");click("Candidate 好");expectText("你好");
    }
    public void testWebViewMixedInput() throws Exception {
        java.util.concurrent.CountDownLatch loaded=new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.atomic.AtomicReference<android.webkit.WebView> ref=new java.util.concurrent.atomic.AtomicReference<>();
        getInstrumentation().runOnMainSync(()-> {
            android.webkit.WebView web=new android.webkit.WebView(activity); ref.set(web);
            web.getSettings().setJavaScriptEnabled(true);
            web.setWebViewClient(new android.webkit.WebViewClient() {
                @Override public void onPageFinished(android.webkit.WebView view,String url) { loaded.countDown(); }
            });
            activity.setContentView(web);
            web.loadData("<html><meta name='viewport' content='width=device-width,initial-scale=1'><body><h2>MinIME local WebView check</h2><textarea id='editor' aria-label='Web editor' style='width:90%;height:100px'></textarea></body></html>","text/html","UTF-8");
        });
        assertTrue("Local WebView loaded",loaded.await(10,java.util.concurrent.TimeUnit.SECONDS));
        getInstrumentation().runOnMainSync(()->ref.get().evaluateJavascript("document.getElementById('editor').focus()",null));
        focus(ref.get());
        type("zhege  pronunciation budui ");
        java.util.concurrent.CountDownLatch read=new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.atomic.AtomicReference<String> result=new java.util.concurrent.atomic.AtomicReference<>();
        getInstrumentation().runOnMainSync(()->ref.get().evaluateJavascript("document.getElementById('editor').value",value->{result.set(value); read.countDown();}));
        assertTrue(read.await(5,java.util.concurrent.TimeUnit.SECONDS));
        assertEquals("\"這個 pronunciation 不對\"",result.get());
        getInstrumentation().runOnMainSync(()->{ activity.setContentView(new android.widget.TextView(activity)); ref.get().destroy(); });
    }
}
