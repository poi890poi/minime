package dev.minime.ime;

import java.io.*;
import java.util.*;

/** Diagnostic overlay only; no spelling, editor text, or scheduling decisions. */
final class QueueTrace {
    private static final int LIMIT=4096;
    private static final List<Sample> rows=new ArrayList<>();
    private static volatile boolean enabled;
    private static int dropped;
    static final class Sample {
        final int id;
        final long requested=System.nanoTime();
        volatile long scheduled,worker,providers,posted,entered,finished,cancel;
        volatile int terminal; // 0 pending, 1 accepted, 2 cancelled pipeline, 3 stale delivery, 4 failed
        Sample(int id){this.id=id;}
    }
    static synchronized void start(){rows.clear();dropped=0;enabled=true;}
    static synchronized Sample begin(){
        if(!enabled)return null;
        if(rows.size()==LIMIT){dropped++;return null;}
        Sample row=new Sample(rows.size()+1);rows.add(row);return row;
    }
    static void scheduled(Sample s){if(s!=null)s.scheduled=System.nanoTime();}
    static void worker(Sample s){if(s!=null)s.worker=System.nanoTime();}
    static void providers(Sample s,boolean found){if(s!=null){s.providers=System.nanoTime();if(!found)s.terminal=2;}}
    static void posted(Sample s){if(s!=null)s.posted=System.nanoTime();}
    static void entered(Sample s){if(s!=null)s.entered=System.nanoTime();}
    static void finished(Sample s,boolean accepted){if(s!=null){s.finished=System.nanoTime();s.terminal=accepted?1:3;}}
    static void failed(Sample s){if(s!=null){s.finished=System.nanoTime();s.terminal=4;}}
    static void cancel(Sample s){if(s!=null && s.terminal==0 && s.cancel==0)s.cancel=System.nanoTime();}
    static void write(File target)throws Exception {
        List<Sample> snapshot;int overflow;
        synchronized(QueueTrace.class){enabled=false;snapshot=new ArrayList<>(rows);overflow=dropped;}
        // This runs after all timed input. Let already running work finish without
        // adding barriers, cancellation or a new task to either decoder queue.
        long deadline=System.nanoTime()+2_000_000_000L;
        while(snapshot.stream().anyMatch(s->s.worker!=0 && s.terminal==0) && System.nanoTime()<deadline)Thread.sleep(10);
        try(PrintWriter out=new PrintWriter(target,"UTF-8")) {
            out.println("id\trequested_ns\tscheduled_ns\tworker_ns\tproviders_ns\tposted_ns\tentered_ns\tfinished_ns\tcancel_ns\tterminal");
            for(Sample s:snapshot)out.println(s.id+"\t"+s.requested+"\t"+s.scheduled+"\t"+s.worker+"\t"+s.providers+"\t"+s.posted+"\t"+s.entered+"\t"+s.finished+"\t"+s.cancel+"\t"+s.terminal);
        }
        if(overflow!=0)throw new IllegalStateException("Diagnostic row budget exceeded: "+overflow);
        if(snapshot.stream().anyMatch(s->s.worker!=0 && s.terminal==0))throw new IllegalStateException("Diagnostic still has unfinished work after post-replay wait");
    }
}
