package dev.minime.core;

import java.io.*;
import java.nio.file.*;
import java.security.*;
import java.util.*;

/** Review-only diagnostics, no candidate labels or source edits enter production. */
public final class ContractProbe {
    static final Path ASSETS=Paths.get("app/src/main/assets"),OUT=Paths.get("docs/language-contract-review");
    static PhoneticDictionary base;static AddonDictionary all;
    static final class Editor implements CompositionEngine.Editor {
        String text="";public void composing(String s){}public void commit(String s){text+=s;}
        public void delete(){}public void enter(){}public void finish(){}
    }
    static final class Memory implements Learning {
        List<String> namespaces=new ArrayList<>();int continuations;
        public int count(String c,String r,String v){return 0;}
        public void choose(String c,String r,String v){namespaces.add(c);}
        public void rememberEnglish(String c,String w){continuations++;}
    }
    static CompositionEngine engine(InputMode mode,Editor e,Memory m,AddonDictionary addons) {
        CompositionEngine c=new CompositionEngine(e,m);c.dictionary(base);c.start(false,false,false,false,mode.english());
        c.switchMode(mode,false);c.addons(addons,new HashSet<>(Arrays.asList("taiwan","poj","japanese")));c.englishOptions(true,false);return c;
    }
    static void type(CompositionEngine c,String raw){raw.codePoints().forEach(c::type);}
    static String digest(String s) {try{return Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA-256").digest(s.getBytes("UTF-8")));}catch(Exception e){throw new RuntimeException(e);}}
    static List<String> sample(Collection<String> values,int count) {
        List<String> sorted=new ArrayList<>(new HashSet<>(values));sorted.sort(Comparator.comparing(ContractProbe::digest));return sorted.subList(0,Math.min(count,sorted.size()));
    }
    public static void main(String[] args)throws Exception {
        base=PhoneticDictionary.readBinary(Files.newInputStream(Paths.get("app/build/generated/minimeAssets/model.bin")));
        base.englishSpelling(Files.newBufferedReader(ASSETS.resolve("en_spelling.tsv")));
        all=AddonDictionary.withJapaneseBasics(AddonDictionary.read(Files.newBufferedReader(ASSETS.resolve("addons.tsv"))),JapaneseBasics.read(Files.newBufferedReader(ASSETS.resolve("japanese-basic.tsv"))));
        List<String> partials=new ArrayList<>();
        for(String line:Files.readAllLines(Paths.get("docs/japanese-continuity/kana-oracle.tsv"))) {
            String[] p=line.split("\t",-1);if(!p[0].equals("input") && p[0].matches("[a-z]{3,16}") && Integer.parseInt(p[3])>0)partials.add(p[0]);
        }
        try(PrintWriter out=new PrintWriter(OUT.resolve("partial-acceptance.tsv").toFile(),"UTF-8")) {
            out.println("source_input\tcondition\tinput\tchoice\tconsumed\texpected_remaining\tactual_remaining\tdefault_is_prefix\tlearning_namespace\tstatus");
            for(String source:sample(partials,32))for(String condition:Arrays.asList("lower","upper","title","hyphen")) {
                String raw=condition.equals("upper")?source.toUpperCase(Locale.ROOT):condition.equals("title")?Character.toUpperCase(source.charAt(0))+source.substring(1):condition.equals("hyphen")?"ka-"+source:source;
                Editor e=new Editor();Memory m=new Memory();CompositionEngine c=engine(InputMode.JAPANESE_ENGLISH,e,m,all);type(c,raw);
                Candidate choice=c.candidates().stream().filter(v->v.pack.equals("japanese") && v.consumed>0 && v.consumed<raw.length()).findFirst().orElse(null);
                if(choice==null){out.println(source+"\t"+condition+"\t"+raw+"\t\t0\t\t\tfalse\t\tNO_PARTIAL_CHOICE");continue;}
                String expected=raw.substring(choice.consumed);boolean prefix=c.candidates().get(c.preferred()).consumed>0;
                c.select(c.candidates().indexOf(choice));out.println(String.join("\t",source,condition,raw,choice.text,""+choice.consumed,expected,c.raw(),""+prefix,String.join("|",m.namespaces),expected.equals(c.raw())?"PASS":"SUFFIX_LOST"));
            }
        }
        Set<String> contexts=new HashSet<>();
        for(String line:Files.readAllLines(ASSETS.resolve("context.tsv"))) {
            String[] p=line.split("\t",-1);if(p.length==4 && p[0].equals("en") && p[1].matches("[a-z]{2,16}"))contexts.add(p[1]);
        }
        try(PrintWriter out=new PrintWriter(OUT.resolve("english-continuation.tsv").toFile(),"UTF-8")) {
            out.println("context\tmode\tidle_candidates\tprovider_candidates\tenglish_learning_callbacks\tretained_context");
            for(String word:sample(contexts,32))for(InputMode mode:Arrays.asList(InputMode.ENGLISH,InputMode.CHINESE,InputMode.TAIWANESE_ENGLISH,InputMode.JAPANESE_ENGLISH)) {
                Memory m=new Memory();CompositionEngine c=engine(mode,new Editor(),m,all);type(c,word);c.select(0);
                out.println(word+"\t"+mode.id+"\t"+c.candidates().size()+"\t"+base.englishPredictions(word).size()+"\t"+m.continuations+"\t"+c.context());
            }
        }
        try(PrintWriter out=new PrintWriter(OUT.resolve("source-tier.tsv").toFile(),"UTF-8")) {
            out.println("pack\tcondition\tordered_candidates");
            for(String pack:Arrays.asList("poj","japanese"))for(boolean swap:Arrays.asList(false,true)) {
                String common=pack.equals("poj")?"extended_vocabulary":"everyday_vocabulary";
                String text=pack+"\tka'ra\t候選甲\tfixture:A\t"+(swap?common:"everyday_expressions")+"\n"+pack+"\tka'ra\t候選乙\tfixture:B\t"+(swap?"everyday_expressions":common)+"\n";
                out.println(pack+"\t"+(swap?"category-swapped":"original")+"\t"+AddonDictionary.read(new StringReader(text)).lookup("kara",Collections.singleton(pack)));
            }
        }
        System.out.println("Review-only probes complete: 128 partial controls, 128 continuation controls, 4 source-category controls.");
    }
}
