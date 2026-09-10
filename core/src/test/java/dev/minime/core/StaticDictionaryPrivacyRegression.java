package dev.minime.core;

import java.io.StringReader;
import java.util.*;
import static dev.minime.core.Regression.*;

/** Privacy is a personalization boundary, not a static vocabulary switch. */
final class StaticDictionaryPrivacyRegression {
    static final Learning FORBIDDEN=new Learning() {
        private AssertionError fail(){return new AssertionError("private editor accessed personal data");}
        public int count(String c,String r,String v){throw fail();}
        public void choose(String c,String r,String v){throw fail();}
        public List<Candidate> custom(String r){throw fail();}
        public List<Candidate> phrases(String r){throw fail();}
        public List<Candidate> predictEnglish(String c){throw fail();}
        public void rememberEnglish(String c,String w){throw fail();}
        public void observePhrase(String r,String w){throw fail();}
    };
    static List<String> snapshot(CompositionEngine c) {
        List<String> result=new ArrayList<>();
        for(Candidate v:c.candidates())result.add(v.text+":"+v.pack+":"+v.incomplete);
        return result;
    }
    static void run() throws Exception {
        AddonDictionary addons=AddonDictionary.read(new StringReader(
            "poj\tzavora\tfixture-poj\ttest\ttest\n"+
            "japanese\tzavora\tテスト\ttest\ttest\n"+
            "taiwan\tzavora\t測試\ttest\ttest\n"));
        Set<String> enabled=new HashSet<>(Arrays.asList("poj","japanese","taiwan"));
        for(InputMode mode:InputMode.values())for(boolean async:new boolean[]{false,true})
            for(String raw:Arrays.asList("zavora","zavo")) {
                List<String> normal=null;int preferred=-1;
                for(boolean privacy:new boolean[]{false,true}) {
                    Editor editor=new Editor();CompositionEngine c=engine(editor,privacy?FORBIDDEN:Learning.NONE,false);
                    c.start(false,false,privacy,false);c.switchMode(mode,false);c.addons(addons,enabled);c.phraseLearning(true);
                    if(async)c.decoder((d,r,z,ctx,done)->done.accept(d.convert(r,z,ctx)),()->{});
                    type(c,raw);
                    if(!privacy){normal=snapshot(c);preferred=c.preferred();}
                    else {equal(normal,snapshot(c),"static full/partial results survive no-learning flag");equal(preferred,c.preferred(),"static default survives no-learning flag");}
                    if(!mode.pack.isEmpty())yes(c.candidates().stream().anyMatch(v->v.pack.equals(mode.pack)),"focused dictionary available");
                    if(!c.candidates().isEmpty())c.select(c.preferred());
                    c.start(false,true,privacy,false);c.switchMode(mode,true);type(c,raw);
                    yes(c.candidates().stream().noneMatch(v->v.supplemental),"literal field excludes static conversion");
                    c.start(false,true,privacy,true);type(c,raw);
                    equal(0,c.candidates().size(),"secure/direct input excludes suggestions");
                }
            }
    }
}
