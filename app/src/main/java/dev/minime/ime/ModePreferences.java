package dev.minime.ime;

import android.content.*;
import dev.minime.core.InputMode;
import java.util.*;

/** Reuses legacy enable/English preferences; upgrading never destroys their values. */
final class ModePreferences {
    private final Context context;
    private final SharedPreferences settings;
    ModePreferences(Context context) {this.context=context;settings=context.getSharedPreferences("settings",Context.MODE_PRIVATE);}
    Set<String> configured() {return AddonRepository.enabled(context);}
    InputMode mixed() {
        InputMode mode=resolve(InputMode.fromId(settings.getString("mixed_mode","chinese")));
        return !mode.english() && mode.available(configured())?mode:InputMode.CHINESE;
    }
    InputMode resolve(InputMode mode) {
        return mode.secondaryEnglish(true);
    }
    InputMode lastFocused() {
        InputMode saved=resolve(InputMode.fromId(settings.getString("last_focused_mode",settings.getString("mixed_mode","english"))));
        return !saved.pack.isEmpty() && saved.available(configured())?saved:InputMode.ENGLISH;
    }
    InputMode quickTarget(InputMode current) {return current==InputMode.CHINESE?lastFocused():InputMode.CHINESE;}
    InputMode selected() {return settings.getBoolean("english_mode",false)?InputMode.ENGLISH:mixed();}
    void select(InputMode mode) {
        mode=resolve(mode);
        if(!mode.available(configured()))return;
        SharedPreferences.Editor edit=settings.edit().putBoolean("english_mode",mode.english()).putBoolean("english_punctuation",false);
        if(!mode.english())edit.putString("mixed_mode",mode.id);
        if(!mode.pack.isEmpty())edit.putString("last_focused_mode",mode.id);
        edit.apply();
    }
}
