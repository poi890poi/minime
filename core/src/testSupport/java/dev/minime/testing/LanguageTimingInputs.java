package dev.minime.testing;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Frozen labelled timing workload. No target text, ranks or dictionary access. */
public final class LanguageTimingInputs {
    public static final int SHARDS=4;
    public static final String HEADER="id\tmode\tsource\tgenre\tcondition\traw\tsource_file\tsource_line\tdocument\tidentity\tsource_role";
    public static final class Query {
        public final String id,mode,source,genre,condition,raw;
        Query(String[] p){id=p[0];mode=p[1];source=p[2];genre=p[3];condition=p[4];raw=p[5];}
        public static Query legacy(String raw){return new Query(new String[]{"","","","","",raw});}
    }
    public static List<Query> read(InputStream stream)throws IOException {
        List<Query> result=new ArrayList<>();Set<String> ids=new HashSet<>();
        try(BufferedReader in=new BufferedReader(new InputStreamReader(stream,StandardCharsets.UTF_8))) {
            if(!HEADER.equals(in.readLine()))throw new IOException("Unexpected language timing schema");
            String line;while((line=in.readLine())!=null) {
                String[] p=line.split("\t",-1);
                if(p.length!=11 || !p[0].matches("[0-9a-f]{64}") || !p[5].matches("[a-z]{1,32}") || !ids.add(p[0]))throw new IOException("Invalid/duplicate timing input");
                if(!Arrays.asList("chinese","english","taiwanese_english","japanese_english").contains(p[1]))throw new IOException("Unknown mode");
                result.add(new Query(p));
            }
        }
        return Collections.unmodifiableList(result);
    }
    public static List<Query> shard(List<Query> all,String mode,int shard) {
        if(shard<0 || shard>=SHARDS)throw new IllegalArgumentException("shard");
        List<Query> result=new ArrayList<>();Map<String,Integer> positions=new HashMap<>();
        for(Query q:all)if(q.mode.equals(mode)) {
            String stratum=q.source+"\t"+q.condition;
            int index=positions.getOrDefault(stratum,0);positions.put(stratum,index+1);
            if(index%SHARDS==shard)result.add(q);
        }
        if(result.isEmpty())throw new IllegalArgumentException("Empty mode/shard");
        return Collections.unmodifiableList(result);
    }
}
