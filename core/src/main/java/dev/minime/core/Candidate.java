package dev.minime.core;

public final class Candidate {
    public final String text;
    public final boolean literal;
    public final double score;
    final String reading;
    public Candidate(String text, boolean literal, double score) {
        this(text,literal,score,"");
    }
    Candidate(String text, boolean literal, double score,String reading) {
        this.text = text; this.literal = literal; this.score = score; this.reading=reading;
    }
    @Override public String toString() { return text; }
}
