package dev.minime.core;

import java.io.StringReader;
import java.util.*;
import static dev.minime.core.Regression.*;

/** Synthetic context transitions isolate dispatch/identity/privacy, not vocabulary quality. */
final class EnglishContextRegression {
    static final class History implements Learning {
        final List<String> words=new ArrayList<>();final boolean priv;
        History(boolean priv){this.priv=priv;}
        public int count(String c,String r,String v){if(priv)throw new AssertionError("private count");return 0;}
        public void choose(String c,String r,String v){if(priv)throw new AssertionError("private choose");}
        public void rememberEnglish(String c,String w){if(priv)throw new AssertionError("private English write");words.add(c+"|"+w);}
        public List<Candidate> predictEnglish(String c){
            if(priv)throw new AssertionError("private English read");
            return c.isEmpty()?Collections.emptyList():Arrays.asList(new Candidate("personal",true,100),
                new Candidate("かな",true,100),Candidate.supplement("romaji",100).inPack("japanese"));
        }
    }
    static PhoneticDictionary tiny;
    static CompositionEngine setup(Editor e,History h,InputMode mode,boolean literal,boolean direct) {
        CompositionEngine c=new CompositionEngine(e,h);c.dictionary(tiny);c.start(false,literal,h.priv,direct,mode.english());c.switchMode(mode,literal);return c;
    }
    static List<String> texts(CompositionEngine c) {
        List<String> out=new ArrayList<>();for(Candidate x:c.candidates())out.add(x.text);return out;
    }
    public static void main(String[] args)throws Exception {run();System.out.println("PASS English context standalone assertions="+assertions);}
    static void run()throws Exception {
        tiny=PhoneticDictionary.load(new StringReader(""),new StringReader("alpha\t100\nbeta\t100\ngamma\t100\ncan't\t100\n"),new StringReader(""),
            new StringReader("en\talpha\tbeta\t10\nen\talpha beta\tgamma\t10\nen\tbeta gamma\talpha\t10\nen\tcan't\tbeta\t10\n"));
        for(InputMode mode:InputMode.values())for(boolean priv:new boolean[]{false,true})for(boolean literal:new boolean[]{false,true})for(boolean tap:new boolean[]{false,true}) {
            Editor e=new Editor();History h=new History(priv);CompositionEngine c=setup(e,h,mode,literal,false);
            type(c,"ALPHA");if(tap){c.select(0);c.space();}else c.space();
            equal("ALPHA ",e.text,"Context dispatch cannot change spacing or capitalization");
            boolean enabled=mode.englishEnabled() && !literal;
            equal(enabled,c.hasContext(),"Editor lifecycle observes English context on every enabled board");
            equal(enabled,texts(c).contains("beta"),"Static continuation follows accepted English in "+mode);
            equal(enabled && !priv,texts(c).contains("personal"),"Personal continuation respects language and privacy");
            yes(!texts(c).contains("かな") && !texts(c).contains("romaji"),"English continuation rejects foreign identities");
            type(c,"beta");c.select(0);c.space();
            equal(enabled,texts(c).contains("gamma"),"Two-word continuation context");
            type(c,"gamma");c.select(0);c.space();
            equal(enabled,texts(c).contains("alpha"),"Context retains exactly last two English words");
            equal(enabled && !priv?Arrays.asList("|alpha","alpha|beta","alpha beta|gamma"):Collections.emptyList(),h.words,"Shared accepted-word learning with no private writes");
        }
        for(InputMode mode:InputMode.values())if(mode.englishEnabled()) {
            for(String action:Arrays.asList("enter","symbol","abandon","restart","mode","delete")) {
                History h=new History(false);CompositionEngine c=setup(new Editor(),h,mode,false,false);type(c,"alpha ");
                switch(action) {
                    case "enter":c.enter();break;case "symbol":c.literal("。");break;
                    case "abandon":c.abandon();break;case "restart":c.start(false,false,false,false,mode.english());break;
                    case "mode":c.switchMode(mode==InputMode.ENGLISH?InputMode.CHINESE:InputMode.ENGLISH,false);break;
                    case "delete":c.backspace();break;
                }
                equal(Collections.emptyList(),texts(c),"Boundary clears English continuation: "+mode+"/"+action);
                yes(!c.hasContext(),"Boundary releases all context ownership");
            }
            History h=new History(false);Editor e=new Editor();CompositionEngine c=setup(e,h,mode,false,false);
            type(c,"alph");c.select(find(c,"alpha"));yes(texts(c).contains("beta"),"Accepted completion supplies English context");
            c.enter();type(c,"can't ");yes(texts(c).contains("beta"),"Apostrophized spelling supplies English context");
            h=new History(false);c=setup(new Editor(),h,mode,false,true);type(c,"alpha beta ");equal(0,h.words.size(),"Direct fields never learn");
        }
        // Identical Latin spelling is not proof that a focused POJ/Japanese choice is English.
        for(InputMode mode:Arrays.asList(InputMode.TAIWANESE_ENGLISH,InputMode.JAPANESE_ENGLISH)) {
            History h=new History(false);CompositionEngine c=setup(new Editor(),h,mode,false,false);
            AddonDictionary addon=AddonDictionary.read(new StringReader(mode.pack+"\tforeign\talpha\tfixture\tfixture\n"));
            c.addons(addon,Collections.singleton(mode.pack));type(c,"beta");c.select(0);c.space();
            type(c,"foreign");c.select(find(c,"alpha"));equal(1,h.words.size(),"Focused Latin output never enters English history");
            equal(Collections.emptyList(),texts(c),"Focused acceptance clears English context");
        }
        System.out.println("PASS English context: all modes, casing, completion, contraction, privacy, literal/direct fields and lifecycle boundaries");
    }
}
