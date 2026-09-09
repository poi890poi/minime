package dev.minime.core;

import java.io.*;
import java.util.*;

/** Source-owned output alternatives. No runtime translation or homophone guessing. */
public final class PairedForms {
    public static final PairedForms EMPTY=new PairedForms(Collections.emptyMap());
    public static final class Pair {
        public final String phonetic,han,source;
        Pair(String phonetic,String han,String source) {this.phonetic=phonetic;this.han=han;this.source=source;}
        boolean same(Pair other) {return other!=null && phonetic.equals(other.phonetic) && han.equals(other.han) && source.equals(other.source);}
    }
    private final Map<String,Pair> entries;
    private PairedForms(Map<String,Pair> entries) {this.entries=Collections.unmodifiableMap(entries);}
    public static PairedForms read(Reader input)throws IOException {
        Map<String,Pair> result=new HashMap<>();
        try(BufferedReader reader=new BufferedReader(input)) {
            String line;while((line=reader.readLine())!=null) {
                if(line.isEmpty() || line.startsWith("#"))continue;
                String[] p=line.split("\t",-1);
                if(p.length!=4 || !p[0].equals("poj") || p[1].isEmpty() || p[1].length()>96
                        || p[2].isEmpty() || p[2].length()>24 || !p[3].matches("(?:itaigi|taihoa):[0-9]+")
                        || !p[2].codePoints().allMatch(c->Character.UnicodeScript.of(c)==Character.UnicodeScript.HAN)
                        || result.size()>=200000 || result.containsKey(p[1]))throw new IOException("Invalid paired form");
                result.put(p[1],new Pair(p[1],p[2],p[3]));
            }
        }return new PairedForms(result);
    }
    Pair forEntry(String pack,String output,String source) {
        return pack.equals("poj") && (source.startsWith("itaigi:") || source.startsWith("taiwanese-basic:"))?entries.get(output):null;
    }
}
