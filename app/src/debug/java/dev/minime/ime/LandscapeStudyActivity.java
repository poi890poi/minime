package dev.minime.ime;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import dev.minime.core.JoinedKalq;
import java.util.*;

/** Native geometry experiment, excluded from release builds. No suggestion logic. */
public final class LandscapeStudyActivity extends Activity {
    public static volatile LandscapeStudyActivity current;
    public FrameLayout root;
    public EditText editor;
    public final Map<String,SlideKey> letters=new LinkedHashMap<>();
    public final List<String> emitted=new ArrayList<>();
    public float rowHeight,blockWidth,clearHeight;
    private float density;
    private boolean shifted;
    private FrameLayout board;
    private int dp(float v){return Math.round(v*density);}
    @Override public void onCreate(Bundle state){
        super.onCreate(state);current=this;density=getResources().getDisplayMetrics().density;
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        root=new FrameLayout(this);root.setBackgroundColor(Color.WHITE);setContentView(root);
    }
    @Override public void onDestroy(){if(current==this)current=null;super.onDestroy();}
    private void put(FrameLayout parent,View v,float x,float y,float w,float h){
        FrameLayout.LayoutParams p=new FrameLayout.LayoutParams(dp(x+w)-dp(x),dp(y+h)-dp(y));
        p.leftMargin=dp(x);p.topMargin=dp(y);parent.addView(v,p);
    }
    private TextView label(String text,int size,int color){
        TextView v=new TextView(this);v.setText(text);v.setTextSize(size);v.setTextColor(color);v.setGravity(Gravity.CENTER_VERTICAL);return v;
    }
    public void show(String name){
        shifted=false;letters.clear();emitted.clear();root.removeAllViews();
        float w=root.getWidth()/density,h=root.getHeight()/density;
        rowHeight=name.equals("compact")||name.equals("split-low")?25.5f:name.equals("split-tall")?44:(h-88)/4;
        float footer=name.equals("compact")||name.equals("split-low")?34:40;
        clearHeight=h-48-4*rowHeight-footer;
        blockWidth=name.equals("compact")?w/2:name.equals("side-panels")?192:240;
        put(root,label("Notes · "+name+" native prototype",20,0xff263238),16,0,w-32,40);
        editor=new EditText(this);editor.setTextSize(18);editor.setText("Tomorrow, let's meet at the station.\n明天一起去爬山，記得帶水。");
        editor.setShowSoftInputOnFocus(false);editor.setGravity(Gravity.TOP);editor.setSelection(editor.length());
        put(root,editor,16,40,w-32,Math.max(20,h-48));
        board=new FrameLayout(this);board.setMotionEventSplittingEnabled(true);root.addView(board,new FrameLayout.LayoutParams(-1,-1));
        if(!name.equals("side-panels")){
            View bg=new View(this);bg.setBackgroundColor(0xffeceff3);put(board,bg,0,clearHeight,w,h-clearHeight);
        }
        TextView strip=label("中   EN   台   日       Candidate examples · 明天 · tomorrow",18,0xff37474f);
        strip.setBackgroundColor(0xffe2e6ea);put(board,strip,0,clearHeight,w,48);
        for(int r=0;r<4;r++)for(int side=0;side<2;side++){
            String text=r<3?JoinedKalq.row(r).substring(side*4,side*4+4):(side==0?"^^dn":"fv<<");
            float left=side==0?0:w-blockWidth;
            for(int col=0;col<4;col++){
                char ch=text.charAt(col);float x=left+col*blockWidth/4,y=clearHeight+48+r*rowHeight;
                if(ch=='^'||ch=='<'){
                    if(ch=='^'&&col==0||ch=='<'&&col==2)key(ch=='^'?"Shift":"Delete","",x,y,blockWidth/2,rowHeight);
                }else if(ch==' ')key("Space","",x,y,blockWidth/4,rowHeight);
                else key(String.valueOf(ch),JoinedKalq.symbol(r,r==3?(side==0?col-2:col+2):side*4+col,true),x,y,blockWidth/4,rowHeight);
            }
        }
        float y=h-footer;
        if(name.equals("compact"))footer(new String[]{"Symbols",",","Mode","Space",".","Enter"},new float[]{1.6f,1,.9f,4,1,1.5f},0,y,w,footer,10);
        else{
            footer(new String[]{"Symbols",",","Mode","Space"},new float[]{1,.75f,.75f,1.5f},0,y,blockWidth,footer,4);
            footer(new String[]{"Space",".","Enter"},new float[]{2,.75f,1.25f},w-blockWidth,y,blockWidth,footer,4);
        }
    }
    private void footer(String[] names,float[] weights,float x,float y,float width,float h,float sum){
        for(int i=0;i<names.length;i++){float w=width*weights[i]/sum;key(names[i],"",x,y,w,h);x+=w;}
    }
    private void key(String command,String symbol,float x,float y,float w,float h){
        boolean letter=command.length()==1&&Character.isLetter(command.charAt(0));
        String label=command.equals("Space")?"␣":command.equals("Symbols")?"?123":command.equals("Shift")?"⇧":command.equals("Delete")?"⌫":command.equals("Enter")?"↵":command.equals("Mode")?"中":command;
        SlideKey key=new SlideKey(this,label,command,letter?command.toUpperCase(Locale.ROOT):"",symbol,this::accept,k->false);
        key.setTextColor(0xff37474f);key.setBackgroundColor(0xffeceff3);key.setTextSize(18);
        if(letter){key.compactLetterStyle(34);letters.put(command,key);}put(board,key,x,y,w,h);
    }
    private void accept(String command){
        emitted.add(command);
        if(command.equals("Shift")){shifted=!shifted;return;}
        if(command.equals("Delete")){if(editor.length()>0)editor.getText().delete(editor.length()-1,editor.length());return;}
        if(command.equals("Mode")||command.equals("Symbols"))return;
        String text=command.startsWith("LITERAL:")?command.substring(8):command.equals("Space")?" ":command.equals("Enter")?"\n":shifted?command.toUpperCase(Locale.ROOT):command;
        editor.append(text);
    }
    public void realIme(){
        root.removeAllViews();letters.clear();
        editor=new EditText(this);editor.setTextSize(18);editor.setMinLines(2);editor.setHint("Actual MinIME landscape integration");
        root.addView(editor,new FrameLayout.LayoutParams(-1,-1));editor.requestFocus();
        ((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).showSoftInput(editor,InputMethodManager.SHOW_IMPLICIT);
    }
}
