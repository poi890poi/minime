package dev.minime.core;

import java.nio.file.*;
import java.util.*;
import static dev.minime.core.Regression.*;

/** Source-derived punctuation pairs; no word-specific production exceptions. */
final class ApostropheRegression {
    static void run()throws Exception {
        Set<String> bareWords=new HashSet<>();List<String> sources=new ArrayList<>();
        for(String row:Files.readAllLines(Paths.get("app/src/main/assets/en_us.tsv"))) {
            String word=row.split("\t")[0];bareWords.add(word.toLowerCase(Locale.ROOT));
            if(word.matches("[A-Za-z]+(?:'[A-Za-z]+)+") && word.length()<=32)sources.add(word);
        }
        for(String word:sources) {
            String key=word.replace("'","").toLowerCase(Locale.ROOT);
            if(IntentClassifier.technicalWord(key)) {yes(dictionary.englishApostrophes(key).isEmpty(),"technical token preserved");continue;}
            yes(dictionary.englishApostrophes(key).stream().anyMatch(c->c.text.equalsIgnoreCase(word)),"source apostrophe spelling indexed: "+word);
        }
        sources.sort(Comparator.comparingInt(String::hashCode));Set<String> probed=new HashSet<>();
        for(String word:sources) {
            String key=word.replace("'","").toLowerCase(Locale.ROOT);if(IntentClassifier.technicalWord(key) || !probed.add(key))continue;
            for(String raw:new String[]{key,Character.toUpperCase(key.charAt(0))+key.substring(1),key.toUpperCase(Locale.ROOT)})for(boolean english:new boolean[]{false,true}) {
                Editor e=new Editor();CompositionEngine c=engine(e,Learning.NONE,false);c.start(false,false,false,false,english);c.englishOptions(false,false);type(c,raw);
                List<Candidate> matches=dictionary.englishApostrophes(raw,english);if(matches.isEmpty())continue;
                String restored=matches.get(0).text;
                equal(restored,c.candidates().get(1).text,"apostrophe spelling precedes unrelated completions");
                if(!dictionary.validEnglishSpelling(raw)) {c.space();equal(restored+" ",e.text,"restoration independent of autocorrect option");}
            }
            if(probed.size()==48)break;
        }
        int valid=0,contractions=0;
        for(String line:Files.readAllLines(Paths.get("app/src/main/assets/en_spelling.tsv"))) {
            String[] p=line.split("\t");String raw=p[1].replace("'","");
            if(p[0].equals("valid")) {yes(dictionary.validEnglishSpelling(raw),"full source spelling recognized");valid++;continue;}
            contractions++;
            for(boolean english:new boolean[]{false,true}) {
                Editor e=new Editor();CompositionEngine c=engine(e,Learning.NONE,false);c.start(false,false,false,false,english);type(c,raw);c.space();
                equal((dictionary.validEnglishSpelling(raw)?raw:dictionary.englishApostrophes(raw,english).get(0).text)+" ",e.text,"source-annotated contraction acceptance");
            }
        }
        for(String raw:Arrays.asList("cant","well","were","ill","wont","blacks","cockatiels","shiite","ps"))for(boolean english:new boolean[]{false,true}) {
            // Non-contraction metadata prevents English spelling corrections;
            // it must not turn rare words/abbreviations into new Pinyin intent.
            if(!english && dictionary.englishApostrophes(raw,false).isEmpty())continue;
            Editor e=new Editor();CompositionEngine c=engine(e,Learning.NONE,false);c.start(false,false,false,false,english);c.englishOptions(true,false);type(c,raw);c.space();equal(raw+" ",e.text,"valid bare spelling preserved with spelling correction enabled");
        }
        for(String raw:Arrays.asList("ss","ds","es","rs")) {
            yes(dictionary.englishApostrophes(raw,false).isEmpty(),"letter plurals cannot override Pinyin initials");
            Editor e=new Editor();Learning chosen=new Learning() {
                public int count(String context,String input,String output) {return output.equals("測試")?1:0;}
                public void choose(String context,String input,String output) {}
            };CompositionEngine c=engine(e,chosen,false);
            c.decoder((d,r,b,context,reply)->reply.accept(Arrays.asList(new Candidate("測試",false,100))),()->{});
            type(c,raw);equal("測試",c.candidates().get(c.preferred()).text,"validity metadata cannot reclassify Chinese initials");
        }
        for(boolean english:new boolean[]{false,true}) {
            Editor e=new Editor();CompositionEngine c=engine(e,Learning.NONE,false);c.start(false,false,false,false,english);type(c,"dont");c.space();equal("don't ",e.text,"unknown spelling gets source apostrophe");
            c.backspace();equal("dont",c.raw(),"restoration can be undone");c.select(0);equal("dont",e.text,"exact raw recovery");
            e=new Editor();c=engine(e,Learning.NONE,false);c.start(false,true,false,false,english);type(c,"dont ");equal("dont ",e.text,"literal field untouched");
            Memory memory=new Memory();e=new Editor();c=engine(e,memory,false);c.start(false,false,false,false,english);type(c,"dont");c.select(0);
            c.start(false,false,false,false,english);e.text="";type(c,"dont ");equal("dont ",e.text,"explicit raw choice overrides restoration");
            e=new Editor();c=engine(e,memory,false);c.start(false,false,true,false,english);type(c,"dont ");equal("don't ",e.text,"private mode uses source but ignores personal choices");
        }
        yes(dictionary.englishApostrophes("doNt").isEmpty(),"mixed-case identifier untouched");
        System.out.println("PASS apostrophes: "+sources.size()+" source pairs; "+probed.size()+" keys in both modes and three casing styles; "+valid+" valid spellings; "+contractions+" source-annotated contractions");
    }
}
