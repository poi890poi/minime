package dev.minime.core;

import java.io.StringReader;
import java.util.*;
import static dev.minime.core.Regression.*;

final class AddonLoadBarrierRegression {
    static void run()throws Exception {
        AddonDictionary words=AddonDictionary.read(new StringReader("japanese\tzavora\tかな\ttest\ttest\n"));
        Set<String> scope=Collections.singleton("japanese");
        CompositionEngine chinese=engine(new Editor(),Learning.NONE,false);
        chinese.awaitAddons(Collections.singleton("taiwan"));type(chinese,"nihao");
        yes(!chinese.predictionPending(),"optional specialist loading cannot block Chinese base conversion");
        for(boolean async:new boolean[]{false,true}) {
            Editor e=new Editor();CompositionEngine c=engine(e,Learning.NONE,false);
            if(async)c.decoder((d,r,z,ctx,done)->done.accept(Collections.emptyList()),()->{});
            c.switchMode(InputMode.JAPANESE_ENGLISH,false);c.awaitAddons(scope);type(c,"zavora");
            yes(c.predictionPending(),"cold load is pending conversion");c.space();
            equal("",e.text,"Space cannot commit fallback while focused dictionary loads");
            c.switchMode(InputMode.ENGLISH,false);c.addons(words,scope);
            equal("かな",e.text,"queued acceptance resolves once with loaded dictionary");
            equal(InputMode.ENGLISH,c.inputMode(),"queued mode change follows acceptance");
            c.switchMode(InputMode.JAPANESE_ENGLISH,false);c.awaitAddons(scope);type(c,"zavora");c.space();
            c.addons(AddonDictionary.EMPTY,scope);equal("かなzavora ",e.text,"failed load releases explicit acceptance with raw recovery");
            c.awaitAddons(scope);type(c,"zavora");c.space();c.abandon();String previous=e.text;
            c.addons(words,scope);equal(previous,e.text,"lifecycle discards obsolete queued acceptance");
        }
    }
}
