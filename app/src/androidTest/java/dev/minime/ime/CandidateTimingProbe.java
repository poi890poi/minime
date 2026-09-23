package dev.minime.ime;

import dev.minime.core.*;
import java.io.*;
import java.lang.reflect.Field;
import java.util.*;
import java.util.function.*;

/** Test-only, main-thread hooks. Never records ordinary user typing. */
final class CandidateTimingProbe implements AutoCloseable {
    private final CompositionEngine engine;
    private final CompositionEngine.Decoder decoder;
    private final Runnable changed;
    private final Predicate<String> display;
    private final Learning learning;
    private final List<Sample> samples=new ArrayList<>();
    private Sample active;
    private static final class Sample {
        String mode,raw;long requested,delivered,finished,glyph,render,votes,voteCalls;int count;
    }
    private static Object get(Object owner,String name)throws Exception {
        Field f=owner.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(owner);
    }
    private static void set(Object owner,String name,Object value)throws Exception {
        Field f=owner.getClass().getDeclaredField(name);f.setAccessible(true);f.set(owner,value);
    }
    @SuppressWarnings("unchecked")
    CandidateTimingProbe(CompositionEngine engine)throws Exception {
        this.engine=engine;decoder=(CompositionEngine.Decoder)get(engine,"decoder");
        changed=(Runnable)get(engine,"changed");display=(Predicate<String>)get(engine,"displayable");
        learning=(Learning)get(engine,"learning");
        set(engine,"learning",new Learning(){
            public int count(String c,String r,String v){
                long at=System.nanoTime();try{return learning.count(c,r,v);}
                finally{if(active!=null){active.votes+=System.nanoTime()-at;active.voteCalls++;}}
            }
            public void choose(String c,String r,String v){learning.choose(c,r,v);}
            public List<Candidate> custom(String r){return learning.custom(r);}
            public List<Candidate> predictEnglish(String c){return learning.predictEnglish(c);}
            public void rememberEnglish(String c,String w){learning.rememberEnglish(c,w);}
            public void observePhrase(String r,String o){learning.observePhrase(r,o);}
            public List<Candidate> phrases(String r){return learning.phrases(r);}
        });
        // Set fields without refreshing composition or changing request scheduling.
        set(engine,"displayable",(Predicate<String>)text->{
            long at=System.nanoTime();try{return display.test(text);}
            finally{if(active!=null)active.glyph+=System.nanoTime()-at;}
        });
        engine.decoder(new CompositionEngine.Decoder(){
            public void cancel(){decoder.cancel();}
            public void convert(PhoneticDictionary d,String raw,boolean z,String context,Consumer<List<Candidate>> result){
                decoder.convert(d,raw,z,context,result);
            }
            public void trace(PhoneticDictionary d,float[] points,Consumer<List<Candidate>> result){decoder.trace(d,points,result);}
            public void query(PhoneticDictionary d,String raw,boolean z,String context,boolean phonetic,
                    AddonDictionary addons,Set<String> enabled,Consumer<List<Candidate>> result){
                Sample s=new Sample();s.mode=engine.inputMode().id;s.raw=raw;s.requested=System.nanoTime();samples.add(s);
                decoder.query(d,raw,z,context,phonetic,addons,enabled,values->{
                    s.delivered=System.nanoTime();Sample previous=active;active=s;
                    try{result.accept(values);s.count=engine.candidates().size();}
                    finally{s.finished=System.nanoTime();active=previous;}
                });
            }
        },()->{
            long at=System.nanoTime();try{changed.run();}
            finally{if(active!=null)active.render+=System.nanoTime()-at;}
        });
    }
    void write(File directory)throws Exception {
        try(PrintWriter out=new PrintWriter(new File(directory,"candidate-stages.tsv"),"UTF-8")) {
            out.println("mode\tquery\trequested_ns\tdelivered_ns\tfinished_ns\tglyph_ns\trender_ns\tcandidates\tvotes_ns\tvote_calls");
            for(Sample s:samples)out.println(s.mode+"\t"+s.raw+"\t"+s.requested+"\t"+s.delivered+"\t"+s.finished+"\t"+s.glyph+"\t"+s.render+"\t"+s.count+"\t"+s.votes+"\t"+s.voteCalls);
        }
        if(decoder instanceof AsyncDecoder) {
            DecodePipeline.Stats stats=((AsyncDecoder)decoder).stats;
            try(PrintWriter out=new PrintWriter(new File(directory,"candidate-providers.tsv"),"UTF-8")) {
                out.println("stage\tcalls\ttotal_ns");
                for(int i=0;i<3;i++)out.println(new String[]{"base","rime","addons"}[i]+"\t"+stats.calls[i].get()+"\t"+stats.nanos[i].get());
            }
        }
    }
    public void close()throws Exception {engine.decoder(decoder,changed);set(engine,"displayable",display);set(engine,"learning",learning);}
}
