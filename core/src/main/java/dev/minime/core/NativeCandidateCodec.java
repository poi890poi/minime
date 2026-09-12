package dev.minime.core;

/** Native transport is provenance-bearing; rank scores are not confidence. */
public final class NativeCandidateCodec {
    private NativeCandidateCodec() {}
    public static Candidate decode(String raw,String record,int rank) {
        int first=record.indexOf('\t'),second=record.indexOf('\t',first+1);
        if(first<1 || second!=first+2 || second==record.length()-1)return null;
        char kind=record.charAt(first+1);
        if(kind!='S' && kind!='L')return null;
        try {
            int end=Integer.parseInt(record.substring(0,first));
            if(end<=0 || end>raw.length())return null;
            Candidate result=new Candidate(record.substring(second+1),false,100-rank,end==raw.length()?0:end);
            return kind=='S'?result.asConstructed():result;
        } catch(NumberFormatException e) {return null;}
    }
}
