package dev.minime.core;

import java.io.*;
import java.util.*;

/** Deeper source-owned alternatives must remain selectable for abbreviated input. */
public final class PartialDepthRegression {
    static void run()throws Exception{
        Map<String,List<Candidate>> source=new HashMap<>();List<Candidate> words=new ArrayList<>();
        for(int i=0;i<96;i++)words.add(new Candidate("甲"+new String(Character.toChars(0x4e00+i)),false,100-i));
        source.put("ni'hao",words);ReadingUnitIndex index=new ReadingUnitIndex(source,source.keySet());
        for(String raw:Arrays.asList("nihao","nh","nhao","nih","n'hao","ni'h","n'h")){
            List<Candidate> values=index.lookup(raw);
            for(int i=0;i<words.size();i++){
                final String expected=words.get(i).text;
                Regression.yes(values.stream().anyMatch(c->c.text.equals(expected)),"Stored alternatives remain reachable beyond the first row: "+raw+" rank "+(i+1));
                Regression.equal(words.get(i).text,values.get(i).text,"Deeper retrieval preserves frequency order");
            }
            Regression.yes(values.stream().noneMatch(c->c.composed||c.consumed!=0),"Abbreviation lookup returns stored whole entries only");
        }
        System.out.println("PASS source-backed partial depth: 96 alternatives at seven full/initial/mixed spellings");
    }
    public static void main(String[] args)throws Exception{run();}
}
