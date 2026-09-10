package dev.minime.ime;

import android.content.Context;
import java.io.*;
import java.nio.charset.StandardCharsets;

/** Deterministic packaged-data probes; no production entry is selected to satisfy a test. */
final class AddonTestData {
    static String[] probe(Context context,String pack) throws IOException {
        return probe(context,pack,"");
    }
    static String[] probe(Context context,String pack,String categoryPrefix) throws IOException {
        String asset=pack.equals("geography")?"geography.tsv":"addon-"+pack+".tsv";
        try(BufferedReader in=new BufferedReader(new InputStreamReader(context.getAssets().open(asset),StandardCharsets.UTF_8))) {
            String line;while((line=in.readLine())!=null) {
                String[] p=line.split("\t");if(p.length!=5 || !p[0].equals(pack) || !p[4].startsWith(categoryPrefix))continue;
                String key=p[1].replace("'","").replace("-","").replace(" ","");
                if(!key.matches("[a-z]{4,14}") || key.equals(p[2]))continue;
                if(pack.equals("japanese") && p[2].codePoints().noneMatch(cp->Character.UnicodeScript.of(cp)==Character.UnicodeScript.HIRAGANA || Character.UnicodeScript.of(cp)==Character.UnicodeScript.KATAKANA))continue;
                return new String[]{key,p[2]};
            }
        }throw new IOException("No eligible packaged probe for "+pack);
    }
}
