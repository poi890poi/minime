package dev.minime.core;

/** Shift timing belongs to the keyboard session, independent of view replacement. */
public final class ShiftState {
    private boolean shifted,locked;
    private long lastTap=-1;
    public boolean upper() { return shifted || locked; }
    public boolean locked() { return locked; }
    public void tap(long now,long doubleTapTimeout) {
        if(locked) { reset(); return; }
        if(shifted && lastTap>=0 && now>=lastTap && now-lastTap<=doubleTapTimeout) {
            locked=true; shifted=false; lastTap=-1;
        } else { shifted=!shifted; lastTap=shifted?now:-1; }
    }
    public void hold() { locked=!locked; shifted=false; lastTap=-1; }
    public void interrupt() { lastTap=-1; }
    public void consume() { shifted=false; lastTap=-1; }
    public void reset() { shifted=false; locked=false; lastTap=-1; }
}
