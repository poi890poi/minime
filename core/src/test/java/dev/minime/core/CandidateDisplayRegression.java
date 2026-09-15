package dev.minime.core;

import java.io.StringReader;
import java.util.*;
import java.util.function.*;
import static dev.minime.core.Regression.*;

/** Simulated font capabilities, independent of actual vocabulary and phone fonts. */
final class CandidateDisplayRegression {
    static void run() throws Exception {
        for(boolean privateField:new boolean[]{false,true})for(boolean blockedDefault:new boolean[]{false,true})for(int n=0;n<32;n++) {
            String blocked=new String(Character.toChars(0x20000+n));
            String safe=new String(Character.toChars(0x4e00+n));
            Candidate bad=new Candidate(blocked,false,blockedDefault?1000:1),good=new Candidate(safe,false,100),prefix=new Candidate(safe+safe,false,20,2);
            Editor out=new Editor();CompositionEngine e=engine(out,Learning.NONE,false);e.start(false,false,privateField,false);
            e.candidateDisplay(text->!text.contains(blocked));
            e.decoder((d,r,b,c,done)->done.accept(blockedDefault?Arrays.asList(bad,good,prefix):Arrays.asList(good,bad,prefix)),()->{});type(e,"nihao");
            equal(Arrays.asList("nihao",safe,safe+safe),e.candidates().stream().map(c->c.text).collect(java.util.stream.Collectors.toList()),"Capability filter preserves readable order");
            equal(blockedDefault?0:1,e.preferred(),"Removed default falls back to raw; surviving default retains identity");
            e.space();equal(blockedDefault?"nihao ":safe,out.text,"Space cannot accept a hidden candidate");
            e.abandon();type(e,"nihao");Candidate shown=e.candidates().get(2);e.selectCandidate(shown,e.compositionId());
            equal("hao",e.raw(),"Font policy preserves prefix consumption");
        }
        String unsupported=new String(Character.toChars(0x20000));
        Editor out=new Editor();CompositionEngine e=engine(out,Learning.NONE,false);
        e.candidateDisplay(s->false);type(e,unsupported);
        equal(unsupported,e.candidates().get(0).text,"Exact user input survives unavailable fonts");e.select(0);equal(unsupported,out.text,"Literal recovery preserves exact scalars");

        // A readable POJ form survives an unavailable Han alternative, and the
        // original paired identity still owns learning even when Han is preferred.
        PairedForms pairs=PairedForms.read(new StringReader("poj\tabc-def\t"+unsupported+"\titaigi:1\n"));
        AddonDictionary addons=AddonDictionary.read(new StringReader("poj\tabc-def\tabc-def\titaigi:1\tfixture\n"),pairs);
        for(boolean han:new boolean[]{false,true})for(boolean missingHan:new boolean[]{false,true}) {
            Editor output=new Editor();Memory memory=new Memory();CompositionEngine c=engine(output,memory,false);
            c.candidateDisplay(s->missingHan?!s.contains(unsupported):!s.equals("abc-def"));c.addons(addons,Collections.singleton("poj"));c.switchMode(InputMode.TAIWANESE,false);c.pairedTaiwanese(true,han);type(c,"abcdef");
            Candidate displayed=c.candidates().stream().filter(v->v.pair!=null).findFirst().get();
            equal(missingHan?"abc-def":unsupported,displayed.text,"Readable paired form remains available");equal("",displayed.alternateText(),"Unavailable alternate is not advertised");
            equal("",displayed.withScore(7).completing(6).consuming(2).inPack("poj").alternateText(),"Candidate copies preserve alternate availability");
            c.selectAlternative(displayed,c.compositionId());equal("",output.text,"No invisible long-press insertion");
            c.selectCandidate(displayed,c.compositionId());equal(missingHan?"abc-def":unsupported,output.text,"Tap inserts displayed form");
            equal(1,memory.count("FOCUS:poj","abcdef","abc-def"),"Readability preserves paired learning identity");
        }
        // Real delayed callbacks must apply capability filtering before queued Space.
        for(boolean allow:new boolean[]{false,true}) {
            Editor output=new Editor();CompositionEngine c=engine(output,Learning.NONE,false);
            List<Consumer<List<Candidate>>> pending=new ArrayList<>();
            c.decoder((d,r,b,ctx,done)->pending.add(done),()->{});c.candidateDisplay(s->allow || !s.contains(unsupported));type(c,"nihao");
            c.space();pending.get(pending.size()-1).accept(Collections.singletonList(new Candidate(unsupported,false,100)));
            equal(allow?unsupported:"nihao ",output.text,"Queued acceptance uses the same capability-filtered result");
        }
        // Changing platform font support invalidates old touch snapshots.
        Editor output=new Editor();CompositionEngine c=engine(output,Learning.NONE,false);
        c.decoder((d,r,b,ctx,done)->done.accept(Collections.singletonList(new Candidate(unsupported,false,100))),()->{});type(c,"nihao");
        Candidate stale=c.candidates().get(1);long old=c.compositionId();c.candidateDisplay(s->false);c.selectCandidate(stale,old);
        equal("",output.text,"Font changes cannot activate a stale invisible choice");
        Learning learned=new Learning() {
            public int count(String context,String raw,String text){return 0;}
            public void choose(String context,String raw,String text){}
            public List<Candidate> predictEnglish(String context){return Arrays.asList(new Candidate("blocked",true,100),new Candidate("available",true,10));}
            public List<Candidate> custom(String raw){return Collections.singletonList(new Candidate("blocked",true,100));}
        };
        CompositionEngine english=engine(new Editor(),learned,false);english.candidateDisplay(s->!s.equals("blocked"));english.start(false,false,false,false,true);
        yes(english.candidates().stream().noneMatch(v->v.text.equals("blocked")),"Idle English learned predictions use the same capability policy");
        type(english,"fixture");yes(english.candidates().stream().noneMatch(v->v.text.equals("blocked")),"English custom suggestions use the same policy");english.abandon();
        english.decoder(new CompositionEngine.Decoder() {
            public void convert(PhoneticDictionary d,String raw,boolean bpmf,String context,Consumer<List<Candidate>> done){done.accept(Collections.emptyList());}
            public void trace(PhoneticDictionary d,float[] points,Consumer<List<Candidate>> done){done.accept(Arrays.asList(new Candidate("blocked",true,100),new Candidate("available",true,10)));}
        },()->{});
        english.trace(new float[]{0,0},0);equal("available",english.raw(),"Trace never constructs raw text from an unavailable suggestion");
        System.out.println("PASS candidate font capability: order, raw recovery, Space, prefix taps, paired forms and queued acceptance");
    }
}
