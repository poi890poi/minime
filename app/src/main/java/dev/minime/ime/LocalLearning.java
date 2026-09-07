package dev.minime.ime;

import android.content.*;
import dev.minime.core.*;
import java.util.*;

final class LocalLearning implements Learning {
    private final SharedPreferences preferences;
    private final SharedPreferences settings;
    LocalLearning(Context context) { preferences=context.getSharedPreferences("learning",Context.MODE_PRIVATE);settings=context.getSharedPreferences("settings",Context.MODE_PRIVATE); }
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
        List<Candidate> list=new ArrayList<>();
        for(String row:preferences.getString("custom","").split("\n")) {
            String[] p=row.split("\t");
            if(p.length==2 && p[0].equals(raw)) list.add(new Candidate(p[1],p[0].equals(p[1]),0));
        }
        return list;
    }
}
