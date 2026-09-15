package dev.minime.ime;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import dev.minime.core.JoinedKalq;

/** Approved 26/18 portrait geometry. Drawing never changes a key's touch bounds. */
final class PortraitKeyLabels {
    private final Paint main, hint;
    private final Rect mainEnvelope=new Rect(), hintEnvelope=new Rect(), ink=new Rect();
    private final float density;
    PortraitKeyLabels(Paint main,Paint hint,float density) {
        this.main=main;this.hint=hint;this.density=density;
        envelope(main,"abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ",mainEnvelope);
        envelope(hint,JoinedKalq.symbols(true)+JoinedKalq.symbols(false),hintEnvelope);
    }
    private void envelope(Paint paint,String text,Rect result) {
        for(int i=0;i<text.length();i++){paint.getTextBounds(text,i,i+1,ink);result.union(ink);}
    }
    void draw(Canvas canvas,String text,String symbol,int width,int height,boolean alternate) {
        // Fit unusually narrow windows without changing keyboard height or key hit areas.
        float fit=Math.min(1f,Math.min(width/(36*density),height/(44*density)));
        float w=width/fit,h=height/fit;
        canvas.save();canvas.scale(fit,fit);
        if(!alternate) {
            hint.getTextBounds(symbol,0,symbol.length(),ink);
            float slot=Math.max(12*density,ink.width());
            canvas.drawText(symbol,w-4*density-slot/2-ink.exactCenterX(),h-2*density-hintEnvelope.bottom,hint);
        }
        main.getTextBounds(text,0,text.length(),ink);
        float baseline=alternate?h/2-ink.exactCenterY():h/2-mainEnvelope.exactCenterY()-3*density;
        canvas.drawText(text,w/2-ink.exactCenterX(),baseline,main);
        canvas.restore();
    }
}
