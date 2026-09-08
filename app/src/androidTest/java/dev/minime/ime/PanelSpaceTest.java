package dev.minime.ime;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.test.InstrumentationTestCase;
import android.view.*;
import dev.minime.core.*;

@SuppressWarnings("deprecation")
public final class PanelSpaceTest extends InstrumentationTestCase {
    private View find(View v,String description) {
        if(description.contentEquals(v.getContentDescription()==null?"":v.getContentDescription()))return v;
        if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++){View r=find(((ViewGroup)v).getChildAt(i),description);if(r!=null)return r;}
        return null;
    }
    private Rect bounds(KeyboardView root,View view) {
        assertNotNull(view);Rect r=new Rect(0,0,view.getWidth(),view.getHeight());root.offsetDescendantRectToMyCoords(view,r);return r;
    }
    private void measure(View v) {
        int width=Math.round(400*v.getResources().getDisplayMetrics().density);
        v.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED));
        v.layout(0,0,width,v.getMeasuredHeight());
    }
    private CompositionEngine engine() {
        return new CompositionEngine(new CompositionEngine.Editor() {
            public void composing(String s) {} public void commit(String s) {} public void delete() {}
            public void enter() {} public void finish() {}
        },Learning.NONE);
    }
    public void testIdleToolbarUsesTopOfKeyboard() throws Throwable {
        runTestOnUiThread(()-> {
            KeyboardView view=new KeyboardView(getInstrumentation().getTargetContext(),key->{},key->false,(path,caps)->{});
            CompositionEngine e=engine();e.start(false,false,false,false);
            view.render(e,false,false,false,0,false,false,false,true,true,"Enter","");measure(view);
            assertEquals("No reserved blank band above idle toolbar",0,bounds(view,find(view,"Switch to English")).top);
        });
    }
    public void testPaletteReplacesToolbarAndUsesThreeRows() throws Throwable {
        runTestOnUiThread(()-> {
            for(int orientation:new int[]{Configuration.ORIENTATION_PORTRAIT,Configuration.ORIENTATION_LANDSCAPE}) {
                Configuration config=new Configuration(getInstrumentation().getTargetContext().getResources().getConfiguration());config.orientation=orientation;
                Context context=getInstrumentation().getTargetContext().createConfigurationContext(config);
                KeyboardView view=new KeyboardView(context,key->{},key->false,(path,caps)->{});CompositionEngine e=engine();e.start(false,false,false,false);
                view.render(e,false,false,false,0,false,false,false,true,true,"Enter","");measure(view);int height=view.getHeight();Rect space=bounds(view,find(view,"Space"));
                for(int panel:new int[]{1,2}) {
                    view.render(e,false,false,false,panel,false,false,false,true,true,"Enter","");measure(view);
                    assertEquals("Palette height remains fixed",height,view.getHeight());assertEquals(space.top,bounds(view,find(view,"Space")).top);
                    View category=find(view,panel==1?"Symbol category":"Emoji category");
                    assertEquals("Category controls replace the toolbar",0,bounds(view,category).top);
                    ViewGroup palette=(ViewGroup)category.getParent().getParent();
                    assertEquals("Reclaimed space holds another grid row",3,((ViewGroup)palette.getChildAt(1)).getChildCount());
                }
            }
        });
    }
}
