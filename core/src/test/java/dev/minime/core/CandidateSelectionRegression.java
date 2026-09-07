package dev.minime.core;

import java.util.*;
import java.util.function.Consumer;
import static dev.minime.core.Regression.*;

/** Synthetic choices isolate delayed acceptance from language ranking. */
final class CandidateSelectionRegression {
    static void run() {
        for(boolean available:new boolean[]{false,true}) {
            Editor editor=new Editor();Memory memory=new Memory();CompositionEngine e=engine(editor,memory,false);
            List<Consumer<List<Candidate>>> replies=new ArrayList<>();
            e.decoder((d,r,z,c,done)->replies.add(done),()->{});
            type(e,"n");yes(e.predictionPending(),"Query pending");
            Candidate choice=new Candidate("甲",false,1000);
            replies.get(0).accept(Arrays.asList(choice,new Candidate("乙",false,500)));
            yes(!e.predictionPending(),"Query complete");long id=e.compositionId();
            type(e,"i");equal(id,e.compositionId(),"Typing stays in composition");
            e.selectCandidate(choice,id);equal("",editor.text,"Visible choice waits for current prediction");
            e.type('h');equal("ni",e.raw(),"Later typing queues behind explicit choice");
            replies.get(1).accept(available?Arrays.asList(new Candidate("乙",false,2000),choice):Collections.singletonList(new Candidate("乙",false,2000)));
            equal(available?"甲":"",editor.text,"Identity survives reordering; missing identity never selects replacement");
            equal(available?"h":"nih",e.raw(),"Unconsumed typing survives");
            equal(available?1:0,memory.count("START_OR_LATIN","ni","甲"),"Only accepted current choice learns");
        }
        for(boolean sameConsumed:new boolean[]{false,true}) {
            Editor editor=new Editor();CompositionEngine e=engine(editor,Learning.NONE,false);
            List<Consumer<List<Candidate>>> replies=new ArrayList<>();e.decoder((d,r,z,c,done)->replies.add(done),()->{});
            type(e,"niha");Candidate choice=new Candidate("甲",false,1000,2);long id=e.compositionId();
            replies.get(3).accept(Collections.singletonList(choice));e.type('o');e.selectCandidate(choice,id);
            replies.get(4).accept(Collections.singletonList(new Candidate("甲",false,1000,sameConsumed?2:3)));
            equal("甲",editor.text,"Displayed word uses its current decoder alignment");
            equal(sameConsumed?"hao":"ao",e.raw(),"Partial choice preserves the currently decoded suffix");
            yes(id!=e.compositionId(),"Partial acceptance ends presentation ownership");
        }
        for(boolean restart:new boolean[]{false,true}) {
            Editor editor=new Editor();CompositionEngine e=engine(editor,Learning.NONE,false);
            type(e,"ni");Candidate old=e.candidates().get(0);long id=e.compositionId();
            if(restart)e.start(false,false,false,false);else e.abandon();
            type(e,"ni");String committed=editor.text;e.selectCandidate(old,id);
            equal(committed,editor.text,"Old editor choice cannot accept a new composition");
            equal("ni",e.raw(),"New spelling remains intact");
        }
        Editor editor=new Editor();CompositionEngine e=engine(editor,Learning.NONE,false);
        List<Consumer<List<Candidate>>> replies=new ArrayList<>();e.decoder((d,r,z,c,done)->replies.add(done),()->{});
        type(e,"ni");e.space();equal("",editor.text,"Space still waits for its prediction");
        replies.get(0).accept(Collections.singletonList(new Candidate("乙",false,1000)));
        yes(e.predictionPending(),"Obsolete callback cannot finish current prediction");
        replies.get(1).accept(Collections.singletonList(new Candidate("甲",false,1000)));
        equal("甲",editor.text,"Space accepts latest default");
    }
}
