package dev.minime.core;
import java.util.*;
import static dev.minime.core.Regression.*;
final class VisibleDefaultRegression {
    static void run() {
        for(boolean privateField:new boolean[]{false,true}) {
            Editor editor=new Editor();CompositionEngine e=new CompositionEngine(editor,Learning.NONE);
            e.dictionary(dictionary);e.start(false,false,privateField,false);
            Candidate first=new Candidate("甲",false,100,2),second=new Candidate("乙",false,99,3),whole=new Candidate("丙丁",false,98),alternative=new Candidate("戊己",false,97);
            e.decoder((d,r,b,c,done)->done.accept(Arrays.asList(first,second,whole,alternative)),()->{});
            type(e,"nihao");
            equal("丙丁",e.candidates().get(e.preferred()).text,"whole-input Space choice survives prefix alternatives");
            equal(1,e.preferred(),"Space default is first visible candidate");
            equal("nihao",e.candidates().get(0).text,"literal recovery keeps its slot");
            equal("戊己",e.candidates().get(2).text,"another whole-input choice precedes partial recovery");
            equal(Arrays.asList("甲","乙"),Arrays.asList(e.candidates().get(3).text,e.candidates().get(4).text),"recovery order stays stable");
            e.space();equal("丙丁",editor.text,"visible winner agrees with acceptance");
        }
        // A crowded phrase list used to bury even the first character. Exercise
        // actual snapshot selection, delimiters, and supplementary Han boundaries.
        for(boolean privateField:new boolean[]{false,true})for(String raw:new String[]{"nihao","ni'hao"}) {
            Editor editor=new Editor();CompositionEngine e=new CompositionEngine(editor,Learning.NONE);
            e.dictionary(dictionary);e.start(false,false,privateField,false);
            List<Candidate> choices=new ArrayList<>();
            for(int i=0;i<24;i++)choices.add(new Candidate("詞組"+i,false,100-i));
            choices.add(new Candidate("甲乙",false,70,2));
            choices.add(new Candidate("甲",false,69,2));
            choices.add(new Candidate("𠀀",false,68,2));
            e.decoder((d,r,b,c,done)->done.accept(r.equals(raw)?choices:Collections.singletonList(new Candidate("好",false,100))),()->{});
            type(e,raw);
            equal("詞組0",e.candidates().get(e.preferred()).text,"character access preserves whole default");
            equal("甲",e.candidates().get(3).text,"first character stays reachable despite 24 phrases");
            equal("𠀀",e.candidates().get(4).text,"supplementary Han gets the same character access");
            equal(28,e.candidates().size(),"mixed display removes no choices");
            Candidate displayed=e.candidates().get(3);
            e.selectCandidate(displayed,e.compositionId());
            equal("甲",editor.text,"tap commits exactly the selected character");
            equal("hao",e.raw(),"remaining syllable survives character tap and delimiter");
            equal("hao",editor.composing,"remaining Pinyin stays composing");
            e.space();equal("甲好",editor.text,"Space resolves only the remaining input");
        }
    }
}
