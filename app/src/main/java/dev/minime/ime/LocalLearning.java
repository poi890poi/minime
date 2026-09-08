package dev.minime.ime;

import android.content.*;
import dev.minime.core.*;
import java.util.*;

final class LocalLearning implements Learning {
    private final SharedPreferences preferences;
    private final SharedPreferences settings;
    private String phraseSource,customSource;
    private PhraseLexicon phraseCache;
    private final Map<String,List<Candidate>> customCache=new HashMap<>();
    LocalLearning(Context context) { preferences=context.getSharedPreferences("learning",Context.MODE_PRIVATE);settings=context.getSharedPreferences("settings",Context.MODE_PRIVATE); }
    private PhraseLexicon phraseLexicon() {
        String saved=preferences.getString("phrases_v1","");
        if(!saved.equals(phraseSource)) {phraseCache=new PhraseLexicon(saved);phraseSource=saved;}
        return phraseCache;
    }
    public void observePhrase(String reading,String output) {
        if(!settings.getBoolean("phrase_learning",false))return;
        PhraseLexicon lexicon=phraseLexicon();
        lexicon.observe(reading,output);phraseSource=lexicon.serialize();preferences.edit().putString("phrases_v1",phraseSource).apply();
    }
    public List<Candidate> phrases(String raw) {
        if(!settings.getBoolean("phrase_learning",false))return Collections.emptyList();
        return phraseLexicon().lookup(raw);
    }
    public void rememberEnglish(String context,String word) {
        if(!settings.getBoolean("english_learning",false) || context.isEmpty())return;
        choose("EN_NEXT",context,word);
        int at=context.lastIndexOf(' ');if(at>=0)choose("EN_NEXT",context.substring(at+1),word);
    }
    public List<Candidate> predictEnglish(String context) {
        List<Candidate> result=new ArrayList<>();if(!settings.getBoolean("english_learning",false) || context.isEmpty())return result;
        String prefix="EN_NEXT\t"+context+"\t";
        for(Map.Entry<String,?> e:preferences.getAll().entrySet())if(e.getKey().startsWith(prefix) && e.getValue() instanceof Integer)
            result.add(new Candidate(e.getKey().substring(prefix.length()),true,(Integer)e.getValue()));
        result.sort(Comparator.comparingDouble((Candidate c)->c.score).reversed());return result;
    }
    private static String key(String c,String r,String v) { return c+"\t"+r+"\t"+v; }
    public int count(String c,String r,String v) { return preferences.getInt(key(c,r,v),0); }
    public void choose(String c,String r,String v) {
        if (preferences.getAll().size() >= 2000 && !preferences.contains(key(c,r,v))) return;
        preferences.edit().putInt(key(c,r,v),Math.min(100,count(c,r,v)+1)).apply();
    }
    public List<Candidate> custom(String raw) {
        String saved=preferences.getString("custom","");
        if(!saved.equals(customSource)) {
            customCache.clear();
            for(String row:saved.split("\n")) {
                String[] p=row.split("\t");
                if(p.length==2)customCache.computeIfAbsent(p[0],key->new ArrayList<>()).add(new Candidate(p[1],p[0].equals(p[1]),0));
            }
            customSource=saved;
        }
        return new ArrayList<>(customCache.getOrDefault(raw,Collections.emptyList()));
    }
}
