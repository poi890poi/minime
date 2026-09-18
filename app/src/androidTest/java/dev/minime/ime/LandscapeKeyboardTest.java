package dev.minime.ime;

import android.content.*;
import android.content.res.Configuration;
import android.graphics.*;
import android.test.InstrumentationTestCase;
import android.view.*;
import dev.minime.core.*;
import java.util.*;

/** Real Android ink bounds, symbol actions and height stability in short rows. */
@SuppressWarnings("deprecation")
public final class LandscapeKeyboardTest extends InstrumentationTestCase {
    private void collect(View v,List<SlideKey> out) {
        if(v instanceof SlideKey && v.getContentDescription()!=null && v.getContentDescription().toString().matches("[a-zA-Z]"))out.add((SlideKey)v);
        if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++)collect(((ViewGroup)v).getChildAt(i),out);
    }
    private Rect ink(Bitmap bitmap,int color) {
        Rect out=new Rect();
        for(int y=0;y<bitmap.getHeight();y++)for(int x=0;x<bitmap.getWidth();x++)if(bitmap.getPixel(x,y)==color)out.union(x,y,x+1,y+1);
        return out;
    }
    public void testLandscapeInkAndSymbolActions() throws Throwable {
        Context base=getInstrumentation().getTargetContext();
        SharedPreferences settings=base.getSharedPreferences("settings",Context.MODE_PRIVATE);
        boolean existed=settings.contains("joined_kalq"),old=settings.getBoolean("joined_kalq",false);
        final Throwable[] failure={null};
        try {
            getInstrumentation().runOnMainSync(()->{try {
                for(int width:new int[]{480,600,760,960})for(float scale:new float[]{1f,1.3f,2f})for(boolean ascii:new boolean[]{false,true})for(boolean caps:new boolean[]{false,true}) {
                    int expectedHeight=-1;
                    for(boolean joined:new boolean[]{false,true}) {
                        Configuration config=new Configuration(base.getResources().getConfiguration());config.orientation=Configuration.ORIENTATION_LANDSCAPE;config.fontScale=scale;
                        Context context=base.createConfigurationContext(config);float density=context.getResources().getDisplayMetrics().density;
                        settings.edit().putBoolean("joined_kalq",joined).commit();
                        List<String> emitted=new ArrayList<>();KeyboardView board=new KeyboardView(context,emitted::add,k->false,(p,c)->{});
                        CompositionEngine engine=new CompositionEngine(new CompositionEngine.Editor(){
                            public void composing(String s){}public void commit(String s){}public void delete(){}public void enter(){}public void finish(){}
                        },Learning.NONE);engine.start(false,false,false,false,ascii);
                        board.render(engine,false,caps,caps,0,false,ascii,ascii,true,true,"Enter","");
                        board.measure(View.MeasureSpec.makeMeasureSpec(Math.round(width*density),View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED));
                        board.layout(0,0,board.getMeasuredWidth(),board.getMeasuredHeight());
                        if(expectedHeight<0)expectedHeight=board.getHeight();else assertEquals("Layouts keep the same landscape height",expectedHeight,board.getHeight());
                        List<SlideKey> keys=new ArrayList<>();collect(board,keys);assertEquals(26,keys.size());
                        Set<String> symbols=new HashSet<>();
                        for(SlideKey key:keys) {
                            Bitmap pixels=Bitmap.createBitmap(key.getWidth(),key.getHeight(),Bitmap.Config.ARGB_8888);key.draw(new Canvas(pixels));
                            Rect main=ink(pixels,key.getCurrentTextColor()),hint=ink(pixels,0xff9aa6aa);
                            assertFalse("Letter ink exists",main.isEmpty());assertFalse("Symbol ink exists",hint.isEmpty());
                            for(Rect r:new Rect[]{main,hint})assertTrue("Ink fits the key",r.left>0 && r.top>0 && r.right<pixels.getWidth() && r.bottom<pixels.getHeight());
                            assertTrue("Symbols use horizontal space without covering letters",main.right<hint.left);
                            pixels.recycle();emitted.clear();key.performClick();assertEquals(Collections.singletonList(key.getText().toString()),emitted);
                            android.view.accessibility.AccessibilityNodeInfo node=key.createAccessibilityNodeInfo();
                            try {for(android.view.accessibility.AccessibilityNodeInfo.AccessibilityAction action:node.getActionList())if(action.getLabel()!=null && action.getLabel().toString().startsWith("Slide down: ")) {
                                String symbol=action.getLabel().toString().substring("Slide down: ".length());symbols.add(symbol);emitted.clear();
                                assertTrue(key.performAccessibilityAction(action.getId(),null));assertEquals(Collections.singletonList("LITERAL:"+symbol),emitted);
                            }}finally{node.recycle();}
                        }
                        assertTrue("Digits and punctuation remain accessible",symbols.size()>=20);
                    }
                }
            }catch(Throwable t){failure[0]=t;}});
            if(failure[0]!=null)throw failure[0];
        } finally {
            SharedPreferences.Editor edit=settings.edit();if(existed)edit.putBoolean("joined_kalq",old);else edit.remove("joined_kalq");edit.commit();
        }
    }
}
