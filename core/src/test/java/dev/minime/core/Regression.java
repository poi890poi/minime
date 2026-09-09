package dev.minime.core;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public final class Regression {
    static int assertions;
    static PhoneticDictionary dictionary;
    static final List<Long> latencies = new ArrayList<>();
    static final class Editor implements CompositionEngine.Editor {
        String text = "", composing = "";
        int enters, deletes;
        public void composing(String s) { composing = s; }
        public void commit(String s) { text += s; composing = ""; }
        public void delete() { deletes++; if (!text.isEmpty()) text = text.substring(0, text.offsetByCodePoints(text.length(), -1)); }
        public void enter() { enters++; text += "\n"; }
        public void finish() { text += composing; composing = ""; }
    }
    static final class Memory implements Learning {
        Map<String,Integer> votes = new HashMap<>();
        public int count(String c,String r,String v) { return votes.getOrDefault(c+"|"+r+"|"+v,0); }
        public void choose(String c,String r,String v) { votes.put(c+"|"+r+"|"+v,count(c,r,v)+1); }
    }
    static CompositionEngine engine(Editor e, Learning learning, boolean zhuyin) {
        CompositionEngine c = new CompositionEngine(e, learning); c.dictionary(dictionary); c.start(zhuyin,false,false,false); return c;
    }
    static void equal(Object expected, Object actual, String label) {
        assertions++; if (!Objects.equals(expected, actual)) throw new AssertionError(label+" expected ["+expected+"] actual ["+actual+"]");
    }
    static void yes(boolean condition, String label) { equal(true, condition, label); }
    static void type(CompositionEngine c, String text) {
        for (int cp : text.codePoints().toArray()) {
            long before = System.nanoTime(); c.type(cp); latencies.add(System.nanoTime()-before);
            if (!c.raw().isEmpty()) equal(c.raw(), c.candidates().get(0).text, "raw candidate survives every key");
        }
    }
    static int find(CompositionEngine c, String value) {
        for (int i=0;i<c.candidates().size();i++) if (value.equals(c.candidates().get(i).text)) return i;
        throw new AssertionError("Missing candidate "+value+" for "+c.raw()+": "+c.candidates());
    }
    public static void main(String[] args) throws Exception {
        ReadingIndexParity.run();
        Path assets = Paths.get("app/src/main/assets");
        long start = System.nanoTime();
        dictionary = args.length>0?PhoneticDictionary.readBinary(Files.newInputStream(Paths.get(args[0]))):PhoneticDictionary.load(Files.newBufferedReader(assets.resolve("zh_tw.tsv")), Files.newBufferedReader(assets.resolve("en_us.tsv")), Files.newBufferedReader(assets.resolve("syllables.tsv")),Files.newBufferedReader(assets.resolve("context.tsv")));
        dictionary.englishSpelling(Files.newBufferedReader(assets.resolve("en_spelling.tsv")));
        ModeRegression.run();
        ModePriorityRegression.run();
        JapaneseBasicsRegression.run();
        PairedFormsRegression.run();
        System.out.printf(Locale.ROOT, "Dictionary startup %.0f ms%n", (System.nanoTime()-start)/1e6);
        for (String line : Files.readAllLines(Paths.get("core/src/test/resources/mixed-corpus.tsv"), StandardCharsets.UTF_8)) {
            if (line.startsWith("#") || line.isEmpty()) continue;
            String[] p = line.split("\t", -1);
            Editor e = new Editor(); CompositionEngine c = engine(e,Learning.NONE,p[1].equals("zhuyin"));
            String script = p[2].replace("<SP>", " ");
            while (!script.isEmpty()) {
                int pick = script.indexOf("<PICK:");
                if (pick < 0) { type(c, script); break; }
                type(c, script.substring(0,pick));
                int end = script.indexOf('>',pick);
                c.select(find(c,script.substring(pick+6,end)));
                script = script.substring(end+1);
            }
            equal(p[3], e.text, p[0]); equal("",c.raw(),p[0]+" ends committed");
            System.out.println("PASS corpus "+p[0]);
        }
        Editor e = new Editor(); Memory memory = new Memory(); CompositionEngine c = engine(e,memory,false);
        for (String word : Arrays.asList("pin","ming","shanghai")) {
            type(c,word); yes(c.candidates().size()>1,"Chinese alternative for "+word);
            c.select(0); equal(word,e.text,"literal choice"); e.text="";
        }
        type(c,"ming"); c.select(find(c,"明")); equal("明",e.text,"Chinese choice");
        c.start(false,false,false,false); type(c,"ming"); equal("明",c.candidates().get(c.preferred()).text,"local learning"); c.abandon();
        e = new Editor(); c = engine(e,Learning.NONE,false);
        type(c,"meeting"); c.backspace(); equal("meetin",c.raw(),"one-letter backspace"); type(c,"g"); c.space(); equal("meeting ",e.text,"literal space");
        c.backspace(); equal("meeting",e.text,"delete just space");
        type(c,"zhongwen"); c.abandon(); equal("meetingzhongwen",e.text,"cursor move preserves raw without conversion"); equal("",c.raw(),"cursor resets");
        e = new Editor(); c = engine(e,Learning.NONE,true);
        type(c,"ㄋㄧˇ"); c.backspace(); equal("ㄋㄧ",c.raw(),"tone deletion"); c.backspace(); equal("ㄋ",c.raw(),"symbol deletion");
        e = new Editor(); c = engine(e,memory,false); c.start(false,true,true,true); type(c,"ming@123");
        equal("ming@123",e.text,"secure direct input"); equal("",c.raw(),"no secure buffer"); yes(c.candidates().isEmpty(),"no secure candidates");
        int votes = memory.votes.size(); c.space(); c.enter(); equal(votes,memory.votes.size(),"no secure learning");
        e = new Editor(); c = engine(e,Learning.NONE,false); c.start(false,true,false,false); type(c,"ming"); c.space(); equal("ming ",e.text,"URL field literal");
        e = new Editor(); c = engine(e,Learning.NONE,false); type(c,"zhongwen"); c.enter(); equal("中文\n",e.text,"Enter commits then acts"); equal(1,e.enters,"one editor action");
        type(c,"ming"); c.commitRaw(false); equal("中文\nming",e.text,"raw commit");
        e = new Editor(); c = engine(e,Learning.NONE,false); type(c,"zhongwen"); c.space(); type(c,","); c.space(); equal("中文，",e.text,"Chinese punctuation");
        type(c,"API v2.0"); c.space(); equal("中文，API v2.0 ",e.text,"ASCII technical punctuation");
        e = new Editor(); c = engine(e,Learning.NONE,false); type(c,"nihao"); c.select(find(c,"你好"));
        type(c,"😀"); c.backspace(); equal("",c.raw(),"supplementary code point deletion");
        // Independent coverage probes outside acceptance examples, without ranking overrides.
        for (String[] pair : new String[][]{{"diannao","電腦"},{"ruanti","軟體"},{"taiwan","臺灣"},{"xuexiao","學校"},{"ㄊㄞˊㄅㄟˇ","台北"}}) {
            boolean bpmf=IntentClassifier.isZhuyin(pair[0].codePointAt(0));
            yes(dictionary.convert(pair[0],bpmf).stream().anyMatch(v -> v.text.equals(pair[1])),"source coverage "+pair[1]);
        }
        yes(dictionary.convert("xi'an",false).stream().anyMatch(v -> v.text.equals("西安")),"explicit syllable boundary");
        yes(dictionary.convert("xi'an",false).stream().noneMatch(v -> v.text.equals("先")),"apostrophe prevents merged syllable");
        yes(dictionary.convert("wo'xiang'chifan",false).stream().anyMatch(v -> v.text.equals("我想吃飯")),"phrase segmentation");
        e=new Editor(); memory=new Memory(); c=engine(e,memory,false);
        c.start(false,false,true,false); type(c,"ming"); c.select(find(c,"明")); equal(0,memory.votes.size(),"no-personalized-learning choice ignored");
        e=new Editor(); c=engine(e,Learning.NONE,false); type(c,"ming"); c.select(0); c.space(); type(c,"zhongwen"); c.space();
        equal("ming 中文",e.text,"literal recovery does not lock English");
        for(String token:Arrays.asList(".ming","/ming","#ming","@ming","'ming'")) {
            e=new Editor(); c=engine(e,Learning.NONE,false); type(c,"zhongwen "+token+" ");
            equal("中文"+token+" ",e.text,"technical token prefix after Chinese: "+token);
        }
        e=new Editor(); c=engine(e,Learning.NONE,false); type(c,"nihao？");
        equal("你好？",e.text,"explicit Taiwan punctuation commits phonetic token first");
        equal("",c.raw(),"punctuation ends composition");
        e=new Editor(); c=engine(e,Learning.NONE,false); type(c,"meeting！");
        equal("meeting！",e.text,"explicit punctuation after literal token");
        e=new Editor(); c=engine(e,Learning.NONE,true); type(c,"ㄓㄨ"); c.space();
        equal("ㄓㄨˉ",c.raw(),"Space enters first tone before accepting"); equal("",e.text,"first-tone Space does not commit");
        yes(c.candidates().stream().anyMatch(v->v.text.equals("朱")),"first-tone reading remains reachable");
        yes(c.candidates().stream().noneMatch(v->v.text.equals("主")),"explicit first tone excludes third tone");
        c.space(); equal("",c.raw(),"second Space accepts first-tone composition");
        yes(dictionary.convert("ㄒㄧˉㄢˉ",true).stream().anyMatch(v->v.text.equals("西安")),"first-tone syllable boundary allows 西安");
        yes(dictionary.convert("ㄒㄧˉㄢˉ",true).stream().noneMatch(v->v.text.equals("先")),"first-tone boundary prevents merging as 先");
        e=new Editor(); c=engine(e,Learning.NONE,false); type(c,"nihao"); c.literal("m"); c.literal("ing"); c.space();
        equal("你好ming ",e.text,"literal slide accepts composition then commits exact text"); equal("",c.raw(),"literal slide leaves no composing token");
        type(c,"zhongwen"); c.confirm(); equal("你好ming 中文",e.text,"confirmation does not send Enter"); equal(0,e.enters,"confirmation action boundary");
        // Real source readings, independently observed reference behaviors; no expected words in runtime rules.
        for(String[] pair:new String[][]{{"nh","你好"},{"nih","你好"},{"nhao","你好"},{"jt","今天"},{"jtian","今天"},
                {"srf","輸入法"},{"shrf","輸入法"},{"srufa","輸入法"},{"mwt","沒問題"},{"mwenti","沒問題"},
                {"wxsrf","我想輸入法"},{"sh","生活"},{"n'h","你好"},{"ni'h","你好"},{"meiwen","沒問題"}}) {
            e=new Editor(); c=engine(e,Learning.NONE,false); type(c,pair[0]);
            equal("",e.text,"partial phonetics remain uncommitted");
            c.select(find(c,pair[1])); equal(pair[1],e.text,"select abbreviated phrase "+pair[0]);
        }
        e=new Editor(); c=engine(e,Learning.NONE,false); type(c,"srf"); c.backspace();
        equal("sr",c.raw(),"abbreviation backspace keeps exact input"); type(c,"ufa");
        c.select(find(c,"輸入法")); equal("輸入法",e.text,"refine an abbreviation without restarting");
        yes(dictionary.convert("n'h",false).stream().noneMatch(v->v.text.codePointCount(0,v.text.length())==1),"apostrophe forces two syllables even when abbreviated");
        e=new Editor(); memory=new Memory(); c=engine(e,memory,false); type(c,"nh"); c.select(find(c,"你好"));
        c.start(false,false,false,false); type(c,"nh"); c.space(); equal("你好你好",e.text,"explicit abbreviation choice can become local default");
        c.start(false,false,true,false); type(c,"nh");
        equal(dictionary.convert("nh",false).get(0).text,c.candidates().get(c.preferred()).text,"private field uses generic abbreviation ranking");
        e=new Editor(); c=engine(e,Learning.NONE,false); type(c,"nh"); c.select(0); c.space(); equal("nh ",e.text,"abbreviation exact recovery");
        for(String input:Arrays.asList("s".repeat(32),"w".repeat(96),"abcdefghijklmnopqrstuvwxyz".repeat(3))) {
            long begun=System.nanoTime(); dictionary.convert(input,false);
            yes(System.nanoTime()-begun<2_000_000_000L,"bounded ambiguous input "+input.length());
        }
        ShiftState shift=new ShiftState();
        shift.tap(1000,300);yes(shift.upper() && !shift.locked(),"one tap gives one-shot Shift");
        shift.tap(1200,300);yes(shift.locked(),"double tap locks caps");
        shift.consume();yes(shift.upper() && shift.locked(),"caps survive letters");
        shift.tap(1300,300);yes(!shift.upper(),"one tap unlocks caps");
        shift.tap(2000,300);shift.tap(2400,300);yes(!shift.upper(),"slow taps cancel rather than lock");
        shift.tap(3000,300);shift.interrupt();shift.tap(3100,300);yes(!shift.locked(),"intervening action cancels double tap");
        shift.reset();shift.tap(4000,300);shift.consume();yes(!shift.upper(),"one-shot ends after a letter");
        shift.hold();yes(shift.locked(),"hold shortcut retained");shift.reset();yes(!shift.upper(),"new field resets caps");
        for(String text:Arrays.asList("，","。","…","😀","❤️","👍🏽","👨‍👩‍👧‍👦","🇹🇼")) {
            e=new Editor();c=engine(e,Learning.NONE,false);type(c,"nihao");c.literal(text);
            equal("你好"+text,e.text,"whole symbol commits after composition: "+text);equal("",c.raw(),"no residual symbol composition");
        }
        yes(dictionary.englishCompletions("PRONUN").stream().anyMatch(v->v.text.equals("PRONUNCIATION")),"all-caps completion retains caps");
        for(String prefix:Arrays.asList("pronun","Pronun","PRONUN","keybo","translat")) {
            e=new Editor();c=engine(e,Learning.NONE,false);c.start(false,false,false,false,true);type(c,prefix);
            yes(c.candidates().size()>1,"explicit English has completions: "+prefix);
            yes(c.candidates().stream().allMatch(v->v.literal),"English alternatives stay literal");
            yes(dictionary.englishCompletions(prefix).stream().allMatch(v->v.text.startsWith(prefix)),"English completions preserve spelling and case");
            equal(0,c.preferred(),"Space preserves exact English prefix");
            String chosen=c.candidates().get(1).text;c.select(1);c.space();equal(chosen+" ",e.text,"explicit completion then real Space");
        }
        for(String token:Arrays.asList("nihao","teh","camelCase","HTTPServer","v2","a@b.com","https://example.com","don't")) {
            e=new Editor();c=engine(e,Learning.NONE,true);c.start(true,false,false,false,true);type(c,token);c.space();
            equal(token+" ",e.text,"English retains spelling with saved Zhuyin layout");
        }
        yes(dictionary.englishCompletions("pRo").isEmpty(),"mixed-case identifiers receive no case-changing completion");
        for(boolean secure:new boolean[]{false,true}) {
            e=new Editor();c=engine(e,Learning.NONE,false);c.start(false,true,secure,secure,true);type(c,"pronun");
            equal(secure?0:1,c.candidates().size(),"restricted English fields suppress suggestions");
        }
        e=new Editor();memory=new Memory();c=engine(e,memory,false);c.start(false,false,true,false,true);type(c,"pronun");
        c.select(find(c,"pronunciation"));equal(0,memory.votes.size(),"private English selection never learns");
        c.start(false,false,false,false);type(c,"nihao");yes(c.candidates().stream().anyMatch(v->v.text.equals("你好")),"return to Chinese conversion");
        GapRegression.run();
        PartialSelectionRegression.run();
        EnglishCandidateRegression.run();
        MixedLearningRegression.run();
        CapitalizedVocabularyRegression.run();
        PinyinContinuityRegression.run();
        CandidateSelectionRegression.run();
        AddonLearningRegression.run();
        SharedPartialRegression.run();
        AddonAsyncRegression.run();
        ApostropheRegression.run();
        ProvenanceRegression.run();
        CandidateMergeRegression.run();
        GlyphOrderRegression.run();
        Collections.sort(latencies);
        System.out.printf(Locale.ROOT,"PASS %d assertions; desktop key processing p50=%.2f ms p95=%.2f ms max=%.2f ms (%d keys; not Android latency)%n", assertions,latencies.get(latencies.size()/2)/1e6,latencies.get(latencies.size()*95/100)/1e6,latencies.get(latencies.size()-1)/1e6,latencies.size());
    }
}
