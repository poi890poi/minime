package dev.minime.core;

import java.util.*;
import java.util.regex.Pattern;

/** Source-attested single characters with an explicit, recoverable input span. */
final class FirstGlyphIndex {
    private static final Pattern PINYIN=Pattern.compile("[a-zv]+(?:'[a-zv]+)*");
    private final Map<String,List<Candidate>> exact=new HashMap<>(),prefix=new HashMap<>();
    private final Set<String> syllables;
    FirstGlyphIndex(Map<String,List<Candidate>> words,Set<String> syllables) {
        this.syllables=syllables;
        for(String reading:syllables) {
            List<Candidate> glyphs=new ArrayList<>();
            for(Candidate c:words.getOrDefault(reading,Collections.emptyList()))
                if(c.text.codePointCount(0,c.text.length())==1 && Character.UnicodeScript.of(c.text.codePointAt(0))==Character.UnicodeScript.HAN)glyphs.add(c);
            if(glyphs.isEmpty())continue;
            exact.put(reading,glyphs);
            for(int end=1;end<reading.length();end++) {
                List<Candidate> at=prefix.computeIfAbsent(reading.substring(0,end),k->new ArrayList<>());
                for(Candidate c:glyphs)at.add(c.completing(c.score-.7-.08*(reading.length()-end)));
            }
        }
        for(List<Candidate> values:prefix.values()) {
            values.sort(Comparator.comparingDouble((Candidate c)->c.score).reversed().thenComparing(c->c.text));
            Set<String> seen=new HashSet<>();values.removeIf(c->!seen.add(c.text));
        }
    }
    private boolean unit(String s){return syllables.contains(s)||prefix.containsKey(s);}
    private int cost(String s){return syllables.contains(s)?0:1;}
    List<Candidate> lookup(String raw) {
        int n=raw.length();
        if(n<2 || n>96 || !PINYIN.matcher(raw).matches())return Collections.emptyList();
        // Prefer complete syllables, then the fewest abbreviated units. This
        // establishes a raw boundary only; it never assembles candidate text.
        int[] missing=new int[n+1],units=new int[n+1];Arrays.fill(missing,1000);missing[n]=0;
        for(int start=n-1;start>=0;start--) {
            for(int end=start+1;end<=Math.min(n,start+6);end++) {
                String part=raw.substring(start,end);if(part.indexOf('\'')>=0)break;
                if(!unit(part))continue;
                int next=end<n&&raw.charAt(end)=='\''?end+1:end;
                if(missing[next]>=1000)continue;
                int m=cost(part)+missing[next],u=1+units[next];
                if(m<missing[start] || (m==missing[start] && u<units[start])) {missing[start]=m;units[start]=u;}
            }
        }
        int bestEnd=0,bestMissing=1000,bestUnits=1000;
        for(int end=1;end<Math.min(n,7);end++) {
            String part=raw.substring(0,end);if(part.indexOf('\'')>=0)break;
            if(!exact.containsKey(part)&&!prefix.containsKey(part))continue;
            int next=raw.charAt(end)=='\''?end+1:end;
            if(next>=n)continue;
            if(missing[next]>=1000)continue;
            int m=cost(part)+missing[next],u=1+units[next];
            if(m<bestMissing || (m==bestMissing && (u<bestUnits || (u==bestUnits && end>bestEnd)))) {
                bestEnd=end;bestMissing=m;bestUnits=u;
            }
        }
        // A later typo must not erase a recoverable first syllable. A single
        // intact syllable needs no first-character fallback.
        if(bestEnd==0 && !syllables.contains(raw)) {
            for(int end=1;end<Math.min(n,7);end++) {
                String part=raw.substring(0,end);if(part.indexOf('\'')>=0)break;
                if(exact.containsKey(part))bestEnd=end;
            }
        }
        if(bestEnd==0)return Collections.emptyList();
        String first=raw.substring(0,bestEnd);
        List<Candidate> values=exact.getOrDefault(first,prefix.getOrDefault(first,Collections.emptyList()));
        List<Candidate> result=new ArrayList<>(values.size());
        for(Candidate c:values)result.add(c.consuming(bestEnd));
        return result;
    }
}
