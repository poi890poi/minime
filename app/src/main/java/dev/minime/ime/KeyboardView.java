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
    private final LinearLayout strip, keys;
    private final TextView status;
    private String layoutKey="", lastRaw="";
    private int page;
    private static final int INK=0xff263238, BLUE=0xff176b91, BACK=0xffdfe3e5;
    private static final String[] ZHUYIN={"ㄅㄉˇˋㄓˊ˙ㄚㄞㄢ","ㄆㄊㄍㄐㄔㄗㄧㄛㄟㄣ","ㄇㄋㄎㄑㄕㄘㄨㄜㄠㄤ","ㄈㄌㄏㄒㄖㄙㄩㄝㄡㄥ"};
    private static final String[] ZH_DOWN={"1234567890","qwertyuiop","asdfghjkl：","zxcvbnm…！？"};
    private static final String[] ZH_UP={"!@#$%^&*()","QWERTYUIOP","ASDFGHJKL ","ZXCVBNM   "};
    private static final String[] QWERTY={"qwertyuiop","asdfghjkl","zxcvbnm"};
    private static final String[] Q_DOWN={"1234567890","@*+-=/#（）","、「」？！～."};
    private static final String[] EN_DOWN={"1234567890","@*+-=/#()","':\"?!~…"};
    KeyboardView(Context context,Consumer<String> press,Predicate<String> longPress) {
        super(context); this.press=press; this.longPress=longPress;
        setOrientation(VERTICAL); setBackgroundColor(BACK); setPadding(dp(3),0,dp(3),dp(3));
        setMotionEventSplittingEnabled(false);
        status=new TextView(context); status.setTextColor(INK); status.setTextSize(12); status.setPadding(dp(8),0,0,0); addView(status);
        strip=new LinearLayout(context); strip.setGravity(Gravity.CENTER_VERTICAL); addView(strip,new LayoutParams(-1,dp(48)));
        keys=new LinearLayout(context); keys.setOrientation(VERTICAL); addView(keys);
        setOnApplyWindowInsetsListener((view,insets)-> {
            android.graphics.Insets bars=insets.getSystemWindowInsets();
            setPadding(dp(3)+bars.left,0,dp(3)+bars.right,Math.max(dp(3),bars.bottom)); return insets;
        });
    }
    private int dp(float value) { return Math.round(value*getResources().getDisplayMetrics().density); }
    private TextView button(String label,String command,String up,String down,boolean accent,int height,float weight) {
        TextView b;
        if(command.startsWith("CANDIDATE:")) {
            // Let the horizontal candidate scroller intercept drags normally.
            b=new TextView(getContext()); b.setText(label); b.setGravity(Gravity.CENTER);
            b.setMaxLines(1); b.setFocusable(true); b.setClickable(true);
            b.setOnClickListener(v->press.accept(command));
        } else b=new SlideKey(getContext(),label,command,up,down,press,longPress);
        b.setTextColor(accent?Color.WHITE:INK);
        b.setTextSize(Math.min(21,height*.43f));
        b.setContentDescription(command.startsWith("CANDIDATE:")?"Candidate "+label:label);
        GradientDrawable shape=new GradientDrawable(); shape.setColor(accent?BLUE:0xfff9fafb); shape.setCornerRadius(dp(4));
        b.setBackground(new RippleDrawable(ColorStateList.valueOf(0x33263238),shape,null));
        LayoutParams lp=new LayoutParams(0,dp(height),weight); lp.setMargins(dp(2),dp(2),dp(2),dp(2)); b.setLayoutParams(lp);
        return b;
    }
    private TextView plain(String label,String command,int height,float weight) { return button(label,command,"","",false,height,weight); }
    private LinearLayout row(int height) {
        LinearLayout row=new LinearLayout(getContext()); row.setMotionEventSplittingEnabled(false);
        keys.addView(row,new LayoutParams(-1,dp(height+4))); return row;
    }
    private void spacer(LinearLayout row,float weight) { row.addView(new View(getContext()),new LayoutParams(0,1,weight)); }
    private void simpleRow(String text,int height) {
        LinearLayout r=row(height); text.codePoints().forEach(c->{String s=new String(Character.toChars(c)); r.addView(plain(s,s,height,1));});
    }
    private TextView punctuation(boolean comma,boolean ascii,boolean allowWidthChoice,int height) {
        String label=comma?(ascii?",":"，"):(ascii?".":"。");
        TextView key=button(label,"INSERT:"+label,comma?(ascii?"，":","):(ascii?"。":"."),comma?"、":"…",false,height,1);
        key.setOnLongClickListener(v->{
            press.accept(comma?"EMOJI":"PUNCTUATION");return true;
        });return key;
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
        if(!lastRaw.equals(engine.raw())) { page=0; lastRaw=engine.raw(); }
        strip.removeAllViews();
        List<Candidate> candidates=engine.candidates();
        if(!candidates.isEmpty()) {
            int from=0;
            if(!engine.raw().isEmpty()) {
                TextView raw=plain(engine.raw(),"CANDIDATE:0",42,1);
                raw.setTextColor(engine.preferred()==0?BLUE:INK); raw.setTextSize(15);
                raw.setEllipsize(android.text.TextUtils.TruncateAt.END); raw.setContentDescription("Exact input "+engine.raw());
                strip.addView(raw,new LayoutParams(dp(90),dp(42))); from=1;
            }
            HorizontalScrollView scroll=new HorizontalScrollView(getContext()); scroll.setHorizontalScrollBarEnabled(false);
            LinearLayout words=new LinearLayout(getContext()); scroll.addView(words); strip.addView(scroll,new LayoutParams(0,-1,1));
            int pageCount=Math.max(1,(candidates.size()-from+23)/24);
            page%=pageCount;
            int start=from+page*24;
            for(int i=start;i<Math.min(candidates.size(),start+24);i++) {
                Candidate c=candidates.get(i); TextView word=button(c.text,"CANDIDATE:"+i,"","",!engine.raw().isEmpty() && engine.preferred()==i,42,1);
                word.setPadding(dp(14),0,dp(14),0); words.addView(word,new LayoutParams(-2,dp(42)));
            }
            if(pageCount>1) {
                TextView more=plain("⋯","MORE",42,1); more.setContentDescription("Next candidate page");
                more.setOnClickListener(v->{ page=(page+1)%pageCount; render(engine,zhuyin,shifted,caps,panel,numeric,asciiPunctuation,english,allowLanguageSwitch,enter,loading); });
                strip.addView(more,new LayoutParams(dp(40),dp(42)));
            }
        } else {
            TextView layout=plain(zhuyin?"拼音 layout":"注音 layout","LAYOUT",42,1); layout.setTextSize(14);
            strip.addView(layout);
            TextView next=plain("Next keyboard","NEXT_IME",42,1); next.setTextSize(13); strip.addView(next);
        }
        TextView menu=plain("⚙","SETTINGS",42,1); menu.setContentDescription("Settings");
        strip.addView(menu,new LayoutParams(dp(42),dp(42)));
        boolean landscape=getResources().getConfiguration().orientation==Configuration.ORIENTATION_LANDSCAPE;
        int height=landscape?34:48;
        String nextLayout=zhuyin+":"+shifted+":"+caps+":"+panel+":"+numeric+":"+asciiPunctuation+":"+english+":"+allowLanguageSwitch+":"+enter+":"+height;
        if(nextLayout.equals(layoutKey)) return;
        layoutKey=nextLayout; keys.removeAllViews();
        if(panel==3) {
            punctuationChoices(asciiPunctuation,allowLanguageSwitch && !english,height);
        } else if(panel>0) {
            keys.addView(new SymbolPanel(getContext(),panel==2,press));
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
                    line.addView(button(label,label,upper,(english?EN_DOWN:Q_DOWN)[r].substring(i,i+1),false,height,1));
                }
                if(r==1) spacer(line,.5f);
                if(r==2) line.addView(plain("⌫","DELETE",height,1.5f));
            }
        }
        LinearLayout bottom=row(height);
        TextView symbol=plain(panel>0?"ABC":"?123",panel>0?"LETTERS":"SYMBOLS",height,1.5f); symbol.setTextSize(15); bottom.addView(symbol);
        bottom.addView(punctuation(true,asciiPunctuation,allowLanguageSwitch && !english,height));
        TextView emoji=plain(panel==2?"#+":"☺",panel==2?"SYMBOLS":"EMOJI",height,1);
        emoji.setContentDescription(panel==2?"Symbols":"Emoji");bottom.addView(emoji);
        if(allowLanguageSwitch) {
            TextView language=plain(english?"中":"EN","LANGUAGE",height,1.2f);language.setTextSize(15);
            language.setContentDescription(english?"Switch to Chinese":"Switch to English");bottom.addView(language);
        }
        TextView space=plain("Space","SPACE",height,3.5f); space.setTextSize(16); bottom.addView(space);
        if(zhuyin && panel==0 && !numeric) bottom.addView(plain("ㄦ","ㄦ",height,1));
        else bottom.addView(punctuation(false,asciiPunctuation,allowLanguageSwitch && !english,height));
        if(zhuyin || panel>0 || numeric) bottom.addView(plain("⌫","DELETE",height,1.5f));
        TextView action=button(enter,"ENTER","","",true,height,1.5f); action.setTextSize(14); bottom.addView(action);
    }
}
