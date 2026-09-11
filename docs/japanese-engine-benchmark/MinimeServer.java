package dev.minime.core;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Evaluation protocol. The server never receives reference outputs. */
public final class MinimeServer {
    static final Path ASSET=Paths.get("app/build/generated/minimeAssets");
    static final Base64.Encoder ENC=Base64.getEncoder();
    static class Editor implements CompositionEngine.Editor {
        String text="";
        public void composing(String s) {} public void commit(String s) {text+=s;}
        public void delete() {throw new AssertionError("Unexpected editor delete");}
        public void enter() {} public void finish() {}
    }
    static String b64(String s) {return ENC.encodeToString(s.getBytes(java.nio.charset.StandardCharsets.UTF_8));}
    static String decode(String s) {return new String(Base64.getDecoder().decode(s),java.nio.charset.StandardCharsets.UTF_8);}
    public static void main(String[] args) throws Exception {
        long at=System.nanoTime();
        PhoneticDictionary d=PhoneticDictionary.readBinary(Files.newInputStream(ASSET.resolve("model.bin")));
        d.englishSpelling(Files.newBufferedReader(ASSET.resolve("en_spelling.tsv")));
        AddonDictionary a=AddonDictionary.readBinary(Files.newInputStream(ASSET.resolve("addon-japanese.bin")));
        a=AddonDictionary.withJapaneseBasics(a,JapaneseBasics.read(Files.newBufferedReader(ASSET.resolve("japanese-basic.tsv"))));
        Editor editor=new Editor(); CompositionEngine c=new CompositionEngine(editor,Learning.NONE);
        c.dictionary(d); c.start(false,false,true,false,false); c.switchMode(InputMode.JAPANESE_ENGLISH,false);
        c.addons(a,Set.of("japanese")); c.englishOptions(true,false);
        System.out.println("READY\t"+(System.nanoTime()-at));
        BufferedReader in=new BufferedReader(new InputStreamReader(System.in,java.nio.charset.StandardCharsets.UTF_8));
        String line;
        while((line=in.readLine())!=null) {
            String[] p=line.split("\t",-1); List<Long> times=new ArrayList<>(); at=System.nanoTime();
            switch(p[0]) {
                case "R": c.start(false,false,true,false,false);c.switchMode(InputMode.JAPANESE_ENGLISH,false);editor.text="";break;
                case "T": for(int cp:decode(p[1]).codePoints().toArray()) {long key=System.nanoTime();c.type(cp);times.add(System.nanoTime()-key);}break;
                case "B": for(int i=0;i<Integer.parseInt(p[1]);i++)c.backspace();break;
                case "S": c.selectCandidate(c.candidates().get(Integer.parseInt(p[1])),c.compositionId());break;
                case "L": c.literal(decode(p[1]));break;
                case "V": break;
                default: throw new IllegalArgumentException(p[0]);
            }
            long elapsed=System.nanoTime()-at;
            StringBuilder out=new StringBuilder("OK\t"+elapsed+"\t"+b64(c.raw())+"\t"+b64(editor.text)+"\t"+c.preferred()+"\t");
            for(long t:times)out.append(t).append(',');
            for(Candidate v:c.candidates())out.append('\t').append(b64(v.text)).append(':').append(v.consumed);
            System.out.println(out);
        }
    }
}
