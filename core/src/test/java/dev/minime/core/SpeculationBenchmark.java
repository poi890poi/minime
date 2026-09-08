package dev.minime.core;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.*;
import java.util.zip.*;

/** Frozen-label evaluation only. Targets and membership never reach the decoder. */
public final class SpeculationBenchmark {
    static String json(String s) {return "\""+s.replace("\\","\\\\").replace("\"","\\\"")+"\"";}
    static BufferedReader reader(String path)throws IOException {return new BufferedReader(new InputStreamReader(new GZIPInputStream(Files.newInputStream(Paths.get(path))),StandardCharsets.UTF_8));}
    static BufferedWriter writer(String path)throws IOException {return new BufferedWriter(new OutputStreamWriter(new GZIPOutputStream(Files.newOutputStream(Paths.get(path))),StandardCharsets.UTF_8));}
    static final class Case {String group,id,expected;Case(String[] p){group=p[0];id=p[1];expected=p[3];}}
    static final class Answer {String raw;List<Candidate> candidates,nativeCandidates=Collections.emptyList();long micros;Answer(String r,List<Candidate> c,long t){raw=r;candidates=c;micros=t;}}
    static final class Native implements AutoCloseable {
        final Process process;final BufferedWriter input;final BufferedReader output;
        Native(int index)throws Exception {
            Path root=Paths.get("").toAbsolutePath();String user=root.resolve("artifacts/speculation-rime-users/"+index).toString();Files.createDirectories(Paths.get(user));
            process=new ProcessBuilder(root.resolve("artifacts/desktop-rime.exe").toString(),root.resolve(".tools/rime-evaluation/msvc/dist/lib/rime.dll").toString(),root.resolve("app/src/main/rimeAssets/rime").toString(),user).redirectError(ProcessBuilder.Redirect.INHERIT).start();
            input=new BufferedWriter(new OutputStreamWriter(process.getOutputStream(),StandardCharsets.UTF_8));output=new BufferedReader(new InputStreamReader(process.getInputStream(),StandardCharsets.UTF_8));
            if(!"READY 1.16.1".equals(output.readLine()))throw new IOException("Pinned Rime unavailable");
        }
        List<Candidate> query(String raw)throws IOException {
            input.write(raw);input.newLine();input.flush();List<Candidate> found=new ArrayList<>();String line;
            while((line=output.readLine())!=null && !line.equals("END")) {int tab=line.indexOf('\t'),end=Integer.parseInt(line.substring(0,tab));found.add(new Candidate(line.substring(tab+1),false,100-found.size(),end==raw.length()?0:end));}
            if(line==null)throw new EOFException("Rime exited");return found;
        }
        public void close()throws Exception {input.close();if(!process.waitFor(10,TimeUnit.SECONDS))process.destroyForcibly();output.close();}
    }
    static boolean composed(Candidate c) {
        try{return Candidate.class.getField("composed").getBoolean(c);}catch(ReflectiveOperationException e){return false;}
    }
    static final class Stat {
        long n,top1,top8,any,without1,without8,withoutAny,composedHit,composedTop8,empty;
        void add(List<Candidate> candidates,String expected,BufferedWriter output,String group,String id,String raw,long micros)throws IOException {
            int rank=0,without=0,at=0;boolean hitComposed=false;
            for(int i=0;i<candidates.size();i++) {
                Candidate c=candidates.get(i);if(!composed(c))at++;
                if(c.text.equals(expected) && c.consumed==0) {rank=i+1;hitComposed=composed(c);if(!hitComposed)without=at;break;}
            }
            n++;if(rank==1)top1++;if(rank>0 && rank<=8)top8++;if(rank>0)any++;
            if(without==1)without1++;if(without>0 && without<=8)without8++;if(without>0)withoutAny++;
            if(hitComposed)composedHit++;if(hitComposed && rank<=8)composedTop8++;if(candidates.isEmpty())empty++;
            output.write(group+'\t'+id+'\t'+raw+'\t'+expected+'\t'+rank+'\t'+without+'\t'+hitComposed+'\t'+candidates.size()+'\t'+micros+'\n');
        }
        String json(){return "{\"n\":"+n+",\"top1\":"+top1+",\"top8\":"+top8+",\"any\":"+any+",\"without_composed_top1\":"+without1+",\"without_composed_top8\":"+without8+",\"without_composed_any\":"+withoutAny+",\"composed_target_hits\":"+composedHit+",\"composed_target_top8\":"+composedTop8+",\"empty\":"+empty+"}";}
    }
    public static void main(String[] args)throws Exception {
        if(args.length<2 || args.length>3)throw new IllegalArgumentException("output-prefix threads [input-hash-modulus]");
        String prefix=args[0];int threads=Integer.parseInt(args[1]);
        boolean nativeMode=Boolean.getBoolean("minime.benchmark.native");
        List<Native> natives=Collections.synchronizedList(new ArrayList<>());java.util.concurrent.atomic.AtomicInteger nativeIds=new java.util.concurrent.atomic.AtomicInteger();
        ThreadLocal<Native> localNative=ThreadLocal.withInitial(()->{try{Native n=new Native(nativeIds.incrementAndGet());natives.add(n);return n;}catch(Exception e){throw new RuntimeException(e);}});
        PhoneticDictionary dictionary=PhoneticDictionary.readBinary(Files.newInputStream(Paths.get("app/build/generated/minimeAssets/model.bin")));
        TreeMap<String,List<Case>> cases=new TreeMap<>();Set<String> vocabulary=new HashSet<>();
        try(BufferedReader in=reader("docs/speculation/inputs.tsv.gz")){String line;while((line=in.readLine())!=null){String[] p=line.split("\t");cases.computeIfAbsent(p[2],k->new ArrayList<>()).add(new Case(p));}}
        try(BufferedReader in=reader("docs/speculation/vocabulary.txt.gz")){String line;while((line=in.readLine())!=null)vocabulary.add(line);}
        if(args.length==3) {int modulus=Integer.parseInt(args[2]);cases.keySet().removeIf(raw->Math.floorMod(raw.hashCode(),modulus)!=0);}
        ExecutorService workers=Executors.newFixedThreadPool(threads);ArrayDeque<Future<Answer>> pending=new ArrayDeque<>();
        Iterator<String> inputs=cases.keySet().iterator();Map<String,Stat> stats=new TreeMap<>();List<Long> latency=new ArrayList<>();
        Set<String> uniqueComposed=new HashSet<>(),attestedComposed=new HashSet<>();
        Set<String> uniqueNative=new HashSet<>(),attestedNative=new HashSet<>();long nativeOccurrences=0,nativeAttestedOccurrences=0;
        long totalCandidates=0,totalComposed=0,attestedOccurrences=0;int done=0;long start=System.nanoTime();MessageDigest signature=MessageDigest.getInstance("SHA-256");
        try(BufferedWriter rows=writer(prefix+"-targets.tsv.gz");BufferedWriter raw=writer(prefix+"-candidates.jsonl.gz")) {
            while(inputs.hasNext() || !pending.isEmpty()) {
                while(inputs.hasNext() && pending.size()<threads*4) {
                    String input=inputs.next();pending.add(workers.submit(()->{
                        long before=System.nanoTime();List<Candidate> found=dictionary.convert(input,false,"");List<Candidate> nativeChoices=nativeMode?localNative.get().query(input):Collections.emptyList();
                        if(!nativeChoices.isEmpty()) {List<Candidate> merged=new ArrayList<>(nativeChoices);Set<String> seen=new HashSet<>();for(Candidate c:merged)seen.add(c.text);for(Candidate c:found)if(seen.add(c.text))merged.add(c);found=merged;}
                        Answer a=new Answer(input,found,(System.nanoTime()-before)/1000);a.nativeCandidates=nativeChoices;return a;
                    }));
                }
                Answer answer=pending.removeFirst().get();latency.add(answer.micros);List<String> values=new ArrayList<>();
                Set<String> nativeTexts=new HashSet<>();for(Candidate c:answer.nativeCandidates){nativeTexts.add(c.text);nativeOccurrences++;uniqueNative.add(c.text);if(vocabulary.contains(c.text)){nativeAttestedOccurrences++;attestedNative.add(c.text);}}
                signature.update((answer.raw+'\n').getBytes(StandardCharsets.UTF_8));
                for(Candidate c:answer.candidates) {
                    totalCandidates++;boolean generated=composed(c);
                    if(generated){totalComposed++;uniqueComposed.add(c.text);if(vocabulary.contains(c.text)){attestedOccurrences++;attestedComposed.add(c.text);}}
                    values.add("["+json(c.text)+","+c.score+","+generated+","+json(nativeTexts.contains(c.text)?"native-unclassified":generated?"core-sequence":"core-unit")+","+c.consumed+"]");
                    signature.update((c.text+'\t'+Double.toString(c.score)+'\n').getBytes(StandardCharsets.UTF_8));
                }
                raw.write("{\"input\":"+json(answer.raw)+",\"candidates\":["+String.join(",",values)+"]}\n");
                for(Case item:cases.get(answer.raw)) {
                    stats.computeIfAbsent(item.group,k->new Stat()).add(answer.candidates,item.expected,rows,item.group,item.id,answer.raw,answer.micros);
                    if(nativeMode)stats.computeIfAbsent("native-only/"+item.group,k->new Stat()).add(answer.nativeCandidates,item.expected,rows,"native-only/"+item.group,item.id,answer.raw,answer.micros);
                }
                if(++done%1000==0){rows.flush();raw.flush();System.out.printf(Locale.ROOT,"%d/%d unique inputs, %.1f seconds%n",done,cases.size(),(System.nanoTime()-start)/1e9);}
            }
        } finally {workers.shutdownNow();for(Native n:natives)n.close();}
        Collections.sort(latency);List<String> groups=new ArrayList<>();for(Map.Entry<String,Stat> e:stats.entrySet())groups.add(json(e.getKey())+":"+e.getValue().json());
        String result="{\"mode\":"+json(nativeMode?"Rime-plus-core":"core-fallback")+",\"threads\":"+threads+",\"unique_inputs\":"+done+",\"vocabulary_entries\":"+vocabulary.size()+",\"candidate_signature_sha256\":"+json(HexFormat.of().formatHex(signature.digest()))
            +",\"native_occurrences\":"+nativeOccurrences+",\"native_attested_occurrences\":"+nativeAttestedOccurrences+",\"unique_native_outputs\":"+uniqueNative.size()+",\"attested_unique_native_outputs\":"+attestedNative.size()
            +",\"candidate_occurrences\":"+totalCandidates+",\"composed_occurrences\":"+totalComposed+",\"attested_composed_occurrences\":"+attestedOccurrences+",\"unique_composed_outputs\":"+uniqueComposed.size()+",\"attested_unique_composed_outputs\":"+attestedComposed.size()
            +",\"query_p50_us\":"+latency.get(latency.size()/2)+",\"query_p95_us\":"+latency.get(latency.size()*95/100)+",\"query_max_us\":"+latency.get(latency.size()-1)+",\"elapsed_seconds\":"+(System.nanoTime()-start)/1e9+",\"groups\":{"+String.join(",",groups)+"}}\n";
        Files.writeString(Paths.get(prefix+"-summary.json"),result);System.out.println(result);
    }
}
