package dev.minime.core;

import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Frozen source-text intentions are scoring labels only; never inputs to selection. */
public final class EnglishCompletionEvaluation {
    static final class Result {
        String base,top,margin;long nanos;
        Result(PhoneticDictionary d,String raw) {
            long started=System.nanoTime();
            Regression.Editor editor=new Regression.Editor();
            CompositionEngine engine=new CompositionEngine(editor,Learning.NONE);engine.dictionary(d);
            engine.start(false,false,false,false,true);engine.englishOptions(true,false);
            raw.codePoints().forEach(engine::type);engine.space();base=editor.text.stripTrailing();top=base;margin=base;
            if(base.equals(raw) && !d.validEnglishSpelling(raw) && !IntentClassifier.technicalWord(raw)) {
                List<Candidate> choices=d.englishCompletions(raw,true);
                if(!choices.isEmpty()) {
                    top=choices.get(0).text;
                    if(choices.get(0).score>=100 && (choices.size()==1 || choices.get(0).score-choices.get(1).score>=8))margin=top;
                }
            }
            nanos=System.nanoTime()-started;
        }
    }
    public static void main(String[] args)throws Exception {
        Path assets=Paths.get("app/src/main/assets");
        PhoneticDictionary d=PhoneticDictionary.load(Files.newBufferedReader(assets.resolve("zh_tw.tsv")),Files.newBufferedReader(assets.resolve("en_us.tsv")),Files.newBufferedReader(assets.resolve("syllables.tsv")),Files.newBufferedReader(assets.resolve("context.tsv")));
        d.englishSpelling(Files.newBufferedReader(assets.resolve("en_spelling.tsv")));
        Map<String,Result> cache=new HashMap<>();
        try(java.io.BufferedWriter out=Files.newBufferedWriter(Paths.get(args[1]),StandardCharsets.UTF_8)) {
            out.write("genre\tdocument\tcondition\texpected\traw\tbaseline\ttop\tmargin\tquery_ns\n");
            for(String row:Files.readAllLines(Paths.get(args[0]),StandardCharsets.UTF_8)) {
                String[] cells=row.split("\t",-1);if(cells.length<4 || !cells[0].startsWith("en-"))continue;
                for(String word:cells[3].split(" ")) {
                    if(!word.matches("[a-z]+(?:'[a-z]+)*"))continue;
                    Map<String,String> cases=new LinkedHashMap<>();cases.put("complete",word);
                    if(word.length()>=4) {
                        cases.put("missing-last",word.substring(0,word.length()-1));
                        cases.put("half-prefix",word.substring(0,(word.length()+1)/2));
                        // One deterministic adjacent-key slip, independent of candidate scores.
                        int at=word.length()/2;
                        while(word.charAt(at)=='\'')at++; // Apostrophes have no letter-key centre.
                        float[] point=EnglishTrace.point(word.charAt(at));
                        {
                            char nearest='a';double distance=Double.MAX_VALUE;
                            for(char ch='a';ch<='z';ch++)if(ch!=word.charAt(at)) {
                                float[] p=EnglishTrace.point(ch);double ds=Math.pow(point[0]-p[0],2)+Math.pow(point[1]-p[1],2);
                                if(ds<distance) {distance=ds;nearest=ch;}
                            }
                            cases.put("adjacent-key",word.substring(0,at)+nearest+word.substring(at+1));
                        }
                    }
                    for(Map.Entry<String,String> sample:cases.entrySet()) {
                        String raw=sample.getValue();Result r=cache.computeIfAbsent(raw,k->new Result(d,k));
                        out.write(String.join("\t",cells[0],cells[1],sample.getKey(),word,raw,r.base,r.top,r.margin,Long.toString(r.nanos))+"\n");
                    }
                }
            }
        }
        System.out.println("Evaluated unique spellings="+cache.size()+"; cached core queries, not touch latency");
    }
}
