package dev.minime.core;

import java.io.StringReader;
import java.util.*;
import static dev.minime.core.Regression.*;

/** Synthetic acceptance/lifecycle contracts, not language-model accuracy. */
final class CompletionBoundaryRegression {
    public static void main(String[] args) throws Exception {run();}
    static PhoneticDictionary tiny;
    static CompositionEngine setup(Editor e,InputMode mode) {
        CompositionEngine c=new CompositionEngine(e,Learning.NONE);c.dictionary(tiny);
        c.start(false,false,false,false,mode.english());c.switchMode(mode,false);return c;
    }
    static void completion(CompositionEngine c) {type(c,"alph");c.select(find(c,"alpha"));}
    static void run() throws Exception {
        tiny=PhoneticDictionary.load(new StringReader(""),new StringReader("alpha\t100\nbeta\t100\n"),new StringReader(""),new StringReader(""));
        for(InputMode mode:InputMode.values())if(mode.englishEnabled()) {
            for(String action:Arrays.asList("space","select","enter","confirm","raw","literal","edit")) {
                Editor e=new Editor();CompositionEngine c=setup(e,mode);completion(c);
                equal("alpha",e.text,"completion has no immediate trailing space: "+mode);
                type(c,"beta");
                if(action.equals("edit")) {c.backspace();type(c,"a");}
                switch(action) {
                    case "space":case "edit":c.space();break;
                    case "select":c.select(0);break;
                    case "enter":c.enter();break;
                    case "confirm":c.confirm();break;
                    case "raw":c.commitRaw(false);break;
                    case "literal":c.literal(",");break;
                }
                String suffix=action.equals("space")||action.equals("edit")?" ":action.equals("enter")?"\n":action.equals("literal")?",":"";
                equal("alpha beta"+suffix,e.text,"separate accepted English: "+mode+"/"+action);
            }
            for(String separator:Arrays.asList(" ",",",".","。","\n")) {
                Editor e=new Editor();CompositionEngine c=setup(e,mode);completion(c);
                if(separator.equals(" "))c.space();else if(separator.equals("\n"))c.enter();else c.literal(separator);
                type(c,"beta");c.space();equal("alpha"+separator+"beta ",e.text,"explicit boundary cancels pending separator: "+mode+separator);
            }
            Editor e=new Editor();CompositionEngine c=setup(e,mode);completion(c);c.abandon();type(c,"beta");c.space();
            equal("alphabeta ",e.text,"cursor/lifecycle invalidation cannot insert a separator");
            e=new Editor();c=setup(e,mode);completion(c);c.backspace();type(c,"beta");c.space();
            equal("alphbeta ",e.text,"delete accepted completion cancels boundary");
        }
        for(String output:Arrays.asList("你好","かな","alpha")) {
            Editor e=new Editor();CompositionEngine c=setup(e,InputMode.CHINESE);completion(c);
            c.decoder((d,r,z,ctx,done)->done.accept(Collections.singletonList(
                output.equals("alpha")?Candidate.supplement(output,100).inPack("poj"):new Candidate(output,false,100))),()->{});
            type(c,"zxcv");c.select(find(c,output));equal("alpha"+output,e.text,"foreign acceptance has no English separator");
        }
        Editor e=new Editor();CompositionEngine c=setup(e,InputMode.CHINESE);completion(c);
        List<Runnable> pending=new ArrayList<>();
        c.decoder((d,r,z,ctx,done)->pending.add(()->done.accept(Collections.emptyList())),()->{});
        type(c,"beta");c.space();equal("alpha",e.text,"deferred prediction owns acceptance");
        pending.get(pending.size()-1).run();equal("alpha beta ",e.text,"queued Space resolves boundary once");
        e=new Editor();c=setup(e,InputMode.ENGLISH);completion(c);type(c,"éclair");c.space();
        equal("alpha éclair ",e.text,"existing English Unicode letter boundary remains supported");
        System.out.println("PASS completion boundary: mixed acceptance, punctuation, edits, foreign provenance and async Space");
    }
}
