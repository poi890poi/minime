package dev.minime.core;

public final class Candidate {
    public final String text;
    public final boolean literal;
    public final double score;
    final String reading;
    /** ASCII phonetic prefix consumed by an explicit choice; zero means the whole token. */
    public final int consumed;
    /** Supplemental choices never train the base decoder's automatic preference. */
    public final boolean supplemental;
    private Candidate(String text,double score) {
        this.text=text;this.literal=false;this.score=score;this.reading="";this.consumed=0;this.supplemental=true;
    }
    public static Candidate supplement(String text,double score) {return new Candidate(text,score);}
    public Candidate(String text,boolean literal,double score,int consumed) {
        this.text=text;this.literal=literal;this.score=score;this.reading="";this.consumed=consumed;this.supplemental=false;
    }
    public Candidate(String text, boolean literal, double score) {
        this(text,literal,score,"");
    }
    Candidate(String text, boolean literal, double score,String reading) {
        this.text = text; this.literal = literal; this.score = score; this.reading=reading; this.consumed=0;this.supplemental=false;
    }
    @Override public String toString() { return text; }
}
