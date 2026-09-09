package dev.minime.core;

import java.util.*;

/** Explicit language scope, independent of keyboard layout and inferred token intent. */
public enum InputMode {
    CHINESE("chinese","中",""), ENGLISH("english","EN",""),
    TAIWANESE("taiwanese","台","poj"), JAPANESE("japanese","日","japanese");
    public final String id,label,pack;
    InputMode(String id,String label,String pack) {this.id=id;this.label=label;this.pack=pack;}
    public boolean english() {return this==ENGLISH;}
    public boolean available(Set<String> configured) {return pack.isEmpty() || configured.contains(pack);}
    public Set<String> packs(Set<String> configured) {
        Set<String> result=new HashSet<>();
        if(!english())for(String name:configured)
            if(name.equals("taiwan") || name.equals("geography") || (!pack.isEmpty() && name.equals(pack)))result.add(name);
        return Collections.unmodifiableSet(result);
    }
    public static InputMode fromId(String value) {
        for(InputMode mode:values())if(mode.id.equals(value))return mode;
        return CHINESE;
    }
}
