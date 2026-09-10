package dev.minime.ime;

import android.content.*;
import android.test.AndroidTestCase;
import java.io.*;
import java.nio.charset.StandardCharsets;

@SuppressWarnings("deprecation")
public final class PreferenceBackupTest extends AndroidTestCase {
    private SharedPreferences settings,learning;
    @Override public void setUp()throws Exception {super.setUp();settings=getContext().getSharedPreferences("backup_test_settings",0);learning=getContext().getSharedPreferences("backup_test_learning",0);settings.edit().clear().commit();learning.edit().clear().commit();}
    @Override public void tearDown()throws Exception {settings.edit().clear().commit();learning.edit().clear().commit();super.tearDown();}
    public void testRoundTripRetainsUnicodeAndScopedLearning()throws Exception {
        settings.edit().putBoolean("addon_poj",true).putString("mixed_mode","taiwanese_english").putInt("symbol_page",2).commit();
        learning.edit().putString("custom","abc\t甲乙").putString("recent_emoji","😀").putInt("FOCUS:poj\tabc\tá",3).commit();
        byte[] data=PreferenceBackup.encode(settings,learning);PreferenceBackup.Snapshot decoded=PreferenceBackup.decode(new ByteArrayInputStream(data));
        settings.edit().clear().commit();learning.edit().clear().commit();PreferenceBackup.restore(decoded,settings,learning);
        assertTrue(settings.getBoolean("addon_poj",false));assertEquals(2,settings.getInt("symbol_page",0));
        assertEquals("abc\t甲乙",learning.getString("custom",""));assertEquals(3,learning.getInt("FOCUS:poj\tabc\tá",0));
    }
    private void rejected(String text)throws Exception {
        settings.edit().putBoolean("addon_poj",true).commit();
        try{PreferenceBackup.decode(new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8)));fail("Invalid backup accepted");}catch(IOException expected){}
        assertTrue(settings.getBoolean("addon_poj",false));assertTrue(learning.getAll().isEmpty());
    }
    public void testWrongSettingTypeCannotCrashIme()throws Exception {rejected("{\"format\":\"MinIME preferences\",\"version\":1,\"settings\":{\"addon_poj\":\"true\"},\"learning\":{}}");}
    public void testInvalidLearningCountCannotReplacePreferences()throws Exception {rejected("{\"format\":\"MinIME preferences\",\"version\":1,\"settings\":{},\"learning\":{\"FOCUS:poj\\tabc\\tá\":101}}");}
    public void testFutureVersionAndTrailingDataRejected()throws Exception {
        rejected("{\"format\":\"MinIME preferences\",\"version\":2,\"settings\":{},\"learning\":{}}");
        rejected("{\"format\":\"MinIME preferences\",\"version\":1,\"settings\":{},\"learning\":{}} garbage");
    }
    public void testOversizedDocumentRejected()throws Exception {
        InputStream huge=new InputStream(){int left=PreferenceBackup.MAX_BYTES+1;public int read(){return left-->0?' ': -1;}};
        try{PreferenceBackup.decode(huge);fail("Oversized document accepted");}catch(IOException expected){}
    }
    public void testMalformedCustomEntryRejected()throws Exception {rejected("{\"format\":\"MinIME preferences\",\"version\":1,\"settings\":{},\"learning\":{\"custom\":\"bad row\"}}");}
    public void testDecodeAloneDoesNotChangePreferences()throws Exception {
        settings.edit().putBoolean("addon_poj",true).commit();
        PreferenceBackup.decode(new ByteArrayInputStream("{\"format\":\"MinIME preferences\",\"version\":1,\"settings\":{},\"learning\":{}}".getBytes(StandardCharsets.UTF_8)));
        assertTrue(settings.getBoolean("addon_poj",false));
    }
    public void testFailedWriteRestoresBothPreferenceFiles()throws Exception {
        for(boolean failSettings:new boolean[]{false,true}) {
            settings.edit().clear().putBoolean("addon_poj",true).commit();learning.edit().clear().putString("custom","old\t舊").commit();
            PreferenceBackup.Snapshot replacement=PreferenceBackup.decode(new ByteArrayInputStream("{\"format\":\"MinIME preferences\",\"version\":1,\"settings\":{},\"learning\":{}}".getBytes(StandardCharsets.UTF_8)));
            SharedPreferences delegate=failSettings?settings:learning;int[] writes={0};
            SharedPreferences failing=(SharedPreferences)java.lang.reflect.Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{SharedPreferences.class},(proxy,method,args)-> {
                Object value=method.invoke(delegate,args);
                if(!method.getName().equals("edit"))return value;
                SharedPreferences.Editor edit=(SharedPreferences.Editor)value;
                return java.lang.reflect.Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{SharedPreferences.Editor.class},(ep,em,ea)-> {
                    Object result=em.invoke(edit,ea);
                    if(em.getName().equals("commit")&&writes[0]++==0)return false;
                    return result instanceof SharedPreferences.Editor?ep:result;
                });
            });
            try{PreferenceBackup.restore(replacement,failSettings?failing:settings,failSettings?learning:failing);fail("Write failure accepted");}catch(IOException expected){}
            assertTrue(settings.getBoolean("addon_poj",false));assertEquals("old\t舊",learning.getString("custom",""));
        }
    }

}
