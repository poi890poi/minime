package dev.minime.ime;

import android.graphics.Paint;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Predicate;

/** Device font coverage only. No vocabulary exceptions or Unicode-block blacklist. */
final class CandidateGlyphs implements Predicate<String> {
    private final Paint paint;
    private final Map<Integer,Boolean> supported=new LinkedHashMap<Integer,Boolean>(1024,.75f,true) {
        @Override protected boolean removeEldestEntry(Map.Entry<Integer,Boolean> entry) {return size()>8192;}
    };
    CandidateGlyphs(Paint actualCandidatePaint) {paint=new Paint(actualCandidatePaint);}
    @Override public boolean test(String text) {
        for(int at=0;at<text.length();) {
            int cp=text.codePointAt(at);at+=Character.charCount(cp);
            if(cp>=0xd800 && cp<=0xdfff || cp==0xfffd || (cp>=0xfdd0 && cp<=0xfdef) || (cp&0xffff)>=0xfffe)return false;
            // Format controls and variation selectors modify surrounding glyphs.
            // hasGlyph on a whole POJ cluster would incorrectly demand a ligature.
            if(Character.isWhitespace(cp) || Character.getType(cp)==Character.FORMAT || cp>=0xfe00 && cp<=0xfe0f || cp>=0xe0100 && cp<=0xe01ef)continue;
            if(!supported.computeIfAbsent(cp,key->paint.hasGlyph(new String(Character.toChars(key)))))return false;
        }
        return true;
    }
}
