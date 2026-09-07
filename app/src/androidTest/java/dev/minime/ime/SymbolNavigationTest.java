package dev.minime.ime;

import android.content.*;
import android.test.InstrumentationTestCase;
import android.view.*;
import android.widget.*;
import java.util.*;

@SuppressWarnings("deprecation")
public final class SymbolNavigationTest extends InstrumentationTestCase {
    private SharedPreferences settings;
    @Override protected void setUp() throws Exception {
        super.setUp();settings=getInstrumentation().getTargetContext().getSharedPreferences("settings",Context.MODE_PRIVATE);
        settings.edit().remove("symbol_category").remove("symbol_page").commit();
    }
    @Override protected void tearDown() throws Exception {
        settings.edit().remove("symbol_category").remove("symbol_page").commit();super.tearDown();
    }
    private SymbolPanel panel(boolean privateField) {return new SymbolPanel(getInstrumentation().getTargetContext(),false,privateField,key->{});}
    static View find(View root,String description) {
        if(description.contentEquals(root.getContentDescription()==null?"":root.getContentDescription()))return root;
        if(root instanceof ViewGroup)for(int i=0;i<((ViewGroup)root).getChildCount();i++){View v=find(((ViewGroup)root).getChildAt(i),description);if(v!=null)return v;}
        return null;
    }
    private static void click(View root,String description) {View v=find(root,description);assertNotNull(description,v);assertTrue(v.performClick());}
    static List<String> entries(View root) {
        List<String> result=new ArrayList<>();String description=String.valueOf(root.getContentDescription());
        if(description.startsWith("Symbol ") && !description.equals("Symbol category"))result.add(description.substring(7));
        if(root instanceof ViewGroup)for(int i=0;i<((ViewGroup)root).getChildCount();i++)result.addAll(entries(((ViewGroup)root).getChildAt(i)));
        return result;
    }
    public void testReopenRemembersCategoryAndPageWithoutInsertion() throws Throwable {
        runTestOnUiThread(()-> {
            SymbolPanel first=panel(false);click(first,"Symbol category");click(first,"箭頭 Arrows");click(first,"Next palette page");
            List<String> expected=entries(first);assertFalse(expected.isEmpty());assertEquals(expected,entries(panel(false)));
            click(first,"Symbol category");click(first,"Symbol category");assertEquals("Dismissing chooser preserves page",expected,entries(first));
            click(first,"Symbol category");assertEquals("Open chooser cannot overwrite saved content page",expected,entries(panel(false)));
        });
    }
    public void testInvalidNavigationClampsAndPrivateFieldsStayIsolated() throws Throwable {
        runTestOnUiThread(()-> {
            settings.edit().putString("symbol_category","removed category").putInt("symbol_page",Integer.MAX_VALUE).commit();
            assertEquals("Unknown category returns to Common",entries(panel(true)),entries(panel(false)));
            settings.edit().putString("symbol_category","箭頭 Arrows").putInt("symbol_page",Integer.MAX_VALUE).commit();
            SymbolPanel last=panel(false);assertFalse(find(last,"Next palette page").isEnabled());assertFalse(entries(last).isEmpty());
            Map<String,?> saved=new HashMap<>(settings.getAll());SymbolPanel privatePanel=panel(true);
            assertEquals("Private panel starts at Common", "常用 Common ▾",((TextView)find(privatePanel,"Symbol category")).getText().toString());
            click(privatePanel,"Next palette page");click(privatePanel,"Symbol category");click(privatePanel,"數學 Math");
            assertEquals("Private navigation never changes saved state",saved,settings.getAll());assertEquals(entries(last),entries(panel(false)));
            settings.edit().putInt("symbol_page",-10).commit();assertFalse(find(panel(false),"Previous palette page").isEnabled());
        });
    }
}
