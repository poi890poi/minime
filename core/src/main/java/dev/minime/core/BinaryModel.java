package dev.minime.core;

import java.io.*;
import java.util.*;

/** Versioned data-only model format; shared strings, candidates and lists retain identity. */
final class BinaryModel {
    static final class Writer {
        final DataOutputStream out;
        private final Map<String,Integer> strings=new HashMap<>();
        private final IdentityHashMap<Candidate,Integer> candidates=new IdentityHashMap<>();
        private final IdentityHashMap<List<Candidate>,Integer> lists=new IdentityHashMap<>();
        private final boolean supplemental;
        Writer(DataOutputStream out) {this(out,false);}
        Writer(DataOutputStream out,boolean supplemental) {this.out=out;this.supplemental=supplemental;}
        void string(String value)throws IOException {
            if(value==null){out.writeInt(0);return;}Integer id=strings.get(value);
            if(id!=null){out.writeInt(id);return;}id=strings.size()+1;strings.put(value,id);out.writeInt(-id);out.writeUTF(value);
        }
        void candidate(Candidate value)throws IOException {
            Integer id=candidates.get(value);if(id!=null){out.writeInt(id);return;}
            id=candidates.size()+1;candidates.put(value,id);out.writeInt(-id);
            string(value.text);out.writeBoolean(value.literal);out.writeDouble(value.score);string(value.reading);
            if(supplemental) {
                if(!value.supplemental || value.literal || value.consumed!=0 || value.composed || value.languageCharacter
                        || value.incomplete!=value.abbreviated || !value.reading.isEmpty())throw new IOException("Unsupported indexed supplemental candidate");
                out.writeBoolean(value.abbreviated);string(value.pack);out.writeBoolean(value.pair!=null);
                if(value.pair!=null) {string(value.pair.phonetic);string(value.pair.han);string(value.pair.source);}
            }
        }
        void list(List<Candidate> value)throws IOException {
            if(value==null){out.writeInt(0);return;}Integer id=lists.get(value);if(id!=null){out.writeInt(id);return;}
            id=lists.size()+1;lists.put(value,id);out.writeInt(-id);out.writeInt(value.size());for(Candidate c:value)candidate(c);
        }
        void words(Map<String,List<Candidate>> map)throws IOException {out.writeInt(map.size());for(String key:new TreeSet<>(map.keySet())){string(key);list(map.get(key));}}
        void counts(Map<String,Integer> map)throws IOException {out.writeInt(map.size());for(String key:new TreeSet<>(map.keySet())){string(key);out.writeInt(map.get(key));}}
        void strings(String[] values)throws IOException {out.writeInt(values.length);for(String value:values)string(value);}
        void ints(int[] values)throws IOException {out.writeInt(values.length);for(int value:values)out.writeInt(value);}
        void doubles(double[] values)throws IOException {out.writeInt(values.length);for(double value:values)out.writeDouble(value);}
        void lists(List<Candidate>[] values)throws IOException {out.writeInt(values.length);for(List<Candidate> value:values)list(value);}
    }
    static final class Reader {
        final DataInputStream in;
        private final List<String> strings=new ArrayList<>();
        private final List<Candidate> candidates=new ArrayList<>();
        private final List<List<Candidate>> lists=new ArrayList<>();
        private final boolean supplemental;
        Reader(DataInputStream in) {this(in,false);}
        Reader(DataInputStream in,boolean supplemental) {this.in=in;this.supplemental=supplemental;}
        int size()throws IOException {int n=in.readInt();if(n<0 || n>4000000)throw new IOException("Invalid model size");return n;}
        String string()throws IOException {int id=in.readInt();if(id==0)return null;if(id>0)return strings.get(id-1);String value=in.readUTF();strings.add(value);return value;}
        Candidate candidate()throws IOException {
            int id=in.readInt();if(id>0)return candidates.get(id-1);
            Candidate c=new Candidate(string(),in.readBoolean(),in.readDouble(),string());
            if(supplemental) {
                if(c.literal || !c.reading.isEmpty())throw new IOException("Invalid supplemental candidate");
                c=Candidate.supplement(c.text,c.score,in.readBoolean()).inPack(string());
                if(in.readBoolean())c=c.paired(new PairedForms.Pair(string(),string(),string()));
            }
            candidates.add(c);return c;
        }
        List<Candidate> list()throws IOException {
            int id=in.readInt();if(id==0)return null;if(id>0)return lists.get(id-1);
            int n=size();List<Candidate> value=new ArrayList<>(n);lists.add(value);for(int i=0;i<n;i++)value.add(candidate());return value;
        }
        void words(Map<String,List<Candidate>> map)throws IOException {int n=size();for(int i=0;i<n;i++)map.put(string(),list());}
        void counts(Map<String,Integer> map)throws IOException {int n=size();for(int i=0;i<n;i++)map.put(string(),in.readInt());}
        String[] strings()throws IOException {String[] values=new String[size()];for(int i=0;i<values.length;i++)values[i]=string();return values;}
        int[] ints()throws IOException {int[] values=new int[size()];for(int i=0;i<values.length;i++)values[i]=in.readInt();return values;}
        double[] doubles()throws IOException {double[] values=new double[size()];for(int i=0;i<values.length;i++)values[i]=in.readDouble();return values;}
        @SuppressWarnings("unchecked") List<Candidate>[] lists()throws IOException {List<Candidate>[] values=(List<Candidate>[])new List<?>[size()];for(int i=0;i<values.length;i++)values[i]=list();return values;}
    }
}
