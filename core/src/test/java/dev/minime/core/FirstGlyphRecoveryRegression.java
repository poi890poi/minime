package dev.minime.core;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import static dev.minime.core.Regression.*;

/** Exhaustive source-syllable recovery and lifecycle contracts, not accuracy. */
final class FirstGlyphRecoveryRegression {
    static List<String> signature(List<Candidate> values) {
        List<String> result=new ArrayList<>();for(Candidate c:values)result.add(c.text+":"+c.consumed+":"+c.score);return result;
    }
    static boolean prefix(Candidate c,String raw){return !c.literal&&c.consumed>0&&c.consumed<raw.length();}
    static void run()throws Exception {
        SortedSet<String> all=new TreeSet<>();
        for(String line:Files.readAllLines(Paths.get("app/src/main/assets/syllables.tsv")))all.add(line.split("\t")[0]);
        List<String> syllables=new ArrayList<>(all);int cases=0,steps=0;
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();dictionary.writeBinary(bytes);
        PhoneticDictionary binary=PhoneticDictionary.readBinary(new ByteArrayInputStream(bytes.toByteArray()));
        for(int i=0;i<syllables.size();i++) {
            String first=syllables.get(i),tail=syllables.get((i+71)%syllables.size())+"'"+syllables.get((i+137)%syllables.size());
            String raw=first+"'"+tail;
            List<Candidate> values=dictionary.convert(raw,false);
            List<Candidate> glyphs=new ArrayList<>();
            for(Candidate c:dictionary.convert(first,false))if(c.consumed==0&&!c.incomplete&&c.text.codePointCount(0,c.text.length())==1)glyphs.add(c);
            for(Candidate glyph:glyphs) {
                yes(values.stream().anyMatch(c->c.text.equals(glyph.text)&&c.consumed==first.length()),"Every attested homophone survives explicit first-syllable recovery: "+raw+"/"+glyph.text);
            }
            equal(signature(values),signature(binary.convert(raw,false)),"Text and packaged binary recovery parity: "+raw);
            for(boolean priv:new boolean[]{false,true}) {
                Editor e=new Editor();CompositionEngine c=engine(e,Learning.NONE,false);c.start(false,false,priv,false);type(c,raw);
                List<Candidate> partials=new ArrayList<>();for(Candidate v:c.candidates())if(prefix(v,raw))partials.add(v);
                if(!glyphs.isEmpty())yes(!partials.isEmpty(),"First glyph present in both privacy modes: "+raw);
                if(!partials.isEmpty()) {
                    Candidate chosen=partials.get(0);long id=c.compositionId();c.selectCandidate(chosen,id);
                    String rest=raw.substring(chosen.consumed).replaceFirst("^'+","");
                    if(!glyphs.isEmpty())equal(first.length(),chosen.consumed,"Known first syllable owns its complete span: "+raw);
                    equal(chosen.text,e.text,"Only explicitly chosen glyph committed");
                    equal(rest,c.raw(),"Every unconsumed key retained, including incomplete units: "+raw);
                    c.selectCandidate(chosen,id);equal(rest,c.raw(),"Stale prefix gesture rejected");
                    c.backspace();equal(rest.substring(0,rest.length()-1),c.raw(),"Backspace edits only remaining spelling");
                }
                cases++;
            }
            if(i%16==0)for(int length:new int[]{3,6,10})for(boolean initials:new boolean[]{false,true}) {
                List<String> parts=new ArrayList<>();for(int j=0;j<length;j++){String s=syllables.get((i+j*71)%syllables.size());parts.add(initials?s.substring(0,1):s);}
                Editor e=new Editor();CompositionEngine c=engine(e,Learning.NONE,false);type(c,String.join("'",parts));int guard=0;
                while(!c.raw().isEmpty()) {
                    String before=c.raw();Candidate choice=c.candidates().stream().filter(v->prefix(v,before)).findFirst().orElse(null);
                    if(choice==null) {c.select(0);break;}
                    String committed=e.text;long id=c.compositionId();c.selectCandidate(choice,id);
                    equal(committed+choice.text,e.text,"Repeated recovery commits exactly the selected glyph");
                    equal(before.substring(choice.consumed).replaceFirst("^'+",""),c.raw(),"Repeated recovery retains every unconsumed key");
                    yes(c.raw().length()<before.length(),"Recovery makes progress");
                    if(++guard>96)throw new AssertionError("Recovery loop");steps++;
                }
            }
        }
        // A row with one phrase must not promote its third glyph while trying
        // to reserve two phrase slots. Cover one/two/many and private sessions.
        for(int words:new int[]{1,2,24})for(boolean priv:new boolean[]{false,true}) {
            Editor e=new Editor();CompositionEngine c=engine(e,Learning.NONE,false);c.start(false,false,priv,false);
            c.decoder((d,r,z,ctx,done)-> {
                List<Candidate> out=new ArrayList<>();for(int i=0;i<words;i++)out.add(new Candidate("詞組"+i,false,100-i));
                out.add(new Candidate("甲",false,200,2));out.add(new Candidate("乙",false,190,2));out.add(new Candidate("丙",false,180,2));done.accept(out);
            },()->{});
            type(c,"nihao");equal("詞組0",c.candidates().get(c.preferred()).text,"Whole phrase still owns Space");
            yes(find(c,"甲")<find(c,"乙")&&find(c,"乙")<find(c,"丙"),"Mixed row preserves complete homophone rank with "+words+" phrases");
        }
        for(String raw:Arrays.asList("name@example.com","example.com","abc123","foo_bar","/nihao"))
            yes(dictionary.convert(raw,false).stream().noneMatch(c->prefix(c,raw)),"Technical spelling has no synthetic Chinese prefix: "+raw);
        // Prefix recovery must not activate a later incomplete add-on as a
        // whole-token Space default. Vary glyph count around preview slots.
        for(int count:new int[]{1,2,4,12}) {
            Editor e=new Editor();CompositionEngine c=engine(e,Learning.NONE,false);
            c.addons(AddonDictionary.read(new StringReader("taiwan\tni'hao'ma\t測試詞\tfixture\tfixture\n")),Collections.singleton("taiwan"));
            final int total=count;
            c.decoder((d,r,z,ctx,done)->{List<Candidate> values=new ArrayList<>();for(int i=0;i<total;i++)values.add(new Candidate(new String(Character.toChars(0x4e00+i)),false,100-i,2));done.accept(values);},()->{});
            type(c,"nihaom");equal(0,c.preferred(),"Partial glyphs cannot activate incomplete add-on acceptance");
        }
        System.out.println("PASS first-glyph recovery: "+syllables.size()+" syllables, "+cases+" privacy episodes, "+steps+" repeated prefix selections, complete homophones and binary parity");
    }
}
