package dev.minime.core;

import java.util.*;

/** Bounded local observations, promoted after three accepted occurrences. */
public final class PhraseLexicon {
    private final Map<String,Integer> observations=new TreeMap<>();
    public static final int LIMIT=512, THRESHOLD=3;
    static String normalize(String reading) {return reading.toLowerCase(Locale.ROOT).replace("'","");}
    static boolean valid(String reading,String output) {
        int size=output.codePointCount(0,output.length());
        return !reading.isEmpty() && reading.length()<=96 && reading.matches("[a-zv']+|[\\u3105-\\u3129ˉˊˇˋ˙]+")
            && size>=2 && size<=16 && output.codePoints().allMatch(cp->Character.UnicodeScript.of(cp)==Character.UnicodeScript.HAN);
    }
    public PhraseLexicon(String saved) {
        if(saved.length()>150000)return;
        for(String line:saved.split("\n")) {
            String[] p=line.split("\t",-1);
            if(p.length!=3 || !valid(p[0],p[1]) || observations.size()>=LIMIT)continue;
            try {int count=Integer.parseInt(p[2]);if(count>0)observations.put(normalize(p[0])+"\t"+p[1],Math.min(100,count));}
            catch(NumberFormatException ignored) { }
        }
    }
    public void observe(String reading,String output) {
        if(!valid(reading,output))return;
        String key=normalize(reading)+"\t"+output;
        if(observations.size()<LIMIT || observations.containsKey(key))observations.put(key,Math.min(100,observations.getOrDefault(key,0)+1));
    }
    public List<Candidate> lookup(String reading) {
        String prefix=normalize(reading)+"\t";List<Candidate> result=new ArrayList<>();
        for(Map.Entry<String,Integer> e:observations.entrySet())if(e.getValue()>=THRESHOLD && e.getKey().startsWith(prefix))
            result.add(Candidate.supplement(e.getKey().substring(prefix.length()),e.getValue()));
        result.sort(Comparator.comparingDouble((Candidate c)->c.score).reversed().thenComparing(c->c.text));
        return result.size()>8?new ArrayList<>(result.subList(0,8)):result;
    }
    public String serialize() {
        StringBuilder out=new StringBuilder();observations.forEach((key,count)->out.append(key).append('\t').append(count).append('\n'));return out.toString();
    }
}
