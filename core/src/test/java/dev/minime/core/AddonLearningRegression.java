package dev.minime.core;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import static dev.minime.core.Regression.*;

final class AddonLearningRegression {
    static final class Memory implements Learning {
        PhraseLexicon phrases=new PhraseLexicon("");int observations,votes;
        public int count(String c,String r,String v) {return 0;}
        public void choose(String c,String r,String v) {votes++;}
        public void observePhrase(String r,String v) {observations++;phrases.observe(r,v);}
        public List<Candidate> phrases(String r) {return phrases.lookup(r);}
    }
    static void run() throws Exception {
        AddonDictionary addon=AddonDictionary.combine(AddonDictionary.read(Files.newBufferedReader(Paths.get("app/src/main/assets/addons.tsv"))),AddonDictionary.read(Files.newBufferedReader(Paths.get("app/src/main/assets/geography.tsv"))));
        Set<String> all=new HashSet<>(Arrays.asList("taiwan","japanese","poj","geography"));
        // All input records are parseable; independent sampled queries check composition behavior.
        int samples=0;
        List<String> sourceRows=Files.readAllLines(Paths.get("app/src/main/assets/addons.tsv"));sourceRows.addAll(Files.readAllLines(Paths.get("app/src/main/assets/geography.tsv")));
        sourceRows.sort(Comparator.comparingInt(String::hashCode));Map<String,Integer> sampleCounts=new HashMap<>();
        for(String row:sourceRows) {
            if(row.startsWith("#"))continue;
            String[] p=row.split("\t");
            if(!p[1].matches("[a-z']{2,24}") || sampleCounts.getOrDefault(p[0],0)>=24)continue;
            sampleCounts.merge(p[0],1,Integer::sum);
            equal(0,addon.lookup(p[1],Collections.emptySet()).size(),"disabled add-on");
            Editor e=new Editor();CompositionEngine c=engine(e,Learning.NONE,false);type(c,p[1]);
            String before=c.candidates().get(c.preferred()).text;
            List<String> base=new ArrayList<>();for(Candidate value:c.candidates())base.add(value.text);
            c.addons(addon,all);equal(before,c.candidates().get(c.preferred()).text,"add-ons preserve Space");
            List<String> retained=new ArrayList<>();for(Candidate value:c.candidates())if(!value.supplemental)retained.add(value.text);
            List<String> unpromoted=new ArrayList<>();for(String value:base)if(retained.contains(value))unpromoted.add(value);
            equal(unpromoted,retained,"unpromoted base candidates retain relative order");
            c.addons(addon,Collections.emptySet());equal(base,c.candidates().stream().map(v->v.text).collect(java.util.stream.Collectors.toList()),"off removes supplemental results");samples++;
        }
        yes(samples>30,"source-diverse composition checks");
        Map<String,Integer> everydayCounts=new HashMap<>();Set<String> everydayKeys=new HashSet<>();
        for(String row:sourceRows) {
            String[] p=row.split("\t");if(p.length!=5 || !p[4].startsWith("everyday_"))continue;
            String key=p[1].replace("'","").replace("-","").replace(" ","");
            if(!key.matches("[a-z]{4,24}") || everydayCounts.getOrDefault(p[0],0)>=24 || !everydayKeys.add(p[0]+"\t"+key))continue;
            everydayCounts.merge(p[0],1,Integer::sum);
            for(boolean english:new boolean[]{false,true}) {
                Editor e=new Editor();CompositionEngine c=engine(e,Learning.NONE,false);c.start(false,false,false,false,english);
                type(c,key);String before=c.candidates().get(c.preferred()).text;c.addons(addon,all);
                equal(before,c.candidates().get(c.preferred()).text,"everyday pack preserves default");
                for(Candidate extra:addon.lookup(key,all))if(!extra.text.equals(key))yes(find(c,extra.text)>=0,"everyday source suggestion reachable");
                String expected=addon.lookup(key,Collections.singleton(p[0])).stream().filter(v->!v.text.equals(key)).findFirst().get().text;
                c.select(find(c,expected));equal(expected,e.text,"everyday phrase selected in both modes");
            }
        }
        equal(24,everydayCounts.get("poj"),"Taiwanese everyday probes");equal(24,everydayCounts.get("japanese"),"Japanese everyday probes");
        for(boolean english:new boolean[]{false,true}) {
            Editor e=new Editor();Learning manual=new Learning() {
                public int count(String context,String raw,String output) {return 0;}
                public void choose(String context,String raw,String output) {}
                public List<Candidate> custom(String raw) {return raw.equals("fixture")?Arrays.asList(new Candidate("自訂詞",false,0)):Collections.emptyList();}
            };
            CompositionEngine c=engine(e,manual,false);c.start(false,false,false,false,english);
            c.decoder((d,r,b,context,done)->done.accept(Arrays.asList(new Candidate("推測詞",false,1000))),()->{});
            type(c,"fixture");equal("自訂詞",c.candidates().get(1).text,"exact custom entry outranks decoder score scale");
        }
        // Orthographic round-trip fixtures are independent of production selection.
        // They cannot cause a dictionary entry to be imported or promoted.
        addon=AddonDictionary.read(new StringReader("poj\tliho\tlí hó\tfixture\tfixture\npoj\tli2-ho2\tlí hó\tfixture\tfixture\npoj\tboeiaukin\tbōe-iàu-kín\tfixture\tfixture\npoj\tchinphainnse\tchin-pháiⁿ-sè\tfixture\tfixture\n"));
        {
            Editor e=new Editor();CompositionEngine c=engine(e,Learning.NONE,false);
            c.decoder((d,r,b,context,done)-> {
                List<Candidate> list=new ArrayList<>();for(int i=0;i<12;i++)list.add(new Candidate("山"+(char)(0x4e00+i),false,100-i));done.accept(list);
            },()->{});
            type(c,"shanyu");String before=c.candidates().get(c.preferred()).text;
            String target="山"+(char)(0x4e00+10);
            AddonDictionary specific=AddonDictionary.read(new StringReader("taiwan\tshanyu\t"+target+"\ttest:fixture\tfixture\n"));
            c.addons(specific,Collections.singleton("taiwan"));equal(1,find(c,target),"exact source match precedes decoder guesses");
            equal(before,c.candidates().get(c.preferred()).text,"promotion never changes default identity");
            equal(1L,c.candidates().stream().filter(v->v.text.equals(target)).count(),"promotion has no duplicate row");
        }
        for(boolean english:new boolean[]{false,true}) {
            Memory m=new Memory();Editor e=new Editor();CompositionEngine c=engine(e,m,false);c.start(false,false,false,false,english);c.addons(addon,all);
            type(c,"liho");c.select(find(c,"lí hó"));equal("lí hó",e.text,"original POJ output");equal(0,m.votes,"POJ never trains base preference");
            c.start(false,false,true,false,english);type(c,"liho");yes(c.candidates().stream().noneMatch(v->v.supplemental),"private hides all add-ons");
            c.start(false,true,false,false,english);type(c,"liho");yes(c.candidates().stream().noneMatch(v->v.supplemental),"literal fields exclude add-ons");
        }
        {
            Editor e=new Editor();CompositionEngine c=engine(e,Learning.NONE,false);type(c,"nihao");
            String first=c.candidates().get(1).text;yes(dictionary.exactChinese("nihao",false,first),"fixture is a source-attested full reading");
            c.addons(AddonDictionary.read(new StringReader("taiwan\tni'hao\t測試\tfixture\tfixture\n")),all);
            equal(first,c.candidates().get(1).text,"full lexical match stays ahead of add-on alternatives");
            e=new Editor();c=engine(e,Learning.NONE,false);
            c.decoder((d,r,b,context,done)->done.accept(Arrays.asList(new Candidate("甲",false,100),new Candidate("乙",false,90))),()->{});
            c.addons(AddonDictionary.read(new StringReader("taiwan\tcs\t測試\tfixture\tfixture\n")),all);type(c,"cs");
            equal("甲",c.candidates().get(1).text,"initial alias preserves leading base choice");
            equal(3,find(c,"測試"),"initial alias retains previous insertion boundary");
        }
        yes(addon.lookup("li2-ho2",Collections.singleton("poj")).stream().anyMatch(c->c.text.equals("lí hó")),"numbered POJ alias");
        yes(addon.lookup("boeiaukin",Collections.singleton("poj")).stream().anyMatch(c->c.text.equals("bōe-iàu-kín")),"POJ oe preserved");
        yes(addon.lookup("chinphainnse",Collections.singleton("poj")).stream().anyMatch(c->c.text.contains("ⁿ")),"POJ nasal output");
        yes(addon.lookup("hoolah",Collections.singleton("poj")).isEmpty(),"no invented key");
        try {AddonDictionary.read(new StringReader("bad\tentry"));throw new AssertionError("invalid asset accepted");}catch(IOException expected) { }

        PhraseLexicon lexicon=new PhraseLexicon("");
        lexicon.observe("shan'yu","山雨");lexicon.observe("shanyu","山雨");equal(0,lexicon.lookup("shanyu").size(),"two observations not promoted");
        lexicon.observe("shanyu","山雨");equal("山雨",lexicon.lookup("shanyu").get(0).text,"three observations promote");
        lexicon=new PhraseLexicon(lexicon.serialize());equal("山雨",lexicon.lookup("shan'yu").get(0).text,"saved learning survives reload");
        equal(0,lexicon.lookup("shan").size(),"no learned prefix guesses");
        for(String output:Arrays.asList("x","山","山a","😀風"))lexicon.observe("abc",output);
        equal(0,lexicon.lookup("abc").size(),"only bounded Han phrases");
        for(int i=0;i<600;i++)lexicon.observe("shan",new String(Character.toChars(0x4e00+i))+"雨");
        equal(PhraseLexicon.LIMIT,lexicon.serialize().split("\n").length,"bounded storage");

        Memory m=new Memory();Editor e=new Editor();CompositionEngine c=engine(e,m,false);
        c.decoder((d,r,b,context,reply)->reply.accept(Arrays.asList(new Candidate("山",false,100,4),new Candidate("雨",false,90))),()->{});
        c.phraseLearning(true);
        for(int repeat=0;repeat<3;repeat++) {
            c.start(false,false,false,false);type(c,"shan'yu");c.select(find(c,"山"));c.select(find(c,"雨"));
        }
        equal("山雨",m.phrases.lookup("shanyu").get(0).text,"partial accepted segments join");
        int observed=m.observations;c.phraseLearning(false);c.start(false,false,false,false);type(c,"shanyu");c.select(find(c,"雨"));equal(observed,m.observations,"off prevents writes");
        c.phraseLearning(true);
        for(int boundary=0;boundary<5;boundary++) {
            c.start(false,false,false,false);type(c,"shan'yu");c.select(find(c,"山"));
            if(boundary==0)c.backspace();if(boundary==1)c.literal("，");if(boundary==2)c.abandon();if(boundary==3)c.enter();if(boundary==4)c.start(false,false,true,false);
            observed=m.observations;c.commitRaw(false);type(c,"yu");c.select(find(c,"雨"));equal(observed,m.observations,"edit/lifecycle/private boundary breaks phrase");
        }
        System.out.println("PASS add-ons: "+samples+" source-derived composition probes; bounded repeated learning and privacy");
    }
}
