package dev.minime.core;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.zip.GZIPInputStream;

/** Isolated ranking overhead, not end-to-end input latency. */
public final class ConstructionPolicyBenchmark {
    static volatile long sink;
    public static void main(String[] args) throws Exception {
        List<List<Candidate>> samples=new ArrayList<>();
        try(BufferedReader in=new BufferedReader(new InputStreamReader(new GZIPInputStream(Files.newInputStream(Paths.get(args[0]))),StandardCharsets.UTF_8))) {
            String line;while((line=in.readLine())!=null) {
                String raw=line.split("\t")[2];List<Candidate> choices=new ArrayList<>();choices.add(new Candidate(raw,true,0));
                while(!(line=in.readLine()).equals("END"))choices.add(NativeCandidateCodec.decode(raw,line,choices.size()-1));samples.add(choices);
            }
        }
        for(int pass=0;pass<5;pass++)for(List<Candidate> row:samples)sink+=ConstructionPolicy.rank(row,c->c.composed).get(row.size()-1).text.length();
        for(int pass=0;pass<3;pass++) {
            long[] times=new long[samples.size()];int i=0;
            for(List<Candidate> row:samples) {
                long before=System.nanoTime();List<Candidate> ranked=ConstructionPolicy.rank(row,c->c.composed);times[i++]=System.nanoTime()-before;sink+=ranked.get(ranked.size()-1).text.length();
            }
            Arrays.sort(times);System.out.printf(Locale.ROOT,"pass=%d inputs=%d p50_us=%.3f p95_us=%.3f max_us=%.3f%n",pass,times.length,times[times.length/2]/1000.,times[(int)Math.ceil(times.length*.95)-1]/1000.,times[times.length-1]/1000.);
        }
    }
}
