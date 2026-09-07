package dev.minime.ime;

import android.inputmethodservice.InputMethodService;
import android.content.*;
import android.os.*;
import android.view.*;
import android.view.inputmethod.*;
import dev.minime.core.*;

public final class MiniMeService extends InputMethodService {
    private CompositionEngine engine;
    private AsyncDecoder decoder;
    private KeyboardView keyboard;
    private boolean zhuyin, english, englishPunctuation, destroyed, ready;
    private int panel;
    private final ShiftState shift=new ShiftState();
    private String dictionaryStatus="Loading offline dictionary…";
    private EditorInfo editorInfo=new EditorInfo();
    private EditorPolicy policy=new EditorPolicy(editorInfo);
    private final SelectionState selection=new SelectionState();
    @Override public void onCreate() {
        super.onCreate();
        engine=new CompositionEngine(new AndroidEditor(this::getCurrentInputConnection,()->editorInfo,selection),new LocalLearning(this));
        decoder=new AsyncDecoder(new Handler(Looper.getMainLooper()));engine.decoder(decoder,this::render);
        DictionaryRepository.load(this).whenComplete((dictionary,error)->new Handler(Looper.getMainLooper()).post(()-> {
            if(destroyed) return;
            if(error==null) { engine.dictionary(dictionary); ready=true; dictionaryStatus=""; }
            else dictionaryStatus="Dictionary unavailable · literal input works";
            render();
        }));
    }
    @Override public View onCreateInputView() {
        keyboard=new KeyboardView(this,this::key,this::longKey);
        render(); return keyboard;
    }
    @Override public void onStartInput(EditorInfo attribute,boolean restarting) {
        super.onStartInput(attribute,restarting);
        boolean resume=restarting && !engine.raw().isEmpty() && editorInfo.fieldId==attribute.fieldId
            && java.util.Objects.equals(editorInfo.packageName,attribute.packageName) && editorInfo.inputType==attribute.inputType
            && selection.owns(attribute.initialSelStart,attribute.initialSelEnd,engine.raw().length());
        if(resume) {
            InputConnection input=getCurrentInputConnection();
            // Validate only our bounded composing text; never retain editor surroundings.
            CharSequence owned=input==null?null:input.getTextBeforeCursor(engine.raw().length(),0);
            resume=owned!=null && engine.raw().contentEquals(owned)
                && input.setComposingRegion(attribute.initialSelEnd-engine.raw().length(),attribute.initialSelEnd);
        }
        editorInfo=attribute; policy=new EditorPolicy(attribute);
        if(resume) {render();return;}
        zhuyin=getSharedPreferences("settings",MODE_PRIVATE).getBoolean("zhuyin",false);
        englishPunctuation=getSharedPreferences("settings",MODE_PRIVATE).getBoolean("english_punctuation",false);
        english=getSharedPreferences("settings",MODE_PRIVATE).getBoolean("english_mode",false);
        shift.reset(); panel=0;
        if(restarting) engine.abandon();
        selection.start(attribute.initialSelStart,attribute.initialSelEnd);
        engine.start(zhuyin,policy.literal,policy.privateField,policy.secure || policy.numeric,english);
        engine.englishOptions(getSharedPreferences("settings",MODE_PRIVATE).getBoolean("english_correction",false),
            getSharedPreferences("settings",MODE_PRIVATE).getBoolean("double_space_period",true));
        render();
    }
    @Override public void onFinishInput() {
        engine.abandon(); super.onFinishInput();
    }
    @Override public void onStartInputView(EditorInfo attribute,boolean restarting) {
        super.onStartInputView(attribute,restarting);
        if(!engine.raw().isEmpty()) {
            InputConnection input=getCurrentInputConnection();int end=selection.cursor();
            CharSequence owned=input==null?null:input.getTextBeforeCursor(engine.raw().length(),0);
            if(!selection.owns(end,end,engine.raw().length()) || owned==null || !engine.raw().contentEquals(owned)
                    || !input.setComposingRegion(end-engine.raw().length(),end))engine.abandon();
        }
        render();
    }
    @Override public void onFinishInputView(boolean finishingInput) {
        if(finishingInput)engine.abandon(); super.onFinishInputView(finishingInput);
    }
    @Override public void onUpdateSelection(int oldStart,int oldEnd,int newStart,int newEnd,int candidatesStart,int candidatesEnd) {
        super.onUpdateSelection(oldStart,oldEnd,newStart,newEnd,candidatesStart,candidatesEnd);
        if(selection.update(newStart,newEnd) && (!engine.raw().isEmpty() || !engine.context().isEmpty())) {
            engine.abandon(); render();
        }
        else if(english) {
            boolean upper=shift.upper();InputConnection input=getCurrentInputConnection();
            shift.automatic(!policy.literal && input!=null && input.getCursorCapsMode(editorInfo.inputType)!=0);
            if(upper!=shift.upper())render();
        }
    }
    @Override public boolean onEvaluateFullscreenMode() { return false; }
    @Override public void onDestroy() { destroyed=true;decoder.close(); super.onDestroy(); }
    private void render() {
        InputConnection input=getCurrentInputConnection();
        shift.automatic(english && !policy.literal && input!=null && input.getCursorCapsMode(editorInfo.inputType)!=0);
        if(keyboard!=null) keyboard.render(engine,zhuyin && !policy.literal && !english,shift.upper(),shift.locked(),panel,policy.numeric,
            policy.literal || policy.numeric || english || englishPunctuation,english || policy.literal,!policy.literal && !policy.numeric,
            EditorPolicy.enterLabel(editorInfo),ready ? "" : dictionaryStatus);
    }
    private boolean longKey(String value) {
        if(value.equals("SPACE") && engine.deferUntilReady(()->longKey(value),false))return true;
        if(value.equals("SHIFT")) { shift.hold(); render(); return true; }
        if(value.equals("SYMBOLS")) { shift.interrupt(); panel=2; render(); return true; }
        if(value.equals("SPACE")) { engine.commitRaw(true); render(); return true; }
        return false;
    }
    private void key(String value) {
        if(getCurrentInputConnection()==null) return;
        boolean commit=value.equals("SPACE") || value.equals("ENTER") || value.equals("LANGUAGE") || value.equals("LAYOUT")
            || value.startsWith("INSERT:") || value.startsWith("LITERAL:");
        if(engine.deferUntilReady(()->key(value),commit))return;
        if(!value.equals("SHIFT")) shift.interrupt();
        switch(value) {
            case "SHIFT": shift.tap(SystemClock.uptimeMillis(),ViewConfiguration.getDoubleTapTimeout()); break;
            case "SPACE": engine.space(); break;
            case "DELETE": engine.backspace(); break;
            case "ENTER":
                int action=EditorPolicy.action(editorInfo);
                if(!policy.literal && !english && !engine.raw().isEmpty()) engine.confirm();
                else engine.enter();
                break;
            case "SYMBOLS": panel=panel==1?0:1; break;
            case "EMOJI": panel=panel==2?0:2; break;
            case "PUNCTUATION": panel=3; break;
            case "LETTERS": panel=0; break;
            case "LANGUAGE":
                engine.confirm();english=!english;englishPunctuation=false;shift.reset();panel=0;
                getSharedPreferences("settings",MODE_PRIVATE).edit().putBoolean("english_mode",english).putBoolean("english_punctuation",false).apply();
                engine.start(zhuyin,policy.literal,policy.privateField,policy.secure || policy.numeric,english);
                break;
            case "PUNCT_WIDTH":
                englishPunctuation=!englishPunctuation;panel=0;
                getSharedPreferences("settings",MODE_PRIVATE).edit().putBoolean("english_punctuation",englishPunctuation).apply();
                break;
            case "LAYOUT": zhuyin=!zhuyin; engine.layout(zhuyin); getSharedPreferences("settings",MODE_PRIVATE).edit().putBoolean("zhuyin",zhuyin).apply(); break;
            case "SETTINGS": startActivity(new Intent(this,SettingsActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); break;
            case "NEXT_IME": switchToNextInputMethod(false); break;
            default:
                if(value.startsWith("INSERT:")) { engine.literal(value.substring(7)); shift.consume(); if(panel==3) panel=0; }
                else if(value.startsWith("LITERAL:")) { engine.literal(value.substring(8)); shift.consume(); }
                else if(value.startsWith("CANDIDATE:")) engine.select(Integer.parseInt(value.substring(10)));
                else { value.codePoints().forEach(engine::type); shift.consume(); }
        }
        render();
    }
    @Override public boolean onKeyDown(int keyCode,KeyEvent event) {
        if(event.isCtrlPressed() || event.isAltPressed() || event.isMetaPressed()) return super.onKeyDown(keyCode,event);
        if(keyCode==KeyEvent.KEYCODE_DEL && !engine.raw().isEmpty()) { key("DELETE"); return true; }
        if(keyCode==KeyEvent.KEYCODE_SPACE) { key("SPACE"); return true; }
        if(keyCode==KeyEvent.KEYCODE_ENTER) { key("ENTER"); return true; }
        int cp=event.getUnicodeChar();
        if(cp!=0 && (cp & KeyCharacterMap.COMBINING_ACCENT)==0) { key(new String(Character.toChars(cp))); return true; }
        return super.onKeyDown(keyCode,event);
    }
}
