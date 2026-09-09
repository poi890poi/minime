package dev.minime.ime;

import android.test.InstrumentationTestCase;
import android.content.Context;
import android.graphics.Typeface;
import android.view.*;
import android.widget.*;
import dev.minime.core.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.json.*;

/** Android font/layout measurement for frozen core results; no rank-based visibility proxy. */
@SuppressWarnings({"deprecation","unchecked","rawtypes"})
public final class ModeRenderingMetricsTest extends InstrumentationTestCase {
    static View find(View root,String description) {
        if(description.contentEquals(root.getContentDescription()==null?"":root.getContentDescription()))return root;
        if(root instanceof ViewGroup)for(int i=0;i<((ViewGroup)root).getChildCount();i++){View v=find(((ViewGroup)root).getChildAt(i),description);if(v!=null)return v;}
        return null;
    }
    public void testFrozenCandidateGeometry()throws Exception {
        Context context=getInstrumentation().getTargetContext();android.util.DisplayMetrics dm=context.getResources().getDisplayMetrics();
        int width=dm.widthPixels;JSONObject geometry=new JSONObject().put("width",width).put("density",dm.density).put("scaledDensity",dm.scaledDensity).put("fontScale",context.getResources().getConfiguration().fontScale);
        PhoneticDictionary dictionary=PhoneticDictionary.load(new StringReader("abc\tㄅ\t甲乙\t100\tㄅ\n"),new StringReader("abcdef\t100\nabcxyz\t90\n"),new StringReader("abc\tㄅ\n"));
        for(String mode:Arrays.asList("chinese","english","taiwanese","japanese")) {
            JSONObject frame=new JSONObject();geometry.put(mode,frame);
            getInstrumentation().runOnMainSync(()-> {
                try {
                    CompositionEngine engine=new CompositionEngine(new CompositionEngine.Editor(){public void composing(String s){}public void commit(String s){}public void delete(){}public void enter(){}public void finish(){}},Learning.NONE);
                    engine.dictionary(dictionary);engine.start(false,false,false,false,mode.equals("english"));
                    KeyboardView view=new KeyboardView(context,key->{},key->false,(p,c)->{},Runnable::run);
                    try {
                        Class type=Class.forName("dev.minime.core.InputMode");Object selected=Enum.valueOf(type,mode.toUpperCase(Locale.ROOT));
                        CompositionEngine.class.getMethod("switchMode",type,boolean.class).invoke(engine,selected,false);
                        java.lang.reflect.Method options=KeyboardView.class.getDeclaredMethod("modeOptions",type,Set.class);options.setAccessible(true);
                        options.invoke(view,Enum.valueOf(type,"CHINESE"),new HashSet<>(Arrays.asList("taiwan","geography","poj","japanese")));
                    }catch(ClassNotFoundException baseline) {}
                    "abc".codePoints().forEach(engine::type);
                    view.render(engine,false,false,false,0,false,mode.equals("english"),mode.equals("english"),true,true,"Enter","");
                    layout(view,width);View row=find(view,"Candidate list");assertNotNull(row);
                    frame.put("rowWidth",row.getMeasuredWidth()).put("keyboardHeight",view.getMeasuredHeight());
                    find(view,"Expand candidates").performClick();layout(view,width);View page=find(view,"Expanded candidate list");assertNotNull(page);
                    frame.put("pageWidth",page.getMeasuredWidth()).put("pageHeight",page.getMeasuredHeight());
                }catch(Exception e){throw new RuntimeException(e);}
            });
        }
        List<String> texts=new ArrayList<>();
        try(BufferedReader in=new BufferedReader(new InputStreamReader(getInstrumentation().getContext().getAssets().open("mode-coverage-texts.txt"),StandardCharsets.UTF_8))){String line;while((line=in.readLine())!=null)texts.add(line);}
        File output=new File(context.getExternalFilesDir(null),"mode-font-metrics.tsv");
        try(PrintWriter out=new PrintWriter(output,"UTF-8")) {
            out.println("text\tzh_normal\tzh_bold\ten_normal\ten_bold\tpage_width\tpage_height");
            for(int from=0;from<texts.size();from+=256) {
                List<String> chunk=texts.subList(from,Math.min(texts.size(),from+256));List<String> measured=new ArrayList<>();
                getInstrumentation().runOnMainSync(()-> {
                    TextView cell=new TextView(context);cell.setLayoutParams(new ViewGroup.LayoutParams(-2,-2));cell.setGravity(android.view.Gravity.CENTER);cell.setMinWidth(Math.round(48*dm.density));
                    for(String text:chunk) {
                        cell.setText(text);StringBuilder line=new StringBuilder(text);cell.setSingleLine(true);cell.setMinHeight(0);cell.setPadding(Math.round(12*dm.density),0,Math.round(12*dm.density),0);
                        for(int size:new int[]{20,18})for(boolean bold:new boolean[]{false,true}) {
                            cell.setTextSize(size);cell.setTypeface(null,bold?Typeface.BOLD:Typeface.NORMAL);
                            cell.measure(View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED),View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED));line.append('\t').append(cell.getMeasuredWidth());
                        }
                        cell.setSingleLine(false);cell.setMaxLines(Integer.MAX_VALUE);cell.setTextSize(20);cell.setTypeface(null,Typeface.NORMAL);cell.setMinHeight(Math.round(48*dm.density));cell.setPadding(Math.round(12*dm.density),Math.round(4*dm.density),Math.round(12*dm.density),Math.round(4*dm.density));
                        cell.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.AT_MOST),View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED));
                        measured.add(line.append('\t').append(cell.getMeasuredWidth()).append('\t').append(cell.getMeasuredHeight()).toString());
                    }
                });
                for(String line:measured)out.println(line);
            }
        }
        geometry.put("texts",texts.size());
        try(PrintWriter out=new PrintWriter(new File(context.getExternalFilesDir(null),"mode-geometry.json"),"UTF-8")){out.println(geometry.toString(2));}
    }
    private static void layout(View view,int width) {
        view.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED));view.layout(0,0,width,view.getMeasuredHeight());
    }
}
