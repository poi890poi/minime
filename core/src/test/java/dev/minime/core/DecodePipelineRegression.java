package dev.minime.core;

import java.util.*;
import java.util.function.*;
import static dev.minime.core.Regression.*;

final class DecodePipelineRegression {
    static void run() {
        for(int supersede=-1;supersede<4;supersede++) {
            DecodePipeline.Requests requests=new DecodePipeline.Requests();
            DecodePipeline.Stats stats=new DecodePipeline.Stats();BooleanSupplier current=requests.next();
            final int stageToCancel=supersede;List<Integer> calls=new ArrayList<>();
            List<Supplier<List<Candidate>>> stages=new ArrayList<>();
            for(int i=0;i<3;i++) {final int stage=i;stages.add(()-> {
                calls.add(stage);if(stageToCancel==stage)requests.cancel();
                return new ArrayList<>(Collections.singletonList(new Candidate("stage"+stage,false,stage)));
            });}
            if(supersede==3)requests.cancel();
            List<Candidate> result=DecodePipeline.run(current,stages.get(0),stages.get(1),stages.get(2),stats);
            if(supersede==-1) {
                equal(3,calls.size(),"live query runs all providers");yes(result!=null,"live result delivered");
                List<Candidate> expected=CandidateMerge.merge(Collections.singletonList(new Candidate("stage1",false,1)),new ArrayList<>(Collections.singletonList(new Candidate("stage0",false,0))));
                expected.add(new Candidate("stage2",false,2));equal(expected.toString(),result.toString(),"live ordering unchanged");
            } else {
                equal(null,result,"obsolete result suppressed");equal(supersede==3?0:supersede+1,calls.size(),"obsolete downstream providers skipped");
                equal(1L,stats.cancelled.get(),"cancellation measured once");
            }
        }
        DecodePipeline.Requests requests=new DecodePipeline.Requests();BooleanSupplier old=requests.next(),current=requests.next();
        yes(!old.getAsBoolean() && current.getAsBoolean(),"new request invalidates previous delivery");
        requests.cancel();yes(!current.getAsBoolean(),"lifecycle invalidates latest delivery");
        Editor e=new Editor();CompositionEngine c=engine(e,Learning.NONE,false);int[] cancelled={0};
        c.decoder(new CompositionEngine.Decoder() {
            public void convert(PhoneticDictionary d,String r,boolean z,String context,Consumer<List<Candidate>> done){}
            public void cancel(){cancelled[0]++;}
        },()->{});
        type(c,"za");int before=cancelled[0];c.switchMode(InputMode.ENGLISH,false);
        yes(cancelled[0]>before,"mode with no worker query cancels obsolete work");before=cancelled[0];c.abandon();
        yes(cancelled[0]>before,"lifecycle cancels obsolete work");
    }
}
