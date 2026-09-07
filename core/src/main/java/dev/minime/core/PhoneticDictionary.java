package dev.minime.core;

import java.io.*;
import java.util.*;

/** Immutable after loading; all language data comes from attributed offline sources. */
public final class PhoneticDictionary {
    private static final int BEAM = 6;
    private static final int MAX_KEY = 32;
    private final Map<String, List<Candidate>> pinyin = new HashMap<>();
    private final Map<String, List<Candidate>> zhuyin = new HashMap<>();
    private final NavigableMap<String, Integer> english = new TreeMap<>();
    private final NavigableMap<String, Integer> foldedEnglish = new TreeMap<>();
    private final Map<String, List<Candidate>> continuations = new HashMap<>();
    private final Set<String> syllables = new HashSet<>();
    private ReadingIndex pinyinPrefixes,zhuyinPrefixes;
    private PinyinSyllableIndex pinyinSyllables;
    private ContextModel contextModel=new ContextModel();
    public void writeBinary(OutputStream target)throws IOException {
        try(DataOutputStream stream=new DataOutputStream(new BufferedOutputStream(target))) {
            stream.writeInt(0x4d494d45);stream.writeInt(1);BinaryModel.Writer out=new BinaryModel.Writer(stream);
            out.words(pinyin);out.words(zhuyin);out.counts(english);out.words(continuations);out.strings(new TreeSet<>(syllables).toArray(new String[0]));
            pinyinPrefixes.write(out);zhuyinPrefixes.write(out);pinyinSyllables.write(out);contextModel.write(out);
        }
    }
    public static PhoneticDictionary readBinary(InputStream source)throws IOException {
        try(DataInputStream stream=new DataInputStream(new BufferedInputStream(source))) {
            if(stream.readInt()!=0x4d494d45 || stream.readInt()!=1)throw new IOException("Unsupported dictionary format");
            BinaryModel.Reader in=new BinaryModel.Reader(stream);PhoneticDictionary d=new PhoneticDictionary();
            in.words(d.pinyin);in.words(d.zhuyin);in.counts(d.english);in.words(d.continuations);Collections.addAll(d.syllables,in.strings());
            d.pinyinPrefixes=new ReadingIndex(in);d.zhuyinPrefixes=new ReadingIndex(in);d.pinyinSyllables=new PinyinSyllableIndex(in);d.contextModel=new ContextModel(in);
            if(stream.read()!=-1)throw new IOException("Trailing model data");d.indexEnglish();return d;
        } catch(IndexOutOfBoundsException e) {throw new IOException("Invalid model reference",e);}
    }
    public static PhoneticDictionary load(Reader chinese,Reader english,Reader syllables,Reader context)throws IOException {
        PhoneticDictionary d=load(chinese,english,syllables);d.contextModel=ContextModel.load(context);
        d.contextModel.contractions().forEach(d.english::putIfAbsent);d.indexEnglish();return d;
    }
    public List<Candidate> englishPredictions(String context) { return contextModel.english(context); }
    public List<Candidate> englishTrace(float[] points) {return EnglishTrace.decode(foldedEnglish,points);}

    public static PhoneticDictionary load(Reader chinese, Reader english, Reader syllables) throws IOException {
        PhoneticDictionary d = new PhoneticDictionary();
        Set<String> readings=new HashSet<>();
        try (BufferedReader r = new BufferedReader(chinese)) {
            String line;
            while ((line = r.readLine()) != null) {
                String[] p = line.split("\t");
                if (p.length != 5) throw new IOException("Invalid Chinese dictionary row");
                // Log probability with a word-boundary cost: longer known phrases beat isolated glyphs.
                Candidate c = new Candidate(p[2], false, Math.log10(Double.parseDouble(p[3]) + 1) - 9,p[4]);
                add(d.pinyin, normalize(p[0]), c);
                if (p[0].contains("'")) add(d.pinyin, p[0], c);
                readings.add(p[0]);
                add(d.zhuyin, p[1], c);
                add(d.zhuyin, "~" + toneless(p[1]), new Candidate(c.text, false, c.score - 3,c.reading));
                int n = p[2].codePointCount(0, p[2].length());
                if (n >= 2 && n <= 6) {
                    for (int i = 1; i < n; i++) {
                        int at = p[2].offsetByCodePoints(0, i);
                        add(d.continuations, p[2].substring(0, at), new Candidate(p[2].substring(at), false, c.score));
                    }
                }
            }
        }
        try (BufferedReader r = new BufferedReader(english)) {
            String line;
            while ((line = r.readLine()) != null) {
                String[] p = line.split("\t"); d.english.put(p[0], Integer.parseInt(p[1]));
            }
        }
        try (BufferedReader r = new BufferedReader(syllables)) {
            String line;
            while ((line = r.readLine()) != null) d.syllables.add(line.split("\t")[0]);
        }
        for (Map<String, List<Candidate>> m : Arrays.asList(d.pinyin, d.zhuyin, d.continuations))
            for (List<Candidate> list : m.values()) {
                list.sort(Comparator.comparingDouble((Candidate c) -> c.score).reversed().thenComparing(c -> c.text));
                Set<String> seen = new HashSet<>(); list.removeIf(c -> !seen.add(c.text));
            }
        d.pinyinSyllables=new PinyinSyllableIndex(d.pinyin,readings); d.zhuyinPrefixes=new ReadingIndex(d.zhuyin);
        d.pinyinPrefixes=new ReadingIndex(d.pinyin);
        d.indexEnglish();
        return d;
    }
    private static void add(Map<String, List<Candidate>> map, String key, Candidate c) {
        map.computeIfAbsent(key, k -> new ArrayList<>()).add(c);
    }
    public static String normalize(String s) { return s.toLowerCase(Locale.ROOT).replace("ü", "v").replace("'", "").replace(" ", ""); }
    private static String toneless(String s) { return s.replaceAll("[ˊˇˋ˙ˉ]", ""); }
    public boolean isEnglish(String raw) { return english.containsKey(raw.toLowerCase(Locale.ROOT)); }
    public boolean isEnglish(String raw,boolean latinContext) {return (latinContext?foldedEnglish:english).containsKey(raw.toLowerCase(Locale.ROOT));}
    private void indexEnglish() {
        foldedEnglish.clear();english.forEach((word,frequency)->foldedEnglish.merge(word.toLowerCase(Locale.ROOT),frequency,Math::max));
    }
    /** Literal intent must not turn into Chinese solely because abbreviation search found a word. */
    public boolean hasCompletePinyin(String raw) {
        String key=raw.toLowerCase(Locale.ROOT).replace('ü','v').replace(' ', '\'');
        boolean[] reachable=new boolean[key.length()+1]; reachable[0]=true;
        for(int end=1;end<=key.length();end++) {
            if(key.charAt(end-1)=='\'') reachable[end]=reachable[end-1];
            for(int start=Math.max(0,end-MAX_KEY);start<end && !reachable[end];start++)
                if(reachable[start] && pinyin.containsKey(key.substring(start,end))) reachable[end]=true;
        }
        return !key.isEmpty() && reachable[key.length()];
    }
    public boolean legalPinyin(String raw) {
        for (String token : raw.toLowerCase(Locale.ROOT).replace('ü', 'v').split("[' ]", -1)) {
            if (token.isEmpty()) return false;
            boolean[] ok = new boolean[token.length() + 1]; ok[0] = true;
            for (int i = 1; i <= token.length(); i++) for (int j = Math.max(0, i - 6); j < i; j++)
                if (ok[j] && syllables.contains(token.substring(j, i))) { ok[i] = true; break; }
            if (!ok[token.length()]) return false;
        }
        return !raw.isEmpty();
    }
    public List<Candidate> convert(String raw, boolean bpmf) { return convert(raw,bpmf,""); }
    public List<Candidate> convert(String raw, boolean bpmf,String context) {
        String key = bpmf ? raw.replace(" ", "") : raw.toLowerCase(Locale.ROOT).replace("ü", "v").replace(' ', '\'');
        if (key.isEmpty() || key.length() > 96) return Collections.emptyList();
        Map<String, List<Candidate>> index = bpmf ? zhuyin : pinyin;
        List<Candidate> exact = index.get(bpmf?key.replace("ˉ", ""):key);
        // Keep every exact homophone reachable by scrolling, including rare glyphs.
        List<Candidate> result = new ArrayList<>();
        if (exact != null) for(Candidate c:exact) if(!bpmf || firstTonesMatch(key,c.reading)) result.add(c);
        if (bpmf) {
            List<Candidate> relaxed = index.get("~" + toneless(key));
            if (relaxed != null) for(Candidate c:relaxed) if(firstTonesMatch(key,c.reading)) result.add(c);
        }
        List<List<Candidate>> paths = new ArrayList<>();
        for (int i = 0; i <= key.length(); i++) paths.add(new ArrayList<>());
        paths.get(0).add(new Candidate("", false, 0));
        for (int end = 1; end <= key.length(); end++) {
            List<Candidate> choices = paths.get(end);
            if (!bpmf && key.charAt(end - 1) == '\'') choices.addAll(paths.get(end - 1));
            for (int start = Math.max(0, end - MAX_KEY); start < end; start++) {
                if (paths.get(start).isEmpty()) continue;
                String part=key.substring(start,end);
                List<Candidate> words = index.get(bpmf?part.replace("ˉ", ""):part);
                if (words == null) continue;
                if(bpmf && part.indexOf('ˉ')>=0) {
                    words=new ArrayList<>(words); words.removeIf(c->!firstTonesMatch(part,c.reading));
                }
                for (Candidate before : paths.get(start)) for (int w = 0; w < Math.min(3, words.size()); w++) {
                    Candidate word = words.get(w);
                    choices.add(new Candidate(before.text + word.text, false, before.score + word.score));
                }
            }
            choices.sort(Comparator.comparingDouble((Candidate c) -> c.score).reversed());
            Set<String> seen = new HashSet<>(); choices.removeIf(c -> !seen.add(c.text));
            if (choices.size() > BEAM) choices.subList(BEAM, choices.size()).clear();
        }
        result.addAll(paths.get(key.length()));
        if(bpmf) result.addAll(zhuyinPrefixes.complete(key.replace("ˉ", ""),(reading,c)->firstTonesMatch(key,c.reading)));
        else {
            result.addAll(pinyinSyllables.convert(key));
            result.addAll(pinyinPrefixes.complete(key,(reading,c)->true));
        }
        // Apply only the boundary signal; retain dictionary word probabilities.
        if(!bpmf && !context.isEmpty())result.replaceAll(c->new Candidate(c.text,c.literal,
            c.score+.5*(contextModel.chinese(context,c.text)-contextModel.chinese("",c.text)),c.reading));
        result.sort(Comparator.comparingDouble((Candidate c) -> c.score).reversed().thenComparing(c -> c.text));
        Set<String> seen = new HashSet<>(); result.removeIf(c -> !seen.add(c.text));
        return result;
    }
    /** Explicit first tone constrains both tone and the source syllable boundary. */
    private static boolean firstTonesMatch(String raw,String reading) {
        int offset=0;
        for(int i=0;i<raw.length();i++) {
            char ch=raw.charAt(i);
            if(ch=='ˉ') {
                int at=0; boolean found=false;
                for(int j=0;j<reading.length();j++) {
                    char r=reading.charAt(j);
                    if(r=='ˉ' && at==offset) { found=true; break; }
                    if(r>=0x3105 && r<=0x3129) at++;
                }
                if(!found) return false;
            } else if(ch>=0x3105 && ch<=0x3129) offset++;
        }
        return true;
    }
    public List<Candidate> englishCompletions(String raw) {
        return englishCompletions(raw,false);
    }
    public List<Candidate> englishCompletions(String raw,boolean latinContext) {
        if (raw.length() < 2 || !raw.matches("[A-Za-z]+(?:'[A-Za-z]*)?")) return Collections.emptyList();
        String key = raw.toLowerCase(Locale.ROOT);
        boolean caps=raw.equals(raw.toUpperCase(Locale.ROOT));
        boolean title=raw.equals(Character.toUpperCase(key.charAt(0))+key.substring(1));
        if(!caps && !title && !raw.equals(key)) return Collections.emptyList();
        Comparator<Candidate> order=Comparator.comparingDouble((Candidate c)->c.score).reversed().thenComparing(c->c.text);
        PriorityQueue<Candidate> top=new PriorityQueue<>(order.reversed());
        for (Map.Entry<String, Integer> e : (latinContext?foldedEnglish:english).tailMap(key).entrySet()) {
            if (!e.getKey().startsWith(key)) break;
            if(!latinContext && !e.getKey().equals(e.getKey().toLowerCase(Locale.ROOT)))continue;
            if (!e.getKey().equals(key)) {
                String text = e.getKey();
                if (caps) text=text.toUpperCase(Locale.ROOT);
                else if (title) text = Character.toUpperCase(text.charAt(0)) + text.substring(1);
                top.add(new Candidate(text, true, e.getValue()));
                if(top.size()>24) top.remove();
            }
        }
        List<Candidate> result=new ArrayList<>(top);result.sort(order);return result;
    }
    public List<Candidate> predict(String context) {
        List<Candidate> result = new ArrayList<>();
        for (int i = 0; i < context.length(); i++) {
            List<Candidate> found = continuations.get(context.substring(i));
            if (found != null) result.addAll(found.subList(0, Math.min(6, found.size())));
        }
        Set<String> seen = new HashSet<>(); result.removeIf(c -> !seen.add(c.text));
        return result.subList(0, Math.min(6, result.size()));
    }
    /** One-edit spelling alternatives, bounded by the input length, with source frequencies. */
    public List<Candidate> englishCorrections(String raw) {
        if(raw.length()<2 || raw.length()>32 || !raw.matches("[A-Za-z]+(?:'[A-Za-z]+)?")
                || IntentClassifier.technicalWord(raw.toLowerCase(Locale.ROOT))) return Collections.emptyList();
        String key=raw.toLowerCase(Locale.ROOT);
        boolean caps=raw.equals(raw.toUpperCase(Locale.ROOT));
        boolean title=raw.equals(Character.toUpperCase(key.charAt(0))+key.substring(1));
        if(!caps && !title && !raw.equals(key))return Collections.emptyList();
        Set<String> edits=new HashSet<>();
        boolean apostropheOnly=raw.length()<3 || isEnglish(raw,true);
        for(int i=0;i<=key.length();i++) {
            if(apostropheOnly) {edits.add(key.substring(0,i)+"'"+key.substring(i));continue;}
            if(i<key.length())edits.add(key.substring(0,i)+key.substring(i+1));
            if(i+1<key.length())edits.add(key.substring(0,i)+key.charAt(i+1)+key.charAt(i)+key.substring(i+2));
            for(char c: "abcdefghijklmnopqrstuvwxyz'".toCharArray()) {
                edits.add(key.substring(0,i)+c+key.substring(i));
                if(i<key.length())edits.add(key.substring(0,i)+c+key.substring(i+1));
            }
        }
        List<Candidate> result=new ArrayList<>();
        for(String word:edits)if(foldedEnglish.containsKey(word)) {
            String text=caps?word.toUpperCase(Locale.ROOT):title?Character.toUpperCase(word.charAt(0))+word.substring(1):word;
            if(text.startsWith("i'"))text="I"+text.substring(1);
            result.add(new Candidate(text,true,foldedEnglish.get(word)));
        }
        result.sort(Comparator.comparingDouble((Candidate c)->c.score).reversed().thenComparing(c->c.text));
        return result.subList(0,Math.min(8,result.size()));
    }
}
