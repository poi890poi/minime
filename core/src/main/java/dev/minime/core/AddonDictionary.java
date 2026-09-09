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
    private final JapaneseBasics japaneseBasics;
    private AddonDictionary(Map<String,List<Candidate>> entries) {this.entries=entries;first=null;second=null;japaneseBasics=JapaneseBasics.EMPTY;}
    private AddonDictionary(AddonDictionary first,AddonDictionary second) {this(first,second,first.japaneseBasics!=JapaneseBasics.EMPTY?first.japaneseBasics:second.japaneseBasics);}
    private AddonDictionary(AddonDictionary first,AddonDictionary second,JapaneseBasics basics) {entries=Collections.emptyMap();this.first=first;this.second=second;japaneseBasics=basics;}
    public static AddonDictionary withJapaneseBasics(AddonDictionary words,JapaneseBasics basics) {return new AddonDictionary(words,EMPTY,Objects.requireNonNull(basics));}
    public static AddonDictionary combine(AddonDictionary a,AddonDictionary b) {
        if(a==EMPTY)return b;if(b==EMPTY)return a;
        return new AddonDictionary(a,b);
    }
    public static AddonDictionary read(Reader input) throws IOException {
        return read(input,PairedForms.EMPTY);
    }
    public static AddonDictionary read(Reader input,PairedForms pairs) throws IOException {
        Map<String,List<Candidate>> entries=new HashMap<>();
        Map<String,Candidate> values=new HashMap<>();Map<String,String> strings=new HashMap<>();
        Map<String,Map<String,List<Candidate>>> unitEntries=new HashMap<>(),prefixEntries=new HashMap<>();int count=0;
        try(BufferedReader reader=new BufferedReader(input)) {
            String line;while((line=reader.readLine())!=null) {
                if(line.isEmpty() || line.startsWith("#"))continue;
                String[] p=line.split("\t",-1);
                if(p.length!=5 || !p[0].matches("[a-z][a-z0-9_-]{0,31}") || p[1].isEmpty() || p[1].length()>96
                        || p[2].isEmpty() || p[2].length()>96 || p[3].isEmpty() || ++count>200000)throw new IOException("Invalid add-on entry");
                // Ordinary common Japanese vocabulary must not consume the
                // existing expression/name search budget as its corpus grows.
                p[0]=strings.computeIfAbsent(p[0],k->k);p[2]=strings.computeIfAbsent(p[2],k->k);
                String indexPack=p[0].equals("japanese") && p[4].equals("everyday_vocabulary")?"japanese-common":p[0];
                String key=indexPack+"\t"+normalize(p[1]);
                // Generated Chinese initials contain one ASCII letter per Han
                // glyph. Full multisyllable readings retain source separators.
                boolean abbreviated=p[1].matches("[a-z]+") && p[1].length()==p[2].codePointCount(0,p[2].length())
                    && p[2].codePoints().allMatch(cp->Character.UnicodeScript.of(cp)==Character.UnicodeScript.HAN);
                PairedForms.Pair pair=pairs.forEntry(p[0],p[2],p[3]);
                String identity=p[0]+"\t"+p[2]+"\t"+abbreviated+"\t"+(pair==null?"":pair.source);
                Candidate value=values.get(identity);
                if(value==null) {value=Candidate.supplement(p[2],0,abbreviated).inPack(p[0]).paired(pair);values.put(identity,value);}
                entries.computeIfAbsent(key,k->new ArrayList<>()).add(value);
                // Generated initial aliases remain exact aliases. Index source
                // readings, not abbreviations of abbreviations.
                if(!abbreviated) {
                    prefixEntries.computeIfAbsent(indexPack,k->new HashMap<>()).computeIfAbsent(normalize(p[1]),k->new ArrayList<>()).add(value);
                    String reading=p[1].toLowerCase(Locale.ROOT).replaceAll("[- ']+","'");
                    if(reading.matches("[a-z0-9]+(?:'[a-z0-9]+)*")) {
                        unitEntries.computeIfAbsent(indexPack,k->new HashMap<>()).computeIfAbsent(reading,k->new ArrayList<>()).add(value);
                    }
                }
            }
        }
        // Builder pools must not survive while the compact search arrays are
        // allocated. Release each temporary map as its index takes ownership.
        values.clear();strings.clear();
        AddonDictionary result=new AddonDictionary(entries);
        for(Iterator<Map.Entry<String,Map<String,List<Candidate>>>> it=prefixEntries.entrySet().iterator();it.hasNext();) {
            Map.Entry<String,Map<String,List<Candidate>>> item=it.next();
            result.prefixes.put(item.getKey(),new ReadingIndex(item.getValue()));it.remove();
        }
        for(Iterator<Map.Entry<String,Map<String,List<Candidate>>>> it=unitEntries.entrySet().iterator();it.hasNext();) {
            Map.Entry<String,Map<String,List<Candidate>>> item=it.next();
            result.units.put(item.getKey(),new ReadingUnitIndex(item.getValue(),item.getValue().keySet()));it.remove();
        }
        return result;
    }
    public List<Candidate> lookup(String raw,Set<String> enabled) {
        List<Candidate> words=lookupWords(raw,enabled);
        if(enabled.contains("japanese")) {
            List<Candidate> vocabulary=lookupWords(raw,Collections.singleton("japanese-common"));
            if(!vocabulary.isEmpty()) {
                List<Candidate> combined=new ArrayList<>();Set<String> seen=new HashSet<>();
                for(boolean incomplete:new boolean[]{false,true}) {
                    for(Candidate c:words)if(c.incomplete==incomplete && seen.add(c.text))combined.add(c);
                    for(Candidate c:vocabulary)if(c.incomplete==incomplete && seen.add(c.text))combined.add(c);
                }
                words=combined;
            }
        }
        return japaneseBasics!=JapaneseBasics.EMPTY && enabled.contains("japanese")?japaneseBasics.merge(raw,words):words;
    }
    private List<Candidate> lookupWords(String raw,Set<String> enabled) {
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
