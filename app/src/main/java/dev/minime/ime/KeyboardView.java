package dev.minime.ime;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.*;
import android.view.*;
import android.widget.*;
import dev.minime.core.*;
import java.util.*;
import java.util.function.*;

/** Layout selection is explicit; token inference never moves the keys. */
final class KeyboardView extends LinearLayout {
    private final Consumer<String> press;
    private final Predicate<String> longPress;
    private final BiConsumer<float[],Integer> trace;
    private final TextView[] letters=new TextView[26];
    private final List<Float> points=new ArrayList<>();
    private boolean traceEnabled,tracePossible,tracing;
    private int traceCase;
    private float traceX,traceY,pitchX,pitchY;
    private final LinearLayout strip, keys;
    private final TextView status,phonetics;
    private HorizontalScrollView candidateScroll;
    private String layoutKey="", lastRaw="";
    private boolean expanded;
    private static final int INK=0xff37474f, BLUE=0xff4db6ac, BACK=0xffeceff1;
    private static final String[] ZHUYIN={"ㄅㄉˇˋㄓˊ˙ㄚㄞㄢ","ㄆㄊㄍㄐㄔㄗㄧㄛㄟㄣ","ㄇㄋㄎㄑㄕㄘㄨㄜㄠㄤ","ㄈㄌㄏㄒㄖㄙㄩㄝㄡㄥ"};
    private static final String[] ZH_DOWN={"1234567890","qwertyuiop","asdfghjkl：","zxcvbnm…！？"};
    private static final String[] ZH_UP={"!@#$%^&*()","QWERTYUIOP","ASDFGHJKL ","ZXCVBNM   "};
    private static final String[] QWERTY={"qwertyuiop","asdfghjkl","zxcvbnm"};
    private static final String[] Q_DOWN={"1234567890","@*+-=/#（）","、「」？！～."};
    private static final String[] EN_DOWN={"1234567890","@*+-=/#()","':\"?!~…"};
    KeyboardView(Context context,Consumer<String> press,Predicate<String> longPress,BiConsumer<float[],Integer> trace) {
        super(context); this.press=press; this.longPress=longPress;this.trace=trace;
        setOrientation(VERTICAL); setBackgroundColor(BACK); setPadding(0,0,0,0);
        setMotionEventSplittingEnabled(false);
        status=new TextView(context); status.setTextColor(INK); status.setTextSize(12); status.setPadding(dp(8),0,0,0); addView(status);
        phonetics=new TextView(context);phonetics.setTextColor(Color.BLACK);phonetics.setTextSize(14);phonetics.setGravity(Gravity.CENTER_VERTICAL);
        phonetics.setPadding(dp(8),0,dp(8),0);phonetics.setMaxLines(1);phonetics.setEllipsize(android.text.TextUtils.TruncateAt.END);
        phonetics.setClickable(true);phonetics.setFocusable(true);phonetics.setOnClickListener(v->{expanded=false;press.accept("CANDIDATE:0");});
        addView(phonetics,new LayoutParams(-1,dp(24)));
        strip=new LinearLayout(context); strip.setGravity(Gravity.CENTER_VERTICAL); strip.setBackgroundColor(0xffe4e7e9); addView(strip,new LayoutParams(-1,dp(48)));
        keys=new LinearLayout(context); keys.setOrientation(VERTICAL); addView(keys);
        setOnApplyWindowInsetsListener((view,insets)-> {
            android.graphics.Insets bars=insets.getSystemWindowInsets();
            setPadding(bars.left,0,bars.right,bars.bottom); return insets;
        });
    }
    private float[] center(View view) {int[] at=new int[2];view.getLocationOnScreen(at);return new float[]{at[0]+view.getWidth()/2f,at[1]+view.getHeight()/2f};}
    @Override public boolean dispatchTouchEvent(MotionEvent e) {
        int action=e.getActionMasked();
        if(action==MotionEvent.ACTION_DOWN) {
            tracePossible=false;tracing=false;points.clear();
            if(traceEnabled && letters['q'-'a']!=null) {
                for(TextView key:letters)if(key!=null) {float[] c=center(key);if(Math.abs(e.getRawX()-c[0])<key.getWidth()/2f && Math.abs(e.getRawY()-c[1])<key.getHeight()/2f)tracePossible=true;}
                float[] q=center(letters['q'-'a']),w=center(letters['w'-'a']),a=center(letters[0]);
                pitchX=w[0]-q[0];pitchY=a[1]-q[1];traceX=q[0]-.5f*pitchX;traceY=q[1];
                tracePossible&=pitchX>0 && pitchY>0;
            }
        }
        if(tracePossible && (action==MotionEvent.ACTION_DOWN || action==MotionEvent.ACTION_MOVE || action==MotionEvent.ACTION_UP)) {
            float x=(e.getRawX()-traceX)/pitchX,y=(e.getRawY()-traceY)/pitchY;
            if(points.size()<1024) {points.add(x);points.add(y);}
            float dx=x-points.get(0),dy=y-points.get(1);
            if(!tracing && Math.abs(dx)<.5 && Math.abs(dy)>.5)tracePossible=false;
            else if(!tracing && action==MotionEvent.ACTION_MOVE && Math.abs(dx)>.7) {
                MotionEvent cancel=MotionEvent.obtain(e);cancel.setAction(MotionEvent.ACTION_CANCEL);super.dispatchTouchEvent(cancel);cancel.recycle();tracing=true;
            }
            if(tracing) {
                invalidate();
                if(action==MotionEvent.ACTION_UP) {float[] path=new float[points.size()];for(int i=0;i<path.length;i++)path[i]=points.get(i);tracing=false;tracePossible=false;trace.accept(path,traceCase);}
                return true;
            }
        }
        if(action==MotionEvent.ACTION_CANCEL || action==MotionEvent.ACTION_POINTER_DOWN) {tracing=false;tracePossible=false;invalidate();}
        return super.dispatchTouchEvent(e);
    }
    @Override protected void dispatchDraw(android.graphics.Canvas canvas) {
        super.dispatchDraw(canvas);if(!tracing || points.size()<4)return;
        int[] at=new int[2];getLocationOnScreen(at);android.graphics.Paint paint=new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
        paint.setColor(0xaa176b91);paint.setStrokeWidth(dp(4));paint.setStrokeCap(android.graphics.Paint.Cap.ROUND);
        for(int i=2;i<points.size();i+=2)canvas.drawLine(traceX+points.get(i-2)*pitchX-at[0],traceY+points.get(i-1)*pitchY-at[1],traceX+points.get(i)*pitchX-at[0],traceY+points.get(i+1)*pitchY-at[1],paint);
    }
    private int dp(float value) { return Math.round(value*getResources().getDisplayMetrics().density); }
    private TextView button(String label,String command,String up,String down,boolean accent,int height,float weight) {
        TextView b;
        if(command.startsWith("CANDIDATE:")) {
            // Let the horizontal candidate scroller intercept drags normally.
            b=new TextView(getContext()); b.setText(label); b.setGravity(Gravity.CENTER);
            b.setMaxLines(1); b.setFocusable(true); b.setClickable(true);
            b.setOnClickListener(v->{expanded=false;press.accept(command);});
        } else b=new SlideKey(getContext(),label,command,up,down,press,longPress);
        b.setTextColor(accent?Color.BLACK:INK);
        b.setTextSize(Math.min(21,height*.43f));
        b.setContentDescription(command.startsWith("CANDIDATE:")?"Candidate "+label:label);
        GradientDrawable shape=new GradientDrawable(); shape.setColor(Color.TRANSPARENT);
        b.setBackground(new RippleDrawable(ColorStateList.valueOf(0x33263238),shape,null));
        LayoutParams lp=new LayoutParams(0,dp(height),weight); b.setLayoutParams(lp);
        if(b instanceof SlideKey && KeyboardIcon.supports(command)) ((SlideKey)b).icon(new KeyboardIcon(command,label,INK,BLUE));
        return b;
    }
    private TextView plain(String label,String command,int height,float weight) { return button(label,command,"","",false,height,weight); }
    private LinearLayout row(int height) {
        LinearLayout row=new LinearLayout(getContext()); row.setMotionEventSplittingEnabled(false);
        keys.addView(row,new LayoutParams(-1,dp(height))); return row;
    }
    private void spacer(LinearLayout row,float weight) { row.addView(new View(getContext()),new LayoutParams(0,1,weight)); }
    private void simpleRow(String text,int height) {
        LinearLayout r=row(height); text.codePoints().forEach(c->{String s=new String(Character.toChars(c)); r.addView(plain(s,s,height,1));});
    }
    private TextView punctuation(boolean comma,boolean ascii,boolean allowWidthChoice,int height) {
        String label=comma?(ascii?",":"，"):(ascii?".":"。");
        TextView key=button(label,"INSERT:"+label,comma?(ascii?"，":","):(ascii?"。":"."),comma?"、":"…",false,height,1);
        if(comma) { ((SlideKey)key).emojiHint(); key.setOnLongClickListener(v->{press.accept("EMOJI");return true;}); }
        else ((SlideKey)key).punctuationPalette(new String[]{".","。",",","，","、","…","?","？","!","！",":","：",";","；","_","%","$","^","&",":P",":D",":(",":)","^_^"},allowWidthChoice,ascii);
        return key;
    }
    private void punctuationChoices(boolean ascii,boolean allowWidthChoice,int height) {
        String[] choices={".","。",",","，","、","…","?","？","!","！",":","：",";","；","_","%","$","^","&",":P",":D",":(",":)","^_^"};
        for(int r=0;r<4;r++) {
            LinearLayout line=row(height);
            for(int c=0;c<6;c++) {String choice=choices[r*6+c];line.addView(plain(choice,"INSERT:"+choice,height,1));}
        }
        if(allowWidthChoice) {
            TextView width=plain(ascii?"Use Chinese punctuation":"Use English punctuation","PUNCT_WIDTH",height,1);
            width.setTextSize(14);row(height).addView(width);
        }
    }
    void render(CompositionEngine engine,boolean zhuyin,boolean shifted,boolean caps,int panel,boolean numeric,boolean asciiPunctuation,boolean english,boolean allowLanguageSwitch,String enter,String loading) {
        String hint=engine.privateField()?"Private input · learning off":loading;
        status.setText(hint); status.setVisibility(hint.isEmpty()?GONE:VISIBLE);
        int previousScroll=candidateScroll==null?0:candidateScroll.getScrollX();
        if(!lastRaw.equals(engine.raw())) {previousScroll=0;lastRaw=engine.raw();}
        final int restoreScroll=previousScroll;
        phonetics.setText(engine.raw());phonetics.setContentDescription("Exact input "+engine.raw());
        phonetics.setVisibility(!english && !engine.raw().isEmpty() && panel==0?VISIBLE:GONE);
        strip.removeAllViews();candidateScroll=null;
        List<Candidate> candidates=engine.candidates();
        if(candidates.isEmpty() || panel!=0)expanded=false;
        traceEnabled=english && allowLanguageSwitch && !numeric && !zhuyin && panel==0 && !expanded;
        traceCase=caps?2:shifted?1:0;
        if(!candidates.isEmpty()) {
            int from=!engine.raw().isEmpty() && !english?1:0;
            candidateScroll=new HorizontalScrollView(getContext());candidateScroll.setHorizontalScrollBarEnabled(false);candidateScroll.setContentDescription("Candidate list");
            LinearLayout words=new LinearLayout(getContext());candidateScroll.addView(words);strip.addView(candidateScroll,new LayoutParams(0,-1,1));
            for(int i=from;i<candidates.size();i++) {
                Candidate c=candidates.get(i);TextView word=button(c.text,"CANDIDATE:"+i,"","",false,48,1);
                word.setTextSize(english?18:20);word.setTextColor(!engine.raw().isEmpty() && engine.preferred()==i?Color.BLACK:0xff5d6b71);
                word.setTypeface(null,!engine.raw().isEmpty() && engine.preferred()==i?android.graphics.Typeface.BOLD:android.graphics.Typeface.NORMAL);
                word.setMinWidth(dp(48));word.setPadding(dp(12),0,dp(12),0);
                if(i==0 && !engine.raw().isEmpty())word.setContentDescription("Exact input "+engine.raw());
                words.addView(word,new LayoutParams(-2,dp(48)));
                View divider=new View(getContext());divider.setBackgroundColor(0xffc3cbcf);LayoutParams rule=new LayoutParams(dp(1),dp(26));rule.gravity=Gravity.CENTER_VERTICAL;words.addView(divider,rule);
            }
            HorizontalScrollView currentScroll=candidateScroll;currentScroll.post(()->currentScroll.scrollTo(restoreScroll,0));
            TextView expand=plain(expanded?"⌃":"⌄","EXPAND",42,1);
            expand.setContentDescription(expanded?"Collapse candidates":"Expand candidates");
            expand.setOnClickListener(v->{expanded=!expanded;render(engine,zhuyin,shifted,caps,panel,numeric,asciiPunctuation,english,allowLanguageSwitch,enter,loading);});
            strip.addView(expand,new LayoutParams(dp(40),dp(42)));
        } else {
            TextView chinese=plain("中",english?"LANGUAGE":"LAYOUT",48,1); chinese.setTextSize(23); ((SlideKey)chinese).icon(null);
            chinese.setContentDescription(english?"Switch to Chinese":zhuyin?"拼音 layout":"注音 layout");
            if(!english)chinese.setBackgroundColor(BACK);
            strip.addView(chinese,new LayoutParams(dp(75),dp(48)));
            TextView latin=plain("En","LANGUAGE",48,1); latin.setTextSize(23); ((SlideKey)latin).icon(null);
            latin.setContentDescription(english?"English selected":"Switch to English");
            latin.setOnClickListener(v->{if(!english)press.accept("LANGUAGE");});
            if(english)latin.setBackgroundColor(BACK);
            strip.addView(latin,new LayoutParams(dp(75),dp(48)));
            strip.addView(new View(getContext()),new LayoutParams(0,1,1));
            TextView next=plain("Next keyboard","NEXT_IME",42,1);
            strip.addView(next,new LayoutParams(dp(42),dp(42)));
        }
        TextView menu=plain("⚙","SETTINGS",42,1); menu.setContentDescription("Settings");
        if(candidates.isEmpty() || panel!=0)strip.addView(menu,new LayoutParams(dp(42),dp(42)));
        boolean landscape=getResources().getConfiguration().orientation==Configuration.ORIENTATION_LANDSCAPE;
        int height=landscape?34:59;
        String nextLayout=zhuyin+":"+shifted+":"+caps+":"+panel+":"+numeric+":"+asciiPunctuation+":"+english+":"+allowLanguageSwitch+":"+enter+":"+height+":"+expanded+(expanded?candidates.toString():"");
        if(nextLayout.equals(layoutKey)) return;
        layoutKey=nextLayout; keys.removeAllViews();
        if(expanded) {
            ScrollView scroll=new ScrollView(getContext());scroll.setContentDescription("Expanded candidate list");
            CandidateFlowLayout grid=new CandidateFlowLayout(getContext());scroll.addView(grid);
            int first=engine.raw().isEmpty()?0:1;
            for(int i=first;i<candidates.size();i++) {
                TextView word=button(candidates.get(i).text,"CANDIDATE:"+i,"","",false,48,1);
                word.setTextColor(engine.preferred()==i && !engine.raw().isEmpty()?Color.BLACK:0xff5d6b71);
                word.setTextSize(20);word.setMinWidth(dp(48));word.setMinHeight(dp(48));word.setPadding(dp(12),dp(4),dp(12),dp(4));
                word.setSingleLine(false);word.setMaxLines(Integer.MAX_VALUE);
                grid.addView(word,new ViewGroup.LayoutParams(-2,-2));
            }
            keys.addView(scroll,new LayoutParams(-1,dp(height*(zhuyin?4:3))));
        } else if(panel==3) {
            punctuationChoices(asciiPunctuation,allowLanguageSwitch && !english,height);
        } else if(panel>0) {
            keys.addView(new SymbolPanel(getContext(),panel==2,engine.privateField(),press));
        } else if(numeric) {
            simpleRow("123",height); simpleRow("456",height); simpleRow("789",height);
            simpleRow("+0.-",height);
        } else if(zhuyin) {
            for(int r=0;r<4;r++) {
                LinearLayout line=row(height);
                for(int i=0;i<10;i++) {
                    String label=ZHUYIN[r].substring(i,i+1), up=ZH_UP[r].substring(i,i+1).trim(), down=ZH_DOWN[r].substring(i,i+1);
                    line.addView(button(label,label,up,down,false,height,1));
                }
            }
        } else {
            for(int r=0;r<3;r++) {
                LinearLayout line=row(height);
                if(r==1) spacer(line,.5f);
                if(r==2) line.addView(plain(caps?"⇪":shifted?"⬆":"⇧","SHIFT",height,1.5f));
                for(int i=0;i<QWERTY[r].length();i++) {
                    String lower=QWERTY[r].substring(i,i+1), upper=lower.toUpperCase(Locale.ROOT);
                    String label=shifted?upper:lower;
                    TextView letter=button(label,label,upper,(english?EN_DOWN:Q_DOWN)[r].substring(i,i+1),false,height,1);
                    ((SlideKey)letter).qwertyStyle();
                    letters[lower.charAt(0)-'a']=letter;line.addView(letter);
                }
                if(r==1) spacer(line,.5f);
                if(r==2) line.addView(plain("⌫","DELETE",height,1.5f));
            }
        }
        LinearLayout bottom=row(height);
        TextView symbol=plain(panel>0?"ABC":"?123",panel>0?"LETTERS":"SYMBOLS",height,1.6f); symbol.setTextSize(16); bottom.addView(symbol);
        bottom.addView(punctuation(true,asciiPunctuation,allowLanguageSwitch && !english,height));
        if(allowLanguageSwitch) {
            TextView language=plain(english?"中":"EN","LANGUAGE",height,.9f);language.setTextSize(15);
            language.setContentDescription(english?"Switch to Chinese":"Switch to English");bottom.addView(language);
        }
        TextView space=plain(english?"English":zhuyin?"注音":"拼音","SPACE",height,4); space.setTextSize(14); space.setTextColor(0xff6d7b80); space.setContentDescription("Space");
        GradientDrawable spaceShape=new GradientDrawable();spaceShape.setColor(0xffcbd0d3);spaceShape.setCornerRadius(dp(2));
        space.setBackground(new RippleDrawable(ColorStateList.valueOf(0x33263238),new InsetDrawable(spaceShape,dp(12),dp(14),dp(12),dp(14)),null));bottom.addView(space);
        if(zhuyin && panel==0 && !numeric) bottom.addView(plain("ㄦ","ㄦ",height,1));
        else bottom.addView(punctuation(false,asciiPunctuation,allowLanguageSwitch && !english,height));
        if(zhuyin || panel>0 || numeric || expanded) bottom.addView(plain("⌫","DELETE",height,1.5f));
        TextView action=button(enter,"ENTER","","",true,height,1.5f); action.setTextSize(14); bottom.addView(action);
    }
}
