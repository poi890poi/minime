package dev.minime.core;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;

/** Reused independent English text, hash sampling before candidate inspection. */
public final class MixedEnglishEvaluation {
    static String digest(byte[] bytes)throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }
    static String quote(String value) {return "\""+value.replace("\\","\\\\").replace("\"","\\\"")+"\"";}
    static void configuration(String[] args)throws Exception {
        Path assets=Paths.get("app/src/main/assets");
        Map<String,Path> inputs=new TreeMap<>();
        inputs.put("chinese",args.length>4?Paths.get(args[4]):assets.resolve("zh_tw.tsv"));
        inputs.put("english",assets.resolve("en_us.tsv"));inputs.put("syllables",assets.resolve("syllables.tsv"));
        inputs.put("context",args.length>2?Paths.get(args[2]):assets.resolve("context.tsv"));
        inputs.put("spelling",args.length>3?Paths.get(args[3]):assets.resolve("en_spelling.tsv"));
        inputs.put("corpus-ewt",Paths.get("docs/conversation-ranking/corpus/inputs.tsv"));
        inputs.put("corpus-gum",Paths.get("docs/conversation-ranking/corpus/gum-test.tsv"));
        if(args.length>=2){inputs.put("taiwan",assets.resolve("addons.tsv"));inputs.put("geography",Paths.get(args[1]));}
        List<String> fields=new ArrayList<>(),runtime=new ArrayList<>();
        for(Map.Entry<String,Path> entry:inputs.entrySet())fields.add(quote(entry.getKey())+":"+quote(digest(Files.readAllBytes(entry.getValue()))));
        for(Class<?> type:Arrays.asList(CompositionEngine.class,PhoneticDictionary.class,CandidateOrder.class,AddonDictionary.class,MixedEnglishEvaluation.class))
            try(InputStream in=type.getResourceAsStream(type.getSimpleName()+".class")) {
                if(in==null)throw new IOException("Missing runtime identity: "+type);
                runtime.add(quote(type.getSimpleName())+":"+quote(digest(in.readAllBytes())));
            }
        String result="{\n\"format\":1,\n\"packs\":"+(args.length>=2)+",\n\"inputs\":{"+String.join(",",fields)+"},\n\"runtime\":{"+String.join(",",runtime)+"},\n\"output_sha256\":"+quote(digest(Files.readAllBytes(Paths.get(args[0]))))+"\n}\n";
        Files.writeString(Paths.get(args[0]+".config.json"),result,StandardCharsets.UTF_8);
    }
    static String hash(String text) {
        try {return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8)));}
        catch(Exception e) {throw new IllegalStateException(e);}
    }
    static final class Result {
        final List<String> values;final String space;final long nanos;
        Result(PhoneticDictionary dictionary,AddonDictionary addons,String raw,boolean english) {
            Regression.Editor editor=new Regression.Editor();
            CompositionEngine c=new CompositionEngine(editor,Learning.NONE);c.dictionary(dictionary);
            c.start(false,false,false,false,english);
            c.addons(addons,new HashSet<>(Arrays.asList("taiwan","geography")));
            long start=System.nanoTime();raw.codePoints().forEach(c::type);nanos=System.nanoTime()-start;
            values=new ArrayList<>();for(Candidate candidate:c.candidates())values.add(candidate.text);
            c.space();space=editor.text;
        }
    }
    public static void main(String[] args)throws Exception {
        Path assets=Paths.get("app/src/main/assets");
        PhoneticDictionary d=PhoneticDictionary.load(Files.newBufferedReader(args.length>4?Paths.get(args[4]):assets.resolve("zh_tw.tsv")),Files.newBufferedReader(assets.resolve("en_us.tsv")),Files.newBufferedReader(assets.resolve("syllables.tsv")),Files.newBufferedReader(args.length>2?Paths.get(args[2]):assets.resolve("context.tsv")));
        d.englishSpelling(Files.newBufferedReader(args.length>3?Paths.get(args[3]):assets.resolve("en_spelling.tsv")));
        AddonDictionary addons=args.length<2?AddonDictionary.EMPTY:AddonDictionary.combine(
            AddonDictionary.read(Files.newBufferedReader(assets.resolve("addons.tsv"))),
            AddonDictionary.read(Files.newBufferedReader(Paths.get(args[1]))));
        Map<String,Result> cache=new HashMap<>();
        try(BufferedWriter out=Files.newBufferedWriter(Paths.get(args[0]))) {
            out.write("source\tword\tcondition\traw\tmode\trank\tspace\tfirst8\tquery_ns\tinventory_sha256\n");
            for(String file:Arrays.asList("inputs.tsv","gum-test.tsv")) {
                Set<String> words=new HashSet<>();
                for(String line:Files.readAllLines(Paths.get("docs/conversation-ranking/corpus",file))) {
                    String[] cells=line.split("\t",-1);if(cells.length<4 || !cells[0].startsWith("en-"))continue;
                    for(String word:cells[3].split(" "))if(word.matches("[a-z]+(?:'[a-z]+)*") && word.length()>=4)words.add(word);
                }
                List<String> sample=new ArrayList<>(words);sample.sort(Comparator.comparing(word->hash("mixed-english-20260919|"+file+"|"+word)));
                sample=sample.subList(0,Math.min(512,sample.size()));
                for(String word:sample) {
                    Map<String,String> conditions=new LinkedHashMap<>();conditions.put("whole",word);
                    conditions.put("missing-last",word.substring(0,word.length()-1));conditions.put("half-prefix",word.substring(0,(word.length()+1)/2));
                    for(Map.Entry<String,String> condition:conditions.entrySet())for(boolean english:new boolean[]{false,true}) {
                        String raw=condition.getValue(),mode=english?"English":"Chinese";
                        Result r=cache.computeIfAbsent(mode+"|"+raw,key->new Result(d,addons,raw,english));
                        out.write(String.join("\t",file,word,condition.getKey(),raw,mode,Integer.toString(r.values.indexOf(word)+1),r.space,
                            String.join("|",r.values.subList(0,Math.min(8,r.values.size()))),Long.toString(r.nanos),hash(String.join("|",r.values)))+"\n");
                    }
                }
                out.flush();System.out.println(file+": "+sample.size()+" hash-selected words; unique mode/input queries="+cache.size());
            }
        }
        configuration(args);
    }
}
