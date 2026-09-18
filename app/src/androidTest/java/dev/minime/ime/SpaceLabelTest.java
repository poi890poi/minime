package dev.minime.ime;

import android.content.*;
import android.content.res.Configuration;
import android.graphics.*;
import android.test.InstrumentationTestCase;
import android.view.*;
import android.widget.TextView;
import dev.minime.core.*;

/** A visible mode label is required even in the short landscape footer. */
@SuppressWarnings("deprecation")
public final class SpaceLabelTest extends InstrumentationTestCase {
    private TextView space(View v){
        if(v instanceof TextView && "Space".contentEquals(v.getContentDescription()==null?"":v.getContentDescription()) && ((TextView)v).getText().length()>1)return (TextView)v;
        if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++){TextView found=space(((ViewGroup)v).getChildAt(i));if(found!=null)return found;}
        return null;
    }
    public void testSpaceLabelInkIsNotClipped()throws Throwable{
        final Throwable[] failure={null};getInstrumentation().runOnMainSync(()->{try{
            Context base=getInstrumentation().getTargetContext();
            for(boolean landscape:new boolean[]{true,false})for(float scale:new float[]{1,1.3f})for(boolean english:new boolean[]{false,true}){
                Configuration config=new Configuration(base.getResources().getConfiguration());config.orientation=landscape?Configuration.ORIENTATION_LANDSCAPE:Configuration.ORIENTATION_PORTRAIT;config.fontScale=scale;
                Context context=base.createConfigurationContext(config);KeyboardView board=new KeyboardView(context,s->{},s->false,(p,c)->{});
                CompositionEngine engine=new CompositionEngine(new CompositionEngine.Editor(){public void composing(String s){}public void commit(String s){}public void delete(){}public void enter(){}public void finish(){}},Learning.NONE);
                engine.start(false,false,false,false,english);engine.switchMode(english?InputMode.ENGLISH:InputMode.CHINESE,false);
                board.render(engine,false,false,false,0,false,english,english,true,true,"Enter","");
                int width=Math.round((landscape?720:360)*context.getResources().getDisplayMetrics().density);
                board.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED));board.layout(0,0,width,board.getMeasuredHeight());
                TextView key=space(board);assertNotNull(key);String label=key.getText().toString();Rect expected=new Rect();key.getPaint().getTextBounds(label,0,label.length(),expected);
                Bitmap pixels=Bitmap.createBitmap(key.getWidth(),key.getHeight(),Bitmap.Config.ARGB_8888);key.draw(new Canvas(pixels));Rect actual=new Rect();
                for(int y=0;y<pixels.getHeight();y++)for(int x=0;x<pixels.getWidth();x++)if(pixels.getPixel(x,y)==key.getCurrentTextColor())actual.union(x,y,x+1,y+1);
                pixels.recycle();
                assertTrue("Space label "+label+" landscape="+landscape+" scale="+scale+" expected ink height="+expected.height()+" actual="+actual.height()+" padding="+key.getPaddingTop()+","+key.getPaddingBottom(),actual.height()>=expected.height()-2);
            }
        }catch(Throwable t){failure[0]=t;}});if(failure[0]!=null)throw failure[0];
    }
}
