package dev.minime.core;

import java.io.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;
import java.util.function.Consumer;
import java.util.zip.*;

/** Isolated experiments. Reference labels are used only after a query returns. */
public final class ProposalBenchmark {
    static final Path ROOT=Paths.get(""), OUT=Paths.get("docs/language-contract-benchmark"), WORK=Paths.get("artifacts/language-contract-benchmark"), ASSET=Paths.get("app/build/generated/minimeAssets");
    static final Set<String> ENABLED=new HashSet<>(Arrays.asList("taiwan","geography","poj","japanese"));
    static final InputMode[] MODES={InputMode.CHINESE,InputMode.ENGLISH,InputMode.TAIWANESE_ENGLISH,InputMode.JAPANESE_ENGLISH};
    static String variant;static PhoneticDictionary base;static AddonDictionary all;
    static class Editor implements CompositionEngine.Editor {
        String text="";public void composing(String s){}public void commit(String s){text+=s;}public void delete(){}public void enter(){}public void finish(){}
    }
    static class Memory implements Learning {
        List<String> namespaces=new ArrayList<>();int english;
        public int count(String c,String r,String v){return 0;}
        public void choose(String c,String r,String v){namespaces.add(c);}
        public void rememberEnglish(String c,String w){english++;}
    }
    static class Decoder implements CompositionEngine.Decoder,AutoCloseable {
        SpeculationBenchmark.Native nativeRime;final boolean cache;
        Map<String,List<Candidate>> values=new HashMap<>();Runnable pending;
        long coreNs,nativeNs,addonNs;
        Decoder(boolean cache){this.cache=cache;}
        List<Candidate> nativeQuery(String raw){try{if(nativeRime==null)nativeRime=new SpeculationBenchmark.Native(121);return nativeRime.query(raw);}catch(Exception e){throw new RuntimeException(e);}}
        public void convert(PhoneticDictionary d,String r,boolean z,String c,Consumer<List<Candidate>> done){throw new AssertionError();}
        public void query(PhoneticDictionary d,String r,boolean z,String c,boolean phonetic,AddonDictionary a,Set<String> packs,Consumer<List<Candidate>> done){
            pending=()->{
                String key=phonetic+"\t"+c+"\t"+r+"\t"+new TreeSet<>(packs);
                List<Candidate> found=cache?values.get(key):null;
                if(found==null){
                    found=new ArrayList<>();long at=System.nanoTime();
                    if(phonetic){List<Candidate> core=d.convert(r,z,c);coreNs+=System.nanoTime()-at;at=System.nanoTime();List<Candidate> nat=nativeQuery(r);nativeNs+=System.nanoTime()-at;found.addAll(CandidateMerge.merge(nat,core));}
                    at=System.nanoTime();found.addAll(a.lookup(r,packs));addonNs+=System.nanoTime()-at;
                    if(cache)values.put(key,found);
                }
                done.accept(new ArrayList<>(found));
            };
        }
        void flush(){if(pending!=null){Runnable r=pending;pending=null;r.run();}}
        public void close()throws Exception {if(nativeRime!=null)nativeRime.close();}
    }
    static void load()throws Exception {
        base=PhoneticDictionary.readBinary(Files.newInputStream(ASSET.resolve("model.bin")));
        base.englishSpelling(Files.newBufferedReader(ASSET.resolve("en_spelling.tsv")));
        all=AddonDictionary.EMPTY;
        for(String p:Arrays.asList("taiwan","poj","japanese")) {
            Path path=variant.equals("dedup")?WORK.resolve("dedup/addon-"+p+".bin"):ASSET.resolve("addon-"+p+".bin");
            all=AddonDictionary.combine(all,AddonDictionary.readBinary(Files.newInputStream(path)));
        }
        all=AddonDictionary.withJapaneseBasics(all,JapaneseBasics.read(Files.newBufferedReader(ASSET.resolve("japanese-basic.tsv"))));
        all=AddonDictionary.combine(all,AddonDictionary.read(Files.newBufferedReader(ASSET.resolve("geography.tsv"))));
    }
    static CompositionEngine engine(InputMode mode,Editor editor,Memory m,boolean priv,Decoder decoder){
        CompositionEngine c=new CompositionEngine(editor,m);c.dictionary(base);c.start(false,false,priv,false,mode.english());c.switchMode(mode,false);
        if(decoder!=null)c.decoder(decoder,()->{});c.addons(all,ENABLED);c.englishOptions(true,false);return c;
    }
    static void type(CompositionEngine c,String s){s.codePoints().forEach(c::type);}
    static String hash(String s){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new RuntimeException(e);}}
    static String sig(CompositionEngine c){StringBuilder b=new StringBuilder();for(Candidate v:c.candidates())b.append(v.text).append('\t').append(v.literal).append('\t').append(v.consumed).append('\t').append(v.incomplete).append('\t').append(v.pack).append('\n');return hash(b.toString());}
    static int rank(List<Candidate> list,String target,boolean raw){int at=0;for(Candidate c:list){if(!raw && c.literal && at==0){at++;continue;}if(c.text.equals(target)&&c.consumed==0)return at;at++;}return -1;}
    static PrintWriter writer(String suffix)throws Exception {return new PrintWriter(new OutputStreamWriter(new GZIPOutputStream(Files.newOutputStream(OUT.resolve(variant+"-"+suffix+".tsv.gz"))),StandardCharsets.UTF_8));}
    static List<String[]> rows()throws Exception {List<String[]> r=new ArrayList<>();for(String line:Files.readAllLines(OUT.resolve("inputs.tsv")))r.add(line.split("\t",-1));return r;}
    static List<InputMode> modes(String group){return group.startsWith("en-")?Arrays.asList(MODES):Collections.singletonList(group.startsWith("zh-")?InputMode.CHINESE:group.startsWith("poj")?InputMode.TAIWANESE_ENGLISH:InputMode.JAPANESE_ENGLISH);}
    static void context(CompositionEngine c,Decoder d,String ctx){if(!ctx.isEmpty())for(String s:ctx.split(" ")){type(c,s);d.flush();c.select(0);c.space();d.flush();}}
    static void coverage()throws Exception {
        try(Decoder d=new Decoder(true);PrintWriter out=writer("coverage")){
            out.println("row\tmode\tdefault_hit\trank_including_raw\tsuggestion_rank\tcount\tdefault_text\tsignature\tcomplete_choices\tincomplete_choices");int row=0;
            for(String[] p:rows()){
                for(InputMode mode:modes(p[0])){
                    CompositionEngine c=engine(mode,new Editor(),new Memory(),false,d);context(c,d,p[6]);type(c,p[4]);d.flush();
                    Candidate chosen=c.candidates().get(c.preferred());int exact=0,partial=0;for(Candidate v:c.candidates()){if(v.incomplete)partial++;else if(v.consumed==0)exact++;}
                    out.println(row+"\t"+mode.id+"\t"+(chosen.text.equals(p[5])&&chosen.consumed==0)+"\t"+rank(c.candidates(),p[5],true)+"\t"+rank(c.candidates(),p[5],false)+"\t"+c.candidates().size()+"\t"+chosen.text+"\t"+sig(c)+"\t"+exact+"\t"+partial);
                }
                if(++row%3000==0){out.flush();System.out.println(variant+" coverage "+row);}
            }
        }
    }
    static void mechanics()throws Exception {
        List<String> inputs=new ArrayList<>();for(String line:Files.readAllLines(Paths.get("docs/japanese-continuity/kana-oracle.tsv"))) {
            String[] p=line.split("\t",-1);if(!p[0].equals("input")&&p[0].matches("[a-z]{3,16}")&&Integer.parseInt(p[3])>0)inputs.add(p[0]);
        }
        inputs=new ArrayList<>(new HashSet<>(inputs));inputs.sort(Comparator.comparing(ProposalBenchmark::hash));
        try(Decoder d=new Decoder(true);PrintWriter out=writer("acceptance")){
            out.println("source\tcondition\taction\tprivate\tavailable\tsuffix_ok\tdefault_prefix\tfocus_learning\tother_learning\tcommitted\tremaining");
            for(String source:inputs.subList(0,Math.min(256,inputs.size())))for(String form:Arrays.asList("lower","upper","title","hyphen"))for(boolean priv:new boolean[]{false,true})for(String action:Arrays.asList("tap","space","stale")){
                String raw=form.equals("upper")?source.toUpperCase(Locale.ROOT):form.equals("title")?Character.toUpperCase(source.charAt(0))+source.substring(1):form.equals("hyphen")?"ka-"+source:source;
                Editor e=new Editor();Memory m=new Memory();CompositionEngine c=engine(InputMode.JAPANESE_ENGLISH,e,m,priv,d);type(c,raw);d.flush();
                Candidate partial=c.candidates().stream().filter(v->v.pack.equals("japanese")&&v.consumed>0&&v.consumed<raw.length()).findFirst().orElse(null);
                if(partial==null){out.println(source+"\t"+form+"\t"+action+"\t"+priv+"\tfalse\tfalse\tfalse\t0\t0\t\t");continue;}
                boolean prefix=c.candidates().get(c.preferred()).consumed>0;String expected=raw.substring(partial.consumed);boolean ok;
                if(action.equals("tap")){c.selectCandidate(partial,c.compositionId());d.flush();ok=c.raw().equals(expected);}
                else if(action.equals("space")){c.space();d.flush();ok=!prefix;}
                else{long id=c.compositionId();c.switchMode(InputMode.ENGLISH,false);d.flush();c.selectCandidate(partial,id);ok=c.raw().equals(raw)&&e.text.isEmpty();}
                long focus=m.namespaces.stream().filter(s->s.startsWith("FOCUS:")).count();
                out.println(String.join("\t",source,form,action,""+priv,"true",""+ok,""+prefix,""+focus,""+(m.namespaces.size()-focus),e.text,c.raw()));
            }
        }
        try(Decoder d=new Decoder(true);PrintWriter out=writer("continuation")){
            out.println("row\tmode\tprivate\tprovider_count\tidle_count\ttarget_rank\tlearning_calls\tcontext");int row=0;
            for(String[] p:rows()){
                if(p[0].startsWith("en-")&&p[3].equals("full")&&!p[6].isEmpty())for(InputMode mode:MODES)for(boolean priv:new boolean[]{false,true}){
                    Memory m=new Memory();CompositionEngine c=engine(mode,new Editor(),m,priv,d);
                    context(c,d,p[6]);out.println(row+"\t"+mode.id+"\t"+priv+"\t"+base.englishPredictions(p[6]).size()+"\t"+c.candidates().size()+"\t"+rank(c.candidates(),p[5],true)+"\t"+m.english+"\t"+c.context());
                }row++;
            }
        }
    }
    static void performance(int pass)throws Exception {
        List<String[]> corpus=rows();
        try(Decoder d=new Decoder(false);PrintWriter out=writer("latency-"+pass)){
            out.println("mode\titem\tkey\tphase\ttotal_ns\tcore_ns\tnative_ipc_ns\taddon_ns\tapply_and_raw_ns\tcount\tsignature");
            for(InputMode mode:MODES){
                List<String[]> selected=new ArrayList<>();Set<String> seen=new HashSet<>();
                for(String[] p:corpus)if(modes(p[0]).contains(mode)&&p[3].equals("full")&&seen.add(p[4])&&p[4].length()<=24)selected.add(p);
                selected.sort(Comparator.comparing(p->hash(p[4])));selected=selected.subList(0,Math.min(192,selected.size()));
                for(int cycle=0;cycle<2;cycle++)for(int i=0;i<selected.size();i++){
                    String raw=selected.get(i)[4];CompositionEngine c=engine(mode,new Editor(),new Memory(),false,d);
                    for(int k=0;k<raw.length();k++){
                        long cn=d.coreNs,nn=d.nativeNs,an=d.addonNs,at=System.nanoTime();c.type(raw.charAt(k));d.flush();long total=System.nanoTime()-at;
                        long core=d.coreNs-cn,nat=d.nativeNs-nn,addon=d.addonNs-an;
                        out.println(mode.id+"\t"+i+"\t"+k+"\t"+(cycle==0?"first-pass":"repeat-pass")+"\t"+total+"\t"+core+"\t"+nat+"\t"+addon+"\t"+(total-core-nat-addon)+"\t"+c.candidates().size()+"\t"+sig(c));
                    }
                }
                out.flush();System.out.println(variant+" perf "+pass+" "+mode.id);
            }
        }
    }
    static long heap(){System.gc();return Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory();}
    static void build()throws Exception {
        PairedForms pairs=PairedForms.read(Files.newBufferedReader(ASSET.resolve("paired-forms.tsv")));
        for(String p:Arrays.asList("taiwan","poj","japanese"))AddonDictionary.read(Files.newBufferedReader(ASSET.resolve("addon-"+p+".tsv")),pairs).writeBinary(Files.newOutputStream(WORK.resolve(variant+"/addon-"+p+".bin")));
    }
    public static void main(String[] args)throws Exception {
        variant=args[0];String command=args[1];if(command.equals("build")){build();return;}
        long before=heap(),at=System.nanoTime();load();long loaded=System.nanoTime()-at;long retained=heap()-before;
        Files.writeString(OUT.resolve(variant+"-load-"+command+(args.length>2?args[2]:"")+".json"),"{\"load_ns\":"+loaded+",\"retained_heap_bytes\":"+retained+",\"java\":\""+System.getProperty("java.version")+"\"}\n");
        if(command.equals("coverage"))coverage();else if(command.equals("mechanics"))mechanics();else if(command.equals("perf"))performance(Integer.parseInt(args[2]));
        System.out.println("DONE "+variant+" "+command);
    }
}
