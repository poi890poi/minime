package dev.minime.core;

import java.util.*;

/** Explicit language scope, independent of keyboard layout and inferred token intent. */
public enum InputMode {
    CHINESE("chinese","中",""), ENGLISH("english","EN",""),
    TAIWANESE("taiwanese","台","poj"), JAPANESE("japanese","日","japanese"),
    TAIWANESE_ENGLISH("taiwanese_english","台","poj"), JAPANESE_ENGLISH("japanese_english","日","japanese");
    public final String id,label,pack;
    InputMode(String id,String label,String pack) {this.id=id;this.label=label;this.pack=pack;}
    public boolean english() {return this==ENGLISH;}
    public boolean chineseEnabled() {return this==CHINESE || this==TAIWANESE || this==JAPANESE;}
    public boolean englishEnabled() {return this==CHINESE || english() || this==TAIWANESE_ENGLISH || this==JAPANESE_ENGLISH;}
    public boolean taiwanese() {return pack.equals("poj");}
    public boolean japanese() {return pack.equals("japanese");}
    public InputMode family() {return taiwanese()?TAIWANESE:japanese()?JAPANESE:this;}
    public InputMode secondaryEnglish(boolean enabled) {return taiwanese()?(enabled?TAIWANESE_ENGLISH:TAIWANESE):japanese()?(enabled?JAPANESE_ENGLISH:JAPANESE):this;}
    public String description() {return english()?"English":this==CHINESE?"中文 / English":(taiwanese()?"台語":"日本語")+(englishEnabled()?" / English":" / 中文");}
    public boolean available(Set<String> configured) {return pack.isEmpty() || configured.contains(pack);}
    public Set<String> packs(Set<String> configured) {
        Set<String> result=new HashSet<>();
        if(!english())for(String name:configured)
            if((chineseEnabled() && (name.equals("taiwan") || name.equals("geography"))) || (!pack.isEmpty() && name.equals(pack)))result.add(name);
        return Collections.unmodifiableSet(result);
    }
    public static InputMode fromId(String value) {
        for(InputMode mode:values())if(mode.id.equals(value))return mode;
        return CHINESE;
    }
}
