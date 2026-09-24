package dev.minime.core;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import jdk.jfr.consumer.*;

/** Exports counts/method names only; never exports JFR environment or user text. */
public final class ProfileEvents {
    private static final class Stat {long count,total;}
    private static final Map<String,Stat> stats=new TreeMap<>();
    private static void add(String category,String key,long total,String unit) {
        String identity=category+"\t"+key+"\t"+unit;
        Stat s=stats.computeIfAbsent(identity,k->new Stat());s.count++;s.total+=total;
    }
    private static String method(RecordedFrame frame) {
        RecordedMethod m=frame.getMethod();
        return (m.getType().getName()+"."+m.getName()).replaceAll("[+/]0x[0-9a-fA-F]+(?:\\.\\d+)?","");
    }
    public static void main(String[] args)throws Exception {
        Path output=Paths.get(args[1]);if(Files.exists(output))throw new IOException("Refusing to overwrite aggregates");
        long loss=0;
        try(RecordingFile file=new RecordingFile(Paths.get(args[0]))) {
            while(file.hasMoreEvents()) {
                RecordedEvent event=file.readEvent();String type=event.getEventType().getName();
                add("events",type,0,"events");
                if(type.equals("jdk.DataLoss")){loss++;continue;}
                if(type.equals("jdk.GarbageCollection") || type.equals("jdk.GCPhasePause"))
                    add("gc",type,event.getDuration().toNanos(),"nanoseconds");
                boolean cpu=type.equals("jdk.ExecutionSample") || type.equals("jdk.NativeMethodSample");
                boolean allocation=type.equals("jdk.ObjectAllocationSample");
                if(!cpu && !allocation)continue;
                RecordedStackTrace trace=event.getStackTrace();
                if(trace==null || trace.getFrames().isEmpty()) {
                    add("missing_stack",type,0,"events");
                    if(allocation)add("allocation_scope","unknown_stack",event.getLong("weight"),"sampled_weight_bytes");
                    else add("cpu_scope","unknown_stack",0,"samples");
                    continue;
                }
                List<String> frames=new ArrayList<>();for(RecordedFrame frame:trace.getFrames())frames.add(method(frame));
                if(trace.isTruncated())add("truncated_stack",type,0,"events");
                boolean lookup=frames.contains("dev.minime.core.AddonDictionary.lookup");
                if(cpu) {
                    add("cpu_scope",lookup?"lookup":"outside_lookup",0,"samples");
                    if(lookup) {
                        add("cpu_lookup_leaf",frames.get(0),0,"samples");
                        for(String frame:frames)if(frame.startsWith("dev.minime.core.")) {
                            add("cpu_lookup_nearest_core",frame,0,"samples");break;
                        }
                        for(String frame:new LinkedHashSet<>(frames))
                            if(frame.startsWith("dev.minime.core."))add("cpu_lookup_inclusive",frame,0,"samples");
                    }
                } else {
                    long weight=event.getLong("weight");
                    add("allocation_scope",lookup?"lookup":"outside_lookup",weight,"sampled_weight_bytes");
                    if(lookup) {
                        add("allocation_lookup_class",event.getClass("objectClass").getName(),weight,"sampled_weight_bytes");
                        for(String frame:frames)if(frame.startsWith("dev.minime.core.")) {
                            add("allocation_lookup_nearest_core",frame,weight,"sampled_weight_bytes");break;
                        }
                    }
                }
            }
        }
        try(PrintWriter out=new PrintWriter(Files.newBufferedWriter(output))) {
            out.println("category\tname\tunit\tcount\ttotal");
            stats.forEach((key,value)->out.println(key+"\t"+value.count+"\t"+value.total));
            if(out.checkError())throw new IOException("Incomplete aggregate output");
        }
        if(loss!=0)throw new IOException("JFR reports data loss; attribution is blocked");
        System.out.println("Exported "+stats.size()+" aggregate counters; no JFR DataLoss events");
    }
}
