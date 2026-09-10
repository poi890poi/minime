package dev.minime.core;
import java.nio.file.*;import java.io.*;import java.util.*;
public final class UnmarkedProbe {
 public static void main(String[] args)throws Exception {
  AddonDictionary words=AddonDictionary.readBinary(Files.newInputStream(Paths.get("app/build/generated/minimeAssets/addon-poj.bin")));
  PhoneticDictionary base=PhoneticDictionary.readBinary(Files.newInputStream(Paths.get("app/build/generated/minimeAssets/model.bin")));
  CompositionEngine c=new CompositionEngine(new CompositionEngine.Editor(){public void composing(String t){}public void commit(String t){}public void delete(){}public void enter(){}public void finish(){}},Learning.NONE);
  c.dictionary(base);c.addons(words,Collections.singleton("poj"));
  try(PrintWriter out=new PrintWriter("artifacts/poj-unmarked/results.tsv","UTF-8")) {
   out.println("query\toutput\tgroup\tlookup_rank\tlookup_paired\tvisible_focused_rank\traw_row_paired\tdefault_output");
   for(String line:Files.readAllLines(Paths.get("artifacts/poj-unmarked/inputs.tsv"))) {
    String[] p=line.split("\t");List<Candidate> matches=words.lookup(p[0],Collections.singleton("poj"));int rank=0;boolean paired=false;
    for(int i=0;i<matches.size();i++)if(matches.get(i).text.equals(p[1])){rank=i+1;paired=matches.get(i).pair!=null;break;}
    c.start(false,false,false,false);c.switchMode(InputMode.TAIWANESE_ENGLISH,false);p[0].codePoints().forEach(c::type);
    int visible=0;for(int i=0;i<c.candidates().size();i++)if(c.candidates().get(i).text.equals(p[1])&&c.candidates().get(i).pack.equals("poj")){visible=i+1;break;}
    out.println(line+"\t"+rank+"\t"+paired+"\t"+visible+"\t"+(c.candidates().get(0).pair!=null)+"\t"+c.candidates().get(c.preferred()).text);
   }
  }
 }
}
