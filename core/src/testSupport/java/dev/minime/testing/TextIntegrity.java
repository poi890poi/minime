package dev.minime.testing;

import java.io.*;
import java.nio.charset.*;
import java.util.*;

/** Test-only checks: valid Unicode is separate from installed font coverage. */
public final class TextIntegrity {
    private TextIntegrity() {}
    public static Reader utf8(InputStream input) {
        return new InputStreamReader(input, StandardCharsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT));
    }
    public static String codePoints(String text) {
        StringJoiner out=new StringJoiner(" ");
        text.codePoints().forEach(cp->out.add(String.format(Locale.ROOT,"U+%04X",cp)));
        return out.toString();
    }
    public static List<String> problems(String text) {
        List<String> out=new ArrayList<>();
        for(int at=0;at<text.length();) {
            int cp=text.codePointAt(at);
            String kind=null;
            if(cp>=0xd800 && cp<=0xdfff)kind="unpaired-surrogate";
            else if(cp==0xfffd)kind="replacement-character";
            else if((cp>=0xfdd0 && cp<=0xfdef) || (cp&0xffff)>=0xfffe)kind="noncharacter";
            else if((cp<32 && cp!='\t' && cp!='\n' && cp!='\r') || (cp>=0x7f && cp<=0x9f))kind="control-character";
            if(kind!=null)out.add(kind+" at UTF-16 "+at+" "+String.format(Locale.ROOT,"U+%04X",cp));
            at+=Character.charCount(cp);
        }
        return out;
    }
    public static void require(String text,String location) {
        List<String> errors=problems(text);
        if(!errors.isEmpty())throw new AssertionError(location+": "+errors);
    }
}
