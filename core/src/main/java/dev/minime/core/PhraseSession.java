package dev.minime.core;

import java.util.*;
import java.util.function.BiConsumer;

/** Bounded accepted segments only; never reads editor text or crosses an edit boundary. */
final class PhraseSession {
    private final ArrayDeque<String[]> segments=new ArrayDeque<>();
    void clear() {segments.clear();}
    void accept(String reading,String output,BiConsumer<String,String> observe) {
        if(!reading.matches("[a-zv']+|[\\u3105-\\u3129ˉˊˇˋ˙]+")
                || !output.codePoints().allMatch(cp->Character.UnicodeScript.of(cp)==Character.UnicodeScript.HAN)
                || output.codePointCount(0,output.length())>16) {clear();return;}
        segments.addLast(new String[]{reading,output});while(segments.size()>8)segments.removeFirst();
        String raw="",text="";
        for(Iterator<String[]> it=segments.descendingIterator();it.hasNext();) {
            String[] part=it.next();raw=part[0]+raw;text=part[1]+text;
            if(raw.length()>96 || text.codePointCount(0,text.length())>16)break;
            if(text.codePointCount(0,text.length())>=2)observe.accept(raw,text);
        }
    }
}
