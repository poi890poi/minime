package dev.minime.core;

import static dev.minime.core.Regression.*;

final class EnglishCandidateRegression {
    static void run() {
        // Independently authored continuation vocabulary, not model entries.
        for(String raw:new String[]{"time","home","good","please","morning","love","today","really"}) {
            Editor e=new Editor();CompositionEngine c=engine(e,Learning.NONE,false);type(c,raw);
            equal(0,c.preferred(),"English remains the default: "+raw);
            if(!dictionary.englishCompletions(raw).isEmpty())
                yes(c.candidates().get(1).literal,"English completion is immediately reachable for "+raw);
            if(raw.equals("time"))yes(c.candidates().subList(1,3).stream().anyMatch(v->!v.literal),"Chinese alternative remains near the front");
            c.space();equal(raw+" ",e.text,"English Space output: "+raw);
        }
        Editor e=new Editor();Memory memory=new Memory();CompositionEngine c=engine(e,memory,false);
        type(c,"can");c.select(find(c,"餐"));c.start(false,false,false,false);type(c,"can");
        equal("餐",c.candidates().get(c.preferred()).text,"explicit learned Chinese default survives candidate presentation");
        c.select(0);equal("餐can",e.text,"literal recovery still selects raw index zero");
    }
}
