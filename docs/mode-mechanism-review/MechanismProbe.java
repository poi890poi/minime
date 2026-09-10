package dev.minime.core;
import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Read-only diagnostic: production source samples and one representation control. */
public final class MechanismProbe {
 static final class Memory implements Learning {
  int choices,phrases; public int count(String c,String r,String v){return 0;}
  public void choose(String c,String r,String v){choices++;}
  public void observePhrase(String r,String v){phrases++;}
 }
 static PhoneticDictionary base;
 static CompositionEngine engine(AddonDictionary words,Memory memory,InputMode mode,boolean priv) {
  CompositionEngine c=new CompositionEngine(new CompositionEngine.Editor(){public void composing(String t){}public void commit(String t){}public void delete(){}public void enter(){}public void finish(){}},memory);
  c.dictionary(base);c.start(false,false,priv,false,mode.english());c.switchMode(mode,false);
  c.addons(words,new HashSet<>(Arrays.asList("poj","japanese","taiwan")));c.phraseLearning(true);return c;
 }
 static void type(CompositionEngine c,String s){s.codePoints().forEach(c::type);}
 static Set<String> texts(List<Candidate> list){Set<String>s=new TreeSet<>();for(Candidate c:list)s.add(c.text);return s;}
 public static void main(String[] args)throws Exception {
  Path assets=Paths.get("app/src/main/assets");
  base=PhoneticDictionary.readBinary(Files.newInputStream(Paths.get("app/build/generated/minimeAssets/model.bin")));
  List<String> rows=Files.readAllLines(assets.resolve("addons.tsv"));
  AddonDictionary words=AddonDictionary.read(new StringReader(String.join("\n",rows)));
  try(PrintWriter report=new PrintWriter("artifacts/mode-mechanism-review/probes.tsv","UTF-8")) {
   report.println("pack\tquery\tnormal_focused\tprivate_focused\taccepted_output\tchoice_writes_after_3\tphrase_observations_after_3\tidle_candidates");
   List<String> sample=new ArrayList<>(rows);sample.sort(Comparator.comparingInt(String::hashCode));
   for(String pack:Arrays.asList("poj","japanese")) {
    Set<String> seen=new HashSet<>();int n=0;
    for(String row:sample){String[]p=row.split("\t");if(p.length!=5 || !p[0].equals(pack))continue;
     String query=p[1].replaceAll("[- ']+","");if(!query.matches("[a-z]{2,16}") || !seen.add(query))continue;
     InputMode mode=pack.equals("poj")?InputMode.TAIWANESE_ENGLISH:InputMode.JAPANESE_ENGLISH;
     Memory m=new Memory();CompositionEngine c=engine(words,m,mode,false);type(c,query);
     Candidate selected=c.candidates().stream().filter(v->v.pack.equals(pack)).findFirst().orElse(null);if(selected==null)continue;
     long normal=c.candidates().stream().filter(v->v.pack.equals(pack)).count();
     for(int i=0;i<3;i++){if(i>0){c.start(false,false,false,false);c.switchMode(mode,false);type(c,query);}for(int j=0;j<c.candidates().size();j++)if(c.candidates().get(j).text.equals(selected.text)){c.select(j);break;}}
     int idle=c.candidates().size();CompositionEngine privateEngine=engine(words,new Memory(),mode,true);type(privateEngine,query);
     report.println(pack+"\t"+query+"\t"+normal+"\t"+privateEngine.candidates().stream().filter(v->v.pack.equals(pack)).count()+"\t"+selected.text+"\t"+m.choices+"\t"+m.phrases+"\t"+idle);
     if(++n==16)break;
    }
   }
  }
  // Control: same distinct source candidates, readings and order; remove only
  // repeated rows within an index's reading/output identity before index building.
  Set<String> unique=new HashSet<>();List<String> compact=new ArrayList<>();Map<String,List<String>> terminals=new TreeMap<>();
  for(String row:rows){if(row.startsWith("#"))continue;String[]p=row.split("\t");
   String tier=p[0]+((p[0].equals("poj") && p[4].equals("extended_vocabulary")) || (p[0].equals("japanese") && p[4].equals("everyday_vocabulary"))?"-common":"");
   String key=p[1].toLowerCase(Locale.ROOT).replaceAll("[- ']+","'");
   if(p[0].equals("poj")||p[0].equals("japanese"))terminals.computeIfAbsent(tier+"\t"+key,k->new ArrayList<>()).add(p[2]);
   if(unique.add(tier+"\t"+key+"\t"+p[2]))compact.add(row);
  }
  AddonDictionary control=AddonDictionary.read(new StringReader(String.join("\n",compact)));
  Set<String> queries=new TreeSet<>();
  for(Map.Entry<String,List<String>> e:terminals.entrySet())if(e.getValue().size()>24 && new HashSet<>(e.getValue().subList(0,24)).size()<new HashSet<>(e.getValue()).size()){
   String key=e.getKey().split("\t")[1];for(int len=1;len<=key.length();len++)queries.add(key.substring(0,len));
  }
  try(PrintWriter report=new PrintWriter("artifacts/mode-mechanism-review/dedup.tsv","UTF-8")){
   report.println("query\tbefore_count\tafter_count\tgained\tlost");
   for(String query:queries){Set<String>a=texts(words.lookup(query,Collections.singleton("poj"))),b=texts(control.lookup(query,Collections.singleton("poj")));Set<String> gain=new TreeSet<>(b),loss=new TreeSet<>(a);gain.removeAll(a);loss.removeAll(b);
    report.println(query+"\t"+a.size()+"\t"+b.size()+"\t"+String.join("|",gain)+"\t"+String.join("|",loss));}
  }
  System.out.println("Completed source-derived privacy/learning probes and "+queries.size()+" duplicate-row controls.");
 }
}
