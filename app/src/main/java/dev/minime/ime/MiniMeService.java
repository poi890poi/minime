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
    private ModePreferences modes;
    private boolean zhuyin, english, englishPunctuation, destroyed, ready;
    private int panel;
    private final ShiftState shift=new ShiftState();
    private String dictionaryStatus="Loading offline dictionary…";
    private EditorInfo editorInfo=new EditorInfo();
    private EditorPolicy policy=new EditorPolicy(editorInfo);
    private boolean literalInput() { return policy.literal(english); }
    private final SelectionState selection=new SelectionState();
    private java.util.Set<String> activeAddons=java.util.Collections.emptySet();
    private long addonRequest;
    private boolean addonLoading;
    private void configureAddons() {
        java.util.Set<String> enabled=AddonRepository.enabled(this);
        AddonRepository.retainEnabled(enabled);
        engine.phraseLearning(getSharedPreferences("settings",MODE_PRIVATE).getBoolean("phrase_learning",false));
        engine.focusedLearning(getSharedPreferences("settings",MODE_PRIVATE).getBoolean("focused_choice_learning",true));
        engine.pairedTaiwanese(getSharedPreferences("settings",MODE_PRIVATE).getBoolean("paired_taiwanese",true),
            getSharedPreferences("settings",MODE_PRIVATE).getBoolean("taiwanese_han_primary",false));
        java.util.Set<String> requested=literalInput() || policy.secure || policy.numeric
            ?java.util.Collections.emptySet():engine.inputMode().packs(enabled);
        if(requested.equals(activeAddons))return;
        activeAddons=requested;long request=++addonRequest;
        addonLoading=!requested.isEmpty();
        if(requested.isEmpty()) {engine.addons(AddonDictionary.EMPTY,requested);return;}
        engine.awaitAddons(requested);
        AddonRepository.load(this,requested).whenComplete((dictionary,error)->new Handler(Looper.getMainLooper()).post(()-> {
            if(destroyed || request!=addonRequest)return;
            addonLoading=false;
            if(error==null) {engine.addons(dictionary,requested);render();}
            else {
                activeAddons=java.util.Collections.emptySet();
                engine.addons(AddonDictionary.EMPTY,requested);render();
                android.widget.Toast.makeText(this,"Optional dictionaries unavailable; check MinIME settings",android.widget.Toast.LENGTH_LONG).show();
            }
        }));
    }
    @Override public void onCreate() {
        super.onCreate();
        modes=new ModePreferences(this);
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
        keyboard=new KeyboardView(this,this::key,this::longKey,(points,capitalization)->{engine.trace(points,capitalization);shift.consume();render();},action->{
            if(getCurrentInputConnection()==null)return;
            shift.interrupt();action.run();render();
        });
        render(); return keyboard;
    }
    @Override public void onStartInput(EditorInfo attribute,boolean restarting) {
        super.onStartInput(attribute,restarting);
        EditorPolicy nextPolicy=new EditorPolicy(attribute);
        boolean sameField=restarting && editorInfo.fieldId==attribute.fieldId
            && java.util.Objects.equals(editorInfo.packageName,attribute.packageName) && editorInfo.inputType==attribute.inputType
            && policy.privateField==nextPolicy.privateField && literalInput()==nextPolicy.literal(english);
        boolean resume=sameField && !engine.raw().isEmpty()
            && selection.owns(attribute.initialSelStart,attribute.initialSelEnd,engine.raw().length());
        if(resume) {
            InputConnection input=getCurrentInputConnection();
            // Validate only our bounded composing text; never retain editor surroundings.
            CharSequence owned=input==null?null:input.getTextBeforeCursor(engine.raw().length(),0);
            resume=owned!=null && engine.raw().contentEquals(owned)
                && input.setComposingRegion(attribute.initialSelEnd-engine.raw().length(),attribute.initialSelEnd);
        }
        editorInfo=attribute; policy=nextPolicy;
        if(resume) {configureAddons();render();return;}
        zhuyin=getSharedPreferences("settings",MODE_PRIVATE).getBoolean("zhuyin",false);
        decoder.rime(RimeBackend.enabled(this));
        if(RimeBackend.enabled(this))RimeBackend.load(this).whenComplete((loaded,error)->new Handler(Looper.getMainLooper()).post(()-> {
            if(!destroyed && Boolean.TRUE.equals(loaded) && RimeBackend.enabled(this) && !english && !literalInput() && !engine.raw().isEmpty()) {
                engine.refresh();render();
            }
        }));
        englishPunctuation=getSharedPreferences("settings",MODE_PRIVATE).getBoolean("english_punctuation",false);
        // Chrome may restart an empty editor after a commit. Keep an explicit
        // language choice for that field rather than applying its default again.
        if(!sameField)english=policy.preferEnglish || modes.selected().english();
        shift.reset(); panel=0;
        if(restarting) engine.abandon();
        selection.start(attribute.initialSelStart,attribute.initialSelEnd);
        engine.start(zhuyin,literalInput(),policy.privateField,policy.secure || policy.numeric,english);
        engine.switchMode(english?InputMode.ENGLISH:modes.mixed(),literalInput());
        configureAddons();
        engine.englishOptions(getSharedPreferences("settings",MODE_PRIVATE).getBoolean("english_correction",false),
            getSharedPreferences("settings",MODE_PRIVATE).getBoolean("double_space_period",true));
        render();
    }
    @Override public void onFinishInput() {
        if(keyboard!=null)keyboard.inputActive(false);
        engine.abandon(); super.onFinishInput();
    }
    @Override public void onStartInputView(EditorInfo attribute,boolean restarting) {
        super.onStartInputView(attribute,restarting);
        if(!english && engine.inputMode()!=modes.mixed())engine.switchMode(modes.mixed(),literalInput());
        configureAddons();
        if(keyboard!=null)keyboard.inputActive(true);
        if(!engine.raw().isEmpty()) {
            InputConnection input=getCurrentInputConnection();int end=selection.cursor();
            CharSequence owned=input==null?null:input.getTextBeforeCursor(engine.raw().length(),0);
            if(!selection.owns(end,end,engine.raw().length()) || owned==null || !engine.raw().contentEquals(owned)
                    || !input.setComposingRegion(end-engine.raw().length(),end))engine.abandon();
        }
        render();
    }
    @Override public void onFinishInputView(boolean finishingInput) {
        if(keyboard!=null)keyboard.inputActive(false);
        if(finishingInput)engine.abandon(); super.onFinishInputView(finishingInput);
    }
    @Override public void onUpdateSelection(int oldStart,int oldEnd,int newStart,int newEnd,int candidatesStart,int candidatesEnd) {
        super.onUpdateSelection(oldStart,oldEnd,newStart,newEnd,candidatesStart,candidatesEnd);
        if(selection.update(newStart,newEnd) && (!engine.raw().isEmpty() || !engine.context().isEmpty())) {
            engine.abandon(); render();
        }
        else if(english) {
            boolean upper=shift.upper();InputConnection input=getCurrentInputConnection();
            shift.automatic(!literalInput() && input!=null && input.getCursorCapsMode(editorInfo.inputType)!=0);
            if(upper!=shift.upper())render();
        }
    }
    @Override public boolean onEvaluateFullscreenMode() { return false; }
    @Override public void onDestroy() { destroyed=true;decoder.close(); super.onDestroy(); }
    private void render() {
        InputConnection input=getCurrentInputConnection();
        shift.automatic(english && !literalInput() && input!=null && input.getCursorCapsMode(editorInfo.inputType)!=0);
        if(keyboard!=null) {keyboard.modeOptions(modes.mixed(),modes.configured());keyboard.render(engine,zhuyin && engine.inputMode()==InputMode.CHINESE && !literalInput() && !english,shift.upper(),shift.locked(),panel,policy.numeric,
            literalInput() || policy.numeric || english || englishPunctuation,english || literalInput(),!policy.literal && !policy.numeric,!literalInput(),
            EditorPolicy.enterLabel(editorInfo),!ready?dictionaryStatus:addonLoading?"Loading "+engine.inputMode().label+" dictionary…":"");}
    }
    private void switchMode(InputMode mode) {
        mode=modes.resolve(mode);
        if(!mode.available(modes.configured()))return;
        english=mode.english();englishPunctuation=false;shift.reset();panel=0;
        modes.select(mode);engine.switchMode(mode,literalInput());configureAddons();
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
        boolean commit=value.equals("SPACE") || value.equals("ENTER") || value.equals("LAYOUT")
            || value.startsWith("INSERT:") || value.startsWith("LITERAL:");
        if(engine.deferUntilReady(()->key(value),commit))return;
        if(!value.equals("SHIFT")) shift.interrupt();
        switch(value) {
            case "SHIFT": shift.tap(SystemClock.uptimeMillis(),ViewConfiguration.getDoubleTapTimeout()); break;
            case "SPACE": engine.space(); break;
            case "DELETE": engine.backspace(); break;
            case "ENTER":
                int action=EditorPolicy.action(editorInfo);
                if(!literalInput() && !english && !engine.raw().isEmpty()) engine.confirm();
                else engine.enter();
                break;
            case "SYMBOLS": panel=panel==1?0:1; break;
            case "EMOJI": panel=panel==2?0:2; break;
            case "PUNCTUATION": panel=3; break;
            case "LETTERS": panel=0; break;
            case "LANGUAGE":
                switchMode(english?modes.mixed():InputMode.ENGLISH);
                break;
            case "PUNCT_WIDTH":
                englishPunctuation=!englishPunctuation;panel=0;
                getSharedPreferences("settings",MODE_PRIVATE).edit().putBoolean("english_punctuation",englishPunctuation).apply();
                break;
            case "LAYOUT": zhuyin=!zhuyin; engine.layout(zhuyin); getSharedPreferences("settings",MODE_PRIVATE).edit().putBoolean("zhuyin",zhuyin).apply(); break;
            case "SETTINGS": startActivity(new Intent(this,SettingsActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); break;
            case "NEXT_IME": switchToNextInputMethod(false); break;
            default:
                if(value.startsWith("MODE:"))switchMode(InputMode.fromId(value.substring(5)));
                else if(value.startsWith("INSERT:")) { engine.literal(value.substring(7)); shift.consume(); if(panel==3) panel=0; }
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
