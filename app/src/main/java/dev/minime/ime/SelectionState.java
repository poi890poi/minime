package dev.minime.ime;

import java.util.ArrayDeque;

/** Distinguishes our queued InputConnection updates from an editor cursor move. */
final class SelectionState {
    private int start=-1,end=-1,composingStart=-1;
    private boolean deletion;
    private String committed="";
    private int committedEnd=-1;
    private boolean rememberCommitted=true;
    private void clearCommitted() {committed="";committedEnd=-1;}
    void rememberCommitted(boolean enabled) {rememberCommitted=enabled;if(!enabled)clearCommitted();}
    private final ArrayDeque<Integer> expected=new ArrayDeque<>();
    void start(int start,int end) { this.start=start; this.end=end; composingStart=-1; deletion=false; expected.clear();clearCommitted(); }
    void write(String text,boolean composing) {
        int at=composingStart>=0?composingStart:Math.min(start,end);
        if(at<0) {clearCommitted();return;}
        if(at!=committedEnd)clearCommitted();
        if(!composing && rememberCommitted) {
            // Only text sent by this IME, never arbitrary editor surroundings.
            committed+=text;
            int cut=Math.max(0,committed.length()-96);
            if(cut>0 && Character.isLowSurrogate(committed.charAt(cut)))cut++;
            committed=committed.substring(cut);committedEnd=at+text.length();
        }
        start=end=at+text.length(); composingStart=composing?at:-1;
        expected.add(end); if(expected.size()>256) expected.removeFirst();
    }
    void finish() { composingStart=-1; }
    boolean owns(int nextStart,int nextEnd,int length) { return composingStart>=0 && start==end && end==nextStart && end==nextEnd && end-composingStart==length; }
    int cursor() {return end;}
    String committedText() {return committed;}
    boolean ownsCommitted(int nextStart,int nextEnd) {return !committed.isEmpty() && start==end && end==committedEnd && end==nextStart && end==nextEnd;}
    void rewind(int count) { start=end=Math.max(0,end-count);composingStart=-1;expected.clear();clearCommitted(); }
    void delete() { deletion=true; composingStart=-1; expected.clear();clearCommitted(); }
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
