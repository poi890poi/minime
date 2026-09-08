package dev.minime.core;

import java.io.*;
import java.util.*;

/** Immutable exact-key add-on lookup. It never changes base decoding or Space defaults. */
public final class AddonDictionary {
    public static final AddonDictionary EMPTY=new AddonDictionary(Collections.emptyMap());
    private final Map<String,List<Candidate>> entries;
    private final AddonDictionary first,second;
    private AddonDictionary(Map<String,List<Candidate>> entries) {this.entries=entries;first=null;second=null;}
    private AddonDictionary(AddonDictionary first,AddonDictionary second) {entries=Collections.emptyMap();this.first=first;this.second=second;}
    public static AddonDictionary combine(AddonDictionary a,AddonDictionary b) {
        if(a==EMPTY)return b;if(b==EMPTY)return a;
        return new AddonDictionary(a,b);
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
                // Generated Chinese initials contain one ASCII letter per Han
                // glyph. Full multisyllable readings retain source separators.
                boolean abbreviated=p[1].matches("[a-z]+") && p[1].length()==p[2].codePointCount(0,p[2].length())
                    && p[2].codePoints().allMatch(cp->Character.UnicodeScript.of(cp)==Character.UnicodeScript.HAN);
                entries.computeIfAbsent(key,k->new ArrayList<>()).add(Candidate.supplement(p[2],0,abbreviated));
            }
        }
        return new AddonDictionary(entries);
    }
    public List<Candidate> lookup(String raw,Set<String> enabled) {
        if(raw.length()>96 || enabled.isEmpty())return Collections.emptyList();
        List<Candidate> result=new ArrayList<>();Set<String> seen=new HashSet<>();String key=normalize(raw);
        for(String pack:new TreeSet<>(enabled))if(append(pack+"\t"+key,result,seen))break;
        return result;
    }
    private boolean append(String key,List<Candidate> result,Set<String> seen) {
        if(first!=null)return first.append(key,result,seen) || second.append(key,result,seen);
        for(Candidate c:entries.getOrDefault(key,Collections.emptyList()))if(seen.add(c.text)) {result.add(c);if(result.size()==8)return true;}
        return false;
    }
    private static String normalize(String key) {return key.toLowerCase(Locale.ROOT).replace("'","").replace("-","").replace(" ","");}
}
