package dev.minime.ime;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.*;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.*;
import java.util.function.*;

/** A gesture belongs to its initial key and produces at most one selection. */
final class SlideKey extends TextView {
    private static final int SLIDE_UP=0x01020001, SLIDE_DOWN=0x01020002;
    private final String label, command, up, down;
    private final Consumer<String> press;
    private final Predicate<String> hold;
    private final Paint hintPaint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private KeyboardIcon icon;
    private boolean centeredHint,emojiHint;
    void icon(KeyboardIcon value) {icon=value;}
    void qwertyStyle() {centeredHint=true;setPadding(0,0,0,Math.round(15*getResources().getDisplayMetrics().density));}
    void emojiHint() {emojiHint=true;setPadding(0,Math.round(14*getResources().getDisplayMetrics().density),0,0);}
    private float originX,originY;
    private int direction, activePointer=-1;
    private boolean active, consumed, cancelled;
    private PopupMenu menu;
    private PopupWindow palette;
    private final java.util.List<TextView> paletteKeys=new java.util.ArrayList<>();
    private int paletteSelection=-1;
    void punctuationPalette(String[] values,boolean widthChoice,boolean ascii) {
        setOnLongClickListener(v->{
            dismissPalette();LinearLayout body=new LinearLayout(getContext());body.setOrientation(LinearLayout.VERTICAL);body.setBackgroundColor(0xffdfe3e5);
            int cell=Math.round(42*getResources().getDisplayMetrics().density);
            for(int row=0;row<(values.length+5)/6;row++) {
                LinearLayout line=new LinearLayout(getContext());body.addView(line);
                for(int col=0;col<6 && row*6+col<values.length;col++) {
                    String text=values[row*6+col];TextView key=new TextView(getContext());key.setText(text);key.setTextSize(20);key.setTextColor(0xff263238);key.setGravity(Gravity.CENTER);
                    key.setContentDescription("Punctuation "+text);key.setFocusable(true);key.setClickable(true);key.setOnClickListener(view->{dismissPalette();press.accept("INSERT:"+text);});
                    paletteKeys.add(key);line.addView(key,new LinearLayout.LayoutParams(0,cell,1));
                }
            }
            if(widthChoice) {
                Button width=new Button(getContext());width.setText(ascii?"Use Chinese punctuation":"Use English punctuation");
                width.setOnClickListener(view->{dismissPalette();press.accept("PUNCT_WIDTH");});body.addView(width,new LinearLayout.LayoutParams(-1,cell));
            }
            int[] at=new int[2];getLocationOnScreen(at);
            int screen=getResources().getDisplayMetrics().widthPixels,width=Math.min(screen-16,cell*6),height=cell*((values.length+5)/6+(widthChoice?1:0));
            palette=new PopupWindow(body,width,height,false);palette.setElevation(12);palette.setOutsideTouchable(true);palette.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(0xffdfe3e5));
            palette.showAtLocation(getRootView(),Gravity.TOP|Gravity.LEFT,Math.max(0,Math.min(at[0]+getWidth()/2-width/2,screen-width)),Math.max(0,at[1]-height));
            return true;
        });
    }
    private void dismissPalette() { if(palette!=null)palette.dismiss();palette=null;paletteKeys.clear();paletteSelection=-1; }
    private void trackPalette(float x,float y) {
        paletteSelection=-1;
        for(int i=0;i<paletteKeys.size();i++) {TextView key=paletteKeys.get(i);int[] at=new int[2];key.getLocationOnScreen(at);
            boolean selected=x>=at[0] && x<at[0]+key.getWidth() && y>=at[1] && y<at[1]+key.getHeight();
            key.setBackgroundColor(selected?0xffa7d8ee:0xfff9fafb);if(selected)paletteSelection=i;
        }
    }
    private final Runnable repeat=new Runnable() {
        public void run() {
            if(!active || cancelled || direction!=0) return;
            consumed=true; press.accept(command); postDelayed(this,75);
        }
    };
    private final Runnable longAction=()-> {
        if(active && !cancelled && direction==0) consumed=performLongClick();
    };
    SlideKey(Context context,String label,String command,String up,String down,
             Consumer<String> press,Predicate<String> hold) {
        super(context); this.label=label; this.command=command; this.up=up; this.down=down;
        this.press=press; this.hold=hold;
        setText(label); setGravity(Gravity.CENTER); setTextSize(21); setMaxLines(1);
        setFocusable(true); setClickable(true); setContentDescription(label);
        setOnClickListener(v->press.accept(command));
        setOnLongClickListener(v-> {
            if(hold.test(command)) return true;
            if(up.isEmpty() && down.isEmpty()) return false;
            menu=new PopupMenu(getContext(),this);
            if(!up.isEmpty()) menu.getMenu().add("↑ "+up).setOnMenuItemClickListener(item->{ alternate(up); return true; });
            if(!down.isEmpty()) menu.getMenu().add("↓ "+down).setOnMenuItemClickListener(item->{ alternate(down); return true; });
            menu.show(); return true;
        });
        hintPaint.setColor(0xff68777b); hintPaint.setTextAlign(Paint.Align.RIGHT);
        hintPaint.setTextSize(10*getResources().getDisplayMetrics().scaledDensity);
    }
    @Override protected void onDraw(Canvas canvas) {
        if(icon==null)super.onDraw(canvas);
        else {icon.setBounds(0,0,getWidth(),getHeight());icon.draw(canvas);}
        if(direction==0 && centeredHint) {
            hintPaint.setTextAlign(Paint.Align.CENTER);hintPaint.setFakeBoldText(true);
            canvas.drawText(down,getWidth()/2f,getHeight()-9*getResources().getDisplayMetrics().density-hintPaint.descent(),hintPaint);
        } else if(direction==0 && emojiHint) {
            hintPaint.setTextAlign(Paint.Align.CENTER);hintPaint.setFakeBoldText(false);
            float density=getResources().getDisplayMetrics().density,x=getWidth()/2f,y=12*density;
            hintPaint.setStyle(Paint.Style.STROKE);hintPaint.setStrokeWidth(density);canvas.drawCircle(x,y,5*density,hintPaint);
            canvas.drawArc(x-3*density,y-2*density,x+3*density,y+3*density,25,130,false,hintPaint);
            hintPaint.setStyle(Paint.Style.FILL);canvas.drawCircle(x-1.7f*density,y-1.5f*density,.7f*density,hintPaint);canvas.drawCircle(x+1.7f*density,y-1.5f*density,.7f*density,hintPaint);
        } else if(direction==0) {
            float pad=3*getResources().getDisplayMetrics().density;
            if(!up.isEmpty() && !up.equals(label)) canvas.drawText(up,getWidth()-pad,pad-hintPaint.ascent(),hintPaint);
            if(!down.isEmpty()) canvas.drawText(down,getWidth()-pad,getHeight()-pad-hintPaint.descent(),hintPaint);
        }
    }
    // A new letter contact completes the older plain tap in finger-down order.
    // Slides, holds and cancelled gestures retain their own lifecycle.
    void finishTapForOverlap() {
        if(active && !consumed && !cancelled && direction==0) {reset();performClick();}
    }
    @Override public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info); info.setClassName(Button.class.getName());
        if(!up.isEmpty()) info.addAction(new AccessibilityNodeInfo.AccessibilityAction(SLIDE_UP,"Slide up: "+up));
        if(!down.isEmpty()) info.addAction(new AccessibilityNodeInfo.AccessibilityAction(SLIDE_DOWN,"Slide down: "+down));
    }
    @Override public boolean performAccessibilityAction(int action,Bundle args) {
        if(action==SLIDE_UP && !up.isEmpty()) { alternate(up); return true; }
        if(action==SLIDE_DOWN && !down.isEmpty()) { alternate(down); return true; }
        return super.performAccessibilityAction(action,args);
    }
    @Override public boolean performClick() { return super.performClick(); }
    private void alternate(String value) { press.accept("LITERAL:"+value); }
    private void stopTimers() { removeCallbacks(repeat); removeCallbacks(longAction); }
    private void reset() { active=false; activePointer=-1; stopTimers(); direction=0; setText(label); setPressed(false); }
    @Override protected void onDetachedFromWindow() {
        reset();dismissPalette(); if(menu!=null) menu.dismiss(); super.onDetachedFromWindow();
    }
    @Override public boolean onTouchEvent(MotionEvent event) {
        int action=event.getActionMasked();
        if(action==MotionEvent.ACTION_POINTER_DOWN) {
            if(active)return true;
            action=MotionEvent.ACTION_DOWN;
        }
        if(action==MotionEvent.ACTION_POINTER_UP) {
            if(event.getPointerId(event.getActionIndex())!=activePointer)return true;
            action=MotionEvent.ACTION_UP;
        }
        int pointer=action==MotionEvent.ACTION_DOWN?event.getActionIndex():event.findPointerIndex(activePointer);
        float x=pointer<0?0:event.getX(pointer),y=pointer<0?0:event.getY(pointer);
        switch(action) {
            case MotionEvent.ACTION_DOWN:
                active=true; activePointer=event.getPointerId(pointer); consumed=false; cancelled=false; direction=0;
                originX=x; originY=y; setPressed(true);
                getParent().requestDisallowInterceptTouchEvent(true);
                postDelayed(command.equals("DELETE")?repeat:longAction,ViewConfiguration.getLongPressTimeout());
                return true;
            case MotionEvent.ACTION_CANCEL:
                cancelled=true;dismissPalette(); reset(); return true;
            case MotionEvent.ACTION_MOVE:
                if(palette!=null && palette.isShowing()) {trackPalette(event.getRawX(),event.getRawY());return true;}
                if(!active || consumed || cancelled) return true;
                float dx=x-originX,dy=y-originY;
                float threshold=18*getResources().getDisplayMetrics().density;
                if(Math.abs(dx)>threshold || Math.abs(dy)>threshold) stopTimers();
                if(Math.abs(dx)>threshold && Math.abs(dx)>Math.abs(dy)) {
                    cancelled=true; setPressed(false); return true;
                }
                direction=Math.abs(dy)>=threshold && Math.abs(dy)>Math.abs(dx)*1.2f ? (dy<0?-1:1):0;
                setText(direction<0?up:direction>0?down:label);
                setPressed(direction==0 || !(direction<0?up:down).isEmpty());
                return true;
            case MotionEvent.ACTION_UP:
                if(!active) return true;
                if(palette!=null && palette.isShowing()) {
                    trackPalette(event.getRawX(),event.getRawY());int picked=paletteSelection;reset();
                    if(picked>=0) {String text=paletteKeys.get(picked).getText().toString();dismissPalette();press.accept("INSERT:"+text);}return true;
                }
                int selected=direction;
                boolean emit=!consumed && !cancelled;
                float slop=ViewConfiguration.get(getContext()).getScaledTouchSlop();
                boolean inside=pointer>=0 && x>=-slop && x<getWidth()+slop && y>=-slop && y<getHeight()+slop;
                reset();
                if(emit) {
                    if(selected==0 && inside) performClick();
                    else if(selected<0 && !up.isEmpty()) alternate(up);
                    else if(selected>0 && !down.isEmpty()) alternate(down);
                }
                return true;
            default: return true;
        }
    }
}
