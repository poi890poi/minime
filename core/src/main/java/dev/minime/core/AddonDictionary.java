package dev.minime.core;

import java.io.*;
import java.util.*;

/** Immutable exact-key add-on lookup. It never changes base decoding or Space defaults. */
public final class AddonDictionary {
    public static final AddonDictionary EMPTY=new AddonDictionary(Collections.emptyMap());
    private final Map<String,List<Candidate>> entries;
    private AddonDictionary(Map<String,List<Candidate>> entries) {this.entries=entries;}
    public static AddonDictionary combine(AddonDictionary a,AddonDictionary b) {
        Map<String,List<Candidate>> entries=new HashMap<>(a.entries);
        b.entries.forEach((key,values)-> {
            List<Candidate> merged=new ArrayList<>(entries.getOrDefault(key,Collections.emptyList()));merged.addAll(values);entries.put(key,merged);
        });return new AddonDictionary(entries);
    }
    public static AddonDictionary read(Reader input) throws IOException {
        Map<String,List<Candidate>> entries=new HashMap<>();int count=0;
        try(BufferedReader reader=new BufferedReader(input)) {
            String line;while((line=reader.readLine())!=null) {
                if(line.isEmpty() || line.startsWith("#"))continue;
                String[] p=line.split("\t",-1);
                if(p.length!=5 || !p[0].matches("[a-z][a-z0-9_-]{0,31}") || p[1].isEmpty() || p[1].length()>96
                        || p[2].isEmpty() || p[2].length()>96 || p[3].isEmpty() || ++count>200000)throw new IOException("Invalid add-on entry");
                String key=p[0]+"\t"+normalize(p[1]);
                entries.computeIfAbsent(key,k->new ArrayList<>()).add(Candidate.supplement(p[2],0));
            }
        }
        return new AddonDictionary(entries);
    }
    public List<Candidate> lookup(String raw,Set<String> enabled) {
        if(raw.length()>96 || enabled.isEmpty())return Collections.emptyList();
        List<Candidate> result=new ArrayList<>();Set<String> seen=new HashSet<>();String key=normalize(raw);
        for(String pack:new TreeSet<>(enabled))for(Candidate c:entries.getOrDefault(pack+"\t"+key,Collections.emptyList()))
            if(seen.add(c.text)) {result.add(c);if(result.size()==8)return result;}
        return result;
    }
    private static String normalize(String key) {return key.toLowerCase(Locale.ROOT).replace("'","").replace("-","").replace(" ","");}
}
