package dev.minime.core;

import static dev.minime.core.Regression.*;
import java.io.*;

final class CapitalizedVocabularyRegression {
    static void run() throws Exception {
        for(String word:new String[]{"january","september","london","chicago","html","british","american","thursday"}) {
            yes(dictionary.isEnglish(word,true),"capitalized upstream entry recognized in Latin context: "+word);
            Editor e=new Editor();CompositionEngine c=engine(e,Learning.NONE,false);type(c,"in "+word+" ");
            equal("in "+word+" ",e.text,"source-backed word preserved after Latin");
            c.start(false,false,false,false,true);e.text="";c.englishOptions(true,false);type(c,word+" ");
            equal(word+" ",e.text,"English correction respects capitalized-source vocabulary");
            c.start(false,false,true,false);e.text="";type(c,"in "+word+" ");
            equal("in "+word+" ",e.text,"private fields retain nonpersonal lexical recognition");
        }
        Editor e=new Editor();CompositionEngine c=engine(e,Learning.NONE,false);type(c,"taiwan");
        yes(!c.candidates().get(c.preferred()).literal,"capitalized name does not take over fresh Pinyin");
        c.start(false,false,false,false);type(c,"in taiwan");equal(0,c.preferred(),"same spelling is recognized in established Latin input");
        c.select(find(c,"臺灣"));equal("in 臺灣",e.text,"explicit Chinese still available at Latin boundary");
        c.start(false,false,false,false,true);type(c,"jan");
        yes(c.candidates().stream().anyMatch(v->v.text.equals("january")),"English prefix uses complete source vocabulary");
        // Serialized dictionaries reconstruct the derived case-folded lookup.
        ByteArrayOutputStream out=new ByteArrayOutputStream();dictionary.writeBinary(out);
        PhoneticDictionary restored=PhoneticDictionary.readBinary(new ByteArrayInputStream(out.toByteArray()));
        yes(restored.isEnglish("thursday",true),"binary restores extended recognition");
        equal(dictionary.isEnglish("taiwan"),restored.isEnglish("taiwan"),"binary preserves Chinese-boundary recognition");
    }
}
