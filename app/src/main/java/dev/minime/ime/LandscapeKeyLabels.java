package dev.minime.ime;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import dev.minime.core.JoinedKalq;

/** Uses landscape's horizontal room without adding rows or moving touch targets. */
final class LandscapeKeyLabels {
    private final Paint main,hint;
    private final Rect letters=new Rect(),symbols=new Rect(),ink=new Rect();
    private final float density;
    LandscapeKeyLabels(Paint main,Paint hint,float density) {
        this.main=main;this.hint=hint;this.density=density;
        envelope(main,"abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ",letters);
        envelope(hint,JoinedKalq.symbols(true)+JoinedKalq.symbols(false),symbols);
    }
    private void envelope(Paint paint,String text,Rect out) {
        for(int i=0;i<text.length();i++){paint.getTextBounds(text,i,i+1,ink);out.union(ink);}
    }
    void draw(Canvas canvas,String text,String symbol,int width,int height,boolean alternate) {
        float gap=8*density,pad=3*density;
        float contentWidth=letters.width()+gap+symbols.width();
        float contentHeight=Math.max(letters.height(),symbols.height());
        float fit=Math.min(1f,Math.min(width/(contentWidth+2*pad),height/(contentHeight+2*pad)));
        if(fit<=0)return;
        float w=width/fit,h=height/fit,left=(w-contentWidth)/2;
        canvas.save();canvas.scale(fit,fit);
        if(!alternate) {
            hint.getTextBounds(symbol,0,symbol.length(),ink);
            canvas.drawText(symbol,left+letters.width()+gap+symbols.width()/2f-ink.exactCenterX(),h/2-symbols.exactCenterY(),hint);
        }
        main.getTextBounds(text,0,text.length(),ink);
        float center=alternate?w/2:left+letters.width()/2f;
        canvas.drawText(text,center-ink.exactCenterX(),h/2-letters.exactCenterY(),main);
        canvas.restore();
    }
}
