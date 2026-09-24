package dev.minime.ime;

import java.nio.file.*;
import java.util.*;

/** Local observer contract, not evidence of Android queue behavior. */
public final class QueueTraceContract {
    static void require(boolean value){if(!value)throw new AssertionError();}
    public static void main(String[] args)throws Exception {
        Path out=Paths.get(args[0]);Files.createDirectories(out);
        require(QueueTrace.begin()==null);
        QueueTrace.start();
        QueueTrace.Sample accepted=QueueTrace.begin();
        QueueTrace.scheduled(accepted);QueueTrace.worker(accepted);QueueTrace.providers(accepted,true);
        QueueTrace.posted(accepted);QueueTrace.entered(accepted);QueueTrace.finished(accepted,true);
        QueueTrace.cancel(accepted);require(accepted.cancel==0);
        QueueTrace.Sample queued=QueueTrace.begin();QueueTrace.scheduled(queued);QueueTrace.cancel(queued);
        QueueTrace.Sample cancelled=QueueTrace.begin();QueueTrace.scheduled(cancelled);QueueTrace.worker(cancelled);
        QueueTrace.cancel(cancelled);QueueTrace.providers(cancelled,false);
        QueueTrace.Sample stale=QueueTrace.begin();QueueTrace.scheduled(stale);QueueTrace.worker(stale);
        QueueTrace.providers(stale,true);QueueTrace.posted(stale);QueueTrace.cancel(stale);
        QueueTrace.entered(stale);QueueTrace.finished(stale,false);
        QueueTrace.Sample failed=QueueTrace.begin();QueueTrace.scheduled(failed);QueueTrace.worker(failed);QueueTrace.failed(failed);
        QueueTrace.write(out.resolve("observer-contract.tsv").toFile());
        require(QueueTrace.begin()==null);
        require(accepted.id==1 && queued.id==2 && cancelled.id==3 && stale.id==4 && failed.id==5);
        require(accepted.requested<=accepted.scheduled && accepted.scheduled<=accepted.worker
                && accepted.worker<=accepted.providers && accepted.providers<=accepted.posted
                && accepted.posted<=accepted.entered && accepted.entered<=accepted.finished);
        require(queued.worker==0 && queued.cancel>=queued.scheduled && queued.terminal==0);
        require(cancelled.terminal==2 && cancelled.posted==0 && stale.terminal==3 && failed.terminal==4);
        require(Files.readAllLines(out.resolve("observer-contract.tsv")).size()==6);
        QueueTrace.start();
        for(int i=0;i<4096;i++){QueueTrace.Sample s=QueueTrace.begin();require(s!=null);QueueTrace.cancel(s);}
        require(QueueTrace.begin()==null);
        boolean rejected=false;try{QueueTrace.write(out.resolve("overflow.tsv").toFile());}catch(IllegalStateException expected){rejected=true;}
        require(rejected);
        System.out.println("PASS observer: identity, monotonic stages, accepted/queued/cancelled/stale/failed states, disabled capture and bounded overflow");
    }
}
