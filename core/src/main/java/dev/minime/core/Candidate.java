package dev.minime.core;

public final class Candidate {
    public final String text;
    public final boolean literal;
    public final double score;
    final String reading;
    /** ASCII phonetic prefix consumed by an explicit choice; zero means the whole token. */
    public final int consumed;
    public Candidate(String text,boolean literal,double score,int consumed) {
        this.text=text;this.literal=literal;this.score=score;this.reading="";this.consumed=consumed;
    }
    public Candidate(String text, boolean literal, double score) {
        this(text,literal,score,"");
    }
    Candidate(String text, boolean literal, double score,String reading) {
        this.text = text; this.literal = literal; this.score = score; this.reading=reading; this.consumed=0;
    }
    @Override public String toString() { return text; }
}
