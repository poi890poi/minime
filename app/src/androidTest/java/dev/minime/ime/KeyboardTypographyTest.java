package dev.minime.ime;

import android.content.*;
import android.content.res.Configuration;
import android.graphics.*;
import android.test.InstrumentationTestCase;
import android.view.*;
import java.io.*;
import java.util.*;
import dev.minime.core.*;

/** Compares production key pixels with independently approved pre-implementation renders. */
@SuppressWarnings("deprecation")
public final class KeyboardTypographyTest extends InstrumentationTestCase {
    private KeyboardView board;
    private final List<SlideKey> letters=new ArrayList<>();
    private boolean existed,old;
    @Override protected void setUp()throws Exception{super.setUp();SharedPreferences p=getInstrumentation().getTargetContext().getSharedPreferences("settings",Context.MODE_PRIVATE);existed=p.contains("joined_kalq");old=p.getBoolean("joined_kalq",false);}
    @Override protected void tearDown()throws Exception{try{SharedPreferences.Editor e=getInstrumentation().getTargetContext().getSharedPreferences("settings",Context.MODE_PRIVATE).edit();if(existed)e.putBoolean("joined_kalq",old);else e.remove("joined_kalq");e.commit();}finally{super.tearDown();}}
    private void collect(View v){
        if(v instanceof SlideKey && v.getContentDescription()!=null && v.getContentDescription().toString().matches("[a-zA-Z]"))letters.add((SlideKey)v);
        if(v instanceof android.view.ViewGroup)for(int i=0;i<((android.view.ViewGroup)v).getChildCount();i++)collect(((android.view.ViewGroup)v).getChildAt(i));
    }
    private void render(Context context,boolean joined,boolean ascii,boolean caps){
        context.getSharedPreferences("settings",Context.MODE_PRIVATE).edit().putBoolean("joined_kalq",joined).commit();
        board=new KeyboardView(context,s->{},s->false,(p,c)->{});
        CompositionEngine engine=new CompositionEngine(new CompositionEngine.Editor(){
            public void composing(String s){}public void commit(String s){}public void delete(){}public void enter(){}public void finish(){}
        },Learning.NONE);
        engine.start(false,false,false,false,ascii);engine.switchMode(ascii?InputMode.ENGLISH:InputMode.CHINESE,false);
        board.render(engine,false,caps,caps,0,false,ascii,ascii,true,true,"Enter","");
        int width=Math.round(360*context.getResources().getDisplayMetrics().density);
        board.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED));
        board.layout(0,0,width,board.getMeasuredHeight());letters.clear();collect(board);
        assertEquals(26,letters.size());
    }
    public void testApprovedPortraitPixels() throws Throwable {
        final Throwable[] failure={null};
        getInstrumentation().runOnMainSync(()->{try {
            Context base=getInstrumentation().getTargetContext();
            Configuration config=new Configuration(base.getResources().getConfiguration());config.orientation=Configuration.ORIENTATION_PORTRAIT;config.fontScale=1;
            Context context=base.createConfigurationContext(config);
            assertEquals("Golden fixtures are from the authorized 3x-density phone",3f,context.getResources().getDisplayMetrics().density,0f);
            for(boolean joined:new boolean[]{false,true})for(boolean ascii:new boolean[]{true,false})for(boolean caps:new boolean[]{false,true}) {
                render(context,joined,ascii,caps);
                String name="symbol-space-"+(joined?"kalq":"qwerty")+(ascii?"-en":"-zh")+(caps?"-caps":"-lower")+"-overlap-26-18-plex-lifted.png";
                Bitmap expected;try(InputStream in=getInstrumentation().getContext().getAssets().open("typography/"+name)){expected=BitmapFactory.decodeStream(in);}
                Bitmap actual=Bitmap.createBitmap(board.getWidth(),board.getHeight(),Bitmap.Config.ARGB_8888);board.draw(new Canvas(actual));
                try(FileOutputStream out=new FileOutputStream(new File(base.getExternalFilesDir(null),"production-"+name))){actual.compress(Bitmap.CompressFormat.PNG,100,out);}
                assertEquals(expected.getWidth(),actual.getWidth());assertEquals(expected.getHeight(),actual.getHeight());
                int differing=0;
                for(SlideKey key:letters){Rect r=new Rect(0,0,key.getWidth(),key.getHeight());board.offsetDescendantRectToMyCoords(key,r);
                    for(int y=r.top;y<r.bottom;y++)for(int x=r.left;x<r.right;x++)if(expected.getPixel(x,y)!=actual.getPixel(x,y))differing++;
                }
                actual.recycle();expected.recycle();assertEquals(name+" changed key pixels",0,differing);
            }
        }catch(Throwable t){failure[0]=t;}});
        if(failure[0]!=null)throw failure[0];
    }
    public void testEnlargedPortraitTextFits()throws Throwable {
        final Throwable[] failure={null};getInstrumentation().runOnMainSync(()->{try{
            Context base=getInstrumentation().getTargetContext();
            for(float scale:new float[]{1.3f,1.5f})for(boolean joined:new boolean[]{false,true})for(boolean caps:new boolean[]{false,true}){
                Configuration c=new Configuration(base.getResources().getConfiguration());c.orientation=Configuration.ORIENTATION_PORTRAIT;c.fontScale=scale;
                render(base.createConfigurationContext(c),joined,true,caps);
                for(SlideKey key:letters){
                    Paint.FontMetrics fm=key.getPaint().getFontMetrics();
                    assertTrue("Enlarged letter fits height",fm.descent-fm.ascent<=key.getHeight()-key.getPaddingBottom()+1);
                    assertTrue("Enlarged letter fits width",key.getPaint().measureText(key.getText().toString())<=key.getWidth()-key.getPaddingLeft()-key.getPaddingRight());
                }
            }
        }catch(Throwable t){failure[0]=t;}});if(failure[0]!=null)throw failure[0];
    }
}
