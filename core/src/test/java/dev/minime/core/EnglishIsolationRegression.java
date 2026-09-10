package dev.minime.core;

import java.io.*;
import java.util.*;
import java.util.function.Consumer;
import static dev.minime.core.Regression.*;

/** Scope mechanics with synthetic source records, not language-quality examples. */
public final class EnglishIsolationRegression {
    private static final Set<String> ALL=new HashSet<>(Arrays.asList("taiwan","geography","poj","japanese"));
    private static final String[] FOREIGN={"自訂詞","かな","カナ","ｶﾅ","漢字かな","API中文","\uD840\uDC00","ㄅㄆ"};
    private static final String[] LATIN={"custom English","can't","café","cafe\u0301","API_v2","https://example.test/a","OK 👍"};
    private static PhoneticDictionary tiny;
    private static CompositionEngine create(Editor e,Learning l,boolean english) {
        CompositionEngine c=new CompositionEngine(e,l);c.dictionary(tiny);c.start(false,false,false,false,english);return c;
    }
    private static Learning manual() {
        return new Learning() {
            public int count(String c,String r,String v){return Arrays.asList(FOREIGN).contains(v)?100:0;}
            public void choose(String c,String r,String v){}
            public List<Candidate> custom(String r) {
                if(!r.equals("fixture"))return Collections.emptyList();
                List<Candidate> result=new ArrayList<>();
                for(String text:FOREIGN)for(boolean literal:new boolean[]{false,true})result.add(new Candidate(text,literal,200));
                for(String text:LATIN)result.add(new Candidate(text,false,0));
                // Script alone cannot distinguish a tagged romanized Japanese entry.
                result.add(Candidate.supplement("romaji",300).inPack("japanese"));
                return Collections.unmodifiableList(result);
            }
            public List<Candidate> phrases(String r){return Collections.singletonList(new Candidate("學習詞",false,900));}
            public List<Candidate> predictEnglish(String context) {
                return context.isEmpty()?Collections.emptyList():Arrays.asList(new Candidate("かな",true,900),new Candidate("中文",true,800),new Candidate("next",true,10));
            }
        };
    }
    public static void main(String[] args)throws Exception {run();System.out.println("PASS English isolation assertions: "+assertions);}
    static void run()throws Exception {
        tiny=PhoneticDictionary.load(new StringReader(""),new StringReader("fixtureword\t100\nhello\t100\n"),new StringReader(""));
        AddonDictionary addon=AddonDictionary.read(new StringReader("taiwan\tfixture\t文化\ttest\tfixture\ngeography\tfixture\t山岳\ttest\tfixture\npoj\tfixture\ttâi-gí\ttest\tfixture\njapanese\tfixture\tかな\ttest\tfixture\n"));
        Editor editor=new Editor();CompositionEngine c=create(editor,Learning.NONE,true);c.addons(addon,ALL);
        c.decoder((d,r,b,x,done)->{throw new AssertionError("English must not query conversion or add-ons");},()->{});
        type(c,"fixture");equal(Arrays.asList("fixture","fixtureword"),texts(c),"configured foreign packs never enter English");
        c.space();equal("fixture ",editor.text,"English Space does not convert");

        Learning saved=manual();editor=new Editor();c=create(editor,saved,true);c.phraseLearning(true);type(c,"fixture");
        for(String text:FOREIGN)yes(index(c,text)<0,"English excludes non-Latin custom output even if marked literal: "+text);
        yes(index(c,"romaji")<0,"source-tagged foreign romanization excluded");
        yes(index(c,"學習詞")<0,"learned conversion phrases stay out of English");
        for(String text:LATIN)yes(index(c,text)>0,"English custom spelling, accents and technical text preserved: "+text);
        c.space();equal("fixture ",editor.text,"foreign choice votes cannot change English Space");
        for(String text:LATIN) {
            editor=new Editor();c=create(editor,saved,true);type(c,"fixture");c.select(index(c,text));equal(text,editor.text,"English custom selection remains available");
        }
        c=create(new Editor(),saved,true);type(c,"hello");c.space();
        equal(Collections.singletonList("next"),texts(c),"foreign saved next-word predictions excluded");
        c.abandon();equal(0,c.candidates().size(),"editor boundary clears prediction context");

        // Scope is reversible; saved values must not be deleted or rewritten.
        for(InputMode mode:InputMode.values())if(!mode.english()) {
            c=create(new Editor(),saved,false);c.switchMode(mode,false);type(c,"fixture");
            for(String text:FOREIGN)yes(index(c,text)>0,"explicit custom output retained outside English");
        }
        c=create(new Editor(),saved,false);c.addons(addon,ALL);c.switchMode(InputMode.JAPANESE_ENGLISH,false);type(c,"fixture");
        Candidate old=c.candidates().get(index(c,"かな"));long id=c.compositionId();
        c.switchMode(InputMode.ENGLISH,false);yes(index(c,"かな")<0,"switch clears foreign custom and pack results");
        c.selectCandidate(old,id);equal("fixture",c.raw(),"old candidate tap cannot accept across mode switch");
        c.switchMode(InputMode.JAPANESE_ENGLISH,false);yes(index(c,"かな")>0,"return restores Japanese candidates");

        // A delayed foreign callback cannot repaint the English composition.
        List<Consumer<List<Candidate>>> replies=new ArrayList<>();
        c=create(new Editor(),Learning.NONE,false);c.addons(addon,ALL);
        c.decoder((d,r,b,x,done)->replies.add(done),()->{});type(c,"fixture");
        c.switchMode(InputMode.ENGLISH,false);replies.get(replies.size()-1).accept(Collections.singletonList(new Candidate("中文",false,1000)));
        equal(Arrays.asList("fixture","fixtureword"),texts(c),"stale decoder results rejected after switching to English");
        for(boolean privateField:new boolean[]{false,true}) {
            c=create(new Editor(),saved,true);c.start(false,true,privateField,false,true);type(c,"fixture");
            equal(Collections.singletonList("fixture"),texts(c),"literal fields retain only raw input");
        }
        // Direct user text and explicit symbol insertion are not automatic suggestions.
        editor=new Editor();c=create(editor,saved,true);type(c,"かな");equal("かな",c.candidates().get(0).text,"exact raw text always recoverable");
        c.select(0);equal("かな",editor.text,"explicit raw recovery is not censored");
        c.literal("中文🙂");equal("かな中文🙂",editor.text,"explicit literal insertion preserved");
    }
    private static List<String> texts(CompositionEngine c) {
        List<String> result=new ArrayList<>();for(Candidate value:c.candidates())result.add(value.text);return result;
    }
    private static int index(CompositionEngine c,String text) {
        for(int i=0;i<c.candidates().size();i++)if(c.candidates().get(i).text.equals(text))return i;
        return -1;
    }
}
