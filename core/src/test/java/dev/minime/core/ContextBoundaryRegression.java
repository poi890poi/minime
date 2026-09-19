package dev.minime.core;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import static dev.minime.core.Regression.*;

/** Bitwise parity is necessary: small arithmetic differences can reorder ties. */
final class ContextBoundaryRegression {
    /** Frozen pre-optimization estimator, independent of the production helper. */
    private static final class Reference {
        final Map<String,Integer> counts=new HashMap<>(),totals=new HashMap<>();
        Reference(List<String> rows) {
            for(String line:rows) {
                String[] p=line.split("\t",-1);if(p[0].equals("en"))continue;
                int count=Integer.parseInt(p[3]);counts.put(p[1]+"\t"+p[2],count);totals.merge(p[1],count,Integer::sum);
            }
        }
        private String tail(String s,int n) {return s.substring(s.offsetByCodePoints(s.length(),-Math.min(n,s.codePointCount(0,s.length()))));}
        double score(String context,String text) {
            double score=0;String before=context;
            for(int cp:text.codePoints().toArray()) {
                String next=new String(Character.toChars(cp)),key=tail(before,2);
                if(!totals.containsKey(key))key=tail(before,1);
                if(!key.isEmpty()&&totals.containsKey(key)) {
                    double prior=(counts.getOrDefault("\t"+next,0)+1.0)/(totals.getOrDefault("",1)+5000.0);
                    double observed=(counts.getOrDefault(key+"\t"+next,0)+20*prior)/(totals.get(key)+20.0);
                    score+=Math.max(-2,Math.min(2,Math.log10(observed/prior)));
                }
                before=tail(before+next,2);
            }
            return score;
        }
    }
    private static Reference reference;
    static void check(ContextModel model,String context,String text) {
        equal(Double.doubleToLongBits(reference.score(context,text)-reference.score("",text)),
            Double.doubleToLongBits(model.chineseBoundary(context,text)),"Exact boundary score: "+context+" / "+text);
    }
    static void run()throws Exception {
        ContextModel model=ContextModel.load(Files.newBufferedReader(Paths.get("app/src/main/assets/context.tsv")));
        reference=new Reference(Files.readAllLines(Paths.get("app/src/main/assets/context.tsv")));
        List<String> contexts=new ArrayList<>();
        for(String line:Files.readAllLines(Paths.get("app/src/main/assets/context.tsv"))) {
            String[] p=line.split("\t",-1);if(p[0].equals("zh")&&!p[1].isEmpty())contexts.add(p[1]);
        }
        int n=0;
        for(String line:Files.readAllLines(Paths.get("app/src/main/assets/zh_tw.tsv"))) {
            String word=line.split("\t")[2],context=contexts.get(n++%contexts.size());
            check(model,context,word);check(model,word,context);check(model,"",word);
        }
        for(String context:Arrays.asList("","的","我們","上下文","𠀀","𠀀文","latin","。"))
            for(String word:Arrays.asList("","的","我們","上下文的測試字串","𠀀","𠀀的文","latin","。"))check(model,context,word);
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();model.write(new BinaryModel.Writer(new DataOutputStream(bytes)));
        ContextModel binary=new ContextModel(new BinaryModel.Reader(new DataInputStream(new ByteArrayInputStream(bytes.toByteArray()))));
        for(String context:contexts.subList(0,Math.min(1000,contexts.size()))) {
            check(binary,context,"測試上下文");
            equal(model.chineseBoundary(context,"測試上下文"),binary.chineseBoundary(context,"測試上下文"),"Serialized counts preserve boundary scoring");
        }
        System.out.println("PASS exact context-boundary score differences across "+n+" source reading rows, Unicode and binary paths");
    }
}
