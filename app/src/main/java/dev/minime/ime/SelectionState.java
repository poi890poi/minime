package dev.minime.ime;

import java.util.ArrayDeque;

/** Distinguishes our queued InputConnection updates from an editor cursor move. */
final class SelectionState {
    private int start=-1,end=-1,composingStart=-1;
    private boolean deletion;
    private final ArrayDeque<Integer> expected=new ArrayDeque<>();
    void start(int start,int end) { this.start=start; this.end=end; composingStart=-1; deletion=false; expected.clear(); }
    void write(String text,boolean composing) {
        int at=composingStart>=0?composingStart:Math.min(start,end);
        if(at<0) return;
        start=end=at+text.length(); composingStart=composing?at:-1;
        expected.add(end); if(expected.size()>256) expected.removeFirst();
    }
    void finish() { composingStart=-1; }
    void rewind(int count) { start=end=Math.max(0,end-count);composingStart=-1;expected.clear(); }
    void delete() { deletion=true; composingStart=-1; expected.clear(); }
    boolean update(int nextStart,int nextEnd) {
        if(nextStart==nextEnd && expected.contains(nextEnd)) {
            while(!expected.isEmpty() && expected.removeFirst()!=nextEnd) { }
            return false;
        }
        if(expected.isEmpty() && nextStart==start && nextEnd==end) return false;
        boolean external=!deletion;
        start(nextStart,nextEnd); return external;
    }
}
