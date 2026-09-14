package dev.minime.ime;

import android.content.*;
import android.test.ActivityInstrumentationTestCase2;
import android.view.*;
import android.widget.Switch;
import java.util.*;

/** Checks the actual settings screen against runtime gates and persisted choices. */
@SuppressWarnings("deprecation")
public final class SettingsDefaultsTest extends ActivityInstrumentationTestCase2<SettingsActivity> {
    public SettingsDefaultsTest() {super(SettingsActivity.class);}
    private final List<Switch> switches=new ArrayList<>();
    private void collect(View view) {
        if(view instanceof Switch)switches.add((Switch)view);
        if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++)collect(((ViewGroup)view).getChildAt(i));
    }
    public void testFreshScreenAndExplicitOverrides() throws Throwable {
        Context context=getInstrumentation().getTargetContext();
        SharedPreferences settings=context.getSharedPreferences("settings",Context.MODE_PRIVATE);
        // tools/test-device.ps1 preserves settings/learning around this session.
        settings.edit().clear().commit();
        SettingsActivity activity=getActivity();
        String[] keys={"zhuyin","rime_pinyin","english_correction","double_space_period","emoji_recents","english_learning",
            "focused_choice_learning","phrase_learning","addon_taiwan","addon_geography","addon_japanese","addon_poj","paired_taiwanese","taiwanese_han_primary"};
        boolean[] expected={false,false,false,true,false,false,true,true,true,true,true,true,true,false};
        runTestOnUiThread(()-> {
            collect(activity.getWindow().getDecorView());
            assertEquals("Every screenshot switch is represented",keys.length,switches.size());
            for(int i=0;i<keys.length;i++)assertEquals(keys[i],expected[i],switches.get(i).isChecked());
        });
        assertFalse("Opening Settings does not materialize defaults",settings.contains("rime_pinyin"));
        assertFalse("Runtime uses original decoder by default",RimeBackend.enabled(context));
        assertEquals(new HashSet<>(Arrays.asList("taiwan","geography","japanese","poj")),AddonRepository.enabled(context));
        LocalLearning learning=new LocalLearning(context);
        context.getSharedPreferences("learning",Context.MODE_PRIVATE).edit().remove("phrases_v1").commit();
        for(int i=0;i<3;i++)learning.observePhrase("shanyu","山雨");
        assertEquals("Default learning survives reload","山雨",new LocalLearning(context).phrases("shanyu").get(0).text);
        runTestOnUiThread(()-> {for(Switch toggle:switches)toggle.performClick();});
        getInstrumentation().waitForIdleSync();
        for(int i=0;i<keys.length;i++)assertEquals("User override persists: "+keys[i],!expected[i],settings.getBoolean(keys[i],expected[i]));
        assertTrue("Explicit Rime opt-in takes effect",RimeBackend.enabled(context));
        assertTrue("Explicit pack opt-outs take effect",AddonRepository.enabled(context).isEmpty());
        assertTrue("Explicit phrase-learning opt-out hides saved phrases",new LocalLearning(context).phrases("shanyu").isEmpty());
        settings.edit().remove("rime_pinyin").remove("phrase_learning").remove("addon_poj").commit();
        assertFalse("Removing override restores decoder default",RimeBackend.enabled(context));
        assertEquals(Collections.singleton("poj"),AddonRepository.enabled(context));
        assertEquals("Default resumes saved learning without erasing it","山雨",new LocalLearning(context).phrases("shanyu").get(0).text);
    }
}
