package dev.minime.core;

import java.nio.file.*;
import java.util.*;

/** Alternating warm measurements of scoring only; never phone latency. */
public final class ContextBoundaryBenchmark {
    private static volatile double sink;
    private static long run(ContextModel model,List<String[]> rows,boolean shared,int repeats) {
        long begin=System.nanoTime();double sum=0;
        for(int n=0;n<repeats;n++)for(String[] row:rows)
            sum+=shared?model.chineseBoundary(row[0],row[1]):model.chinese(row[0],row[1])-model.chinese("",row[1]);
        sink=sum;return System.nanoTime()-begin;
    }
    public static void main(String[] args)throws Exception {
        ContextModel model=ContextModel.load(Files.newBufferedReader(Paths.get("app/src/main/assets/context.tsv")));
        List<String[]> rows=new ArrayList<>();for(String line:Files.readAllLines(Paths.get(args[0])))rows.add(line.split("\t",-1));
        for(String[] row:rows)if(Double.doubleToLongBits(model.chineseBoundary(row[0],row[1]))
                !=Double.doubleToLongBits(model.chinese(row[0],row[1])-model.chinese("",row[1])))throw new AssertionError("Score differs");
        for(int i=0;i<6;i++){run(model,rows,false,10);run(model,rows,true,10);}
        List<String> out=new ArrayList<>();out.add("round\tmethod\tevaluations\tnanoseconds");
        for(int i=0;i<10;i++)for(boolean shared:i%2==0?new boolean[]{false,true}:new boolean[]{true,false})
            out.add(i+"\t"+(shared?"shared":"two-pass")+"\t"+rows.size()*20+"\t"+run(model,rows,shared,20));
        Files.write(Paths.get(args[1]),out);System.out.println("Completed 10 alternating rounds; "+rows.size()+" context/candidate pairs; exact score parity");
    }
}
