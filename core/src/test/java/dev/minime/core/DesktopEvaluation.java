package dev.minime.core;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.function.Consumer;

/** Core + pinned desktop Rime evaluation. No Android dependencies or APK build. */
public final class DesktopEvaluation {
    private static AddonDictionary addons=AddonDictionary.EMPTY;
    private static Set<String> packs=Collections.emptySet();
    private static String json(String s) {return "\""+s.replace("\\","\\\\").replace("\"","\\\"").replace("\n","\\n").replace("\r","\\r").replace("\t","\\t")+"\"";}
    private static final class Editor implements CompositionEngine.Editor {
        StringBuilder text=new StringBuilder();
        public void composing(String s) {} public void commit(String s) {text.append(s);}
        public void delete() {} public void enter() {text.append('\n');} public void finish() {}
    }
    private static final class Memory implements Learning {
        Map<String,Integer> counts=new HashMap<>();
        public int count(String c,String r,String v) {return counts.getOrDefault(c+'\t'+r+'\t'+v,0);}
        public void choose(String c,String r,String v) {counts.merge(c+'\t'+r+'\t'+v,1,Integer::sum);}
    }
    private static final class Native implements AutoCloseable {
        Process process;BufferedWriter input;BufferedReader output;
        Map<String,List<Candidate>> cache=new HashMap<>();List<Long> micros=new ArrayList<>();
        Native(String exe,String dll,String model,String user) throws Exception {
            Files.createDirectories(Paths.get(user));
            process=new ProcessBuilder(exe,dll,model,user).redirectError(ProcessBuilder.Redirect.INHERIT).start();
            input=new BufferedWriter(new OutputStreamWriter(process.getOutputStream(),StandardCharsets.UTF_8));
            output=new BufferedReader(new InputStreamReader(process.getInputStream(),StandardCharsets.UTF_8));
            if(!"READY 1.16.1".equals(output.readLine()))throw new IOException("Pinned Rime unavailable");
        }
        List<Candidate> query(String raw) {
            if(cache.containsKey(raw))return cache.get(raw);
            try {
                long start=System.nanoTime();input.write(raw);input.newLine();input.flush();List<Candidate> found=new ArrayList<>();
                String line;while((line=output.readLine())!=null && !line.equals("END")) {
                    if(line.isEmpty())continue;int tab=line.indexOf('\t'),end=Integer.parseInt(line.substring(0,tab));
                    found.add(new Candidate(line.substring(tab+1),false,100-found.size(),end==raw.length()?0:end));
                }
                if(line==null)throw new EOFException("Desktop Rime exited");
                micros.add((System.nanoTime()-start)/1000);cache.put(raw,found);return found;
            } catch(IOException e) {throw new UncheckedIOException(e);}
        }
        public void close() throws Exception {input.close();if(!process.waitFor(10,java.util.concurrent.TimeUnit.SECONDS)){process.destroyForcibly();throw new IOException("Rime exit timeout");}if(process.exitValue()!=0)throw new IOException("Rime exit "+process.exitValue());}
    }
    private static final class Decoder implements CompositionEngine.Decoder {
        Native nativeRime;String raw,context;PhoneticDictionary dictionary;boolean bpmf;Consumer<List<Candidate>> pending;
        Decoder(Native n) {nativeRime=n;}
        public void convert(PhoneticDictionary d,String r,boolean b,String c,Consumer<List<Candidate>> result) {dictionary=d;raw=r;bpmf=b;context=c;pending=result;}
        // The production engine coalesces unfinished queries. Flush the final
        // query before a token is accepted; no intermediate candidate is reused.
        void flush() {
            if(pending==null)return;Consumer<List<Candidate>> done=pending;pending=null;
            List<Candidate> choices=CandidateMerge.merge(nativeRime.query(raw),dictionary.convert(raw,bpmf,context));
            done.accept(choices);
        }
    }
    private static CompositionEngine engine(Editor e,Learning learning,PhoneticDictionary d,Decoder decoder,boolean english) {
        CompositionEngine c=new CompositionEngine(e,learning);c.dictionary(d);c.start(false,false,false,false,english);c.decoder(decoder,()->{});
        if(!packs.isEmpty())c.addons(addons,packs);return c;
    }
    private static void type(CompositionEngine c,Decoder decoder,String raw) {raw.codePoints().forEach(c::type);decoder.flush();}
    public static void main(String[] args) throws Exception {
        if(args.length!=6)throw new IllegalArgumentException("corpus.tsv output.jsonl native.exe rime.dll model-dir user-dir");
        Path assets=Paths.get("app/src/main/assets");
        if(Boolean.getBoolean("minime.addons")) {addons=AddonDictionary.combine(AddonDictionary.read(Files.newBufferedReader(assets.resolve("addons.tsv"))),AddonDictionary.read(Files.newBufferedReader(assets.resolve("geography.tsv"))));packs=new HashSet<>(Arrays.asList("taiwan","japanese","poj","geography"));}
        PhoneticDictionary dictionary=PhoneticDictionary.load(Files.newBufferedReader(assets.resolve("zh_tw.tsv")),Files.newBufferedReader(assets.resolve("en_us.tsv")),Files.newBufferedReader(assets.resolve("syllables.tsv")),Files.newBufferedReader(assets.resolve("context.tsv")));
        List<String> rows=Files.readAllLines(Paths.get(args[0]),StandardCharsets.UTF_8);
        dictionary.englishSpelling(Files.newBufferedReader(assets.resolve("en_spelling.tsv")));
        try(Native nativeRime=new Native(args[2],args[3],args[4],args[5]);BufferedWriter report=Files.newBufferedWriter(Paths.get(args[1]),StandardCharsets.UTF_8)) {
            Memory primed=new Memory();Decoder decoder=new Decoder(nativeRime);Set<String> vocabulary=new HashSet<>();
            for(String line:rows)if(line.startsWith("en-"))Collections.addAll(vocabulary,line.split("\t",-1)[2].split(" "));
            int primes=0;
            for(String line:Files.readAllLines(assets.resolve("syllables.tsv"),StandardCharsets.UTF_8)) {
                String raw=line.split("\t")[0];if(!vocabulary.contains(raw) || !dictionary.isEnglish(raw))continue;
                // Keep simulated past choices identical across add-on ranking experiments.
                // Supplemental choices deliberately do not train the base model.
                CompositionEngine c=engine(new Editor(),primed,dictionary,decoder,false);
                c.addons(AddonDictionary.EMPTY,Collections.emptySet());type(c,decoder,raw);
                for(int i=1;i<c.candidates().size();i++)if(!c.candidates().get(i).literal && c.candidates().get(i).consumed==0){c.select(i);primes++;break;}
            }
            System.out.println("Data-derived single-syllable English/Pinyin overlap choices: "+primes);
            int completed=0;
            for(String line:rows) {
                String[] p=line.split("\t",-1);boolean english=p[0].startsWith("en-");
                for(String mode:english?new String[]{"fresh","primed","english"}:new String[]{"fresh"}) {
                    Editor editor=new Editor();CompositionEngine c=engine(editor,mode.equals("primed")?primed:Learning.NONE,dictionary,decoder,mode.equals("english"));
                    if(english) {
                        List<String> diagnostics=new ArrayList<>();int at=0;
                        for(String token:p[2].split(" ")) {
                            type(c,decoder,token);Candidate choice=c.candidates().get(c.preferred());
                            diagnostics.add("{\"raw\":"+json(token)+",\"output\":"+json(choice.text)+",\"known\":"+dictionary.isEnglish(token)+",\"position\":"+(at++)+"}");c.space();
                        }
                        report.write("{\"group\":"+json(p[0])+",\"id\":"+json(p[1])+",\"mode\":"+json(mode)+",\"tokens\":["+String.join(",",diagnostics)+"]}");
                    } else {
                        type(c,decoder,p[2]);int rank=0;List<String> top=new ArrayList<>();
                        for(int i=1;i<c.candidates().size();i++) {
                            Candidate candidate=c.candidates().get(i);
                            if(candidate.text.equals(p[3]) && rank==0)rank=i;
                            if(i<=8)top.add(json(candidate.text));
                        }
                        c.space();report.write("{\"group\":"+json(p[0])+",\"id\":"+json(p[1])+",\"input\":"+json(p[2])+",\"expected\":"+json(p[3])+",\"rank\":"+rank+",\"output\":"+json(editor.text.toString())+",\"top\":["+String.join(",",top)+"]}");
                    }
                    report.newLine();
                }
                if(++completed%500==0){report.flush();System.out.println("Completed "+completed+" / "+rows.size()+" inputs; native queries "+nativeRime.cache.size());}
            }
            Collections.sort(nativeRime.micros);int n=nativeRime.micros.size();
            System.out.println("Native uncached IPC+decoder microseconds: n="+n+" p50="+nativeRime.micros.get(n/2)+" p95="+nativeRime.micros.get(n*95/100)+" max="+nativeRime.micros.get(n-1));
        }
    }
}
