package dev.minime.core;

import java.util.*;
import java.util.concurrent.*;

/** Frozen pre-change expressions are the semantic oracle, not language labels. */
public final class ValidationPatternProbe {
    private static final String[] OLD={"[a-zv]+(?:'[a-zv]+)*","[A-Za-z]+(?:'[A-Za-z]+)*","[A-Za-z]+","[A-Za-z]+(?:'[A-Za-z]*)?","[A-Za-z]+(?:'[A-Za-z]+)?"};
    private static volatile int sink;
    private static boolean current(int kind,String raw) {
        switch(kind) {
            case 0:return InputValidators.pinyin(raw);
            case 1:return InputValidators.englishWord(raw);
            case 2:return InputValidators.englishBare(raw);
            case 3:return InputValidators.englishPrefix(raw);
            case 4:return InputValidators.englishCorrection(raw);
            default:throw new AssertionError();
        }
    }
    public static List<String> inputs() {
        Set<String> values=new LinkedHashSet<>();Random random=new Random(20260925);
        for(int i=0;i<128;i++) {
            StringBuilder b=new StringBuilder();for(int j=0,n=random.nextInt(33);j<n;j++)b.append((char)('a'+random.nextInt(26)));
            String s=b.toString();values.add(s);values.add(s.toUpperCase(Locale.ROOT));
            values.add(s+"'");values.add("'"+s);values.add(s+"'a");values.add(s+"''a");values.add(s+"'a'b");
            for(String tail:new String[]{" ","\n","\r\n","0","é","\u0301","ㄚ","あ","\ud83d\ude00","\ud800","\udc00"})values.add(s+tail);
        }
        return new ArrayList<>(values);
    }
    public static int verify(List<String> inputs)throws Exception {
        boolean[][] expected=new boolean[inputs.size()][OLD.length];
        for(int i=0;i<inputs.size();i++)for(int k=0;k<OLD.length;k++)expected[i][k]=inputs.get(i).matches(OLD[k]);
        ExecutorService threads=Executors.newFixedThreadPool(4);
        try {
            List<Future<?>> futures=new ArrayList<>();
            for(int worker=0;worker<4;worker++)futures.add(threads.submit(()->{
                for(int repeat=0;repeat<2;repeat++)for(int i=0;i<inputs.size();i++)for(int k=0;k<OLD.length;k++)
                    if(current(k,inputs.get(i))!=expected[i][k])throw new AssertionError("Validator differs at "+i+"/"+k);
            }));
            for(Future<?> f:futures)f.get();
        } finally {threads.shutdownNow();threads.awaitTermination(10,TimeUnit.SECONDS);}
        return inputs.size()*OLD.length*8;
    }
    public static long measure(List<String> inputs,boolean compiled,int repeats) {
        int matched=0;long begin=System.nanoTime();
        for(int r=0;r<repeats;r++)for(String raw:inputs)for(int kind=0;kind<OLD.length;kind++)
            if(compiled?current(kind,raw):raw.matches(OLD[kind]))matched++;
        long elapsed=System.nanoTime()-begin;sink=matched;return elapsed;
    }
    public static void main(String[] args)throws Exception {
        List<String> inputs=inputs();System.out.println("PASS "+verify(inputs)+" boundary/concurrent validator comparisons (not language accuracy)");
    }
}
