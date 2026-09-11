package dev.minime.core;
import java.io.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.*;
import java.util.zip.*;

/** Natural source text, independent reference annotations; labels only used after lookup. */
public final class ConversationBenchmark {
    static final Path OUT=Paths.get("docs/language-contract-benchmark/conversations");
    static String fold(String s){return Normalizer.normalize(s,Normalizer.Form.NFC).toLowerCase(Locale.ROOT);}
    static int rank(List<Candidate> list,String target,boolean folded){
        for(int i=1;i<list.size();i++){
            Candidate c=list.get(i);if(c.consumed==0&&(folded?fold(c.text).equals(fold(target)):c.text.equals(target)))return i;
        }return -1;
    }
    public static void main(String[] args)throws Exception {
        ProposalBenchmark.variant=args[0];ProposalBenchmark.load();
        try(ProposalBenchmark.Decoder d=new ProposalBenchmark.Decoder(true);
            PrintWriter out=new PrintWriter(new OutputStreamWriter(new GZIPOutputStream(Files.newOutputStream(OUT.resolve(args[0]+".tsv.gz"))),StandardCharsets.UTF_8))){
            out.println("row\tdefault_hit\tcasefold_default_hit\tsuggestion_rank\tcasefold_rank\tcount\tdefault_text\tsignature");int row=0;
            for(String line:Files.readAllLines(OUT.resolve("inputs.tsv"))){
                String[] p=line.split("\t",-1);InputMode mode=p[0].startsWith("poj")?InputMode.TAIWANESE_ENGLISH:InputMode.JAPANESE_ENGLISH;
                CompositionEngine c=ProposalBenchmark.engine(mode,new ProposalBenchmark.Editor(),new ProposalBenchmark.Memory(),false,d);
                ProposalBenchmark.type(c,p[4]);d.flush();Candidate def=c.candidates().get(c.preferred());
                out.println(row+"\t"+(def.consumed==0&&def.text.equals(p[5]))+"\t"+(def.consumed==0&&fold(def.text).equals(fold(p[5])))+"\t"+rank(c.candidates(),p[5],false)+"\t"+rank(c.candidates(),p[5],true)+"\t"+c.candidates().size()+"\t"+def.text+"\t"+ProposalBenchmark.sig(c));
                if(++row%8000==0){out.flush();System.out.println(args[0]+" conversations "+row);}
            }
        }
    }
}
