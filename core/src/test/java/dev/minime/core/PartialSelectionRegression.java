package dev.minime.core;

import java.util.*;
import static dev.minime.core.Regression.*;

/** Explicit prefix selection must preserve every unconsumed phonetic character. */
final class PartialSelectionRegression {
    private static CompositionEngine setup(Editor editor,Memory memory,boolean privateField) {
        CompositionEngine engine=engine(editor,memory,false);
        engine.start(false,false,privateField,false);
        engine.decoder((dictionary,raw,zhuyin,context,done)-> {
            List<Candidate> choices=new ArrayList<>();
            if(raw.equals("nihao") || raw.equals("ni'hao")) {
                choices.add(new Candidate("你",false,200,2));
                choices.add(new Candidate("你好",false,100));
                choices.add(new Candidate("invalid",false,1000,99));
            } else if(raw.equals("hao"))choices.add(new Candidate("好",false,100));
            done.accept(choices);
        },()->{});
        return engine;
    }
    static void run() {
        for(String input:Arrays.asList("nihao","ni'hao")) {
            Editor e=new Editor();Memory memory=new Memory();CompositionEngine c=setup(e,memory,false);
            type(c,input);
            equal("你好",c.candidates().get(c.preferred()).text,"whole phrase remains Space default despite higher prefix score");
            yes(c.candidates().stream().noneMatch(v->v.text.equals("invalid")),"out-of-range consumption rejected");
            c.select(find(c,"你"));equal("你",e.text,"explicit prefix committed");
            equal("hao",c.raw(),"unconsumed phonetics retained");equal("hao",e.composing,"suffix keeps its editor span");
            equal(1,memory.count("START_OR_LATIN","ni","你"),"learn only selected reading");
            equal(0,memory.count("START_OR_LATIN",input,"你"),"never learn whole phrase as prefix output");
            c.space();equal("你好",e.text,"Space completes suffix without loss");
        }
        Editor e=new Editor();Memory memory=new Memory();CompositionEngine c=setup(e,memory,true);
        type(c,"nihao");c.select(find(c,"你"));c.select(0);
        equal("你hao",e.text,"exact recovery retains selected prefix and remaining spelling");equal(0,memory.votes.size(),"private partial choices never learn");
        e=new Editor();c=setup(e,new Memory(),false);type(c,"nihao");c.select(find(c,"你"));
        c.backspace();equal("ha",c.raw(),"delete acts on remaining spelling");c.type('o');c.confirm();equal("你好",e.text,"edited suffix remains convertible");
        e=new Editor();c=setup(e,new Memory(),false);type(c,"nihao");c.select(find(c,"你"));c.literal("。");equal("你好。",e.text,"punctuation confirms the remaining phrase");
        e=new Editor();c=setup(e,new Memory(),false);type(c,"nihao");c.select(find(c,"你"));c.abandon();equal("你hao",e.text,"interruption preserves suffix literally");
        e=new Editor();c=setup(e,new Memory(),false);type(c,"nihao");c.select(find(c,"你"));
        c.backspace();c.backspace();c.backspace();equal("你",e.text,"deleting suffix retains selected prefix");c.backspace();equal("",e.text,"next delete reaches committed prefix");
        e=new Editor();c=setup(e,new Memory(),false);type(c,"nihao");c.space();equal("你好",e.text,"automatic acceptance never drops a suffix");
    }
}
