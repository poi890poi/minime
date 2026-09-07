package dev.minime.ime;

import android.graphics.*;
import android.graphics.drawable.Drawable;

/** Simple geometric controls drawn independently of proprietary keyboard assets. */
final class KeyboardIcon extends Drawable {
    private final String command,label;
    private final int ink,accent;
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    KeyboardIcon(String command,String label,int ink,int accent) {this.command=command;this.label=label;this.ink=ink;this.accent=accent;}
    static boolean supports(String command) {return command.equals("SHIFT") || command.equals("DELETE") || command.equals("ENTER") || command.equals("LANGUAGE") || command.equals("NEXT_IME") || command.equals("SETTINGS") || command.equals("EXPAND");}
    private void line(Canvas c,float... xy) {Path p=new Path();p.moveTo(xy[0],xy[1]);for(int i=2;i<xy.length;i+=2)p.lineTo(xy[i],xy[i+1]);c.drawPath(p,paint);}
    @Override public void draw(Canvas c) {
        Rect b=getBounds();float size=Math.min(b.width(),b.height())*.53f;
        c.save();c.translate(b.exactCenterX(),b.exactCenterY());c.scale(size/24,size/24);
        paint.setColor(ink);paint.setStrokeWidth(2);paint.setStyle(Paint.Style.STROKE);paint.setStrokeJoin(Paint.Join.ROUND);paint.setStrokeCap(Paint.Cap.ROUND);
        switch(command) {
            case "LANGUAGE": case "NEXT_IME":
                c.drawCircle(0,0,9,paint);c.drawOval(new RectF(-4,-9,4,9),paint);c.drawLine(-9,0,9,0,paint);c.drawLine(-7,-5,7,-5,paint);c.drawLine(-7,5,7,5,paint);break;
            case "SHIFT":
                paint.setColor(label.equals("⇧")?0xff859296:ink);paint.setStyle(Paint.Style.FILL);
                Path shift=new Path();shift.moveTo(0,-10);shift.lineTo(11,1);shift.lineTo(5,1);shift.lineTo(5,9);shift.lineTo(-5,9);shift.lineTo(-5,1);shift.lineTo(-11,1);shift.close();c.drawPath(shift,paint);
                if(label.equals("⇪"))c.drawRect(-5,11,5,13,paint);break;
            case "DELETE":
                paint.setColor(0xff859296);paint.setStyle(Paint.Style.FILL);Path delete=new Path();delete.moveTo(-11,0);delete.lineTo(-5,-8);delete.lineTo(11,-8);delete.lineTo(11,8);delete.lineTo(-5,8);delete.close();c.drawPath(delete,paint);
                paint.setColor(Color.WHITE);paint.setStyle(Paint.Style.STROKE);c.drawLine(-1,-4,7,4,paint);c.drawLine(-1,4,7,-4,paint);break;
            case "ENTER":
                paint.setColor(accent);paint.setStyle(Paint.Style.FILL);c.drawCircle(0,0,13,paint);paint.setColor(Color.WHITE);paint.setStyle(Paint.Style.STROKE);
                if(label.equals("Search")) {c.drawCircle(-2,-2,5,paint);c.drawLine(2,2,7,7,paint);}
                else if(label.equals("Done"))line(c,-7,0,-2,5,8,-6);
                else if(label.equals("Next") || label.equals("Go")) {line(c,-8,0,8,0);line(c,2,-6,8,0,2,6);}
                else {line(c,8,-5,8,1,-8,1);line(c,-2,-5,-8,1,-2,7);}break;
            case "EXPAND": line(c,-5,label.equals("⌃")?3:-3,0,label.equals("⌃")?-2:2,5,label.equals("⌃")?3:-3);break;
            case "SETTINGS":
                c.drawCircle(0,0,6,paint);c.drawCircle(0,0,2,paint);
                for(int i=0;i<8;i++){c.save();c.rotate(i*45);c.drawLine(0,-6,0,-9,paint);c.restore();}break;
        }
        c.restore();
    }
    @Override public void setAlpha(int alpha) {paint.setAlpha(alpha);}
    @Override public void setColorFilter(ColorFilter filter) {paint.setColorFilter(filter);}
    @Override public int getOpacity() {return PixelFormat.TRANSLUCENT;}
}
