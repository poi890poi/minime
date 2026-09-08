package dev.minime.ime;

import android.content.*;
import android.test.ActivityInstrumentationTestCase2;
import android.view.*;
import dev.minime.core.*;
import java.util.*;

/** Packaged assets, preference gates, persisted learning, and real candidate taps. */
@SuppressWarnings("deprecation")
public final class AddonIntegrationTest extends ActivityInstrumentationTestCase2<EditorTestActivity> {
    public AddonIntegrationTest() {super(EditorTestActivity.class);}
    private Context context;
    private AddonDictionary addon;
    private PhoneticDictionary dictionary;
    @Override protected void setUp() throws Exception {
        super.setUp();context=getInstrumentation().getTargetContext();
        dictionary=DictionaryRepository.load(context).get(30,java.util.concurrent.TimeUnit.SECONDS);
        addon=AddonRepository.load(context).get(30,java.util.concurrent.TimeUnit.SECONDS);
    }
    public void testPackagedPojCandidateCanBeTapped() throws Throwable {
        String[] probe=AddonTestData.probe(context,"poj");
        EditorTestActivity activity=getActivity();
        runTestOnUiThread(()-> {
            StringBuilder output=new StringBuilder();
            CompositionEngine engine=new CompositionEngine(new CompositionEngine.Editor() {
                public void composing(String text) {}public void commit(String text) {output.append(text);}
                public void delete() {}public void enter() {}public void finish() {}
            },Learning.NONE);
            engine.dictionary(dictionary);engine.start(false,false,false,false);
            engine.addons(addon,Collections.singleton("poj"));probe[0].codePoints().forEach(engine::type);
            KeyboardView view=new KeyboardView(context,key->{},key->false,(path,caps)->{},Runnable::run);
            view.render(engine,false,false,false,0,false,false,false,true,"Enter","");activity.setContentView(view);
            View candidate=find(view,"Candidate "+probe[1]);assertNotNull("POJ must be visible and selectable",candidate);
            candidate.performClick();assertEquals(probe[1],output.toString());assertEquals("",engine.raw());
        });
    }
    public void testSettingsAndLearningPersistence() throws Throwable {
        runTestOnUiThread(()-> {
            SharedPreferences settings=context.getSharedPreferences("settings",Context.MODE_PRIVATE);
            SharedPreferences data=context.getSharedPreferences("learning",Context.MODE_PRIVATE);
            settings.edit().putBoolean("addon_taiwan",false).putBoolean("addon_poj",false).putBoolean("addon_japanese",false).putBoolean("addon_geography",false).putBoolean("phrase_learning",false).commit();
            assertTrue(AddonRepository.enabled(context).isEmpty());
            settings.edit().putBoolean("addon_poj",true).commit();assertEquals(Collections.singleton("poj"),AddonRepository.enabled(context));
            data.edit().remove("phrases_v1").commit();LocalLearning learning=new LocalLearning(context);
            learning.observePhrase("shanyu","山雨");assertFalse(data.contains("phrases_v1"));
            settings.edit().putBoolean("phrase_learning",true).commit();
            learning.observePhrase("shanyu","山雨");learning.observePhrase("shanyu","山雨");assertTrue(learning.phrases("shanyu").isEmpty());
            learning.observePhrase("shan'yu","山雨");assertEquals("山雨",new LocalLearning(context).phrases("shanyu").get(0).text);
            settings.edit().putBoolean("phrase_learning",false).commit();assertTrue(learning.phrases("shanyu").isEmpty());
            data.edit().remove("phrases_v1").commit();settings.edit().putBoolean("phrase_learning",true).commit();assertTrue(learning.phrases("shanyu").isEmpty());
        });
    }
    public void testSavedDictionaryCachesFollowEditsAndDeletion() {
        SharedPreferences settings=context.getSharedPreferences("settings",Context.MODE_PRIVATE),data=context.getSharedPreferences("learning",Context.MODE_PRIVATE);
        settings.edit().putBoolean("phrase_learning",true).commit();
        data.edit().putString("custom","abc\t甲乙\n").putString("phrases_v1","abc\t甲乙\t3\n").commit();
        LocalLearning cached=new LocalLearning(context);
        assertEquals("甲乙",cached.custom("abc").get(0).text);assertEquals("甲乙",cached.phrases("abc").get(0).text);
        cached.custom("abc").clear();assertFalse(cached.custom("abc").isEmpty());
        data.edit().putString("custom","abc\t乙丙\n").putString("phrases_v1","abc\t乙丙\t3\n").commit();
        assertEquals("乙丙",cached.custom("abc").get(0).text);assertEquals("乙丙",cached.phrases("abc").get(0).text);
        data.edit().remove("custom").remove("phrases_v1").commit();
        assertTrue(cached.custom("abc").isEmpty());assertTrue(cached.phrases("abc").isEmpty());
        for(int i=0;i<3;i++)cached.observePhrase("abc","丙丁");assertEquals("丙丁",cached.phrases("abc").get(0).text);
    }
    public void testAsyncDictionaryLookupAndSpaceUseOneResult() throws Throwable {
        java.util.concurrent.CountDownLatch done=new java.util.concurrent.CountDownLatch(1);
        StringBuilder output=new StringBuilder();AsyncDecoder[] worker=new AsyncDecoder[1];
        runTestOnUiThread(()-> {
            try {
                AddonDictionary fixture=AddonDictionary.read(new java.io.StringReader("japanese\tka'na'mi\tかなみ\tfixture\ttest\n"));
                CompositionEngine engine=new CompositionEngine(new CompositionEngine.Editor() {
                    public void composing(String text) {}public void commit(String text) {output.append(text);done.countDown();}
                    public void delete() {}public void enter() {}public void finish() {}
                },Learning.NONE);
                engine.dictionary(dictionary);engine.start(false,false,false,false);engine.addons(fixture,Collections.singleton("japanese"));
                worker[0]=new AsyncDecoder(new android.os.Handler(android.os.Looper.getMainLooper()));
                engine.decoder(worker[0],()->{});"kanami".codePoints().forEach(engine::type);engine.space();
                assertEquals("Space must await the worker",0,output.length());
            } catch(java.io.IOException e) {throw new AssertionError(e);}
        });
        try {assertTrue(done.await(10,java.util.concurrent.TimeUnit.SECONDS));assertEquals("かなみ",output.toString());}
        finally {runTestOnUiThread(()->worker[0].close());}
    }
    private static View find(View view,String description) {
        if(description.contentEquals(view.getContentDescription()==null?"":view.getContentDescription()))return view;
        if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++) {View result=find(((ViewGroup)view).getChildAt(i),description);if(result!=null)return result;}
        return null;
    }
}
