package dev.minime.core;

import java.util.*;

final class ConstructionRegression {
    static void run(PhoneticDictionary dictionary) {
        Candidate generated=NativeCandidateCodec.decode("nihaoshijie","11\tS\t你好世界",0);
        Regression.yes(generated.composed,"native sentence provenance survives bridge");
        Regression.yes(generated.withScore(5).consuming(4).composed,"candidate transformations retain origin");
        Regression.yes(!CandidateMerge.merge(Collections.singletonList(generated),Collections.singletonList(new Candidate(generated.text,false,3))).get(0).composed,"lexical duplicate is attested even when native assembled it");
        Regression.yes(CandidateMerge.merge(Collections.singletonList(generated),Collections.singletonList(new Candidate(generated.text,false,3,2))).get(0).composed,"different consumed span cannot attest a whole construction");
        Regression.equal(null,NativeCandidateCodec.decode("nihao","5\t你好",0),"unversioned originless transport rejected");
        Regression.equal(null,NativeCandidateCodec.decode("nihao","9\tL\t你好",0),"invalid native endpoint rejected");
        List<Candidate> choices=Arrays.asList(generated,new Candidate("你好",false,99,5),new Candidate("你",false,98,2),new Candidate("妳",false,97,2));
        Regression.Editor editor=new Regression.Editor();Regression.Memory memory=new Regression.Memory();
        CompositionEngine engine=new CompositionEngine(editor,memory);engine.dictionary(dictionary);engine.start(false,false,false,false);
        engine.decoder((d,r,b,c,result)->result.accept(choices),()->{});
        Regression.type(engine,"nihaoshijie");
        Regression.equal(4,Regression.find(engine,generated.text),"unknown construction follows three attested prefixes");
        Regression.equal(0,engine.preferred(),"prefix-only alternatives cannot auto-accept unknown assembly");
        engine.space();Regression.equal("nihaoshijie ",editor.text,"space preserves raw when only whole choice is unverified");
        engine.start(false,false,false,false);Regression.type(engine,"nihaoshijie");engine.select(Regression.find(engine,generated.text));
        // Explicit choices are context-scoped; fresh context repeats that intent.
        engine.start(false,false,false,false);Regression.type(engine,"nihaoshijie");
        Regression.equal(generated.text,engine.candidates().get(engine.preferred()).text,"explicit user choice remains usable");
        Regression.Editor e2=new Regression.Editor();CompositionEngine e=new CompositionEngine(e2,Learning.NONE);e.dictionary(dictionary);e.start(false,false,false,false);
        e.decoder((d,r,b,c,result)->result.accept(Arrays.asList(generated,new Candidate("您好世界",false,90))),()->{});
        Regression.type(e,"nihaoshijie");e.space();Regression.equal("您好世界",e2.text,"attested whole choice wins over unverified construction");
        Candidate raw=new Candidate("raw",true,0),one=new Candidate("one",false,1),two=new Candidate("two",false,1),three=new Candidate("three",false,1),four=new Candidate("four",false,1);
        List<Candidate> late=Arrays.asList(raw,one,two,three,four,generated);
        Regression.equal(late,ConstructionPolicy.rank(late,c->c.composed),"late constructions never promoted");
        int[] observations={0};Regression.Memory votes=new Regression.Memory();
        Learning learning=new Learning() {
            public int count(String c,String r,String v){return votes.count(c,r,v);}
            public void choose(String c,String r,String v){votes.choose(c,r,v);}
            public void observePhrase(String r,String v){observations[0]++;}
        };
        CompositionEngine history=new CompositionEngine(new Regression.Editor(),learning);history.dictionary(dictionary);history.phraseLearning(true);
        history.decoder((d,r,b,c,result)->result.accept(Collections.singletonList(generated)),()->{});
        history.start(false,false,false,false);Regression.type(history,"nihaoshijie");history.select(Regression.find(history,generated.text));
        Regression.equal(1,observations[0],"explicit construction selection can teach a phrase");
        for(int i=0;i<3;i++) {
            history.start(false,false,false,false);Regression.type(history,"nihaoshijie");
            Regression.equal(generated.text,history.candidates().get(history.preferred()).text,"confirmed construction remains automatic for same context");history.space();
        }
        Regression.equal(1,observations[0],"automatic repetition cannot attest a constructed phrase");
        history.start(false,false,true,false);Regression.type(history,"nihaoshijie");
        Regression.equal(0,history.preferred(),"private field cannot use previous confirmations");
        history.select(Regression.find(history,generated.text));Regression.equal(1,observations[0],"private explicit selection cannot learn");
    }
}
