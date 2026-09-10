package dev.minime.core;

import java.io.*;
import java.util.*;
import static dev.minime.core.Regression.*;

final class AddonBinaryRegression {
    static byte[] save(AddonDictionary dictionary)throws IOException {
        ByteArrayOutputStream out=new ByteArrayOutputStream();dictionary.writeBinary(out);return out.toByteArray();
    }
    static List<String> identity(List<Candidate> values) {
        List<String> result=new ArrayList<>();
        for(Candidate c:values)result.add(c.text+":"+c.literal+":"+c.supplemental+":"+c.score+":"+c.pack+":"+c.abbreviated+":"+c.incomplete+":"+c.consumed+":"+(c.pair==null?"":c.pair.phonetic+"/"+c.pair.han+"/"+c.pair.source));
        return result;
    }
    static void run()throws Exception {
        PairedForms pairs=PairedForms.read(new StringReader("poj\tfixture-tone\t甲乙\titaigi:1\n"));
        StringBuilder rows=new StringBuilder("poj\tka'na\tfixture-tone\titaigi:1\ttest\njapanese\tka'na\tかな\ttest\teveryday_vocabulary\ntaiwan\tab\t甲乙\ttest\ttest\n");
        for(int i=0;i<40;i++)rows.append("poj\tka'na\tvalue"+i%30+"\ttest\textended_vocabulary\n");
        AddonDictionary text=AddonDictionary.read(new StringReader(rows.toString()),pairs);byte[] bytes=save(text);
        AddonDictionary binary=AddonDictionary.readBinary(new ByteArrayInputStream(bytes));
        yes(Arrays.equals(bytes,save(binary)),"supplemental binary round trip is deterministic");
        for(String query:Arrays.asList("kana","ka'na","ka","k","kn","k'na","ka'n","ab","a","missing"))
            for(Set<String> scope:Arrays.asList(Collections.singleton("poj"),Collections.singleton("japanese"),Collections.singleton("taiwan"),new HashSet<>(Arrays.asList("poj","japanese","taiwan"))))
                equal(identity(text.lookup(query,scope)),identity(binary.lookup(query,scope)),"binary preserves exact/prefix/mixed/initial metadata and duplicate budgets");
        byte[] badMagic=bytes.clone();badMagic[0]=0;
        byte[] trailing=Arrays.copyOf(bytes,bytes.length+1);
        for(byte[] invalid:Arrays.asList(new byte[0],badMagic,Arrays.copyOf(bytes,bytes.length-1),trailing)) {
            try {AddonDictionary.readBinary(new ByteArrayInputStream(invalid));throw new AssertionError("Invalid supplemental model accepted");}
            catch(IOException expected){}
        }
    }
}
