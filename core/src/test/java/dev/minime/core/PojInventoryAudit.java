package dev.minime.core;
import java.nio.file.*;
import java.io.*;
import java.util.*;
import java.util.zip.GZIPInputStream;

/** Exhaustive source-reading retrieval with before/after ranks, not language accuracy. */
public final class PojInventoryAudit {
    public static void main(String[] args)throws Exception {
        AddonDictionary before=AddonDictionary.read(Files.newBufferedReader(Paths.get(args[0])));
        AddonDictionary after=AddonDictionary.read(Files.newBufferedReader(Paths.get(args[1])));
        Set<String> packs=Collections.singleton("poj");
        String last="";List<Candidate> a=Collections.emptyList(),b=a;long usA=0,usB=0;
        try(BufferedReader in=new BufferedReader(new InputStreamReader(new GZIPInputStream(Files.newInputStream(Paths.get(args[2]))),"UTF-8"));
            PrintWriter out=new PrintWriter(args[3],"UTF-8")) {
            out.println("query\toutput\tcategory\tbefore_rank\tafter_rank\tbefore_us\tafter_us");
            String line;while((line=in.readLine())!=null) {
                String[] p=line.split("\t");
                if(!p[0].equals(last)){long now=System.nanoTime();a=before.lookup(p[0],packs);usA=(System.nanoTime()-now)/1000;
                    now=System.nanoTime();b=after.lookup(p[0],packs);usB=(System.nanoTime()-now)/1000;last=p[0];}
                out.println(line+"\t"+rank(a,p[1])+"\t"+rank(b,p[1])+"\t"+usA+"\t"+usB);
            }
        }
    }
    private static int rank(List<Candidate> list,String target){for(int i=0;i<list.size();i++)if(list.get(i).text.equals(target))return i+1;return 0;}
}
