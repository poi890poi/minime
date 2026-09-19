package dev.minime.core;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.zip.GZIPInputStream;

/** Source retrieval audit, with an independent exhaustive syllable-prefix oracle. */
public final class SuggestionCoverageAudit {
    static final class Entry {
        final String text;final String[] units;final double score;final int letters;
        Entry(String[] p){text=p[2];units=p[0].split("'");score=Math.log10(Double.parseDouble(p[3])+1)-9;letters=p[0].replace("'","").length();}
    }
    // Deliberately no trie, beam, source-index bounds or candidate caps here.
    static boolean matches(String raw,String[] units){
        boolean[] at=new boolean[raw.length()+1];at[0]=true;
        for(String unit:units){
            boolean[] next=new boolean[at.length];
            for(int i=0;i<raw.length();i++)if(at[i]){
                for(int used=1;used<=unit.length()&&i+used<=raw.length();used++){
                    if(raw.charAt(i+used-1)!=unit.charAt(used-1))break;
                    int end=i+used;if(end<raw.length()&&raw.charAt(end)=='\'')end++;
                    next[end]=true;
                }
            }at=next;
        }
        return at[raw.length()];
    }
    public static void main(String[] args)throws Exception{
        Path assets=Paths.get("app/src/main/assets");
        PhoneticDictionary d=PhoneticDictionary.load(Files.newBufferedReader(assets.resolve("zh_tw.tsv")),Files.newBufferedReader(assets.resolve("en_us.tsv")),Files.newBufferedReader(assets.resolve("syllables.tsv")),Files.newBufferedReader(assets.resolve("context.tsv")));
        Set<String> oracleInputs=args.length>2?new HashSet<>(Files.readAllLines(Paths.get(args[2]))):Collections.emptySet();
        Map<Character,List<Entry>> byInitial=new HashMap<>();
        Map<String,List<Entry>> byText=new HashMap<>();
        for(String line:Files.readAllLines(assets.resolve("zh_tw.tsv"))){String[] p=line.split("\t");Entry e=new Entry(p);byInitial.computeIfAbsent(p[0].charAt(0),k->new ArrayList<>()).add(e);byText.computeIfAbsent(e.text,k->new ArrayList<>()).add(e);}
        try(BufferedReader in=new BufferedReader(new InputStreamReader(new GZIPInputStream(Files.newInputStream(Paths.get(args[0]))),StandardCharsets.UTF_8));
            BufferedWriter out=Files.newBufferedWriter(Paths.get(args[1]));BufferedWriter oracle=Files.newBufferedWriter(Paths.get(args[1]+".oracle.tsv"))){
            out.write(in.readLine()+"\ttarget_rank\tcandidates\tlookup_ns\ttop8\n");
            oracle.write("raw\ttarget\toracle_rank\tlength_rank\treturned\n");
            String line,lastRaw=null;List<Candidate> found=Collections.emptyList();Map<String,Integer> ranks=new HashMap<>();String preview="";long nanos=0;int rows=0,queries=0,prefixes=0;
            while((line=in.readLine())!=null){
                String[] p=line.split("\t",-1);String raw=p[5];
                if(!raw.equals(lastRaw)){
                    lastRaw=raw;long start=System.nanoTime();found=d.convert(raw,false);nanos=System.nanoTime()-start;queries++;
                    ranks.clear();List<String> top=new ArrayList<>();
                    for(int i=0;i<found.size();i++){
                        Candidate c=found.get(i);if(c.composed)throw new AssertionError("Constructed output: "+raw);
                        if(c.consumed>0 && c.consumed<raw.length() && c.text.codePointCount(0,c.text.length())>1) {
                            String spelling=raw.substring(0,c.consumed);
                            if(byText.getOrDefault(c.text,Collections.emptyList()).stream().noneMatch(e->matches(spelling,e.units)))throw new AssertionError("Unattested phrase or invalid prefix span: "+raw+"/"+c.text+"/"+c.consumed);
                            prefixes++;
                        }
                        if(c.consumed==0)ranks.put(c.text,i+1);if(i<8)top.add(c.text+":"+c.consumed);
                    }preview=String.join("|",top);
                    if(oracleInputs.contains(raw)){
                        Map<String,Double> best=new HashMap<>();int typed=raw.replace("'","").length();
                        for(Entry e:byInitial.getOrDefault(raw.charAt(0),Collections.emptyList()))if(e.units.length<=typed&&matches(raw,e.units)){
                            int missing=e.letters-typed;double score=e.score-(missing==0?0:.7+.08*missing);best.merge(e.text,score,Math::max);
                        }
                        List<String> texts=new ArrayList<>(best.keySet());texts.sort(Comparator.comparingDouble((String t)->best.get(t)).reversed().thenComparing(t->t));
                        Map<Integer,Integer> lengths=new HashMap<>();
                        for(int i=0;i<texts.size();i++){
                            String text=texts.get(i);int length=text.codePointCount(0,text.length()),rank=lengths.merge(length,1,Integer::sum);
                            if(i<24||rank<=6)oracle.write(raw+"\t"+text+"\t"+(i+1)+"\t"+rank+"\t"+ranks.containsKey(text)+"\n");
                        }
                    }
                }
                out.write(line+"\t"+ranks.getOrDefault(p[3],0)+"\t"+found.size()+"\t"+nanos+"\t"+preview+"\n");
                if(++rows%50000==0){out.flush();oracle.flush();System.out.println("Retrieval rows "+rows+", unique queries "+queries);}
            }
            System.out.println("PASS retrieval audit completed: "+rows+" rows / "+queries+" queries; "+prefixes+" phrase-prefix outputs independently checked against source readings; evaluate target ranks separately.");
        }
    }
}
