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
    private boolean afterLatin;
    private boolean zhuyin, literalField, privateField, direct, englishMode;
    private InputMode inputMode=InputMode.CHINESE;
    private boolean pairedTaiwanese=true,hanPrimary;
    private boolean focusedLearning=true;
    public void focusedLearning(boolean enabled) {
        if(focusedLearning==enabled)return;
        if(deferUntilReady(()->focusedLearning(enabled),false))return;
        focusedLearning=enabled;refresh();changed.run();
    }
    public void pairedTaiwanese(boolean enabled,boolean primaryHan) {
        if(pairedTaiwanese==enabled && hanPrimary==primaryHan)return;
        if(deferUntilReady(()->pairedTaiwanese(enabled,primaryHan),false))return;
        pairedTaiwanese=enabled;hanPrimary=primaryHan;compositionId++;refresh();changed.run();
    }
    private boolean completionBoundary;
    private boolean committedEnglishWord;
    private boolean autoCorrect, doubleSpace, automaticCorrection;
    private long spaceAt=-1;
    private String undoSpelling="", undoOutput="";
    public void englishOptions(boolean correction,boolean period) { autoCorrect=correction;doubleSpace=period;refresh(); }
    private void clearAssistance() { spaceAt=-1;undoSpelling="";undoOutput=""; }
    private Intent intent = Intent.LATIN_LITERAL;
    private List<Candidate> candidates = new ArrayList<>();
    private int preferred;
    private final PhraseSession phraseSession=new PhraseSession();
    private boolean phraseLearning;
    public void phraseLearning(boolean enabled) {if(phraseLearning!=enabled) {phraseLearning=enabled;phraseSession.clear();refresh();}}
    private void acceptedPhrase(String reading,Candidate choice) {
        if(!phraseLearning || privateField || literalField || englishMode || choice.literal || choice.supplemental) {phraseSession.clear();return;}
        phraseSession.accept(reading,choice.text,learning::observePhrase);
    }
    private AddonDictionary addons=AddonDictionary.EMPTY;
    private boolean addonsPending;
    private Set<String> enabledAddons=Collections.emptySet();
    /** Keep owned spelling and queued acceptance while a cold language loads. */
    public void awaitAddons(Set<String> enabled) {
        addons=AddonDictionary.EMPTY;enabledAddons=new HashSet<>(enabled);addonsPending=true;refresh();
    }
    public void addons(AddonDictionary dictionary,Set<String> enabled) {
        addons=Objects.requireNonNull(dictionary);enabledAddons=new HashSet<>(enabled);addonsPending=false;refresh();
        if(!pending && !draining)drain();
    }
    public interface Decoder {
        default void cancel() {}
        void convert(PhoneticDictionary dictionary,String raw,boolean zhuyin,String context,java.util.function.Consumer<List<Candidate>> result);
        default void query(PhoneticDictionary dictionary,String raw,boolean zhuyin,String context,boolean phonetic,
                AddonDictionary addons,Set<String> enabled,java.util.function.Consumer<List<Candidate>> result) {
            java.util.function.Consumer<List<Candidate>> done=base->{
                List<Candidate> combined=new ArrayList<>(base);combined.addAll(addons.lookup(raw,enabled));result.accept(combined);
            };
            if(phonetic)convert(dictionary,raw,zhuyin,context,done);else done.accept(Collections.emptyList());
        }
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
    private long revision, compositionId;
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
    private void cancelPending() {if(decoder!=null)decoder.cancel();revision++;compositionId++;pending=false;barrier=false;waiting.clear();phraseSession.clear();}
    public CompositionEngine(Editor editor, Learning learning) { this.editor = editor; this.learning = learning; }
    public void dictionary(PhoneticDictionary dictionary) { this.dictionary = dictionary; refresh(); }
    public void start(boolean zhuyin, boolean literalField, boolean privateField, boolean direct) {
        start(zhuyin,literalField,privateField,direct,false);
    }
    public void start(boolean zhuyin, boolean literalField, boolean privateField, boolean direct, boolean englishMode) {
        cancelPending();raw = ""; context = ""; afterLatin=false;completionBoundary=false;committedEnglishWord=false; clearAssistance(); this.zhuyin = zhuyin; this.literalField = literalField;
        this.privateField = privateField; this.direct = direct; this.englishMode=englishMode; inputMode=englishMode?InputMode.ENGLISH:InputMode.CHINESE; refresh();
    }
    public InputMode inputMode() {return inputMode;}
    /** Reinterpret owned spelling without accepting it or reusing old candidate gestures. */
    public void switchMode(InputMode mode,boolean literal) {
        Objects.requireNonNull(mode);
        if(deferUntilReady(()->switchMode(mode,literal),false))return;
        if(inputMode==mode && literalField==literal)return;
        revision++;compositionId++;pending=false;phraseSession.clear();clearAssistance();
        inputMode=mode;englishMode=mode.english();literalField=literal;
        context="";afterLatin=false;completionBoundary=false;committedEnglishWord=false;
        refresh();changed.run();
    }
    public void layout(boolean zhuyin) { commitDefault(false); this.zhuyin = zhuyin; refresh(); }
    public String raw() { return raw; }
    public String context() { return context; }
    public Intent intent() { return intent; }
    public List<Candidate> candidates() { return Collections.unmodifiableList(candidates); }
    public int preferred() { return preferred; }
    /** Presentation may keep completed suggestions while this query runs; acceptance may not. */
    public boolean predictionPending() { return pending; }
    public long compositionId() { return compositionId; }
    public boolean privateField() { return privateField; }
    public void type(int codePoint) {
        if(deferUntilReady(()->type(codePoint),!raw.isEmpty() && IntentClassifier.isZhuyin(codePoint)!=raw.codePoints().anyMatch(IntentClassifier::isZhuyin)))return;
        if (codePoint == ' ') { space(); return; }
        if (codePoint == '\n') { enter(); return; }
        clearAssistance();
        traced=false;
        String letter = new String(Character.toChars(codePoint));
        resolveCompletionBoundary(letter);
        committedEnglishWord=false;
        if (direct) { editor.commit(letter); return; }
        boolean phonetic = IntentClassifier.isZhuyin(codePoint);
        boolean existingPhonetic = raw.codePoints().anyMatch(IntentClassifier::isZhuyin);
        if (!raw.isEmpty() && phonetic != existingPhonetic) commitDefault(false);
        // ASCII punctuation stays with Latin tokens so email, URLs and identifiers never lose raw input.
        if (!phonetic && !Character.isLetterOrDigit(codePoint) && (codePoint > 126 || codePoint < 33)) {
            commitDefault(false);
            phraseSession.clear();
            editor.commit(letter); context = "";afterLatin=false; refresh(); return;
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
        if (direct || raw.isEmpty()) { phraseSession.clear();editor.commit(" ");if(committedEnglishWord && !literalField && !direct)spaceAt=now;committedEnglishWord=false;refresh(); return; }
        int last=raw.codePointBefore(raw.length());
        if(zhuyin && !literalField && !englishMode && last>=0x3105 && last<=0x3129) {
            raw+="ˉ"; editor.composing(raw); refresh(); return;
        }
        Candidate selected=candidates.isEmpty()?new Candidate(raw,true,0):candidates.get(preferred);
        commitDefault(true);
        if((englishMode || selected.literal) && !literalField && spelling.matches("[A-Za-z]+(?:'[A-Za-z]+)*")) {
            spaceAt=now;
            if(!selected.text.equals(spelling)) {undoSpelling=spelling;undoOutput=selected.text+" ";}
        }
    }
    public void confirm() { if(deferUntilReady(this::confirm,true))return;clearAssistance();completionBoundary=false; commitDefault(false); }
    /** An explicit slide commits its literal output, independent of token inference. */
    public void literal(String text) { if(deferUntilReady(()->literal(text),true))return;clearAssistance();resolveCompletionBoundary(text); commitDefault(false);phraseSession.clear(); editor.commit(text);committedEnglishWord=false; context="";afterLatin=latinBoundary(text); refresh(); }
    public void enter() { if(deferUntilReady(this::enter,true))return;clearAssistance();completionBoundary=false; commitDefault(false);phraseSession.clear(); editor.enter();committedEnglishWord=false; context = "";afterLatin=false; refresh(); }
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
        phraseSession.clear();
        completionBoundary=false;
        committedEnglishWord=false;
        if(!undoSpelling.isEmpty() && raw.isEmpty()) {
            String spelling=undoSpelling;int count=undoOutput.length();clearAssistance();
            raw=spelling;context="";editor.restoreSpelling(count,raw);refresh();preferred=0;return;
        }
        clearAssistance();
        if (raw.isEmpty()) { editor.delete(); context = "";afterLatin=false; }
        else {
            raw = raw.substring(0, raw.offsetByCodePoints(raw.length(), -1));
            editor.composing(raw);
            if (raw.isEmpty()) editor.finish();
        }
        refresh();
    }
    public void select(int index) {
        if (index < 0 || index >= candidates.size()) return;
        selectChoice(candidates.get(index));
    }
    private void selectChoice(Candidate choice) {
        clearAssistance();
        if(partial(choice) && !literalField && !englishMode && !choice.literal) {
            compositionId++;
            String reading=raw.substring(0,choice.consumed),rest=raw.substring(choice.consumed).replaceFirst("^'+","");
            acceptedPhrase(reading,choice);
            if(!privateField)learning.choose(contextKey(),reading,choice.text);
            editor.commit(choice.text);context=privateField?"":tail(context+choice.text,3);afterLatin=false;
            raw=rest;completionBoundary=false;committedEnglishWord=false;
            editor.composing(raw);refresh();return;
        }
        boolean completed=englishMode && !literalField && (traced || !choice.text.equals(raw)) && choice.text.matches("[A-Za-z]+(?:'[A-Za-z]+)*");
        resolveCompletionBoundary(choice.text);
        commit(choice, false, !raw.isEmpty());
        completionBoundary=completed;
    }
    /** A displayed snapshot binds a choice by identity, never by a changing row index. */
    public void selectCandidate(Candidate displayed,long composition) {
        if(composition!=compositionId)return;
        if(deferUntilReady(()->selectCandidate(displayed,composition),true))return;
        for(int i=0;i<candidates.size();i++) {
            Candidate current=candidates.get(i);
            if(current.text.equals(displayed.text) && current.literal==displayed.literal
                    && (displayed.pair==null?current.pair==null:displayed.pair.same(current.pair))) {
                select(i);return;
            }
        }
    }
    /** Long press accepts exactly the advertised alternate of the held snapshot. */
    public void selectAlternative(Candidate displayed,long composition) {
        if(composition!=compositionId || displayed.pair==null || !pairedTaiwanese || !inputMode.taiwanese())return;
        if(deferUntilReady(()->selectAlternative(displayed,composition),true))return;
        for(Candidate current:candidates) {
            if(current.text.equals(displayed.text) && current.literal==displayed.literal
                    && displayed.pair.same(current.pair)) {selectChoice(current.alternative());return;}
        }
    }
    private boolean partial(Candidate c) {
        return c.consumed>0 && c.consumed<raw.length() && raw.matches("[a-zv]+(?:'[a-zv]+)*");
    }
    private void commitDefault(boolean withSpace) {
        if (raw.isEmpty()) return;
        Candidate c = candidates.isEmpty() || (automaticCorrection && !withSpace) ? new Candidate(raw, true, 0) : candidates.get(preferred);
        if(partial(c))c=new Candidate(raw,true,0);
        commit(c, withSpace && c.literal, false);
    }
    private void commit(Candidate c, boolean withSpace, boolean explicit) {
        acceptedPhrase(raw,c);
        if(explicit && !privateField && !literalField) {
            if(focused(c) && focusedLearning)learning.choose("FOCUS:"+c.pack,raw,choiceIdentity(c));
            else if(!c.supplemental)learning.choose(contextKey(),raw,c.text);
        }
        editor.commit(c.text + (withSpace ? " " : ""));
        committedEnglishWord=false;
        if(englishMode && !literalField && c.text.matches("[A-Za-z]+(?:'[A-Za-z]+)*")) {
            committedEnglishWord=!withSpace;
            if(!privateField && !c.supplemental)learning.rememberEnglish(context,c.text.toLowerCase(Locale.ROOT));
            String[] words=(context+" "+c.text.toLowerCase(Locale.ROOT)).trim().split(" ");
            context=words.length>1?words[words.length-2]+" "+words[words.length-1]:words[0];
        } else context = c.literal || privateField ? "" : tail(context + c.text, 3);
        afterLatin=c.literal && latinBoundary(c.text);
        raw = ""; refresh();
    }
    private boolean latinBoundary(String text) {return !englishMode && !literalField && text.matches("[A-Za-z]+(?:'[A-Za-z]+)*");}
    private String contextKey() {
        String base=englishMode?"EN:"+context:!context.isEmpty()?context:afterLatin?"AFTER_LATIN":"START_OR_LATIN";
        return !inputMode.pack.isEmpty()?"MODE:"+inputMode.id+":"+base:base;
    }
    private static String choiceIdentity(Candidate c) {return c.pair==null?c.text:c.pair.phonetic;}
    private int choiceVotes(Candidate c) {
        if(privateField)return 0;
        if(focused(c))return focusedLearning?learning.count("FOCUS:"+c.pack,raw,choiceIdentity(c)):0;
        return learning.count(contextKey(),raw,c.text);
    }
    private static String tail(String text, int n) {
        return text.substring(text.offsetByCodePoints(text.length(), -Math.min(n, text.codePointCount(0, text.length()))));
    }
    /** Call after cursor movement, external edits or lifecycle changes; never rewrite text at the new cursor. */
    public void abandon() { cancelPending();clearAssistance();completionBoundary=false;committedEnglishWord=false; editor.finish(); raw = ""; context = "";afterLatin=false; refresh(); }
    public void refresh() {
        if(decoder!=null)decoder.cancel();
        automaticCorrection=false;
        traced=false;
        long query=++revision;pending=false;
        candidates = new ArrayList<>(); preferred = 0;
        if (direct) return;
        if (raw.isEmpty()) {
            compositionId++;
            if(englishMode && !literalField && dictionary!=null) {
                if(!privateField)candidates.addAll(learning.predictEnglish(context));
                candidates.addAll(dictionary.englishPredictions(context));
                Set<String> seen=new HashSet<>();candidates.removeIf(c->!englishSuggestion(c) || !seen.add(c.text));
            }
            if (!privateField && dictionary != null && !literalField && inputMode.chineseEnabled()) candidates.addAll(dictionary.predict(context));
            return;
        }
        boolean bpmf = raw.codePoints().anyMatch(IntentClassifier::isZhuyin);
        intent = classifier.classify(raw, zhuyin, literalField || englishMode, dictionary,afterLatin);
        // An excluded language cannot suppress the active English dictionary.
        if(!inputMode.chineseEnabled() && !bpmf && intent==Intent.CHINESE_PHONETIC)intent=Intent.LATIN_LITERAL;
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
        // No-personalized-learning editors still need their static language model.
        // Secure/direct and literal fields are excluded independently.
        Set<String> packs=!literalField?inputMode.packs(enabledAddons):Collections.emptySet();
        if(addonsPending && !inputMode.pack.isEmpty() && !packs.isEmpty()) {pending=true;return;}
        if(decoder!=null && dictionary!=null && !literalField && (inputMode.chineseEnabled() || !packs.isEmpty())) {
            pending=true;
            decoder.query(dictionary,raw,bpmf,context,inputMode.chineseEnabled(),addons,packs,result->{
                if(query!=revision)return;
                pending=false;applyCandidates(new ArrayList<>(result));changed.run();drain();
            });
        } else {
            List<Candidate> found=dictionary == null || literalField || !inputMode.chineseEnabled() ? new ArrayList<>() : dictionary.convert(raw,bpmf,context);
            found.addAll(addons.lookup(raw,packs));applyCandidates(found);
        }
    }
    private void applyCandidates(List<Candidate> converted) {
        List<Candidate> addonMatches=new ArrayList<>();
        for(Candidate c:converted)if(c.supplemental)addonMatches.add(c);
        converted.removeIf(c->c.supplemental);
        boolean bpmf=raw.codePoints().anyMatch(IntentClassifier::isZhuyin);
        List<Candidate> custom=!privateField && !literalField?learning.custom(raw):Collections.emptyList();
        if(englishMode) {
            // Custom output has no language tag; literal controls acceptance, not language.
            // Filter before it can influence ranking or suppress English restoration.
            custom=new ArrayList<>(custom);custom.removeIf(c->!englishSuggestion(c));
        }
        converted.removeIf(c->c.consumed<0 || c.consumed>raw.length() || (c.consumed>0 && (c.literal || bpmf || !raw.matches("[a-zv]+(?:'[a-zv]+)*"))));
        if (!englishMode) converted.addAll(custom);
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
            if(conversionInput(bpmf)) preferred=1;
            if (literalVotes > chineseVotes) preferred = 0;
        }
        // Prefix candidates are explicit choices, never a whole-token Space default.
        if(partial(candidates.get(preferred))) {
            preferred=0;
            for(int i=1;i<candidates.size();i++)if(!partial(candidates.get(i))) {preferred=i;break;}
        }
        if (dictionary != null && inputMode.englishEnabled() && !bpmf && !literalField && (intent == Intent.LATIN_LITERAL || intent == Intent.AMBIGUOUS))
            for (Candidate c : dictionary.englishCompletions(raw,afterLatin || englishMode)) if (seen.add(c.text)) candidates.add(c);
        // With a literal default, expose English completions alongside Chinese
        // choices. Preserve each source's order; neither list buries the other.
        if(!englishMode && preferred==0 && !bpmf) {
            List<Candidate> latin=new ArrayList<>(),han=new ArrayList<>();
            for(int i=1;i<candidates.size();i++)(candidates.get(i).literal?latin:han).add(candidates.get(i));
            candidates.subList(1,candidates.size()).clear();
            for(int i=0;i<Math.max(latin.size(),han.size());i++) {
                if(i<latin.size())candidates.add(latin.get(i));
                if(i<han.size())candidates.add(han.get(i));
            }
        }
        if(englishMode && !literalField && !privateField) {
            for(Candidate c:custom)if(seen.add(c.text))candidates.add(new Candidate(c.text,true,c.score));
            candidates.subList(1,candidates.size()).sort(Comparator.comparingInt((Candidate c)->learning.count(contextKey(),raw,c.text)).reversed().thenComparing(Comparator.comparingDouble((Candidate c)->c.score).reversed()));
        }
        if(englishMode && !literalField && dictionary!=null) {
            List<Candidate> corrections=dictionary.englishCorrections(raw);
            for(Candidate c:corrections)if(seen.add(c.text))candidates.add(c);
            if(autoCorrect && !dictionary.validEnglishSpelling(raw) && !corrections.isEmpty() && corrections.get(0).score>=100
                    && (privateField || learning.count(contextKey(),raw,raw)<=learning.count(contextKey(),raw,corrections.get(0).text))
                    && (corrections.size()==1 || corrections.get(0).score-corrections.get(1).score>=8)) {
                for(int i=1;i<candidates.size();i++)if(candidates.get(i).text.equals(corrections.get(0).text)) {preferred=i;automaticCorrection=true;}
            }
        }
        if(!literalField) {
            List<Candidate> apostrophes=dictionary!=null && inputMode.englishEnabled() && !bpmf?dictionary.englishApostrophes(raw,englishMode):Collections.emptyList();
            if(!apostrophes.isEmpty() && dictionary.validEnglishSpelling(raw) && custom.isEmpty()
                    && (privateField || learning.count(contextKey(),raw,candidates.get(preferred).text)<=learning.count(contextKey(),raw,raw))) {
                preferred=0;automaticCorrection=false;
            }
            if(!apostrophes.isEmpty() && !dictionary.validEnglishSpelling(raw) && custom.isEmpty()
                    && (privateField || learning.count(contextKey(),raw,raw)<=learning.count(contextKey(),raw,apostrophes.get(0).text))) {
                Candidate restored=apostrophes.get(0);int existing=-1;
                for(int i=1;i<candidates.size();i++)if(candidates.get(i).text.equals(restored.text)) {existing=i;break;}
                if(existing<0) {candidates.add(restored);existing=candidates.size()-1;}
                preferred=existing;automaticCorrection=true;
            }
            Candidate defaultChoice=candidates.get(preferred);
            int insertion=1;
            List<Candidate> supplements=new ArrayList<>(custom);
            if(!privateField && phraseLearning && inputMode.chineseEnabled())supplements.addAll(learning.phrases(raw));
            // Dedicated modes express language preference, not merely pack scope.
            // Keep explicit overrides and apostrophe recovery, then use the
            // selected language's full and incomplete matches in source order.
            if(!inputMode.pack.isEmpty()) {
                // Stable personal ordering inside each full/incomplete group.
                // With no evidence, keep the existing source order.
                if(!privateField && focusedLearning)addonMatches.sort(Comparator.comparingInt(this::choiceVotes).reversed());
                for(Candidate c:addonMatches)if(focused(c) && !c.incomplete)supplements.add(c);
                for(Candidate c:addonMatches)if(focused(c) && c.incomplete)supplements.add(c);
            }
            // An already leading, source-attested full Chinese reading is not
            // a decoder guess. Keep it ahead of optional dictionary alternatives.
            if(!englishMode && dictionary!=null && candidates.size()>1 && dictionary.exactChinese(raw,bpmf,candidates.get(1).text))supplements.add(candidates.get(1));
            supplements.addAll(apostrophes);
            for(Candidate c:addonMatches)if(!c.incomplete)supplements.add(c);
            for(Candidate c:addonMatches)if(c.incomplete)supplements.add(c);
            Set<String> promoted=new HashSet<>();int partialPreviews=0;
            List<Candidate> unrankedGlyphs=new ArrayList<>();
            for(Candidate c:supplements) {
                // Slot zero owns literal recovery, not a dictionary identity.
                // Equal spelling must retain an add-on's pack, paired output
                // and acceptance/learning semantics alongside the raw choice.
                if((c.text.equals(raw) && !c.supplemental) || !promoted.add(c.text))continue;
                int existing=-1;for(int i=1;i<candidates.size();i++)if(candidates.get(i).text.equals(c.text)) {existing=i;break;}
                // A second source is not evidence that an already attested base
                // entry is more frequent. Preserve its established homophone
                // rank, including after English completion interleaving.
                if(c.supplemental && !focused(c) && existing>=0 && !candidates.get(existing).supplemental && dictionary!=null
                        && (dictionary.exactChinese(raw,bpmf,c.text)
                            || (candidates.get(existing).consumed==0 && c.text.codePointCount(0,c.text.length())==1)))continue;
                // Static dictionaries supply identity/readings, not comparable
                // glyph frequencies. A novel Han glyph must not outrank the
                // decoder's established whole-input glyph alternatives either.
                if(c.supplemental && !focused(c) && existing<0 && hanGlyph(c.text) && candidates.stream().anyMatch(base->
                        !base.supplemental && !base.literal && base.consumed==0 && hanGlyph(base.text))) {
                    unrankedGlyphs.add(c);continue;
                }
                // One early alternate previews incomplete dictionary matches;
                // the rest do not displace the primary decoder's whole first row.
                if(c.incomplete && !focused(c))insertion=Math.max(insertion,Math.min(partialPreviews==0?3:9,candidates.size()));
                if(existing>=0 && existing<insertion)continue;
                Candidate value=existing>=0 && !focused(c) && (!c.supplemental || candidates.get(existing)==defaultChoice)?candidates.get(existing):c;
                if(existing>=0)candidates.remove(existing);
                candidates.add(insertion++,value);
                if(c.incomplete && !focused(c))partialPreviews++;
            }
            for(Candidate c:unrankedGlyphs) {
                int after=1;
                for(int i=1;i<candidates.size();i++)if(candidates.get(i).consumed==0 && hanGlyph(candidates.get(i).text))after=i+1;
                candidates.add(after,c);
            }
            preferred=candidates.indexOf(defaultChoice);
            if(preferred<0)for(int i=0;i<candidates.size();i++)if(candidates.get(i).text.equals(defaultChoice.text)) {preferred=i;break;}
            // Focused defaults use their language-scoped votes below. The
            // legacy text-keyed fallback cannot distinguish a lexical twin
            // from an explicit raw choice with exactly the same spelling.
            if(preferred==0 && !addonMatches.isEmpty() && candidates.size()>1 && !focused(candidates.get(1)) && !candidates.get(1).incomplete && conversionInput(bpmf) && !dictionary.validEnglishSpelling(raw)
                    && (privateField || learning.count(contextKey(),raw,raw)<=learning.count(contextKey(),raw,candidates.get(1).text)))preferred=1;
            // Candidate ordering and automatic acceptance share one winner.
            // Raw recovery stays available; choices consuming only part of the
            // current spelling still require an explicit tap.
            // A dedicated language owns the default, including collisions with
            // valid English spellings, contractions and Mandarin glyphs. Explicit
            // raw recovery, manual entries and literal/private field policy remain.
            boolean focusAvailable=candidates.stream().skip(1).anyMatch(this::focused);
            if(focusAvailable && (privateField || learning.count(contextKey(),raw,raw)<=choiceVotes(candidates.get(1)))) {
                preferred=1;automaticCorrection=false;
            }
            if(preferred>0 && !automaticCorrection) {
                preferred=0;
                for(int i=1;i<candidates.size();i++)if(!partial(candidates.get(i))) {preferred=i;break;}
            }
        }
        for(int i=0;i<candidates.size();i++) {
            Candidate c=candidates.get(i);
            if(c.pair!=null)candidates.set(i,pairedTaiwanese && inputMode.taiwanese()?c.primary(hanPrimary):c.paired(null));
        }
    }
    private boolean focused(Candidate candidate) {
        return !inputMode.pack.isEmpty() && inputMode.pack.equals(candidate.pack);
    }
    private static boolean englishSuggestion(Candidate candidate) {
        if(!candidate.pack.isEmpty())return false;
        for(int at=0;at<candidate.text.length();) {
            int cp=candidate.text.codePointAt(at);at+=Character.charCount(cp);
            Character.UnicodeScript script=Character.UnicodeScript.of(cp);
            if(script!=Character.UnicodeScript.LATIN && script!=Character.UnicodeScript.COMMON && script!=Character.UnicodeScript.INHERITED)return false;
        }
        return true;
    }
    private static boolean hanGlyph(String text) {
        return text.codePointCount(0,text.length())==1 && Character.UnicodeScript.of(text.codePointAt(0))==Character.UnicodeScript.HAN;
    }
    private boolean conversionInput(boolean bpmf) {
        return !bpmf && !literalField && !englishMode && dictionary!=null
            && raw.matches("[a-zv]+(?:'[a-zv]+)*") && raw.length()>1
            && !IntentClassifier.technicalWord(raw) && !dictionary.isEnglish(raw,afterLatin) && dictionary.englishCompletions(raw,afterLatin).isEmpty();
    }
}
