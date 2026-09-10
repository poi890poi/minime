package dev.minime.core;

public final class Candidate {
    public final String text;
    public final boolean literal;
    public final double score;
    final String reading;
    /** ASCII phonetic prefix consumed by an explicit choice; zero means the whole token. */
    public final int consumed;
    /** Supplemental identity; focused choices learn in their own language namespace. */
    public final boolean supplemental;
    public final boolean abbreviated;
    /** True only when the retained path joins more than one lexical unit. */
    public final boolean composed;
    /** The stored reading requires letters that have not been typed yet. */
    public final boolean incomplete;
    public final PairedForms.Pair pair;
    /** Source dictionary pack, retained through matching and display transformations. */
    public final String pack;
    /** Standalone form from the validated Japanese character source. */
    public final boolean languageCharacter;
    private Candidate(String text,boolean literal,double score,String reading,int consumed,boolean supplemental,boolean abbreviated,boolean composed) {
        this(text,literal,score,reading,consumed,supplemental,abbreviated,composed,abbreviated);
    }
    private Candidate(String text,boolean literal,double score,String reading,int consumed,boolean supplemental,boolean abbreviated,boolean composed,boolean incomplete) {
        this(text,literal,score,reading,consumed,supplemental,abbreviated,composed,incomplete,null);
    }
    private Candidate(String text,boolean literal,double score,String reading,int consumed,boolean supplemental,boolean abbreviated,boolean composed,boolean incomplete,PairedForms.Pair pair) {
        this(text,literal,score,reading,consumed,supplemental,abbreviated,composed,incomplete,pair,"");
    }
    private Candidate(String text,boolean literal,double score,String reading,int consumed,boolean supplemental,boolean abbreviated,boolean composed,boolean incomplete,PairedForms.Pair pair,String pack) {
        this(text,literal,score,reading,consumed,supplemental,abbreviated,composed,incomplete,pair,pack,false);
    }
    private Candidate(String text,boolean literal,double score,String reading,int consumed,boolean supplemental,boolean abbreviated,boolean composed,boolean incomplete,PairedForms.Pair pair,String pack,boolean languageCharacter) {
        this.text=text;this.literal=literal;this.score=score;this.reading=reading;this.consumed=consumed;this.supplemental=supplemental;this.abbreviated=abbreviated;this.composed=composed;
        this.incomplete=incomplete;this.pair=pair;this.pack=pack;this.languageCharacter=languageCharacter;
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
        return new Candidate(prefix.text+word.text,false,prefix.score+word.score,"",0,false,false,prefix.composed || word.composed || !prefix.text.isEmpty(),prefix.incomplete || word.incomplete);
    }
    Candidate withScore(double value) {return new Candidate(text,literal,value,reading,consumed,supplemental,abbreviated,composed,incomplete,pair,pack,languageCharacter);}
    Candidate completing(double value) {return new Candidate(text,literal,value,reading,consumed,supplemental,abbreviated,composed,true,pair,pack,languageCharacter);}
    Candidate paired(PairedForms.Pair value) {return pair==value?this:new Candidate(text,literal,score,reading,consumed,supplemental,abbreviated,composed,incomplete,value,pack,languageCharacter);}
    Candidate inPack(String value) {return new Candidate(text,literal,score,reading,consumed,supplemental,abbreviated,composed,incomplete,pair,value,languageCharacter);}
    Candidate asLanguageCharacter() {return new Candidate(text,literal,score,reading,consumed,supplemental,abbreviated,composed,incomplete,pair,pack,true);}
    Candidate consuming(int count) {return new Candidate(text,literal,score,reading,count,supplemental,abbreviated,composed,incomplete,pair,pack,languageCharacter);}
    Candidate primary(boolean han) {return pair==null?this:pairedText(han?pair.han:pair.phonetic);}
    private Candidate pairedText(String value) {return text.equals(value)?this:new Candidate(value,literal,score,reading,consumed,supplemental,abbreviated,composed,incomplete,pair,pack,languageCharacter);}
    public String alternateText() {return pair==null?"":text.equals(pair.phonetic)?pair.han:pair.phonetic;}
    Candidate alternative() {return pair==null?this:pairedText(alternateText());}
    @Override public String toString() { return text; }
}
