package dev.minime.ime;

import android.os.Handler;
import dev.minime.core.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

/** Single offline worker; superseded queued requests are coalesced before execution. */
final class AsyncDecoder implements CompositionEngine.Decoder,AutoCloseable {
    private final ScheduledExecutorService worker=Executors.newSingleThreadScheduledExecutor();
    private final Handler main;
    private ScheduledFuture<?> queued;
    private volatile boolean closed;
    private volatile boolean rime;
    AsyncDecoder(Handler main) {this.main=main;}
    void rime(boolean enabled) {rime=enabled;}
    public void convert(PhoneticDictionary dictionary,String raw,boolean zhuyin,String context,Consumer<List<Candidate>> result) {
        query(dictionary,raw,zhuyin,context,true,AddonDictionary.EMPTY,Collections.emptySet(),result);
    }
    public void query(PhoneticDictionary dictionary,String raw,boolean zhuyin,String context,boolean phonetic,
            AddonDictionary addons,Set<String> enabled,Consumer<List<Candidate>> result) {
        if(queued!=null)queued.cancel(false);
        boolean useRime=phonetic && rime && !zhuyin;
        queued=worker.schedule(()-> {
            List<Candidate> found=phonetic?dictionary.convert(raw,zhuyin,context):new ArrayList<>();
            if(useRime) {
                List<Candidate> nativeChoices=RimeBackend.candidates(raw);
                found=CandidateMerge.merge(nativeChoices,found);
            }
            found.addAll(addons.lookup(raw,enabled));
            List<Candidate> choices=found;
            main.post(()->{if(!closed)result.accept(choices);});
        },8,TimeUnit.MILLISECONDS);
    }
    public void close() {closed=true;worker.shutdownNow();}
    public void trace(PhoneticDictionary dictionary,float[] points,Consumer<List<Candidate>> result) {
        if(queued!=null)queued.cancel(false);
        queued=worker.schedule(()->{List<Candidate> found=dictionary.englishTrace(points);main.post(()->{if(!closed)result.accept(found);});},0,TimeUnit.MILLISECONDS);
    }
}
