package dev.minime.core;

import java.util.*;
import static dev.minime.core.IntentClassifier.Intent;

/** One token at a time: intent never changes the keyboard layout or a persistent language mode. */
public final class CompositionEngine {
    public interface Editor {
        void composing(String text);
        void commit(String text);
        void delete();
        void enter();
        void finish();
        default void restoreSpelling(int count,String spelling) { for(int i=0;i<count;i++)delete();composing(spelling); }
        default void replacePrevious(int count,String text) {for(int i=0;i<count;i++)delete();commit(text);}
    }
    private final Editor editor;
    private final IntentClassifier classifier = new IntentClassifier();
    private final Learning learning;
    private PhoneticDictionary dictionary;
    private String raw = "", context = "";
    private boolean zhuyin, literalField, privateField, direct, englishMode;
    private boolean completionBoundary;
    private boolean autoCorrect, doubleSpace;
    private long spaceAt=-1;
    private String undoSpelling="", undoOutput="";
    public void englishOptions(boolean correction,boolean period) { autoCorrect=correction;doubleSpace=period;refresh(); }
    private void clearAssistance() { spaceAt=-1;undoSpelling="";undoOutput=""; }
    private Intent intent = Intent.LATIN_LITERAL;
    private List<Candidate> candidates = new ArrayList<>();
    private int preferred;
    public interface Decoder {
        void convert(PhoneticDictionary dictionary,String raw,boolean zhuyin,String context,java.util.function.Consumer<List<Candidate>> result);
        default void trace(PhoneticDictionary dictionary,float[] points,java.util.function.Consumer<List<Candidate>> result) {result.accept(dictionary.englishTrace(points));}
    }
    private boolean traced;
    public void trace(float[] points,int capitalization) {
        if(deferUntilReady(()->trace(points,capitalization),true))return;
        if(dictionary==null || !englishMode || literalField || direct)return;
        clearAssistance();resolveCompletionBoundary("a");commitDefault(true);
        long query=++revision;pending=true;barrier=true;
        java.util.function.Consumer<List<Candidate>> done=found->{
            if(query!=revision)return;pending=false;
            if(!found.isEmpty()) {
                candidates=new ArrayList<>();for(Candidate c:found)candidates.add(new Candidate(capitalization==2?c.text.toUpperCase(Locale.ROOT):capitalization==1?Character.toUpperCase(c.text.charAt(0))+c.text.substring(1):c.text,true,c.score));
                raw=candidates.get(0).text;preferred=0;traced=true;editor.composing(raw);
            }
            barrier=false;changed.run();drain();
        };
        if(decoder==null)done.accept(dictionary.englishTrace(points));else decoder.trace(dictionary,points,done);
    }
    private Decoder decoder;
    private Runnable changed=()->{};
    private long revision;
    private boolean pending,barrier,draining;
    private final ArrayDeque<Runnable> waiting=new ArrayDeque<>();
    public void decoder(Decoder value,Runnable changed) {this.decoder=value;this.changed=changed;}
    /** Commit actions wait for their matching prediction; newer typing remains responsive. */
    public boolean deferUntilReady(Runnable action,boolean requiresPrediction) {
        if(barrier || (requiresPrediction && pending)) {
            if(draining)waiting.addFirst(action);else waiting.addLast(action);barrier=true;return true;
        }
        return false;
    }
    private void drain() {
        draining=true;try {while(!waiting.isEmpty()) {barrier=false;waiting.removeFirst().run();if(barrier)break;}}
        finally {draining=false;}
        changed.run();
    }
    private void cancelPending() {revision++;pending=false;barrier=false;waiting.clear();}
    public CompositionEngine(Editor editor, Learning learning) { this.editor = editor; this.learning = learning; }
    public void dictionary(PhoneticDictionary dictionary) { this.dictionary = dictionary; refresh(); }
    public void start(boolean zhuyin, boolean literalField, boolean privateField, boolean direct) {
        start(zhuyin,literalField,privateField,direct,false);
    }
    public void start(boolean zhuyin, boolean literalField, boolean privateField, boolean direct, boolean englishMode) {
        cancelPending();raw = ""; context = ""; completionBoundary=false; clearAssistance(); this.zhuyin = zhuyin; this.literalField = literalField;
        this.privateField = privateField; this.direct = direct; this.englishMode=englishMode; refresh();
    }
    public void layout(boolean zhuyin) { commitDefault(false); this.zhuyin = zhuyin; refresh(); }
    public String raw() { return raw; }
    public String context() { return context; }
    public Intent intent() { return intent; }
    public List<Candidate> candidates() { return Collections.unmodifiableList(candidates); }
    public int preferred() { return preferred; }
    public boolean privateField() { return privateField; }
    public void type(int codePoint) {
        if(deferUntilReady(()->type(codePoint),!raw.isEmpty() && IntentClassifier.isZhuyin(codePoint)!=raw.codePoints().anyMatch(IntentClassifier::isZhuyin)))return;
        if (codePoint == ' ') { space(); return; }
        if (codePoint == '\n') { enter(); return; }
        clearAssistance();
        traced=false;
        String letter = new String(Character.toChars(codePoint));
        resolveCompletionBoundary(letter);
        if (direct) { editor.commit(letter); return; }
        boolean phonetic = IntentClassifier.isZhuyin(codePoint);
        boolean existingPhonetic = raw.codePoints().anyMatch(IntentClassifier::isZhuyin);
        if (!raw.isEmpty() && phonetic != existingPhonetic) commitDefault(false);
        // ASCII punctuation stays with Latin tokens so email, URLs and identifiers never lose raw input.
        if (!phonetic && !Character.isLetterOrDigit(codePoint) && (codePoint > 126 || codePoint < 33)) {
            commitDefault(false);
            editor.commit(letter); context = ""; refresh(); return;
        }
        if (raw.length() + letter.length() > 96) commitRaw(false);
        raw += letter; editor.composing(raw); refresh();
    }
    public void space() {
        space(System.nanoTime()/1000000);
    }
    public void space(long now) {
        if(deferUntilReady(()->space(now),true))return;
        completionBoundary=false;
        if(doubleSpace && englishMode && !literalField && !direct && raw.isEmpty() && spaceAt>=0 && now-spaceAt<=1000 && now>=spaceAt) {
            clearAssistance();editor.replacePrevious(1,". ");context="";refresh();return;
        }
        String spelling=raw;
        clearAssistance();
        if (direct || raw.isEmpty()) { editor.commit(" "); refresh(); return; }
        int last=raw.codePointBefore(raw.length());
        if(zhuyin && !literalField && !englishMode && last>=0x3105 && last<=0x3129) {
            raw+="ˉ"; editor.composing(raw); refresh(); return;
        }
        Candidate selected=candidates.isEmpty()?new Candidate(raw,true,0):candidates.get(preferred);
        commitDefault(true);
        if(englishMode && !literalField && spelling.matches("[A-Za-z]+(?:'[A-Za-z]+)*")) {
            spaceAt=now;
            if(!selected.text.equals(spelling)) {undoSpelling=spelling;undoOutput=selected.text+" ";}
        }
    }
    public void confirm() { if(deferUntilReady(this::confirm,true))return;clearAssistance();completionBoundary=false; commitDefault(false); }
    /** An explicit slide commits its literal output, independent of token inference. */
    public void literal(String text) { if(deferUntilReady(()->literal(text),true))return;clearAssistance();resolveCompletionBoundary(text); commitDefault(false); editor.commit(text); context=""; refresh(); }
    public void enter() { if(deferUntilReady(this::enter,true))return;clearAssistance();completionBoundary=false; commitDefault(false); editor.enter(); context = ""; refresh(); }
    private void resolveCompletionBoundary(String text) {
        if(completionBoundary) {
            completionBoundary=false;
            if(!text.isEmpty() && Character.isLetterOrDigit(text.codePointAt(0))) editor.commit(" ");
        }
    }
    public void commitRaw(boolean space) {
        clearAssistance();
        if (raw.isEmpty()) { if (space) editor.commit(" "); return; }
        commit(new Candidate(raw, true, 0), space, false);
    }
    public void backspace() {
        if(deferUntilReady(this::backspace,false))return;
        completionBoundary=false;
        if(!undoSpelling.isEmpty() && raw.isEmpty()) {
            String spelling=undoSpelling;int count=undoOutput.length();clearAssistance();
            raw=spelling;context="";editor.restoreSpelling(count,raw);refresh();preferred=0;return;
        }
        clearAssistance();
        if (raw.isEmpty()) { editor.delete(); context = ""; }
        else {
            raw = raw.substring(0, raw.offsetByCodePoints(raw.length(), -1));
            editor.composing(raw);
            if (raw.isEmpty()) editor.finish();
        }
        refresh();
    }
    public void select(int index) {
        if (index < 0 || index >= candidates.size()) return;
        clearAssistance();
        Candidate choice=candidates.get(index);
        boolean completed=englishMode && !literalField && (traced || !choice.text.equals(raw)) && choice.text.matches("[A-Za-z]+(?:'[A-Za-z]+)*");
        resolveCompletionBoundary(choice.text);
        commit(choice, false, !raw.isEmpty());
        completionBoundary=completed;
    }
    private void commitDefault(boolean withSpace) {
        if (raw.isEmpty()) return;
        Candidate c = candidates.isEmpty() ? new Candidate(raw, true, 0) : candidates.get(preferred);
        commit(c, withSpace && c.literal, false);
    }
    private void commit(Candidate c, boolean withSpace, boolean explicit) {
        if (explicit && !privateField) learning.choose(contextKey(), raw, c.text);
        editor.commit(c.text + (withSpace ? " " : ""));
        if(englishMode && !literalField && c.text.matches("[A-Za-z]+(?:'[A-Za-z]+)*")) {
            if(!privateField)learning.rememberEnglish(context,c.text.toLowerCase(Locale.ROOT));
            String[] words=(context+" "+c.text.toLowerCase(Locale.ROOT)).trim().split(" ");
            context=words.length>1?words[words.length-2]+" "+words[words.length-1]:words[0];
        } else context = c.literal || privateField ? "" : tail(context + c.text, 3);
        raw = ""; refresh();
    }
    private String contextKey() { return englishMode?"EN:"+context:context.isEmpty() ? "START_OR_LATIN" : context; }
    private static String tail(String text, int n) {
        return text.substring(text.offsetByCodePoints(text.length(), -Math.min(n, text.codePointCount(0, text.length()))));
    }
    /** Call after cursor movement, external edits or lifecycle changes; never rewrite text at the new cursor. */
    public void abandon() { cancelPending();clearAssistance();completionBoundary=false; editor.finish(); raw = ""; context = ""; refresh(); }
    public void refresh() {
        traced=false;
        long query=++revision;pending=false;
        candidates = new ArrayList<>(); preferred = 0;
        if (direct) return;
        if (raw.isEmpty()) {
            if(englishMode && !literalField && dictionary!=null) {
                if(!privateField)candidates.addAll(learning.predictEnglish(context));
                candidates.addAll(dictionary.englishPredictions(context));
                Set<String> seen=new HashSet<>();candidates.removeIf(c->!seen.add(c.text));
            }
            if (!privateField && dictionary != null && !literalField && !englishMode) candidates.addAll(dictionary.predict(context));
            return;
        }
        boolean bpmf = raw.codePoints().anyMatch(IntentClassifier::isZhuyin);
        intent = classifier.classify(raw, zhuyin, literalField || englishMode, dictionary);
        // Raw is always slot zero. The default highlight can point elsewhere without moving it.
        candidates.add(new Candidate(raw, true, 0));
        // Delay ambiguous punctuation so `.ming`, `/ming`, and `#ming` remain one literal token.
        // An isolated separator after Han still offers the Taiwan punctuation as its default.
        if (!literalField && !englishMode && raw.length() == 1 && !context.isEmpty()
                && Character.UnicodeScript.of(context.codePointBefore(context.length())) == Character.UnicodeScript.HAN) {
            int at = ",.?!:;".indexOf(raw.charAt(0));
            if (at >= 0) {
                candidates.add(new Candidate("，。？！：；".substring(at, at + 1), false, 0));
                preferred = 1; return;
            }
        }
        if(decoder!=null && dictionary!=null && !literalField && !englishMode) {
            pending=true;
            decoder.convert(dictionary,raw,bpmf,context,result->{
                if(query!=revision)return;
                pending=false;applyCandidates(new ArrayList<>(result));changed.run();drain();
            });
        } else applyCandidates(dictionary == null || literalField || englishMode ? new ArrayList<>() : dictionary.convert(raw, bpmf,context));
    }
    private void applyCandidates(List<Candidate> converted) {
        boolean bpmf=raw.codePoints().anyMatch(IntentClassifier::isZhuyin);
        if (!privateField && !literalField && !englishMode) converted.addAll(learning.custom(raw));
        if (!privateField) converted.sort(Comparator.comparingInt((Candidate c) -> learning.count(contextKey(), raw, c.text)).reversed()
            .thenComparing(Comparator.comparingDouble((Candidate c) -> c.score).reversed()));
        Set<String> seen = new HashSet<>(); seen.add(raw);
        for (Candidate c : converted) if (seen.add(c.text)) candidates.add(c);
        if (candidates.size() > 1) {
            int literalVotes = privateField ? 0 : learning.count(contextKey(), raw, raw);
            int chineseVotes = privateField ? 0 : learning.count(contextKey(), raw, candidates.get(1).text);
            if (intent == Intent.CHINESE_PHONETIC || ((intent == Intent.AMBIGUOUS || intent == Intent.LATIN_LITERAL) && chineseVotes > literalVotes)) preferred = 1;
            // Partial phonetics are valid Chinese input too. Preserve known
            // English words/completion prefixes and explicit literal recovery.
            if(!bpmf && !literalField && !englishMode && dictionary!=null
                    && raw.matches("[a-zv]+(?:'[a-zv]+)*") && raw.length()>1
                    && !IntentClassifier.technicalWord(raw) && !dictionary.isEnglish(raw) && dictionary.englishCompletions(raw).isEmpty()) preferred=1;
            if (literalVotes > chineseVotes) preferred = 0;
        }
        if (dictionary != null && !bpmf && !literalField && (intent == Intent.LATIN_LITERAL || intent == Intent.AMBIGUOUS))
            for (Candidate c : dictionary.englishCompletions(raw)) if (seen.add(c.text)) candidates.add(c);
        if(englishMode && !literalField && !privateField) {
            for(Candidate c:learning.custom(raw))if(seen.add(c.text))candidates.add(new Candidate(c.text,true,c.score));
            candidates.subList(1,candidates.size()).sort(Comparator.comparingInt((Candidate c)->learning.count(contextKey(),raw,c.text)).reversed().thenComparing(Comparator.comparingDouble((Candidate c)->c.score).reversed()));
        }
        if(englishMode && !literalField && dictionary!=null) {
            List<Candidate> corrections=dictionary.englishCorrections(raw);
            for(Candidate c:corrections)if(seen.add(c.text))candidates.add(c);
            if(autoCorrect && !corrections.isEmpty() && corrections.get(0).score>=100
                    && (corrections.size()==1 || corrections.get(0).score-corrections.get(1).score>=8)) {
                for(int i=1;i<candidates.size();i++)if(candidates.get(i).text.equals(corrections.get(0).text))preferred=i;
            }
        }
    }
}
