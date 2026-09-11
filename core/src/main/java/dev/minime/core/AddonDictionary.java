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
    public String japaneseConversionReading(String raw) {return japaneseBasics.conversionReading(raw);}
    public static AddonDictionary combine(AddonDictionary a,AddonDictionary b) {
        if(a==EMPTY)return b;if(b==EMPTY)return a;
        return new AddonDictionary(a,b);
    }
    public static AddonDictionary read(Reader input) throws IOException {
        return read(input,PairedForms.EMPTY);
    }
    /** Same shared pools/index representation as the base model; built off-device. */
    public void writeBinary(OutputStream output)throws IOException {
        if(first!=null || japaneseBasics!=JapaneseBasics.EMPTY)throw new IOException("Compile a single word index before attaching providers");
        try(DataOutputStream stream=new DataOutputStream(new BufferedOutputStream(output))) {
            stream.writeInt(0x4d414444);stream.writeInt(1);
            BinaryModel.Writer out=new BinaryModel.Writer(stream,true);out.words(entries);
            stream.writeInt(prefixes.size());for(String pack:new TreeSet<>(prefixes.keySet())) {out.string(pack);prefixes.get(pack).write(out);}
            stream.writeInt(units.size());for(String pack:new TreeSet<>(units.keySet())) {out.string(pack);units.get(pack).write(out);}
        }
    }
    public static AddonDictionary readBinary(InputStream input)throws IOException {
        try(DataInputStream stream=new DataInputStream(new BufferedInputStream(input))) {
            if(stream.readInt()!=0x4d414444 || stream.readInt()!=1)throw new IOException("Unsupported supplemental model");
            BinaryModel.Reader in=new BinaryModel.Reader(stream,true);Map<String,List<Candidate>> entries=new HashMap<>();in.words(entries);
            AddonDictionary result=new AddonDictionary(entries);
            int count=in.size();for(int i=0;i<count;i++)result.prefixes.put(in.string(),new ReadingIndex(in));
            count=in.size();for(int i=0;i<count;i++)result.units.put(in.string(),new ReadingUnitIndex(in));
            if(stream.read()!=-1)throw new IOException("Trailing supplemental model data");
            return result;
        } catch(IndexOutOfBoundsException | NullPointerException malformed) {throw new IOException("Invalid supplemental references",malformed);}
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
                        || p[2].isEmpty() || p[2].length()>96 || p[3].isEmpty() || ++count>500000)throw new IOException("Invalid add-on entry");
                // Ordinary common Japanese vocabulary must not consume the
                // existing expression/name search budget as its corpus grows.
                p[0]=strings.computeIfAbsent(p[0],k->k);p[2]=strings.computeIfAbsent(p[2],k->k);
                String indexPack=p[0].equals("japanese") && p[4].equals("everyday_vocabulary")?"japanese-common":p[0].equals("poj") && p[4].equals("extended_vocabulary")?"poj-common":p[0];
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
        // Exact and prefix lookup already own the same full focused readings.
        // Reuse the sorted index instead of retaining a second hash key/list.
        // Keep mixed abbreviated keys intact to preserve their original order.
        for(Iterator<Map.Entry<String,List<Candidate>>> it=entries.entrySet().iterator();it.hasNext();) {
            Map.Entry<String,List<Candidate>> entry=it.next();int tab=entry.getKey().indexOf('\t');
            String pack=entry.getKey().substring(0,tab);
            if((pack.startsWith("poj") || pack.startsWith("japanese")) && entry.getValue().stream().noneMatch(c->c.abbreviated))it.remove();
        }
        // Equal immutable candidate sequences are shared across source aliases
        // and both indexes. List order and duplicate references remain unchanged.
        Map<List<Candidate>,List<Candidate>> lists=new HashMap<>();
        for(Map<String,List<Candidate>> source:prefixEntries.values())source.replaceAll((key,list)->lists.computeIfAbsent(list,k->k));
        for(Map<String,List<Candidate>> source:unitEntries.values())source.replaceAll((key,list)->lists.computeIfAbsent(list,k->k));
        entries.replaceAll((key,list)->lists.computeIfAbsent(list,k->k));lists.clear();
        AddonDictionary result=new AddonDictionary(new HashMap<>(entries));entries.clear();
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
        Set<String> secondary=new HashSet<>(enabled);secondary.remove("poj");secondary.remove("japanese");
        List<List<Candidate>> groups=new ArrayList<>();
        for(String pack:Arrays.asList("poj","japanese"))if(enabled.contains(pack)) {
            // A focused dictionary has the same 24-choice budget as core reading
            // matches. Large general vocabulary cannot consume the expression tier.
            List<Candidate> focused=lookupWords(raw,Collections.singleton(pack),24);
            List<Candidate> common=lookupWords(raw,Collections.singleton(pack+"-common"),24);
            List<Candidate> words=merge(Arrays.asList(focused,common));
            groups.add(pack.equals("japanese") && japaneseBasics!=JapaneseBasics.EMPTY?japaneseBasics.merge(raw,words):words);
        }
        groups.add(lookupWords(raw,secondary,8));
        return merge(groups);
    }
    private static List<Candidate> merge(List<List<Candidate>> groups) {
        List<Candidate> result=new ArrayList<>();Set<String> seen=new HashSet<>();
        for(boolean incomplete:new boolean[]{false,true})for(List<Candidate> group:groups)
            for(Candidate c:group)if(c.incomplete==incomplete && seen.add(c.text))result.add(c);
        return result;
    }
    private List<Candidate> lookupWords(String raw,Set<String> enabled,int limit) {
        if(raw.length()>96 || enabled.isEmpty())return Collections.emptyList();
        List<Candidate> result=new ArrayList<>();Set<String> seen=new HashSet<>();String key=normalize(raw);
        Set<String> packs=new TreeSet<>(enabled);
        for(String pack:packs)if(append(pack+"\t"+key,result,seen,limit))return result;
        List<Candidate> partials=new ArrayList<>();
        for(String pack:packs)complete(pack,raw,partials,limit);
        partials.sort(Comparator.comparingDouble((Candidate c)->c.score).reversed().thenComparing(c->c.text));
        for(Candidate c:partials)if(seen.add(c.text)) {result.add(c);if(result.size()==limit)break;}
        return result;
    }
    private void complete(String pack,String raw,List<Candidate> result,int limit) {
        if(first!=null) {first.complete(pack,raw,result,limit);second.complete(pack,raw,result,limit);return;}
        ReadingIndex prefix=prefixes.get(pack);ReadingUnitIndex unit=units.get(pack);
        if(prefix!=null)result.addAll(prefix.complete(normalize(raw),(key,c)->true,limit));
        if(unit!=null)result.addAll(unit.lookup(raw.toLowerCase(Locale.ROOT).replaceAll("[- ']+","'")));
    }
    private boolean append(String key,List<Candidate> result,Set<String> seen,int limit) {
        if(first!=null)return first.append(key,result,seen,limit) || second.append(key,result,seen,limit);
        List<Candidate> exact=entries.get(key);
        if(exact==null) {
            int tab=key.indexOf('\t');ReadingIndex index=prefixes.get(key.substring(0,tab));
            exact=index==null?Collections.emptyList():index.exact(key.substring(tab+1));
        }
        for(Candidate c:exact)if(seen.add(c.text)) {result.add(c);if(result.size()==limit)return true;}
        return false;
    }
    private static String normalize(String key) {return key.toLowerCase(Locale.ROOT).replace("'","").replace("-","").replace(" ","");}
}
