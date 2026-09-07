package dev.minime.core;

import static dev.minime.core.Regression.*;

final class MixedLearningRegression {
    static void run() {
        // Four reproduced cases, then four fresh cases frozen after proposing
        // context separation. No word-specific production recognition rules.
        for(String[] row:new String[][]{{"can","餐","we can meet today "},{"you","有","see you tomorrow "},
                {"he","和","is he home "},{"me","麼","please call me later "},
                {"men","們","the men are here "},{"she","蛇","is she ready "},
                {"man","慢","the man is here "},{"die","跌","please do not die "}}) {
            Editor e=new Editor();Memory memory=new Memory();CompositionEngine c=engine(e,memory,false);
            type(c,row[0]);c.select(find(c,row[1]));c.start(false,false,false,false);e.text="";
            type(c,row[2]);equal(row[2],e.text,"start-of-input Chinese choice must not replace conversational English: "+row[0]);
            c.start(false,false,false,false);type(c,row[0]);
            equal(row[1],c.candidates().get(c.preferred()).text,"original context still remembers Chinese: "+row[0]);
            c.start(false,false,true,false);e.text="";int count=memory.votes.size();type(c,row[2]);
            equal(row[2],e.text,"private conversation ignores all learned contexts");equal(count,memory.votes.size(),"private input never persists context");
        }
        Editor e=new Editor();Memory memory=new Memory();CompositionEngine c=engine(e,memory,false);
        type(c,"can");c.select(find(c,"餐"));c.start(false,false,false,false);e.text="";
        type(c,"we can");c.select(find(c,"餐"));equal("we 餐",e.text,"explicit Chinese remains available within Latin input");
        equal(1,memory.count("AFTER_LATIN","can","餐"),"explicit choice belongs to the Latin boundary");
        c.start(false,false,false,false);type(c,"we can");equal("餐",c.candidates().get(c.preferred()).text,"deliberate same-context choice is retained");
        c.start(false,false,false,false);type(c,"hello ");c.abandon();type(c,"can");
        equal("餐",c.candidates().get(c.preferred()).text,"cursor/lifecycle interruption clears the Latin boundary");
        c.start(false,false,false,false);type(c,"hello ");c.enter();type(c,"can");
        equal("餐",c.candidates().get(c.preferred()).text,"new line starts a new learning context");
        c.start(false,false,false,false,true);e.text="";type(c,"we can meet ");equal("we can meet ",e.text,"English mode ignores Chinese learning");
    }
}
