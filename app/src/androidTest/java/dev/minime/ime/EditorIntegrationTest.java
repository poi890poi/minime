package dev.minime.ime;

import android.test.ActivityInstrumentationTestCase2;
import android.view.KeyEvent;
import android.view.inputmethod.*;
import android.text.InputType;
import dev.minime.core.*;
import java.util.concurrent.atomic.AtomicReference;

@SuppressWarnings("deprecation")
public final class EditorIntegrationTest extends ActivityInstrumentationTestCase2<EditorTestActivity> {
    public EditorIntegrationTest() { super(EditorTestActivity.class); }
    public void testComposingCommitSelectionAndDelete() throws Throwable {
        EditorTestActivity activity=getActivity();
        long focusDeadline=android.os.SystemClock.uptimeMillis()+5000;
        while(!activity.hasWindowFocus() && android.os.SystemClock.uptimeMillis()<focusDeadline) android.os.SystemClock.sleep(25);
        assertTrue("Test editor has window focus before dispatching deletion",activity.hasWindowFocus());
        runTestOnUiThread(()-> {
            activity.text.setText("");
            EditorInfo info=new EditorInfo(); InputConnection ic=activity.text.onCreateInputConnection(info);
            AndroidEditor editor=new AndroidEditor(()->ic,()->info);
            editor.composing("ming"); assertEquals("ming",activity.text.getText().toString());
            editor.composing("min"); assertEquals("min",activity.text.getText().toString());
            editor.commit("明"); assertEquals("明",activity.text.getText().toString());
            editor.commit(" meeting "); assertEquals("明 meeting ",activity.text.getText().toString());
            activity.text.setSelection(2,9); editor.delete();
        });
        // sendKeyEvent crosses the input dispatcher; an idle app looper does not
        // imply that the dispatched key has returned to the editor yet.
        long deleteDeadline=android.os.SystemClock.uptimeMillis()+3000;
        AtomicReference<String> actual=new AtomicReference<>();
        do {
            getInstrumentation().runOnMainSync(()->actual.set(activity.text.getText().toString()));
            if("明  ".equals(actual.get())) break;
            android.os.SystemClock.sleep(25);
        } while(android.os.SystemClock.uptimeMillis()<deleteDeadline);
        assertEquals("明  ",activity.text.getText().toString());
    }
    public void testEditorActionAndMultilinePolicy() throws Throwable {
        EditorTestActivity activity=getActivity();
        runTestOnUiThread(()-> {
            EditorInfo info=new EditorInfo(); info.inputType=InputType.TYPE_CLASS_TEXT;
            final int[] called={0}; final int[] enterEvents={0};
            BaseInputConnection connection=new BaseInputConnection(activity.text,true) {
                @Override public boolean performEditorAction(int action) { called[0]=action; return true; }
                @Override public boolean sendKeyEvent(KeyEvent event) { if(event.getKeyCode()==KeyEvent.KEYCODE_ENTER) enterEvents[0]++; return true; }
            };
            AndroidEditor editor=new AndroidEditor(()->connection,()->info);
            for(int action:new int[]{EditorInfo.IME_ACTION_GO,EditorInfo.IME_ACTION_SEARCH,EditorInfo.IME_ACTION_SEND,EditorInfo.IME_ACTION_NEXT,EditorInfo.IME_ACTION_DONE,EditorInfo.IME_ACTION_PREVIOUS}) {
                info.imeOptions=action; editor.enter(); assertEquals(action,called[0]);
            }
            assertEquals(0,enterEvents[0]);
            info.imeOptions=EditorInfo.IME_ACTION_SEND|EditorInfo.IME_FLAG_NO_ENTER_ACTION; editor.enter(); assertEquals(2,enterEvents[0]);
            info.imeOptions=EditorInfo.IME_ACTION_NONE; editor.enter(); assertEquals(4,enterEvents[0]);
            info.imeOptions=EditorInfo.IME_ACTION_UNSPECIFIED; info.actionLabel="Custom"; info.actionId=42; editor.enter(); assertEquals(42,called[0]);
        });
    }
    public void testSecurePolicies() {
        for(int type:new int[]{InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD,InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD,InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD,InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_VARIATION_PASSWORD}) {
            EditorInfo info=new EditorInfo(); info.inputType=type; EditorPolicy policy=new EditorPolicy(info);
            assertTrue(policy.secure); assertTrue(policy.privateField); assertTrue(policy.literal);
        }
        EditorInfo info=new EditorInfo(); info.inputType=InputType.TYPE_CLASS_TEXT; info.imeOptions=EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING;
        assertFalse(new EditorPolicy(info).secure); assertTrue(new EditorPolicy(info).privateField);
        info.inputType=InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_URI; assertTrue(new EditorPolicy(info).literal);
        info.inputType=InputType.TYPE_CLASS_NUMBER; assertTrue(new EditorPolicy(info).numeric);
    }
    public void testQueuedSelectionAcknowledgementsAndExternalMove() {
        SelectionState s=new SelectionState(); s.start(0,0);
        s.write("ming",true); s.write("明",false);
        assertFalse(s.update(4,4)); assertFalse(s.update(1,1));
        assertTrue(s.update(0,0));
        s.write("x",true); assertFalse(s.update(1,1));
        s.finish(); s.start(3,7); s.write("A",false); assertFalse(s.update(4,4));
        assertTrue(s.update(0,4));
    }
    public void testRealMultilineEnterAndPasswordMasking() throws Throwable {
        EditorTestActivity activity=getActivity();
        runTestOnUiThread(()-> {
            assertTrue(activity.password.getTransformationMethod() instanceof android.text.method.PasswordTransformationMethod);
            activity.text.setText("line"); activity.text.setSelection(4); activity.text.requestFocus();
            EditorInfo info=new EditorInfo(); InputConnection ic=activity.text.onCreateInputConnection(info);
            new AndroidEditor(()->ic,()->info).enter();
        });
        getInstrumentation().waitForIdleSync();
        assertEquals("line\n",activity.text.getText().toString());
    }
    public void testCoreWithRealInputConnection() throws Throwable {
        PhoneticDictionary dictionary=DictionaryRepository.load(getActivity()).get();
        runTestOnUiThread(()-> {
            EditorTestActivity activity=getActivity(); activity.text.setText("");
            EditorInfo info=new EditorInfo(); InputConnection ic=activity.text.onCreateInputConnection(info);
            CompositionEngine engine=new CompositionEngine(new AndroidEditor(()->ic,()->info),Learning.NONE);
            engine.dictionary(dictionary); engine.start(false,false,false,false);
            "zhege  pronunciation budui ".codePoints().forEach(engine::type);
            assertEquals("這個 pronunciation 不對",activity.text.getText().toString());
            activity.text.setText(""); engine.start(false,false,false,false);
            "mingtian  meeting gaidao  3pm ".codePoints().forEach(engine::type);
            assertEquals("明天 meeting 改到 3pm ",activity.text.getText().toString());
        });
    }
}
