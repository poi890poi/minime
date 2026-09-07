package dev.minime.core;

import java.io.*;
import java.util.*;

/** Immutable source-derived word/character context counts, never evaluation labels. */
public final class ContextModel {
    private final Map<String,List<Candidate>> english=new HashMap<>();
    private final Map<String,Integer> chinese=new HashMap<>(),totals=new HashMap<>();
    public ContextModel() { }
    ContextModel(BinaryModel.Reader in)throws IOException {in.words(english);in.counts(chinese);in.counts(totals);}
    void write(BinaryModel.Writer out)throws IOException {out.words(english);out.counts(chinese);out.counts(totals);}
    public static ContextModel load(Reader source)throws IOException {
        ContextModel m=new ContextModel();
        try(BufferedReader r=new BufferedReader(source)) {String line;while((line=r.readLine())!=null){
            String[] p=line.split("\t",-1);if(p.length!=4)throw new IOException("Invalid context row");int count=Integer.parseInt(p[3]);
            if(p[0].equals("en"))m.english.computeIfAbsent(p[1],k->new ArrayList<>()).add(new Candidate(p[2],true,count));
            else {m.chinese.put(p[1]+"\t"+p[2],count);m.totals.merge(p[1],count,Integer::sum);}
        }}
        for(List<Candidate> list:m.english.values()) {list.sort(Comparator.comparingDouble((Candidate c)->c.score).reversed().thenComparing(c->c.text));if(list.size()>24)list.subList(24,list.size()).clear();}
        return m;
    }
    public List<Candidate> english(String context) {
        if(context.isEmpty())return Collections.emptyList();
        List<Candidate> result=english.get(context.toLowerCase(Locale.ROOT));
        if(result==null){int at=context.lastIndexOf(' ');result=english.get(context.substring(at+1).toLowerCase(Locale.ROOT));}
        return result==null?Collections.emptyList():result;
    }
    public double chinese(String context,String text) {
        double score=0;String before=context;
        for(int cp:text.codePoints().toArray()) {
            String next=new String(Character.toChars(cp));String key=tail(before,2);
            if(!totals.containsKey(key))key=tail(before,1);
            if(!key.isEmpty() && totals.containsKey(key)) {
                double prior=(chinese.getOrDefault("\t"+next,0)+1.0)/(totals.getOrDefault("",1)+5000.0);
                double observed=(chinese.getOrDefault(key+"\t"+next,0)+20*prior)/(totals.get(key)+20.0);
                score+=Math.max(-2,Math.min(2,Math.log10(observed/prior)));
            }
            before=tail(before+next,2);
        }
        return score;
    }
    private static String tail(String value,int count) {return value.substring(value.offsetByCodePoints(value.length(),-Math.min(count,value.codePointCount(0,value.length()))));}
}
