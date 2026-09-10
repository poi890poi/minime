package dev.minime.ime;

import android.test.ActivityInstrumentationTestCase2;
import android.view.*;
import android.widget.*;

@SuppressWarnings("deprecation")
public final class PublishingSettingsTest extends ActivityInstrumentationTestCase2<SettingsActivity> {
    public PublishingSettingsTest(){super(SettingsActivity.class);}
    private View find(View view,String label) {
        if(view instanceof TextView&&label.contentEquals(((TextView)view).getText()))return view;
        if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++){View child=find(((ViewGroup)view).getChildAt(i),label);if(child!=null)return child;}
        return null;
    }
    public void testPrivacyAndMigrationControlsAreAvailable()throws Throwable {
        SettingsActivity activity=getActivity();
        runTestOnUiThread(()-> {
            View root=activity.getWindow().getDecorView();
            assertNotNull(find(root,"Export settings and learned words"));assertNotNull(find(root,"Import settings and learned words"));
            View policy=find(root,"Privacy policy");assertNotNull(policy);policy.performClick();
        });
        getInstrumentation().waitForIdleSync();
        try(java.io.InputStream in=activity.getAssets().open("privacy.txt")) {assertTrue(in.available()>1000);}
    }
}
