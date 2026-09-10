package dev.minime.core;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.*;

/** Cooperative cancellation between providers; never runs native queries in parallel. */
public final class DecodePipeline {
    private DecodePipeline(){}
    public static final class Requests {
        private final AtomicLong revision=new AtomicLong();
        public BooleanSupplier next() {long id=revision.incrementAndGet();return ()->id==revision.get();}
        public void cancel(){revision.incrementAndGet();}
    }
    /** Aggregate in-memory timings only; contains no spelling, text or editor data. */
    public static final class Stats {
        public final AtomicLong requests=new AtomicLong();
        public final AtomicLong[] calls={new AtomicLong(),new AtomicLong(),new AtomicLong()};
        public final AtomicLong[] nanos={new AtomicLong(),new AtomicLong(),new AtomicLong()};
        public final AtomicLong cancelled=new AtomicLong(),delivered=new AtomicLong(),staleDelivery=new AtomicLong();
    }
    private static List<Candidate> measure(Supplier<List<Candidate>> stage,int index,Stats stats) {
        long start=System.nanoTime();
        try {return stage.get();}finally {stats.calls[index].incrementAndGet();stats.nanos[index].addAndGet(System.nanoTime()-start);}
    }
    public static List<Candidate> run(BooleanSupplier current,Supplier<List<Candidate>> base,
            Supplier<List<Candidate>> nativeStage,Supplier<List<Candidate>> addons,Stats stats) {
        if(!current.getAsBoolean()){stats.cancelled.incrementAndGet();return null;}
        List<Candidate> result=measure(base,0,stats);
        if(!current.getAsBoolean()){stats.cancelled.incrementAndGet();return null;}
        if(nativeStage!=null) {
            result=CandidateMerge.merge(measure(nativeStage,1,stats),result);
            if(!current.getAsBoolean()){stats.cancelled.incrementAndGet();return null;}
        }
        result.addAll(measure(addons,2,stats));
        if(!current.getAsBoolean()){stats.cancelled.incrementAndGet();return null;}
        return result;
    }
}
