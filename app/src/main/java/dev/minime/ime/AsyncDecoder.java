package dev.minime.ime;

import android.os.Handler;
import dev.minime.core.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.function.BooleanSupplier;

/** Single offline worker; superseded queued requests are coalesced before execution. */
final class AsyncDecoder implements CompositionEngine.Decoder,AutoCloseable {
    private final ScheduledExecutorService worker=Executors.newSingleThreadScheduledExecutor();
    private final Handler main;
    private ScheduledFuture<?> queued;
    private volatile boolean closed;
    private volatile boolean rime;
    private volatile JapaneseConversion.Provider japanese;
    private final DecodePipeline.Requests requests=new DecodePipeline.Requests();
    final DecodePipeline.Stats stats=new DecodePipeline.Stats();
    AsyncDecoder(Handler main) {this.main=main;}
    void rime(boolean enabled) {rime=enabled;}
    /** Optional injection; no native Japanese dependency is enabled by normal builds. */
    void japanese(JapaneseConversion.Provider provider) {cancel();japanese=provider;}
    public void convert(PhoneticDictionary dictionary,String raw,boolean zhuyin,String context,Consumer<List<Candidate>> result) {
        query(dictionary,raw,zhuyin,context,true,AddonDictionary.EMPTY,Collections.emptySet(),result);
    }
    public void query(PhoneticDictionary dictionary,String raw,boolean zhuyin,String context,boolean phonetic,
            AddonDictionary addons,Set<String> enabled,Consumer<List<Candidate>> result) {
        if(closed)return;
        cancel();BooleanSupplier current=requests.next();
        boolean useRime=phonetic && rime && !zhuyin;
        JapaneseConversion.Provider conversion=japanese;
        stats.requests.incrementAndGet();
        queued=worker.schedule(()-> {
            List<Candidate> choices=DecodePipeline.run(current,
                ()->phonetic?dictionary.convert(raw,zhuyin,context):new ArrayList<>(),
                useRime?()->RimeBackend.candidates(raw):null,
                ()->JapaneseConversion.merge(raw,enabled,addons,addons.lookup(raw,enabled),conversion,current),stats);
            if(choices!=null)deliver(current,choices,result);
        },8,TimeUnit.MILLISECONDS);
    }
    public void cancel() {requests.cancel();if(queued!=null)queued.cancel(false);}
    private void deliver(BooleanSupplier current,List<Candidate> choices,Consumer<List<Candidate>> result) {
        main.post(()-> {
            if(!closed && current.getAsBoolean()) {stats.delivered.incrementAndGet();result.accept(choices);}
            else stats.staleDelivery.incrementAndGet();
        });
    }
    public void close() {closed=true;cancel();worker.shutdownNow();}
    public void trace(PhoneticDictionary dictionary,float[] points,Consumer<List<Candidate>> result) {
        if(closed)return;
        cancel();BooleanSupplier current=requests.next();
        queued=worker.schedule(()-> {
            if(!current.getAsBoolean())return;
            List<Candidate> found=dictionary.englishTrace(points);
            if(current.getAsBoolean())deliver(current,found,result);
        },0,TimeUnit.MILLISECONDS);
    }
}
