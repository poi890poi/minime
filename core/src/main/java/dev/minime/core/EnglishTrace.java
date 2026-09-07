package dev.minime.core;

import java.util.*;

/** Geometric QWERTY templates from the attributed English lexicon. */
final class EnglishTrace {
    private static final String[] ROWS={"qwertyuiop","asdfghjkl","zxcvbnm"};
    private static final float[][] KEYS=new float[26][];
    private static final float[] OUTSIDE={-10,-10};
    static {for(int row=0;row<3;row++)for(int col=0;col<ROWS[row].length();col++)KEYS[ROWS[row].charAt(col)-'a']=new float[]{col+(row==0?.5f:row==1?1f:2f),row};}
    static float[] point(char c) {
        return c>='a' && c<='z'?KEYS[c-'a']:OUTSIDE;
    }
    private static float distance(float x,float y,float a,float b) {return (float)Math.hypot(x-a,y-b);}
    static float[] sample(float[] points) {
        float[] out=new float[64],length=new float[points.length/2];
        for(int i=1;i<length.length;i++)length[i]=length[i-1]+distance(points[i*2],points[i*2+1],points[i*2-2],points[i*2-1]);
        int segment=1;
        for(int i=0;i<32;i++) {
            float target=length[length.length-1]*i/31;
            while(segment<length.length-1 && length[segment]<target)segment++;
            float span=length[segment]-length[segment-1],t=span==0?0:(target-length[segment-1])/span;
            out[i*2]=points[(segment-1)*2]+t*(points[segment*2]-points[(segment-1)*2]);
            out[i*2+1]=points[(segment-1)*2+1]+t*(points[segment*2+1]-points[(segment-1)*2+1]);
        }
        return out;
    }
    static List<Candidate> decode(Map<String,Integer> words,float[] points) {
        if(points.length<4 || points.length>1024)return Collections.emptyList();
        for(float p:points)if(!Float.isFinite(p))return Collections.emptyList();
        float[] path=sample(points);List<Candidate> result=new ArrayList<>();
        for(Map.Entry<String,Integer> entry:words.entrySet()) {
            String word=entry.getKey();if(word.length()<2 || word.length()>24)continue;
            float[] first=point(word.charAt(0)),last=point(word.charAt(word.length()-1));
            if(distance(path[0],path[1],first[0],first[1])>.8 || distance(path[62],path[63],last[0],last[1])>.8)continue;
            boolean letters=true;for(int i=0;i<word.length();i++)if(word.charAt(i)<'a' || word.charAt(i)>'z'){letters=false;break;}
            if(!letters)continue;
            float[] template=new float[word.length()*2];for(int i=0;i<word.length();i++) {float[] p=point(word.charAt(i));template[i*2]=p[0];template[i*2+1]=p[1];}
            float[] shape=sample(template);double error=0;
            for(int i=0;i<32;i++)error+=Math.pow(path[i*2]-shape[i*2],2)+Math.pow(path[i*2+1]-shape[i*2+1],2);
            error/=32;
            if(error<1.2)result.add(new Candidate(word,true,-error+.0008*entry.getValue()));
        }
        result.sort(Comparator.comparingDouble((Candidate c)->c.score).reversed().thenComparing(c->c.text));
        return result.subList(0,Math.min(8,result.size()));
    }
}
