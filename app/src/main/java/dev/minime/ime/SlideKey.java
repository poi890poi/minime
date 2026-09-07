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
    private float originX,originY;
    private int direction;
    private boolean active, consumed, cancelled;
    private PopupMenu menu;
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
        super.onDraw(canvas);
        if(direction==0) {
            float pad=3*getResources().getDisplayMetrics().density;
            if(!up.isEmpty() && !up.equals(label)) canvas.drawText(up,getWidth()-pad,pad-hintPaint.ascent(),hintPaint);
            if(!down.isEmpty()) canvas.drawText(down,getWidth()-pad,getHeight()-pad-hintPaint.descent(),hintPaint);
        }
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
    private void reset() { active=false; stopTimers(); direction=0; setText(label); setPressed(false); }
    @Override protected void onDetachedFromWindow() {
        reset(); if(menu!=null) menu.dismiss(); super.onDetachedFromWindow();
    }
    @Override public boolean onTouchEvent(MotionEvent event) {
        switch(event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                active=true; consumed=false; cancelled=false; direction=0;
                originX=event.getX(); originY=event.getY(); setPressed(true);
                getParent().requestDisallowInterceptTouchEvent(true);
                postDelayed(command.equals("DELETE")?repeat:longAction,ViewConfiguration.getLongPressTimeout());
                return true;
            case MotionEvent.ACTION_POINTER_DOWN:
            case MotionEvent.ACTION_CANCEL:
                cancelled=true; reset(); return true;
            case MotionEvent.ACTION_MOVE:
                if(!active || consumed || cancelled) return true;
                float dx=event.getX()-originX,dy=event.getY()-originY;
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
                int selected=direction;
                boolean emit=!consumed && !cancelled;
                boolean inside=event.getX()>=0 && event.getX()<getWidth() && event.getY()>=0 && event.getY()<getHeight();
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
