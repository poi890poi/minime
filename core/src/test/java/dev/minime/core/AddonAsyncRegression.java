package dev.minime.core;

import java.io.*;
import java.util.*;
import java.util.function.Consumer;
import static dev.minime.core.Regression.*;

final class AddonAsyncRegression {
    static void run()throws Exception {
        for(boolean english:new boolean[]{false,true}) {
            Editor editor=new Editor();CompositionEngine engine=engine(editor,Learning.NONE,false);
            engine.start(false,false,false,false,english);engine.switchMode(InputMode.JAPANESE,false);
            AddonDictionary addon=AddonDictionary.read(new StringReader("japanese\tka'na'mi\tかなみ\tfixture\ttest\n"));
            engine.addons(addon,Collections.singleton("japanese"));List<Runnable> replies=new ArrayList<>();
            engine.decoder(new CompositionEngine.Decoder() {
                public void convert(PhoneticDictionary d,String r,boolean b,String c,Consumer<List<Candidate>> done){throw new AssertionError("query must own both lookups");}
                public void query(PhoneticDictionary d,String raw,boolean b,String c,boolean phonetic,AddonDictionary a,Set<String> enabled,Consumer<List<Candidate>> done) {
                    replies.add(()->done.accept(a.lookup(raw,enabled)));
                }
            },()->{});
            type(engine,"kanami");yes(engine.predictionPending(),"both boards wait for combined query");
            engine.space();equal("",editor.text,"Space waits for current static dictionary search");
            replies.get(0).run();equal("",editor.text,"old partial result cannot drain Space");
            replies.get(5).run();equal("かなみ",editor.text,"same completed snapshot supplies acceptance; English retains literal completion policy");
            type(engine,"kana");int last=replies.size()-1;engine.abandon();replies.get(last).run();
            equal("",engine.raw(),"abandoned editor rejects late add-on result");
        }
    }
}
