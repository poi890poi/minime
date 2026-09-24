package dev.minime.core;

import java.io.*;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import jdk.jfr.*;

/** Local read-only profile. No target labels influence lookup or ordering. */
public final class ProviderProfile {
    record Query(String id,String source,String genre,String condition,String raw) {}
    private static final Set<String> PACKS=Collections.singleton("poj");
    private static final Field[] FIELDS=Arrays.stream(Candidate.class.getDeclaredFields())
        .filter(f->!java.lang.reflect.Modifier.isStatic(f.getModifiers()))
        .sorted(Comparator.comparing(Field::getName)).toArray(Field[]::new);
    static {for(Field field:FIELDS)field.setAccessible(true);}

    private static void string(DataOutputStream out,String text)throws IOException {
        byte[] bytes=text.getBytes(StandardCharsets.UTF_8);out.writeInt(bytes.length);out.write(bytes);
    }
    private static byte[] identity(List<Candidate> values)throws Exception {
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();
        try(DataOutputStream out=new DataOutputStream(bytes)) {
            out.writeInt(values.size());
            for(Candidate value:values)for(Field field:FIELDS) {
                Object item=field.get(value);string(out,field.getName());
                if(item==null)out.writeByte(0);
                else if(item instanceof Boolean b){out.writeByte(1);out.writeBoolean(b);}
                else if(item instanceof Integer i){out.writeByte(2);out.writeInt(i);}
                else if(item instanceof Double d){out.writeByte(3);out.writeLong(Double.doubleToRawLongBits(d));}
                else if(item instanceof String s){out.writeByte(4);string(out,s);}
                else if(item instanceof PairedForms.Pair p){out.writeByte(5);string(out,p.phonetic);string(out,p.han);string(out,p.source);}
                else throw new IllegalStateException("Unfingerprinted candidate field: "+field.getName());
            }
        }
        return MessageDigest.getInstance("SHA-256").digest(bytes.toByteArray());
    }
    private static List<Query> inventory(Path corpus)throws Exception {
        List<Query> queries=new ArrayList<>();int originals=0;
        try(BufferedReader in=Files.newBufferedReader(corpus)) {
            if(!in.readLine().startsWith("id\tmode\tsource\tgenre\tcondition\traw\t"))throw new IOException("Unexpected corpus schema");
            String line;while((line=in.readLine())!=null) {
                String[] p=line.split("\t",-1);if(!p[1].equals("taiwanese_english"))continue;
                originals++;
                for(int n=1;n<=p[5].length();n++)queries.add(new Query(p[0]+":"+n,p[2],p[3],p[4],p[5].substring(0,n)));
            }
        }
        if(originals!=224)throw new IOException("Changed frozen query inventory");
        for(int length=1;length<=3;length++)expand(queries,"",length);
        return queries;
    }
    private static void expand(List<Query> queries,String prefix,int remaining) {
        if(remaining==0){queries.add(new Query("structural:"+prefix,"exhaustive-ascii","synthetic", "length-"+prefix.length(),prefix));return;}
        for(char c='a';c<='z';c++)expand(queries,prefix+c,remaining-1);
    }
    private static void pass(AddonDictionary dictionary,List<Query> queries,int pass,PrintWriter output,List<String> baseline)throws Exception {
        MessageDigest aggregate=MessageDigest.getInstance("SHA-256");int index=0;
        for(Query query:queries) {
            long start=System.nanoTime();List<Candidate> found=dictionary.lookup(query.raw(),PACKS);long nanos=System.nanoTime()-start;
            byte[] hash=identity(found);String hex=HexFormat.of().formatHex(hash);aggregate.update(hash);
            if(pass<0)baseline.add(hex);
            else {
                if(!baseline.get(index).equals(hex))throw new AssertionError("Changed ordered result at "+index);
                output.println(pass+"\t"+index+"\t"+query.source()+"\t"+query.genre()+"\t"+query.condition()+"\t"+query.raw().length()+"\t"+nanos+"\t"+found.size()+"\t"+hex);
            }
            index++;
        }
        if(output!=null)output.flush();
        System.out.println("pass="+pass+" queries="+queries.size()+" result_sha256="+HexFormat.of().formatHex(aggregate.digest()));
    }
    public static void main(String[] args)throws Exception {
        if(args.length!=4)throw new IllegalArgumentException("model corpus output-prefix plain|profile");
        Path prefix=Paths.get(args[2]);boolean profile=args[3].equals("profile");
        if(!profile && !args[3].equals("plain"))throw new IllegalArgumentException("Unknown recording mode");
        Path output=Paths.get(prefix+".tsv"),recording=Paths.get(prefix+".jfr");
        if(Files.exists(output)||Files.exists(recording))throw new IOException("Refusing to overwrite a run");
        List<Query> queries=inventory(Paths.get(args[1]));
        long start=System.nanoTime();AddonDictionary dictionary=AddonDictionary.readBinary(Files.newInputStream(Paths.get(args[0])));
        System.out.println("load_ms="+(System.nanoTime()-start)/1e6+" corpus_prefixes="+(queries.size()-18278)+" structural=18278");
        List<String> baseline=new ArrayList<>();pass(dictionary,queries,-1,null,baseline);
        try(PrintWriter out=new PrintWriter(Files.newBufferedWriter(output));Recording rec=profile?new Recording(Configuration.getConfiguration("profile")):null) {
            out.println("pass\tindex\tsource\tgenre\tcondition\tlength\tnanos\tcandidates\tfingerprint");
            if(rec!=null){rec.setMaxSize(256L*1024*1024);rec.start();}
            for(int round=0;round<3;round++)pass(dictionary,queries,round,out,baseline);
            if(rec!=null){rec.stop();rec.dump(recording);}
            if(out.checkError())throw new IOException("Incomplete profile output");
        }
    }
}
