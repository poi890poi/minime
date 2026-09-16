package dev.minime.ime;

import android.test.AndroidTestCase;
import android.content.*;
import dev.minime.core.InputMode;

@SuppressWarnings("deprecation")
public final class ModePreferencesTest extends AndroidTestCase {
    public void testQuickSwitchRetainsFocusedLanguage() {
        SharedPreferences p=getContext().getSharedPreferences("settings",Context.MODE_PRIVATE);
        p.edit().remove("mixed_mode").remove("last_focused_mode").putBoolean("addon_japanese",true).putBoolean("addon_poj",true).commit();
        ModePreferences modes=new ModePreferences(getContext());assertEquals(InputMode.ENGLISH,modes.quickTarget(InputMode.CHINESE));
        modes.select(InputMode.JAPANESE);modes.select(InputMode.CHINESE);
        modes=new ModePreferences(getContext());assertEquals(InputMode.JAPANESE_ENGLISH,modes.quickTarget(InputMode.CHINESE));
        assertEquals(InputMode.CHINESE,modes.quickTarget(InputMode.JAPANESE_ENGLISH));
        p.edit().putBoolean("addon_japanese",false).commit();assertEquals(InputMode.ENGLISH,modes.quickTarget(InputMode.CHINESE));
        p.edit().putBoolean("addon_japanese",true).commit();assertEquals(InputMode.JAPANESE_ENGLISH,modes.quickTarget(InputMode.CHINESE));
        modes.select(InputMode.TAIWANESE);modes.select(InputMode.CHINESE);assertEquals(InputMode.TAIWANESE_ENGLISH,modes.quickTarget(InputMode.CHINESE));
    }
    public void testEnglishChineseRoundTripReplacesPreviousFocus() {
        SharedPreferences p=getContext().getSharedPreferences("settings",Context.MODE_PRIVATE);
        p.edit().putBoolean("addon_japanese",true).putBoolean("addon_poj",true).commit();
        for(InputMode previous:new InputMode[]{InputMode.JAPANESE,InputMode.TAIWANESE}) {
            ModePreferences modes=new ModePreferences(getContext());
            modes.select(previous);modes.select(InputMode.ENGLISH);
            for(int repeat=0;repeat<3;repeat++) {
                modes=new ModePreferences(getContext());
                assertEquals(InputMode.ENGLISH,modes.selected());
                assertEquals(InputMode.CHINESE,modes.quickTarget(modes.selected()));
                modes.select(modes.quickTarget(modes.selected()));
                modes=new ModePreferences(getContext());
                assertEquals(InputMode.CHINESE,modes.selected());
                assertEquals(InputMode.ENGLISH,modes.quickTarget(modes.selected()));
                modes.select(modes.quickTarget(modes.selected()));
            }
            // Disabling/re-enabling a formerly focused pack must not resurrect it.
            p.edit().putBoolean("addon_japanese",false).putBoolean("addon_poj",false).commit();
            assertEquals(InputMode.ENGLISH,modes.quickTarget(InputMode.CHINESE));
            p.edit().putBoolean("addon_japanese",true).putBoolean("addon_poj",true).commit();
            assertEquals(InputMode.ENGLISH,modes.quickTarget(InputMode.CHINESE));
            modes.select(previous);modes.select(InputMode.CHINESE);
            assertEquals(previous.secondaryEnglish(true),modes.quickTarget(InputMode.CHINESE));
        }
    }
    public void testExistingEnglishSessionRepairsStaleQuickPartner() {
        SharedPreferences p=getContext().getSharedPreferences("settings",Context.MODE_PRIVATE);
        for(String previous:new String[]{"japanese_english","taiwanese_english"}) {
            p.edit().putBoolean("english_mode",true).putBoolean("addon_japanese",true).putBoolean("addon_poj",true)
                .putString("mixed_mode",previous).putString("last_focused_mode",previous).commit();
            ModePreferences modes=new ModePreferences(getContext());
            modes.select(modes.quickTarget(modes.selected()));
            modes=new ModePreferences(getContext());
            assertEquals(InputMode.CHINESE,modes.selected());
            assertEquals(InputMode.ENGLISH,modes.quickTarget(modes.selected()));
        }
    }
    public void testLegacyMigrationAndReversibleDisable() {
        SharedPreferences p=getContext().getSharedPreferences("settings",Context.MODE_PRIVATE);
        // The guarded phone runner backs up/restores these preference files.
        p.edit().remove("mixed_mode").remove("last_focused_mode").putBoolean("english_mode",false).putBoolean("addon_poj",true).putBoolean("addon_japanese",true).commit();
        ModePreferences modes=new ModePreferences(getContext());assertEquals(InputMode.CHINESE,modes.selected());
        assertTrue(p.getBoolean("addon_poj",false));assertTrue(p.getBoolean("addon_japanese",false));
        modes.select(InputMode.JAPANESE);modes.select(InputMode.ENGLISH);
        modes=new ModePreferences(getContext());assertEquals(InputMode.ENGLISH,modes.selected());assertEquals(InputMode.JAPANESE_ENGLISH,modes.mixed());
        assertTrue("Legacy English preference remains readable on rollback",p.getBoolean("english_mode",false));
        p.edit().putBoolean("addon_japanese",false).commit();assertEquals(InputMode.CHINESE,modes.mixed());
        assertEquals("japanese_english",p.getString("mixed_mode",""));
        p.edit().putBoolean("addon_japanese",true).commit();assertEquals(InputMode.JAPANESE_ENGLISH,modes.mixed());
        modes.select(InputMode.TAIWANESE);assertFalse(p.getBoolean("english_mode",true));assertEquals(InputMode.TAIWANESE_ENGLISH,modes.selected());
    }
}
