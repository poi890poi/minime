package dev.minime.core;

import java.nio.file.*;
import java.util.*;

/** Fixed previously exposed conversation/essay inputs; paired A/B timing, not new accuracy. */
public final class PairedFormsBenchmark {
    public static void main(String[] args)throws Exception {
        Path assets=Paths.get("app/src/main/assets");
        long start=System.nanoTime();PairedForms pairs=PairedForms.read(Files.newBufferedReader(assets.resolve("paired-forms.tsv")));
        double metadataMs=(System.nanoTime()-start)/1e6;
        AddonDictionary plain=AddonDictionary.read(Files.newBufferedReader(assets.resolve("addons.tsv")));
        AddonDictionary paired=AddonDictionary.read(Files.newBufferedReader(assets.resolve("addons.tsv")),pairs);
        Set<String> packs=new HashSet<>(Arrays.asList("taiwan","poj"));
        List<String[]> rows=new ArrayList<>();
        for(String line:Files.readAllLines(Paths.get("docs/input-modes/coverage-inputs.tsv")))rows.add(line.split("\t",-1));
        Map<String,Integer> coverage=new TreeMap<>();
        for(String[] row:rows) {
            List<Candidate> a=plain.lookup(row[4],packs),b=paired.lookup(row[4],packs);
            if(!a.toString().equals(b.toString()))throw new AssertionError("changed order");
            if(b.stream().anyMatch(c->c.pair!=null))coverage.merge(row[0]+"/"+row[3],1,Integer::sum);
        }
        for(int warm=0;warm<3;warm++)for(String[] row:rows) {plain.lookup(row[4],packs);paired.lookup(row[4],packs);}
        List<Long> before=new ArrayList<>(),after=new ArrayList<>();
        long checksum=0;
        for(int pass=0;pass<6;pass++)for(int variant=0;variant<2;variant++) {
            boolean enabled=(variant+pass)%2==1;AddonDictionary d=enabled?paired:plain;List<Long> times=enabled?after:before;
            for(String[] row:rows) {long t=System.nanoTime();List<Candidate> result=d.lookup(row[4],packs);times.add(System.nanoTime()-t);checksum+=result.size();}
        }
        if(args.length>0)try(java.io.BufferedWriter out=Files.newBufferedWriter(Paths.get(args[0]))) {
            out.write("pass\tgenre\tcondition\tquery\tplain_ns\tpaired_ns\n");
            for(int i=0;i<before.size();i++) {
                String[] row=rows.get(i%rows.size());out.write((i/rows.size())+"\t"+row[0]+"\t"+row[3]+"\t"+row[4]+"\t"+before.get(i)+"\t"+after.get(i)+"\n");
            }
        }
        Collections.sort(before);Collections.sort(after);
        System.out.println("rows="+rows.size()+" sidecar_bytes="+Files.size(assets.resolve("paired-forms.tsv"))+" metadata_parse_ms="+metadataMs+" checksum="+checksum);
        for(int p:new int[]{50,95,99})System.out.printf(Locale.ROOT,"lookup_p%d_us plain=%.3f paired=%.3f%n",p,before.get((before.size()-1)*p/100)/1e3,after.get((after.size()-1)*p/100)/1e3);
        System.out.printf(Locale.ROOT,"lookup_mean_us plain=%.3f paired=%.3f; max_us plain=%.3f paired=%.3f%n",before.stream().mapToLong(Long::longValue).average().getAsDouble()/1e3,after.stream().mapToLong(Long::longValue).average().getAsDouble()/1e3,before.get(before.size()-1)/1e3,after.get(after.size()-1)/1e3);
        System.out.println("queries_with_paired_result_by_genre_condition="+coverage);
        System.out.println("Output order parity on every frozen row; lookup-only desktop timing. No claim of phone latency or new holdout accuracy.");
    }
}
