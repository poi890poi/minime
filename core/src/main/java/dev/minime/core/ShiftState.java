package dev.minime.core;

/** Shift timing belongs to the keyboard session, independent of view replacement. */
public final class ShiftState {
    private boolean shifted,locked,automatic,suppressed;
    private long lastTap=-1;
    public boolean upper() { return shifted || locked || (automatic && !suppressed); }
    public void automatic(boolean value) { automatic=value; }
    public boolean locked() { return locked; }
    public void tap(long now,long doubleTapTimeout) {
        if(locked) { reset(); return; }
        if(lastTap>=0 && now>=lastTap && now-lastTap<=doubleTapTimeout) {
            locked=true; shifted=false; lastTap=-1;
        } else { boolean wasUpper=upper(); shifted=!wasUpper; suppressed=wasUpper && automatic; lastTap=now; }
    }
    public void hold() { locked=!locked; shifted=false; lastTap=-1; }
    public void interrupt() { lastTap=-1; }
    public void consume() { shifted=false; suppressed=false; lastTap=-1; }
    public void reset() { shifted=false; locked=false; automatic=false; suppressed=false; lastTap=-1; }
}
