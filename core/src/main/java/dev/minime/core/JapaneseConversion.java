package dev.minime.core;

import java.util.*;
import java.util.function.BooleanSupplier;

/** Optional whole-sequence provider. Dictionary predictions remain independently usable. */
public final class JapaneseConversion {
    private JapaneseConversion() {}
    public interface Provider { List<String> convert(String kana); }
    public static List<Candidate> merge(String raw,Set<String> enabled,AddonDictionary dictionary,
            List<Candidate> fallback,Provider provider,BooleanSupplier current) {
        if(provider==null || !enabled.contains("japanese") || !current.getAsBoolean())return fallback;
        String kana=dictionary.japaneseConversionReading(raw);
        if(kana.isEmpty())return fallback;
        List<String> surfaces;
        try {surfaces=provider.convert(kana);}catch(RuntimeException unavailable){return fallback;}
        if(!current.getAsBoolean() || surfaces==null || surfaces.isEmpty())return fallback;
        List<Candidate> converted=new ArrayList<>();Set<String> nativeSeen=new HashSet<>();
        for(String surface:surfaces) {
            if(converted.size()==8)break;
            if(surface==null || surface.isEmpty() || surface.length()>256 || !validJapanese(surface))continue;
            if(nativeSeen.add(surface))converted.add(Candidate.supplement(surface,0).inPack("japanese"));
        }
        if(converted.isEmpty())return fallback;
        List<Candidate> result=new ArrayList<>();Set<String> seen=new HashSet<>();
        // Preserve attested full words and source-ranked single characters.
        for(Candidate c:fallback)if(c.pack.equals("japanese") && !c.incomplete && c.consumed==0
                && (c.languageCharacter || !kanaForm(c.text,kana))) {result.add(c);seen.add(c.text);}
        for(Candidate c:converted)if(seen.add(c.text))result.add(c);
        for(Candidate c:fallback)if(!c.pack.equals("japanese") || seen.add(c.text))result.add(c);
        return result;
    }
    private static boolean kanaForm(String surface,String kana) {
        StringBuilder hira=new StringBuilder();
        surface.codePoints().forEach(cp->hira.appendCodePoint(cp>=0x30a1 && cp<=0x30f6?cp-0x60:cp));
        return hira.toString().equals(kana);
    }
    private static boolean validJapanese(String text) {
        boolean japanese=false;
        for(int cp:text.codePoints().toArray()) {
            Character.UnicodeScript script=Character.UnicodeScript.of(cp);
            if(script==Character.UnicodeScript.HIRAGANA || script==Character.UnicodeScript.KATAKANA || script==Character.UnicodeScript.HAN)japanese=true;
            else if(script!=Character.UnicodeScript.COMMON || Character.isISOControl(cp))return false;
        }
        return japanese;
    }
}
