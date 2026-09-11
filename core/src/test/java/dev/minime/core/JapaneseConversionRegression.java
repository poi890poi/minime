package dev.minime.core;

import java.nio.file.*;
import java.util.*;
import java.util.function.Consumer;
import static dev.minime.core.Regression.*;

final class JapaneseConversionRegression {
    static void run()throws Exception {
        JapaneseBasics basics=JapaneseBasics.read(Files.newBufferedReader(Paths.get("app/src/main/assets/japanese-basic.tsv")));
        AddonDictionary addons=AddonDictionary.withJapaneseBasics(AddonDictionary.EMPTY,basics);
        Set<String> enabled=Collections.singleton("japanese");
        List<Candidate> fallback=addons.lookup("sakurak",enabled);int[] calls={0};
        JapaneseConversion.Provider provider=k->{calls[0]++;return Arrays.asList("試験候補","試験候補","romanization",null,"");};
        equal(fallback,JapaneseConversion.merge("sakurak",enabled,addons,fallback,provider,()->true),"unfinished kana retains complete fallback");
        equal(0,calls[0],"unfinished spelling never reaches converter");
        equal(fallback,JapaneseConversion.merge("sakura",Collections.emptySet(),addons,fallback,provider,()->true),"other modes exclude converter");
        equal(0,calls[0],"excluded mode does not call native");
        List<Candidate> whole=addons.lookup("sakurasekai",enabled);
        List<Candidate> merged=JapaneseConversion.merge("sakurasekai",enabled,addons,whole,provider,()->true);
        equal("試験候補",merged.get(0).text,"conversion precedes whole-kana recovery");
        equal(1L,merged.stream().filter(c->c.text.equals("試験候補")).count(),"native duplicates removed");
        yes(merged.stream().noneMatch(c->c.text.equals("romanization")),"native Latin pollution rejected");
        for(Candidate c:whole)yes(merged.stream().anyMatch(v->v.text.equals(c.text)),"fallback retained");
        equal(whole,JapaneseConversion.merge("sakurasekai",enabled,addons,whole,k->{throw new IllegalStateException();},()->true),"provider failure falls back");
        DecodePipeline.Requests requests=new DecodePipeline.Requests();java.util.function.BooleanSupplier current=requests.next();
        equal(whole,JapaneseConversion.merge("sakurasekai",enabled,addons,whole,k->{requests.cancel();return Arrays.asList("試験候補");},current),"superseded native result discarded");
        Editor editor=new Editor();CompositionEngine engine=engine(editor,Learning.NONE,false);
        engine.addons(addons,enabled);engine.switchMode(InputMode.JAPANESE_ENGLISH,false);
        List<Consumer<List<Candidate>>> pending=new ArrayList<>();List<String> queries=new ArrayList<>();
        engine.decoder(new CompositionEngine.Decoder(){
            public void convert(PhoneticDictionary d,String r,boolean z,String c,Consumer<List<Candidate>> done){throw new AssertionError();}
            public void query(PhoneticDictionary d,String r,boolean z,String c,boolean p,AddonDictionary a,Set<String> e,Consumer<List<Candidate>> done){queries.add(r);pending.add(done);}
        },()->{});
        type(engine,"sakurasekai");engine.space();equal("",editor.text,"Space waits for matching conversion");
        pending.get(0).accept(merged);equal("",editor.text,"obsolete completion cannot commit");
        pending.get(pending.size()-1).accept(merged);equal("試験候補",editor.text,"Space commits displayed winner");
        equal("",engine.raw(),"whole candidate consumes entire spelling");
        type(engine,"sakurasekai");int last=pending.size()-1;engine.switchMode(InputMode.ENGLISH,false);
        pending.get(last).accept(merged);yes(engine.candidates().stream().noneMatch(c->c.pack.equals("japanese")),"late conversion cannot pollute English");
        System.out.println("PASS optional Japanese conversion eligibility, fallback, cancellation and acceptance");
    }
}
