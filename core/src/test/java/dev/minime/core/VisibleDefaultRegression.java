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
    }
}
