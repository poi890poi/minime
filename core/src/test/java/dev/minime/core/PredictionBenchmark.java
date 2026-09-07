package dev.minime.core;

import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Evaluation input is not packaged and never feeds runtime ranking. */
public final class PredictionBenchmark {
    public static void main(String[] args) throws Exception {
        Path assets=Paths.get("app/src/main/assets");
        long started=System.nanoTime();
        PhoneticDictionary d=PhoneticDictionary.load(Files.newBufferedReader(assets.resolve("zh_tw.tsv")),Files.newBufferedReader(assets.resolve("en_us.tsv")),Files.newBufferedReader(assets.resolve("syllables.tsv")));
        long loadMs=(System.nanoTime()-started)/1000000;
        java.lang.reflect.Method contextual=null;
        try { contextual=PhoneticDictionary.class.getMethod("convert",String.class,boolean.class,String.class); } catch(NoSuchMethodException ignored) { }
        List<String> output=new ArrayList<>(); output.add("case\tcontext\tinput\texpected\trank\tmicroseconds\ttop5");
        int count=0,top1=0,top5=0; List<Long> times=new ArrayList<>();
        for(String line:Files.readAllLines(Paths.get(args[0]),StandardCharsets.UTF_8)) {
            if(line.isEmpty() || line.startsWith("#")) continue;
            String[] p=line.split("\t",-1);
            for(int warm=0;warm<3;warm++) d.convert(p[2],false);
            long begin=System.nanoTime();
            @SuppressWarnings("unchecked") List<Candidate> results=contextual==null?d.convert(p[2],false):(List<Candidate>)contextual.invoke(d,p[2],false,p[1]);
            long micros=(System.nanoTime()-begin)/1000; times.add(micros);
            int rank=0; for(int i=0;i<results.size();i++) if(results.get(i).text.equals(p[3])) {rank=i+1;break;}
            count++; if(rank==1) top1++; if(rank>0 && rank<=5) top5++;
            List<String> first=new ArrayList<>();for(Candidate c:results.subList(0,Math.min(5,results.size()))) first.add(c.text);
            output.add(String.join("\t",p[0],p[1],p[2],p[3],Integer.toString(rank),Long.toString(micros),String.join(" | ",first)));
        }
        Collections.sort(times);
        output.add("# top1="+top1+"/"+count+" top5="+top5+"/"+count+" p50_us="+times.get(times.size()/2)+" p95_us="+times.get(times.size()*95/100)+" load_ms="+loadMs);
        Files.write(Paths.get(args[1]),output,StandardCharsets.UTF_8);
        System.out.println(output.get(output.size()-1));
    }
}
