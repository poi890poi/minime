package dev.minime.core;

import java.nio.file.*;
import java.io.*;
import java.util.*;

/** Offline source screen using the actual unchanged ContextModel; no app assets written. */
public final class ContextSourcePilot {
    public static void main(String[] args)throws Exception {
        Path out=Paths.get(args[0]);
        List<String> inputs=Files.readAllLines(out.resolve("contexts.txt"));
        try(PrintWriter writer=new PrintWriter(Files.newBufferedWriter(out.resolve("predictions.tsv")))) {
            for(String name:Arrays.asList("baseline","pilot")) {
                Path source=name.equals("baseline")?Paths.get("app/src/main/assets/context.tsv"):out.resolve("context.tsv");
                long start=System.nanoTime();ContextModel model=ContextModel.load(Files.newBufferedReader(source));
                long loaded=System.nanoTime();
                for(String context:inputs) {
                    List<Candidate> result=model.english(context);
                    writer.print(name+"\t"+context);
                    for(int i=0;i<Math.min(8,result.size());i++)writer.print("\t"+result.get(i).text);
                    writer.println();
                }
                System.out.println(name+" load_ms="+(loaded-start)/1e6+" lookup_and_output_ms="+(System.nanoTime()-loaded)/1e6+" contexts="+inputs.size());
            }
        }
    }
}
