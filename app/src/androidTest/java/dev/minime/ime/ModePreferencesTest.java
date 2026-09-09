package dev.minime.ime;

import android.test.AndroidTestCase;
import android.content.*;
import dev.minime.core.InputMode;

@SuppressWarnings("deprecation")
public final class ModePreferencesTest extends AndroidTestCase {
    public void testLegacyMigrationAndReversibleDisable() {
        SharedPreferences p=getContext().getSharedPreferences("settings",Context.MODE_PRIVATE);
        // The guarded phone runner backs up/restores these preference files.
        p.edit().remove("mixed_mode").putBoolean("english_mode",false).putBoolean("addon_poj",true).putBoolean("addon_japanese",true).commit();
        ModePreferences modes=new ModePreferences(getContext());assertEquals(InputMode.CHINESE,modes.selected());
        assertTrue(p.getBoolean("addon_poj",false));assertTrue(p.getBoolean("addon_japanese",false));
        modes.select(InputMode.JAPANESE);modes.select(InputMode.ENGLISH);
        modes=new ModePreferences(getContext());assertEquals(InputMode.ENGLISH,modes.selected());assertEquals(InputMode.JAPANESE,modes.mixed());
        assertTrue("Legacy English preference remains readable on rollback",p.getBoolean("english_mode",false));
        p.edit().putBoolean("addon_japanese",false).commit();assertEquals(InputMode.CHINESE,modes.mixed());
        assertEquals("japanese",p.getString("mixed_mode",""));
        p.edit().putBoolean("addon_japanese",true).commit();assertEquals(InputMode.JAPANESE,modes.mixed());
        modes.select(InputMode.TAIWANESE);assertFalse(p.getBoolean("english_mode",true));assertEquals(InputMode.TAIWANESE,modes.selected());
    }
}
