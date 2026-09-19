package dev.minime.core;

import java.io.*;
import java.util.*;
import static dev.minime.core.Regression.*;

/** Reading evidence contracts; synthetic fixtures are not language accuracy. */
final class MatchEvidenceRegression {
    private static Candidate only(List<Candidate> values) {
        equal(1,values.size(),"Single source identity survives duplicate match paths");
        return values.get(0);
    }
    private static void evidence(Candidate c,boolean omitted,boolean units,String label) {
        equal(omitted,c.incomplete,label+" untyped letters");
        equal(units,c.abbreviated,label+" all source units matched with omissions");
    }
    static void run(PhoneticDictionary dictionary)throws Exception {
        String reading="ka'na'pa",text="測試詞";
        Map<String,List<Candidate>> source=Collections.singletonMap(reading,
            Collections.singletonList(new Candidate(text,false,4)));
        ReadingUnitIndex units=new ReadingUnitIndex(source,source.keySet());
        evidence(only(units.lookup("kanapa")),false,false,"Fully typed units");
        for(String raw:Arrays.asList("knp","k'na'p","kanap","ka'n'pa")) {
            Candidate c=only(units.lookup(raw));evidence(c,true,true,"Initial/mixed units "+raw);
            equal(0,c.consumed,"Whole-token acceptance span remains separate from spelling evidence");
            for(Candidate copy:Arrays.asList(c.withScore(7),c.consuming(2),c.inPack("poj"),
                    c.asConstructed().asAttested(),c.asLanguageCharacter(),c.asTransliteration(),
                    c.paired(new PairedForms.Pair(text,"甲乙丙","fixture")).primary(true),
                    c.readable(s->true)))evidence(copy,true,true,"Copy retains evidence");
        }
        equal(0,units.lookup("ka").size(),"Unit matcher cannot invent unmatched source units");
        ReadingIndex prefix=new ReadingIndex(Collections.singletonMap("kanapa",source.get(reading)));
        evidence(only(prefix.complete("ka",(key,c)->true)),true,false,"Forward spelling prediction");

        for(String pack:Arrays.asList("taiwan","geography","poj","japanese")) {
            String row=pack+"\t"+reading+"\t"+text+"\tfixture\tfixture\n";
            AddonDictionary original=AddonDictionary.read(new StringReader(row));
            ByteArrayOutputStream bytes=new ByteArrayOutputStream();original.writeBinary(bytes);
            for(AddonDictionary model:Arrays.asList(original,AddonDictionary.readBinary(new ByteArrayInputStream(bytes.toByteArray())))) {
                Set<String> enabled=Collections.singleton(pack);
                evidence(only(model.lookup("kanapa",enabled)),false,false,pack+" exact");
                evidence(only(model.lookup("ka",enabled)),true,false,pack+" future units");
                // Both flat-prefix and source-unit searches return this text at
                // the same score. Dedup must retain unit evidence, not run order.
                evidence(only(model.lookup("kanap",enabled)),true,true,pack+" tied paths");
                evidence(only(model.lookup("knp",enabled)),true,true,pack+" initials");
            }
        }
        Candidate alias=Candidate.supplement(text,0,true);
        evidence(alias,true,true,"Stored initial alias");
        evidence(alias.completing(0),true,false,"New forward completion clears all-unit claim");
        Candidate exact=new Candidate("詞",false,0);
        evidence(Candidate.concatenate(exact,alias),true,true,"All assembled units covered");
        evidence(Candidate.concatenate(exact.completing(0),alias),true,false,"Assembly with unmatched unit");

        int completions=0;
        for(char a='a';a<='z';a++)for(char b='a';b<='z';b++)
            for(Candidate c:dictionary.englishCompletions(""+a+b)) {
                evidence(c,true,false,"English completion");yes(c.literal,"English spacing identity retained");completions++;
            }
        yes(completions>0,"English completion contract exercised on production source");
        System.out.println("PASS reading evidence: source-unit, flat-prefix, all packs, binary/copy parity and English completion");
    }
}
