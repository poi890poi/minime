package dev.minime.core;

import java.io.*;
import java.util.*;

/** Optional source-ranked character choices, separate from the general word lexicon. */
public final class JapaneseBasics {
    public static final JapaneseBasics EMPTY=new JapaneseBasics(Collections.emptyMap(),Collections.emptyMap());
    private final Map<String,List<Candidate>> exact;
    private final ReadingIndex prefixes;
    private final ReadingUnitIndex units;
    private JapaneseBasics(Map<String,List<Candidate>> exact,Map<String,List<Candidate>> readings) {
        this.exact=exact;prefixes=new ReadingIndex(exact);units=new ReadingUnitIndex(readings,readings.keySet());
    }
    private static final Comparator<Candidate> ORDER=Comparator.comparingDouble((Candidate c)->c.score).reversed().thenComparing(c->c.text);
    public static JapaneseBasics read(Reader input)throws IOException {
        Map<String,List<Candidate>> exact=new HashMap<>(),readings=new HashMap<>();int count=0;
        try(BufferedReader in=new BufferedReader(input)) {
            String line;while((line=in.readLine())!=null) {
                if(line.isEmpty() || line.startsWith("#"))continue;
                String[] p=line.split("\t",-1);
                if(p.length!=5 || !p[1].matches("[a-z']{1,48}") || p[2].codePointCount(0,p[2].length())!=1 || ++count>10000)throw new IOException("Invalid Japanese character row");
                int rank;try{rank=Integer.parseInt(p[3]);}catch(NumberFormatException e){throw new IOException("Invalid character rank",e);}
                Character.UnicodeScript script=Character.UnicodeScript.of(p[2].codePointAt(0));
                boolean kana=(p[0].equals("hiragana") && script==Character.UnicodeScript.HIRAGANA) || (p[0].equals("katakana") && script==Character.UnicodeScript.KATAKANA);
                boolean kanji=p[0].equals("kanji") && script==Character.UnicodeScript.HAN;
                if(!(kana && rank==0 && p[4].equals("wanakana:5.3.1")) && !(kanji && rank>=1 && rank<=500 && p[4].equals(String.format(Locale.ROOT,"kanjidic:U%04X",p[2].codePointAt(0)))))throw new IOException("Invalid Japanese character source");
                Candidate c=Candidate.supplement(p[2],kana?2:1-rank/1000.0).inPack("japanese").asLanguageCharacter();
                exact.computeIfAbsent(normalize(p[1]),k->new ArrayList<>()).add(c);
                if(p[1].matches("[a-z]+(?:'[a-z]+)*"))readings.computeIfAbsent(p[1],k->new ArrayList<>()).add(c);
            }
        }
        exact.values().forEach(v->v.sort(ORDER));readings.values().forEach(v->v.sort(ORDER));
        return new JapaneseBasics(exact,readings);
    }
    private static String normalize(String raw){return raw.toLowerCase(Locale.ROOT).replace("'","").replace("-","").replace(" ","");}
    public List<Candidate> lookup(String raw) {
        if(raw.length()>96 || !raw.matches("[a-zA-Z'-]+"))return Collections.emptyList();
        String key=normalize(raw);List<Candidate> result=new ArrayList<>();Set<String> seen=new HashSet<>();
        for(Candidate c:exact.getOrDefault(key,Collections.emptyList()))if(seen.add(c.text)) {result.add(c);if(result.size()==8)return result;}
        List<Candidate> partials=new ArrayList<>(prefixes.complete(key,(k,c)->true));partials.addAll(units.lookup(raw.toLowerCase(Locale.ROOT).replace('-', '\'')));
        partials.sort(ORDER);
        for(Candidate c:partials)if(seen.add(c.text)) {result.add(c);if(result.size()==8)break;}
        return result;
    }
    List<Candidate> merge(String raw,List<Candidate> words) {
        List<Candidate> characters=lookup(raw),result=new ArrayList<>();Set<String> seen=new HashSet<>();
        // Complete lexical matches always precede untyped continuations. Preserve
        // all original words; this extension contributes at most eight choices.
        for(boolean incomplete:new boolean[]{false,true}) {
            for(Candidate c:characters)if(c.incomplete==incomplete && seen.add(c.text))result.add(c);
            for(Candidate c:words)if(c.incomplete==incomplete && seen.add(c.text))result.add(c);
        }
        return result;
    }
}
