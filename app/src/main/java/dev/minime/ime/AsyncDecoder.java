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
    AsyncDecoder(Handler main) {this.main=main;}
    public void convert(PhoneticDictionary dictionary,String raw,boolean zhuyin,String context,Consumer<List<Candidate>> result) {
        if(queued!=null)queued.cancel(false);
        queued=worker.schedule(()-> {
            List<Candidate> found=dictionary.convert(raw,zhuyin,context);
            main.post(()->{if(!closed)result.accept(found);});
        },8,TimeUnit.MILLISECONDS);
    }
    public void close() {closed=true;worker.shutdownNow();}
}
