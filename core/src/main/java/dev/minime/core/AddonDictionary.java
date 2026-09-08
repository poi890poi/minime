package dev.minime.core;

import java.io.*;
import java.util.*;

/** Immutable pack indexes sharing the core's bounded prefix and reading-unit searches. */
public final class AddonDictionary {
    public static final AddonDictionary EMPTY=new AddonDictionary(Collections.emptyMap());
    private final Map<String,List<Candidate>> entries;
    private final Map<String,ReadingIndex> prefixes=new HashMap<>();
    private final Map<String,ReadingUnitIndex> units=new HashMap<>();
    private final AddonDictionary first,second;
    private AddonDictionary(Map<String,List<Candidate>> entries) {this.entries=entries;first=null;second=null;}
    private AddonDictionary(AddonDictionary first,AddonDictionary second) {entries=Collections.emptyMap();this.first=first;this.second=second;}
    public static AddonDictionary combine(AddonDictionary a,AddonDictionary b) {
        if(a==EMPTY)return b;if(b==EMPTY)return a;
        return new AddonDictionary(a,b);
    }
    public static AddonDictionary read(Reader input) throws IOException {
        Map<String,List<Candidate>> entries=new HashMap<>();
        Map<String,Map<String,List<Candidate>>> unitEntries=new HashMap<>(),prefixEntries=new HashMap<>();int count=0;
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
                Candidate value=Candidate.supplement(p[2],0,abbreviated);
                entries.computeIfAbsent(key,k->new ArrayList<>()).add(value);
                // Generated initial aliases remain exact aliases. Index source
                // readings, not abbreviations of abbreviations.
                if(!abbreviated) {
                    prefixEntries.computeIfAbsent(p[0],k->new HashMap<>()).computeIfAbsent(normalize(p[1]),k->new ArrayList<>()).add(value);
                    String reading=p[1].toLowerCase(Locale.ROOT).replaceAll("[- ']+","'");
                    if(reading.matches("[a-z0-9]+(?:'[a-z0-9]+)*")) {
                        unitEntries.computeIfAbsent(p[0],k->new HashMap<>()).computeIfAbsent(reading,k->new ArrayList<>()).add(value);
                    }
                }
            }
        }
        AddonDictionary result=new AddonDictionary(entries);
        unitEntries.forEach((pack,source)->result.units.put(pack,new ReadingUnitIndex(source,source.keySet())));
        prefixEntries.forEach((pack,source)->result.prefixes.put(pack,new ReadingIndex(source)));
        return result;
    }
    public List<Candidate> lookup(String raw,Set<String> enabled) {
        if(raw.length()>96 || enabled.isEmpty())return Collections.emptyList();
        List<Candidate> result=new ArrayList<>();Set<String> seen=new HashSet<>();String key=normalize(raw);
        Set<String> packs=new TreeSet<>(enabled);
        for(String pack:packs)if(append(pack+"\t"+key,result,seen))return result;
        List<Candidate> partials=new ArrayList<>();
        for(String pack:packs)complete(pack,raw,partials);
        partials.sort(Comparator.comparingDouble((Candidate c)->c.score).reversed().thenComparing(c->c.text));
        for(Candidate c:partials)if(seen.add(c.text)) {result.add(c);if(result.size()==8)break;}
        return result;
    }
    private void complete(String pack,String raw,List<Candidate> result) {
        if(first!=null) {first.complete(pack,raw,result);second.complete(pack,raw,result);return;}
        ReadingIndex prefix=prefixes.get(pack);ReadingUnitIndex unit=units.get(pack);
        if(prefix!=null)result.addAll(prefix.complete(normalize(raw),(key,c)->true));
        if(unit!=null)result.addAll(unit.lookup(raw.toLowerCase(Locale.ROOT).replaceAll("[- ']+","'")));
    }
    private boolean append(String key,List<Candidate> result,Set<String> seen) {
        if(first!=null)return first.append(key,result,seen) || second.append(key,result,seen);
        for(Candidate c:entries.getOrDefault(key,Collections.emptyList()))if(seen.add(c.text)) {result.add(c);if(result.size()==8)return true;}
        return false;
    }
    private static String normalize(String key) {return key.toLowerCase(Locale.ROOT).replace("'","").replace("-","").replace(" ","");}
}
