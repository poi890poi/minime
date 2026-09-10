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
    private final Consumer<Runnable> choose;
    private final TextView[] letters=new TextView[26];
    private final List<Float> points=new ArrayList<>();
    private boolean traceEnabled,tracePossible,tracing;
    private int traceCase;
    private float traceX,traceY,pitchX,pitchY;
    private final LinearLayout strip, keys;
    private final TextView status,phonetics;
    private final FrameLayout annotation;
    private final PopupWindow annotationWindow;
    private boolean inputActive,annotationRequested;
    private final Runnable placeAnnotation=this::placeAnnotation;
    private int annotationX=-1,annotationY=-1,annotationWidth=-1;
    private HorizontalScrollView candidateScroll;
    private LinearLayout candidateWords;
    private CandidateFlowLayout candidateGrid;
    private TextView expandButton;
    private String gridKey="";
    private String layoutKey="", lastRaw="";
    private String stripKey="", snapshotMode="";
    private CompositionEngine snapshotEngine;
    private long snapshotComposition=-1;
    private List<Candidate> snapshot=Collections.emptyList();
    private int snapshotPreferred;
    private boolean snapshotHasRaw;
    private boolean candidateGesture;
    private Runnable afterCandidateGesture;
    private boolean expanded;
    private InputMode returnMode=InputMode.CHINESE;
    private Set<String> configuredModes=Collections.emptySet();
    private boolean modeMenu;
    void modeOptions(InputMode mixed,Set<String> configured) {
        returnMode=mixed;configuredModes=new HashSet<>(configured);
    }
    private static String modeName(InputMode mode) {return mode.english()?"English":mode.taiwanese()?"Taiwanese":mode.japanese()?"Japanese":"Chinese";}
    private TextView modeBadge(InputMode current,Runnable show) {
        TextView badge=plain(current.label+"⌄","MODE_PICKER",48,1);badge.setTextSize(15);
        badge.setContentDescription("Choose language mode: "+current.family().id);
        badge.setOnClickListener(anchor->{modeMenu=true;show.run();});
        return badge;
    }
    private static final int INK=0xff37474f, BLUE=0xff4db6ac, BACK=0xffeceff1;
    private static final String[] ZHUYIN={"ㄅㄉˇˋㄓˊ˙ㄚㄞㄢ","ㄆㄊㄍㄐㄔㄗㄧㄛㄟㄣ","ㄇㄋㄎㄑㄕㄘㄨㄜㄠㄤ","ㄈㄌㄏㄒㄖㄙㄩㄝㄡㄥ"};
    private static final String[] ZH_DOWN={"1234567890","qwertyuiop","asdfghjkl：","zxcvbnm…！？"};
    private static final String[] ZH_UP={"!@#$%^&*()","QWERTYUIOP","ASDFGHJKL ","ZXCVBNM   "};
    private static final String[] QWERTY={"qwertyuiop","asdfghjkl","zxcvbnm"};
    private static final String[] Q_DOWN={"1234567890","@*+-=/#（）","、「」？！～."};
    private static final String[] EN_DOWN={"1234567890","@*+-=/#()","':\"?!~…"};
    static Set<String> mainBoardSymbols(boolean zhuyin,boolean english,boolean numeric) {
        Set<String> result=new LinkedHashSet<>();
        String[] down=numeric?new String[]{"1234567890+.-"}:zhuyin?ZH_DOWN:english?EN_DOWN:Q_DOWN;
        for(String row:down)addSymbols(result,row);
        if(zhuyin && !numeric)for(String row:ZH_UP)addSymbols(result,row);
        addSymbols(result,",，、");
        if(!zhuyin || numeric)addSymbols(result,".。…");
        return result;
    }
    private static void addSymbols(Set<String> result,String text) {
        text.codePoints().filter(cp->!Character.isLetter(cp) && !Character.isWhitespace(cp))
            .forEach(cp->result.add(new String(Character.toChars(cp))));
    }
    KeyboardView(Context context,Consumer<String> press,Predicate<String> longPress,BiConsumer<float[],Integer> trace) {
        this(context,press,longPress,trace,Runnable::run);
    }
    KeyboardView(Context context,Consumer<String> press,Predicate<String> longPress,BiConsumer<float[],Integer> trace,Consumer<Runnable> choose) {
        super(context); this.press=press; this.longPress=longPress;this.trace=trace;this.choose=choose;
        setOrientation(VERTICAL); setBackgroundColor(BACK); setPadding(0,0,0,0);
        setMotionEventSplittingEnabled(true);
        annotation=new FrameLayout(context);annotation.setBackgroundColor(BACK);
        status=new TextView(context); status.setTextColor(INK); status.setTextSize(12); status.setPadding(dp(8),0,0,0);
        status.setGravity(Gravity.CENTER_VERTICAL);status.setMaxLines(1);status.setEllipsize(android.text.TextUtils.TruncateAt.END);
        annotation.addView(status,new FrameLayout.LayoutParams(-2,-1));
        phonetics=new TextView(context);phonetics.setTextColor(Color.BLACK);phonetics.setTextSize(14);phonetics.setGravity(Gravity.CENTER_VERTICAL);
        phonetics.setPadding(dp(8),0,dp(8),0);phonetics.setMaxLines(1);phonetics.setEllipsize(android.text.TextUtils.TruncateAt.END);
        phonetics.setClickable(true);phonetics.setFocusable(true);phonetics.setOnClickListener(v->{expanded=false;press.accept("CANDIDATE:0");});
        annotation.addView(phonetics,new FrameLayout.LayoutParams(-2,-1));
        annotationWindow=new PopupWindow(annotation,0,dp(24),false);
        annotationWindow.setInputMethodMode(PopupWindow.INPUT_METHOD_NOT_NEEDED);
        // Position in screen coordinates, including above the IME's own window.
        annotationWindow.setIsLaidOutInScreen(true);
        annotationWindow.setBackgroundDrawable(new ColorDrawable(BACK));
        strip=new LinearLayout(context); strip.setGravity(Gravity.CENTER_VERTICAL); strip.setBackgroundColor(0xffe4e7e9); addView(strip,new LayoutParams(-1,dp(48)));
        keys=new LinearLayout(context); keys.setOrientation(VERTICAL); addView(keys);
        setOnApplyWindowInsetsListener((view,insets)-> {
            android.graphics.Insets bars=insets.getSystemWindowInsets();
            setPadding(bars.left,0,bars.right,bars.bottom); return insets;
        });
    }
    View compositionAnnotation() {return annotation;}
    void inputActive(boolean active) {
        inputActive=active;queueAnnotation();
    }
    private void queueAnnotation() {
        removeCallbacks(placeAnnotation);
        if(!inputActive || !annotationRequested)annotationWindow.dismiss();
        else post(placeAnnotation);
    }
    private void placeAnnotation() {
        if(!inputActive || !annotationRequested || !isAttachedToWindow() || !isShown()
                || getWindowVisibility()!=VISIBLE || getWidth()<=0) {annotationWindow.dismiss();return;}
        int available=getWidth()-getPaddingLeft()-getPaddingRight();
        annotation.measure(MeasureSpec.makeMeasureSpec(available,MeasureSpec.AT_MOST),MeasureSpec.makeMeasureSpec(dp(24),MeasureSpec.EXACTLY));
        int[] at=new int[2];getLocationOnScreen(at);
        int x=at[0]+getPaddingLeft(),y=Math.max(0,at[1]-dp(24)),width=Math.max(1,annotation.getMeasuredWidth());
        if(!annotationWindow.isShowing()) {
            annotationWindow.setWidth(width);annotationWindow.showAtLocation(getRootView(),Gravity.TOP|Gravity.LEFT,x,y);
        } else if(x!=annotationX || y!=annotationY || width!=annotationWidth)annotationWindow.update(x,y,width,dp(24));
        annotationX=x;annotationY=y;annotationWidth=width;
    }
    @Override protected void onLayout(boolean changed,int l,int t,int r,int b) {
        super.onLayout(changed,l,t,r,b);queueAnnotation();
    }
    @Override protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        if(annotationWindow!=null)queueAnnotation();
    }
    private float[] center(View view) {int[] at=new int[2];view.getLocationOnScreen(at);return new float[]{at[0]+(view.getWidth()+view.getPaddingLeft()-view.getPaddingRight())/2f,at[1]+view.getHeight()/2f};}
    @Override public boolean dispatchTouchEvent(MotionEvent e) {
        int action=e.getActionMasked();
        if(action==MotionEvent.ACTION_DOWN) {
            candidateGesture=(strip.getVisibility()==VISIBLE && e.getY()>=strip.getTop() && e.getY()<strip.getBottom())
                || (expanded && e.getY()>=keys.getTop());
        }
        if(action==MotionEvent.ACTION_POINTER_DOWN && !tracing) {
            int pointer=e.getActionIndex();boolean nextLetter=false;
            for(TextView key:letters)if(key!=null && key.getParent()!=null) {
                android.graphics.Rect bounds=new android.graphics.Rect(0,0,key.getWidth(),key.getHeight());
                offsetDescendantRectToMyCoords(key,bounds);
                if(bounds.contains((int)e.getX(pointer),(int)e.getY(pointer))) {nextLetter=true;break;}
            }
            if(nextLetter)for(TextView key:letters.clone())if(key instanceof SlideKey)((SlideKey)key).finishTapForOverlap();
        }
        if(action==MotionEvent.ACTION_DOWN) {
            tracePossible=false;tracing=false;points.clear();
            if(traceEnabled && letters['q'-'a']!=null) {
                for(TextView key:letters)if(key!=null) {int[] at=new int[2];key.getLocationOnScreen(at);if(e.getRawX()>=at[0] && e.getRawX()<at[0]+key.getWidth() && e.getRawY()>=at[1] && e.getRawY()<at[1]+key.getHeight())tracePossible=true;}
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
        boolean handled=super.dispatchTouchEvent(e);
        if(action==MotionEvent.ACTION_UP || action==MotionEvent.ACTION_CANCEL) {
            candidateGesture=false;Runnable update=afterCandidateGesture;afterCandidateGesture=null;
            if(update!=null)update.run();
        }
        return handled;
    }
    @Override protected void onDetachedFromWindow() {
        removeCallbacks(placeAnnotation);annotationWindow.dismiss();
        modeMenu=false;
        candidateGesture=false;afterCandidateGesture=null;super.onDetachedFromWindow();
    }
    @Override protected void dispatchDraw(android.graphics.Canvas canvas) {
        super.dispatchDraw(canvas);if(!tracing || points.size()<4)return;
        int[] at=new int[2];getLocationOnScreen(at);android.graphics.Paint paint=new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
        paint.setColor(0xaa176b91);paint.setStrokeWidth(dp(4));paint.setStrokeCap(android.graphics.Paint.Cap.ROUND);
        for(int i=2;i<points.size();i+=2)canvas.drawLine(traceX+points.get(i-2)*pitchX-at[0],traceY+points.get(i-1)*pitchY-at[1],traceX+points.get(i)*pitchX-at[0],traceY+points.get(i+1)*pitchY-at[1],paint);
    }
    private int dp(float value) { return Math.round(value*getResources().getDisplayMetrics().density); }
    private static final class CandidateWord extends TextView {
        CompositionEngine boundEngine;Candidate boundCandidate;long boundComposition;
        boolean matches(CompositionEngine engine,Candidate value,long composition) {
            Candidate prior=boundCandidate;
            return boundEngine==engine && boundComposition==composition && prior!=null
                && prior.text.equals(value.text) && prior.literal==value.literal && prior.pack.equals(value.pack)
                && prior.consumed==value.consumed && (prior.pair==null?value.pair==null:value.pair!=null
                    && prior.pair.phonetic.equals(value.pair.phonetic) && prior.pair.han.equals(value.pair.han) && prior.pair.source.equals(value.pair.source));
        }
        Runnable selection=()->{},pressedSelection,alternate,pressedAlternate;
        private String hint="",phonetic="";
        private final android.text.TextPaint hintPaint=new android.text.TextPaint();
        private final android.graphics.Paint.FontMetrics hintMetrics=new android.graphics.Paint.FontMetrics();
        private boolean held;
        CandidateWord(Context context) {super(context);}
        void alternate(String text,String reading,Runnable action) {
            alternate=action;setLongClickable(action!=null);
            if(!hint.equals(text) || !phonetic.equals(reading)) {hint=text;phonetic=reading;requestLayout();invalidate();}
        }
        private float hintSize() {return 12*getResources().getDisplayMetrics().scaledDensity;}
        private int hintHeight() {return (int)Math.ceil(hintSize()*1.35f);}
        @Override protected void onMeasure(int width,int height) {
            super.onMeasure(width,height);
            if(!hint.isEmpty() && MeasureSpec.getMode(width)!=MeasureSpec.EXACTLY) {
                int readingWidth=(int)Math.ceil(android.text.Layout.getDesiredWidth(phonetic,getPaint()))+getCompoundPaddingLeft()+getCompoundPaddingRight();
                int desired=resolveSize(Math.max(getMeasuredWidth(),readingWidth),width);
                if(desired!=getMeasuredWidth())super.onMeasure(MeasureSpec.makeMeasureSpec(desired,MeasureSpec.EXACTLY),height);
            }
            if(!hint.isEmpty() && MeasureSpec.getMode(height)!=MeasureSpec.EXACTLY)
                setMeasuredDimension(getMeasuredWidth(),resolveSize(Math.max(getSuggestedMinimumHeight(),
                    (getLayout()==null?getMeasuredHeight():getLayout().getHeight())+getCompoundPaddingTop()+getCompoundPaddingBottom()+hintHeight()),height));
        }
        @Override protected void onDraw(android.graphics.Canvas canvas) {
            if(hint.isEmpty() || getLayout()==null) {super.onDraw(canvas);return;}
            android.text.Layout layout=getLayout();
            int top=getPaddingTop(),bottom=getHeight()-getPaddingBottom(),hintHeight=hintHeight();
            // The primary keeps its original width and line wrapping; annotations never widen the row.
            getPaint().setColor(getCurrentTextColor());
            canvas.save();canvas.translate(getCompoundPaddingLeft(),top+Math.max(0,(bottom-top-hintHeight-layout.getHeight())/2f));
            layout.draw(canvas);canvas.restore();
            android.text.TextPaint paint=hintPaint;paint.set(getPaint());
            paint.setTextSize(hintSize());paint.setTypeface(android.graphics.Typeface.DEFAULT);
            int available=Math.max(0,getWidth()-getPaddingLeft()-getPaddingRight());
            CharSequence shown=android.text.TextUtils.ellipsize(hint,paint,available,android.text.TextUtils.TruncateAt.END);
            float x=getPaddingLeft()+(available-paint.measureText(shown.toString()))/2;
            paint.getFontMetrics(hintMetrics);
            canvas.drawText(shown.toString(),x,bottom-hintMetrics.descent,paint);
        }
        @Override public void onInitializeAccessibilityNodeInfo(android.view.accessibility.AccessibilityNodeInfo info) {
            super.onInitializeAccessibilityNodeInfo(info);
            if(alternate!=null)info.addAction(new android.view.accessibility.AccessibilityNodeInfo.AccessibilityAction(
                android.view.accessibility.AccessibilityNodeInfo.ACTION_LONG_CLICK,"Insert "+hint));
        }
        @Override public boolean onTouchEvent(MotionEvent event) {
            if(event.getActionMasked()==MotionEvent.ACTION_DOWN) {pressedSelection=selection;pressedAlternate=alternate;held=false;}
            if(event.getActionMasked()==MotionEvent.ACTION_CANCEL) {pressedSelection=null;pressedAlternate=null;}
            boolean handled=super.onTouchEvent(event);
            // TextView may post its click; clear a non-clicking release only after it.
            if(event.getActionMasked()==MotionEvent.ACTION_UP)post(()->{pressedSelection=null;pressedAlternate=null;held=false;});
            return handled;
        }
        @Override public boolean performClick() {
            if(held) {held=false;return true;}
            Runnable action=pressedSelection==null?selection:pressedSelection;pressedSelection=null;
            super.performClick();action.run();return true;
        }
        @Override public boolean performLongClick() {
            Runnable action=pressedSelection==null?alternate:pressedAlternate;
            if(action==null || held)return false;
            held=true;action.run();performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);return true;
        }
    }
    private TextView button(String label,String command,String up,String down,boolean accent,int height,float weight) {
        TextView b;
        if(command.startsWith("CANDIDATE:")) {
            // Let the horizontal candidate scroller intercept drags normally.
            b=new CandidateWord(getContext()); b.setText(label); b.setGravity(Gravity.CENTER);
            b.setMaxLines(1); b.setFocusable(true); b.setClickable(true);
            b.setOnClickListener(v->{});
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
    private TextView candidate(CompositionEngine engine,Candidate value,long composition) {
        TextView word=button(value.text,"CANDIDATE:","","",false,48,1);
        bindCandidate(word,engine,value,composition);
        return word;
    }
    private void bindCandidate(TextView word,CompositionEngine engine,Candidate value,long composition) {
        CandidateWord bound=(CandidateWord)word;bound.boundEngine=engine;bound.boundCandidate=value;bound.boundComposition=composition;
        if(!value.text.contentEquals(word.getText()))word.setText(value.text);
        word.setContentDescription("Candidate "+value.text);
        ((CandidateWord)word).selection=()->{expanded=false;choose.accept(()->engine.selectCandidate(value,composition));};
        String alternate=value.alternateText();
        ((CandidateWord)word).alternate(alternate,value.pair==null?"":value.pair.phonetic,alternate.isEmpty()?null:
            ()->{expanded=false;choose.accept(()->engine.selectAlternative(value,composition));});
    }
    private void expandedCandidates(CompositionEngine engine,List<Candidate> candidates,int first,int preferred,String key) {
        if(key.equals(gridKey))return;
        gridKey=key;
        List<TextView> available=candidatePool(candidateGrid);
        while(candidateGrid.getChildCount()>candidates.size()-first)candidateGrid.removeViewAt(candidateGrid.getChildCount()-1);
        for(int i=first;i<candidates.size();i++) {
            TextView word=reuseCandidate(available,engine,candidates.get(i),snapshotComposition);
            word.setTextSize(20);word.setMinWidth(dp(48));word.setMinHeight(dp(48));word.setPadding(dp(12),dp(4),dp(12),dp(4));
            word.setSingleLine(false);word.setMaxLines(Integer.MAX_VALUE);
            placeCandidate(candidateGrid,word,i-first,new ViewGroup.LayoutParams(-2,-2));
            bindCandidate(word,engine,candidates.get(i),snapshotComposition);
            if(i==0 && !engine.raw().isEmpty())word.setContentDescription("Exact input "+engine.raw());
            word.setTextColor(preferred==i && !engine.raw().isEmpty()?Color.BLACK:0xff5d6b71);
        }
    }
    private static List<TextView> candidatePool(ViewGroup parent) {
        List<TextView> result=new ArrayList<>();
        for(int i=0;i<parent.getChildCount();i++)if(parent.getChildAt(i) instanceof CandidateWord)result.add((TextView)parent.getChildAt(i));
        return result;
    }
    private TextView reuseCandidate(List<TextView> available,CompositionEngine engine,Candidate value,long composition) {
        for(Iterator<TextView> it=available.iterator();it.hasNext();) {
            TextView word=it.next();if(((CandidateWord)word).matches(engine,value,composition)) {it.remove();return word;}
        }
        return candidate(engine,value,composition);
    }
    /** Preserve accessibility identity when a word moves; never rebind it to another word. */
    private static void placeCandidate(ViewGroup parent,TextView word,int index,ViewGroup.LayoutParams params) {
        View displaced=parent.getChildAt(index);if(displaced==word)return;
        int previous=parent.indexOfChild(word);
        if(previous>=0 && displaced!=null) {
            parent.removeView(word);parent.removeView(displaced);
            if(previous<index) {parent.addView(displaced,previous);parent.addView(word,index,params);}
            else {parent.addView(word,index,params);parent.addView(displaced,previous);}
        } else {
            if(previous>=0)parent.removeView(word);
            if(displaced!=null)parent.removeView(displaced);
            parent.addView(word,Math.min(index,parent.getChildCount()),params);
        }
    }
    private LinearLayout row(int height) {
        LinearLayout row=new LinearLayout(getContext()); row.setMotionEventSplittingEnabled(true);
        keys.addView(row,new LayoutParams(-1,dp(height))); return row;
    }
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
    void render(CompositionEngine engine,boolean zhuyin,boolean shifted,boolean caps,int panel,boolean numeric,boolean asciiPunctuation,boolean english,boolean allowLanguageSwitch,boolean allowTrace,String enter,String loading) {
        if(candidateGesture && snapshotEngine==engine && snapshotComposition==engine.compositionId()) {
            // Do not remove a touched word or scroller between DOWN and UP/CANCEL.
            afterCandidateGesture=()->render(engine,zhuyin,shifted,caps,panel,numeric,asciiPunctuation,english,allowLanguageSwitch,allowTrace,enter,loading);
            return;
        }
        afterCandidateGesture=null;
        String hint=engine.privateField()?"Private input · learning off":loading;
        status.setText(hint);
        String mode=zhuyin+":"+english+":"+numeric+":"+panel+":"+engine.privateField()+":"+engine.inputMode()+":"+returnMode+":"+new TreeSet<>(configuredModes);
        if(!lastRaw.equals(engine.raw()) || !snapshotMode.equals(mode) || panel!=0)modeMenu=false;
        // Keep the last completed row while its replacement is computed. Core acceptance
        // still uses the current query, and composition ownership prevents cross-editor reuse.
        boolean retain=engine.predictionPending() && !engine.raw().isEmpty() && snapshotHasRaw
            && snapshotEngine==engine && snapshotComposition==engine.compositionId() && snapshotMode.equals(mode);
        if(!retain) {
            if(snapshotEngine!=engine) {stripKey="";gridKey="";}
            snapshot=new ArrayList<>(engine.candidates());snapshotPreferred=engine.preferred();
            snapshotEngine=engine;snapshotComposition=engine.compositionId();snapshotMode=mode;snapshotHasRaw=!engine.raw().isEmpty();
        }
        List<Candidate> candidates=new ArrayList<>(snapshot);
        if(retain && !candidates.isEmpty())candidates.set(0,new Candidate(engine.raw(),true,0));
        int preferred=snapshotPreferred;
        int previousScroll=candidateScroll==null?0:candidateScroll.getScrollX();
        if(!lastRaw.equals(engine.raw()))previousScroll=0;
        final int restoreScroll=previousScroll;
        boolean separatePhonetics=!english && !engine.raw().isEmpty() && preferred!=0;
        phonetics.setText(engine.raw());phonetics.setContentDescription((separatePhonetics?"Exact input ":"Composition buffer ")+engine.raw());
        // Prediction intent may change at every prefix; composition visibility must not.
        boolean showPhonetics=!english && !engine.raw().isEmpty() && panel==0;
        phonetics.setVisibility(showPhonetics?VISIBLE:GONE);
        status.setVisibility(panel==0 && !showPhonetics && !hint.isEmpty()?VISIBLE:GONE);
        annotationRequested=showPhonetics || status.getVisibility()==VISIBLE;queueAnnotation();
        strip.setVisibility(panel==0?VISIBLE:GONE);
        if(candidates.isEmpty() || panel!=0)expanded=false;
        traceEnabled=english && allowTrace && !numeric && !zhuyin && panel==0 && !expanded;
        traceCase=caps?2:shifted?1:0;
        StringBuilder presentation=new StringBuilder(mode).append(':').append(shifted).append(':').append(caps)
            .append(':').append(asciiPunctuation).append(':').append(allowLanguageSwitch).append(':').append(enter)
            .append(':').append(loading).append(':').append(snapshotComposition).append(':').append(expanded)
            .append(':').append(modeMenu)
            .append(':').append(separatePhonetics).append(':').append(preferred).append(':').append(engine.raw().isEmpty());
        for(int i=separatePhonetics?1:0;i<candidates.size();i++) {
            Candidate c=candidates.get(i);presentation.append('|').append(c.text.length()).append(':').append(c.text)
                .append(':').append(c.literal).append(':').append(c.consumed).append(':').append(c.alternateText());
        }
        String nextStrip=presentation.toString();
        if(!nextStrip.equals(stripKey)) {
            stripKey=nextStrip;lastRaw=engine.raw();
            if((expanded || modeMenu) && allowLanguageSwitch) {
                // The expanded page already repeats the strip's words. Use that
                // existing toolbar for mode choices without shrinking either viewport.
                strip.removeAllViews();candidateScroll=null;candidateWords=null;
                for(InputMode choice:Arrays.asList(InputMode.CHINESE,InputMode.ENGLISH,InputMode.TAIWANESE,InputMode.JAPANESE)) {
                    TextView tab=plain(choice.label,"MODE:"+choice.id,48,1);tab.setTextSize(20);
                    tab.setContentDescription("Choose "+choice.id+" mode");
                    if(choice==engine.inputMode().family())tab.setBackgroundColor(BLUE);
                    if(!choice.available(configuredModes))tab.setTextColor(0xff90a4ae);
                    tab.setOnClickListener(v->{expanded=false;modeMenu=false;press.accept(choice.available(configuredModes)?"MODE:"+choice.id:"SETTINGS");});
                    strip.addView(tab,new LayoutParams(0,dp(48),1));
                }
                expandButton=plain("⌃","EXPAND",42,1);expandButton.setContentDescription("Collapse candidates");
                expandButton.setOnClickListener(v->{expanded=false;modeMenu=false;render(engine,zhuyin,shifted,caps,panel,numeric,asciiPunctuation,english,allowLanguageSwitch,allowTrace,enter,loading);});
                strip.addView(expandButton,new LayoutParams(dp(40),dp(42)));
            } else if(!candidates.isEmpty()) {
                int from=separatePhonetics?1:0;
                if(candidateScroll==null) {
                    strip.removeAllViews();
                    candidateScroll=new HorizontalScrollView(getContext());candidateScroll.setHorizontalScrollBarEnabled(false);candidateScroll.setContentDescription("Candidate list");
                    candidateWords=new LinearLayout(getContext());candidateScroll.addView(candidateWords);strip.addView(candidateScroll,new LayoutParams(0,-1,1));
                    expandButton=plain("⌄","EXPAND",42,1);strip.addView(expandButton,new LayoutParams(dp(40),dp(42)));
                }
                while(strip.getChildCount()>2)strip.removeViewAt(strip.getChildCount()-1);
                List<TextView> available=candidatePool(candidateWords);
                while(candidateWords.getChildCount()>(candidates.size()-from)*2)candidateWords.removeViewAt(candidateWords.getChildCount()-1);
                for(int i=from;i<candidates.size();i++) {
                    Candidate c=candidates.get(i);TextView word=reuseCandidate(available,engine,c,snapshotComposition);
                    if((i-from)*2<candidateWords.getChildCount())placeCandidate(candidateWords,word,(i-from)*2,new LayoutParams(-2,dp(48)));
                    else {
                        candidateWords.addView(word,new LayoutParams(-2,dp(48)));
                        View divider=new View(getContext());divider.setBackgroundColor(0xffc3cbcf);LayoutParams rule=new LayoutParams(dp(1),dp(26));rule.gravity=Gravity.CENTER_VERTICAL;candidateWords.addView(divider,rule);
                    }
                    bindCandidate(word,engine,c,snapshotComposition);
                    word.setTextSize(english?18:20);word.setTextColor(!engine.raw().isEmpty() && preferred==i?Color.BLACK:0xff5d6b71);
                    word.setTypeface(null,!engine.raw().isEmpty() && preferred==i?android.graphics.Typeface.BOLD:android.graphics.Typeface.NORMAL);
                    word.setMinWidth(dp(48));word.setPadding(dp(12),0,dp(12),0);
                    if(i==0 && !engine.raw().isEmpty())word.setContentDescription("Exact input "+engine.raw());
                }
                HorizontalScrollView currentScroll=candidateScroll;currentScroll.post(()->currentScroll.scrollTo(restoreScroll,0));
                expandButton.setText(expanded?"⌃":allowLanguageSwitch?engine.inputMode().label+"⌄":"⌄");expandButton.setTextSize(15);
                expandButton.setContentDescription(expanded?"Collapse candidates":"Expand candidates");
                expandButton.setOnClickListener(v->{expanded=!expanded;render(engine,zhuyin,shifted,caps,panel,numeric,asciiPunctuation,english,allowLanguageSwitch,allowTrace,enter,loading);});
            } else {
                strip.removeAllViews();candidateScroll=null;candidateWords=null;expandButton=null;
                TextView chinese=plain("中",engine.inputMode()==InputMode.CHINESE?"LAYOUT":"MODE:chinese",48,1); chinese.setTextSize(23); ((SlideKey)chinese).icon(null);
                chinese.setContentDescription(engine.inputMode()!=InputMode.CHINESE?"Switch to Chinese":zhuyin?"拼音 layout":"注音 layout");
                if(engine.inputMode()==InputMode.CHINESE)chinese.setBackgroundColor(BACK);
                strip.addView(chinese,new LayoutParams(dp(75),dp(48)));
                TextView latin=plain("En","MODE:english",48,1); latin.setTextSize(23); ((SlideKey)latin).icon(null);
                latin.setContentDescription(english?"English selected":"Switch to English");
                latin.setOnClickListener(v->{if(!english)press.accept("MODE:english");});
                if(english)latin.setBackgroundColor(BACK);
                strip.addView(latin,new LayoutParams(dp(75),dp(48)));
                if(allowLanguageSwitch)strip.addView(modeBadge(engine.inputMode(),()->render(engine,zhuyin,shifted,caps,panel,numeric,asciiPunctuation,english,allowLanguageSwitch,allowTrace,enter,loading)),new LayoutParams(dp(48),dp(48)));
                strip.addView(new View(getContext()),new LayoutParams(0,1,1));
                TextView next=plain("Next keyboard","NEXT_IME",42,1);
                strip.addView(next,new LayoutParams(dp(42),dp(42)));
            }
            TextView menu=plain("⚙","SETTINGS",42,1); menu.setContentDescription("Settings");
            if(candidates.isEmpty() || panel!=0)strip.addView(menu,new LayoutParams(dp(42),dp(42)));
        }
        boolean landscape=getResources().getConfiguration().orientation==Configuration.ORIENTATION_LANDSCAPE;
        int height=landscape?34:59;
        // Every layout shares the QWERTY budget; only orientation and system insets resize it.
        keys.setLayoutParams(new LayoutParams(-1,dp(height)*4+(panel==0?0:dp(48))));
        String nextLayout=zhuyin+":"+shifted+":"+caps+":"+panel+":"+numeric+":"+asciiPunctuation+":"+english+":"+allowLanguageSwitch+":"+enter+":"+height+":"+expanded+":"+engine.inputMode()+":"+returnMode;
        if(nextLayout.equals(layoutKey)) {
            if(expanded)expandedCandidates(engine,candidates,separatePhonetics?1:0,preferred,nextStrip);
            return;
        }
        layoutKey=nextLayout; keys.removeAllViews();Arrays.fill(letters,null);candidateGrid=null;gridKey="";
        if(expanded) {
            ScrollView scroll=new ScrollView(getContext());scroll.setContentDescription("Expanded candidate list");
            candidateGrid=new CandidateFlowLayout(getContext());scroll.addView(candidateGrid);
            expandedCandidates(engine,candidates,separatePhonetics?1:0,preferred,nextStrip);
            keys.addView(scroll,new LayoutParams(-1,dp(height*(zhuyin?4:3))));
        } else if(panel==3) {
            punctuationChoices(asciiPunctuation,allowLanguageSwitch && !english,height);
        } else if(panel>0) {
            keys.addView(new SymbolPanel(getContext(),panel==2,engine.privateField(),press,mainBoardSymbols(zhuyin,english,numeric)));
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
                if(r==2) line.addView(plain(caps?"⇪":shifted?"⬆":"⇧","SHIFT",height,1.5f));
                for(int i=0;i<QWERTY[r].length();i++) {
                    String lower=QWERTY[r].substring(i,i+1), upper=lower.toUpperCase(Locale.ROOT);
                    String label=shifted?upper:lower;
                    boolean outer=r==1 && (i==0 || i==QWERTY[r].length()-1);
                    TextView letter=button(label,label,upper,(english?EN_DOWN:Q_DOWN)[r].substring(i,i+1),false,height,outer?1.5f:1);
                    ((SlideKey)letter).qwertyStyle();
                    if(outer)((SlideKey)letter).outerMargin(i==0);
                    letters[lower.charAt(0)-'a']=letter;line.addView(letter);
                }
                if(r==2) line.addView(plain("⌫","DELETE",height,1.5f));
            }
        }
        LinearLayout bottom=row(height);
        TextView symbol=plain(panel>0?"ABC":"?123",panel>0?"LETTERS":"SYMBOLS",height,1.6f); symbol.setTextSize(16); bottom.addView(symbol);
        bottom.addView(punctuation(true,asciiPunctuation,allowLanguageSwitch && !english,height));
        if(allowLanguageSwitch) {
            TextView language=plain(returnMode.label,"LANGUAGE",height,.9f);language.setTextSize(15);((SlideKey)language).icon(null);
            language.setContentDescription("Switch to "+modeName(returnMode));bottom.addView(language);
        }
        String mixedLabel=engine.inputMode()==InputMode.CHINESE?(zhuyin?"注音":"拼音"):engine.inputMode().description();
        TextView space=plain(english?"English":mixedLabel,"SPACE",height,4); space.setTextSize(14); space.setTextColor(0xff6d7b80); space.setContentDescription("Space");
        GradientDrawable spaceShape=new GradientDrawable();spaceShape.setColor(0xffcbd0d3);spaceShape.setCornerRadius(dp(2));
        space.setBackground(new RippleDrawable(ColorStateList.valueOf(0x33263238),new InsetDrawable(spaceShape,dp(12),dp(14),dp(12),dp(14)),null));bottom.addView(space);
        if(zhuyin && panel==0 && !numeric) bottom.addView(plain("ㄦ","ㄦ",height,1));
        else bottom.addView(punctuation(false,asciiPunctuation,allowLanguageSwitch && !english,height));
        if(zhuyin || panel>0 || numeric || expanded) bottom.addView(plain("⌫","DELETE",height,1.5f));
        TextView action=button(enter,"ENTER","","",true,height,1.5f); action.setTextSize(14); bottom.addView(action);
        for(int i=0;i<keys.getChildCount();i++) {
            View child=keys.getChildAt(i);
            child.setLayoutParams(child==bottom?new LayoutParams(-1,dp(height)):new LayoutParams(-1,0,1));
            if(child instanceof LinearLayout && !(child instanceof SymbolPanel)) {
                LinearLayout line=(LinearLayout)child;
                for(int j=0;j<line.getChildCount();j++) {
                    View key=line.getChildAt(j);LayoutParams params=(LayoutParams)key.getLayoutParams();
                    params.height=LayoutParams.MATCH_PARENT;key.setLayoutParams(params);
                }
            }
        }
    }
}
