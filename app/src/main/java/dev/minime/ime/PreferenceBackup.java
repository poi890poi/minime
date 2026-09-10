package dev.minime.ime;

import android.content.SharedPreferences;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Explicit user document transfer. No network, automatic export or stored URI. */
final class PreferenceBackup {
    static final int MAX_BYTES=4*1024*1024;
    private static final Set<String> BOOL=new HashSet<>(Arrays.asList("zhuyin","rime_pinyin","english_correction","double_space_period","emoji_recents","english_learning","focused_choice_learning","phrase_learning","addon_taiwan","addon_geography","addon_japanese","addon_poj","paired_taiwanese","taiwanese_han_primary","english_mode","english_punctuation"));
    static final class Snapshot {
        final Map<String,Object> settings,learning;
        Snapshot(Map<String,Object> s,Map<String,Object> l){settings=Collections.unmodifiableMap(s);learning=Collections.unmodifiableMap(l);}
    }
    static byte[] encode(SharedPreferences settings,SharedPreferences learning)throws IOException {
        try {
            JSONObject root=new JSONObject();root.put("format","MinIME preferences");root.put("version",1);
            root.put("settings",new JSONObject(settings.getAll()));root.put("learning",new JSONObject(learning.getAll()));
            byte[] bytes=root.toString(2).getBytes(StandardCharsets.UTF_8);decode(new ByteArrayInputStream(bytes));return bytes;
        } catch(JSONException e){throw new IOException("Could not encode preferences",e);}
    }
    static Snapshot decode(InputStream input)throws IOException {
        ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] block=new byte[8192];int count;
        while((count=input.read(block))!=-1){if(out.size()+count>MAX_BYTES)throw new IOException("Backup exceeds 4 MB");out.write(block,0,count);}
        try {
            String source=new String(out.toByteArray(),StandardCharsets.UTF_8);JSONTokener tokener=new JSONTokener(source);
            Object parsed=tokener.nextValue();if(!(parsed instanceof JSONObject)||tokener.nextClean()!=0)throw new IOException("Invalid backup document");
            JSONObject root=(JSONObject)parsed;
            if(root.length()!=4||!"MinIME preferences".equals(root.get("format"))||!(root.get("version") instanceof Integer)||root.getInt("version")!=1)throw new IOException("Unsupported backup format");
            return new Snapshot(values(root.getJSONObject("settings"),false),values(root.getJSONObject("learning"),true));
        } catch(JSONException | IllegalArgumentException e){throw new IOException("Invalid backup document",e);}
    }
    private static Map<String,Object> values(JSONObject object,boolean learned)throws JSONException,IOException {
        if(object.length()>(learned?2200:128))throw new IOException("Too many preferences");
        Map<String,Object> result=new TreeMap<>();Iterator<String> keys=object.keys();
        while(keys.hasNext()) {
            String key=keys.next();Object value=object.get(key);
            if(key.isEmpty()||key.length()>(learned?512:128)||!(value instanceof String||value instanceof Integer||value instanceof Boolean))throw new IOException("Unsupported preference");
            if(learned) {
                if(key.equals("custom")||key.equals("phrases_v1")||key.equals("recent_emoji")) {
                    if(!(value instanceof String))throw new IOException("Invalid learned text");
                    int limit=key.equals("custom")?16000:key.equals("recent_emoji")?8192:150000;
                    if(((String)value).length()>limit)throw new IOException("Learned text is too long");
                    if(key.equals("custom"))for(String row:((String)value).split("\n"))if(!row.isEmpty()&&!row.matches("[^\\t\\r\\n]{1,96}\t[^\\t\\r\\n]{1,96}"))throw new IOException("Invalid custom dictionary row");
                } else if(!(value instanceof Integer)||(Integer)value<0||(Integer)value>100||key.split("\t",-1).length!=3)throw new IOException("Invalid learned choice");
            } else {
                if(BOOL.contains(key)&&!(value instanceof Boolean))throw new IOException("Invalid setting type");
                if((key.equals("mixed_mode")||key.equals("symbol_category"))&&!(value instanceof String))throw new IOException("Invalid setting type");
                if(key.equals("symbol_page")&&(!(value instanceof Integer)||(Integer)value<0||(Integer)value>10000))throw new IOException("Invalid symbol page");
                if(value instanceof String&&((String)value).length()>2048)throw new IOException("Setting is too long");
            }
            result.put(key,value);
        }return result;
    }
    private static boolean write(SharedPreferences target,Map<String,?> values) {
        SharedPreferences.Editor edit=target.edit().clear();
        for(Map.Entry<String,?> entry:values.entrySet()) {
            Object v=entry.getValue();if(v instanceof String)edit.putString(entry.getKey(),(String)v);
            else if(v instanceof Boolean)edit.putBoolean(entry.getKey(),(Boolean)v);
            else if(v instanceof Integer)edit.putInt(entry.getKey(),(Integer)v);
            else throw new IllegalArgumentException("Unsupported preference type");
        }return edit.commit();
    }
    static void restore(Snapshot snapshot,SharedPreferences settings,SharedPreferences learning)throws IOException {
        Map<String,?> oldSettings=new HashMap<>(settings.getAll()),oldLearning=new HashMap<>(learning.getAll());
        if(!write(settings,snapshot.settings)||!write(learning,snapshot.learning)) {
            boolean restoredSettings=write(settings,oldSettings),restoredLearning=write(learning,oldLearning);
            throw new IOException(restoredSettings&&restoredLearning?"Import failed; previous preferences restored":"Import failed; could not fully restore previous preferences");
        }
    }
}
