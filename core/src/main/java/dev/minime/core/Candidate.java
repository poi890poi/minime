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
    public final boolean abbreviated;
    /** True only when the retained path joins more than one lexical unit. */
    public final boolean composed;
    private Candidate(String text,boolean literal,double score,String reading,int consumed,boolean supplemental,boolean abbreviated,boolean composed) {
        this.text=text;this.literal=literal;this.score=score;this.reading=reading;this.consumed=consumed;this.supplemental=supplemental;this.abbreviated=abbreviated;this.composed=composed;
    }
    private Candidate(String text,double score,boolean abbreviated) {
        this(text,false,score,"",0,true,abbreviated,false);
    }
    public static Candidate supplement(String text,double score) {return new Candidate(text,score,false);}
    static Candidate supplement(String text,double score,boolean abbreviated) {return new Candidate(text,score,abbreviated);}
    public Candidate(String text,boolean literal,double score,int consumed) {
        this(text,literal,score,"",consumed,false,false,false);
    }
    public Candidate(String text, boolean literal, double score) {
        this(text,literal,score,"");
    }
    Candidate(String text, boolean literal, double score,String reading) {
        this(text,literal,score,reading,0,false,false,false);
    }
    static Candidate concatenate(Candidate prefix,Candidate word) {
        return new Candidate(prefix.text+word.text,false,prefix.score+word.score,"",0,false,false,prefix.composed || word.composed || !prefix.text.isEmpty());
    }
    Candidate withScore(double value) {return new Candidate(text,literal,value,reading,consumed,supplemental,abbreviated,composed);}
    @Override public String toString() { return text; }
}
