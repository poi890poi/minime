package dev.minime.core;

import java.util.*;
import java.util.regex.Pattern;

/** Bounded transduction of the pinned WanaKana mapping tree (MIT).
 * Mirrors its applyMapping fallback; never guesses lexical Kanji. */
final class JapaneseKana {
    private static final Pattern INPUT=Pattern.compile("[a-zA-Z'-]+");
    private static final Pattern HIRAGANA=Pattern.compile("[\\u3041-\\u3096\\u30fc]+");
    private static final class Node {
        final Map<Character,Node> children=new HashMap<>();String output;
    }
    private final Node root=new Node();
    void put(String key,String output) {
        Node at=root;for(char c:key.toCharArray())at=at.children.computeIfAbsent(c,k->new Node());at.output=output;
    }
    List<Candidate> lookup(String raw) {
        if(raw.isEmpty() || raw.length()>96 || !INPUT.matcher(raw).matches())return Collections.emptyList();
        String input=raw.toLowerCase(Locale.ROOT);StringBuilder text=new StringBuilder();int cursor=0;
        while(cursor<input.length()) {
            Node at=root;String value="";int end=cursor;
            while(end<input.length()) {
                char c=input.charAt(end);Node child=at.children.get(c);if(child==null)break;
                value=child.output==null?value+c:child.output;at=child;end++;
            }
            if(end==cursor || !HIRAGANA.matcher(value).matches())break;
            text.append(value);cursor=end;
        }
        if(text.length()==0)return Collections.emptyList();
        String hira=text.toString();StringBuilder kata=new StringBuilder();
        hira.codePoints().forEach(cp->kata.appendCodePoint(cp>=0x3041 && cp<=0x3096?cp+0x60:cp));
        int consumed=cursor==raw.length()?0:cursor;
        return Arrays.asList(Candidate.supplement(hira,-10).inPack("japanese").consuming(consumed).asTransliteration(),
            Candidate.supplement(kata.toString(),-10).inPack("japanese").consuming(consumed).asTransliteration());
    }
}
