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
        if(queued!=null)queued.cancel(false);
        boolean useRime=rime && !zhuyin;
        queued=worker.schedule(()-> {
            List<Candidate> found=dictionary.convert(raw,zhuyin,context);
            if(useRime) {
                List<Candidate> nativeChoices=RimeBackend.candidates(raw);
                if(!nativeChoices.isEmpty()) {
                    Set<String> seen=new HashSet<>();for(Candidate c:nativeChoices)seen.add(c.text);
                    for(Candidate c:found)if(seen.add(c.text))nativeChoices.add(c);
                    found=nativeChoices;
                }
            }
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
