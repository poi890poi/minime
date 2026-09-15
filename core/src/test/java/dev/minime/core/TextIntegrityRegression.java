package dev.minime.core;

import dev.minime.testing.TextIntegrity;
import java.io.*;
import java.nio.charset.*;
import java.nio.file.*;
import java.util.*;
import static dev.minime.core.Regression.*;

/** Encoding contracts, not vocabulary accuracy or a font-coverage claim. */
final class TextIntegrityRegression {
    static void run() throws Exception {
        for(String broken:Arrays.asList("\ud800","\udc00","a\ud800b","\ufffd","\uffff","\ufdd0","\u0000","\u0080"))
            yes(!TextIntegrity.problems(broken).isEmpty(),"Text sentinel rejects "+TextIntegrity.codePoints(broken));
        for(byte[] broken:new byte[][]{{(byte)0xc0,(byte)0xaf},{(byte)0xe4,(byte)0xb8},{(byte)0xed,(byte)0xa0,(byte)0x80},{(byte)0xff}}) {
            boolean rejected=false;
            try(Reader reader=TextIntegrity.utf8(new ByteArrayInputStream(broken))) {while(reader.read()!=-1){}}
            catch(CharacterCodingException expected) {rejected=true;}
            yes(rejected,"Malformed UTF-8 must fail instead of substituting text");
        }
        // Supplementary Han, kana, decomposed POJ, emoji and variation selectors
        // are legal; rejecting non-BMP or combining text would hide the defect.
        List<String> texts=new ArrayList<>(Arrays.asList("台灣","かなカナ漢字","chhiu\u0301 o\u0358","\ud840\udc00","\ud83d\udc69\u200d\ud83d\udcbb","\u2764\ufe0f"));
        for(int cp:new int[]{0x7e,0xa0,0x7ff,0x800,0xd7ff,0xe000,0xfffd-1,0x10000,0x20000,0x2ffff-2,0x10fffd})texts.add(new String(Character.toChars(cp)));
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();
        BinaryModel.Writer writer=new BinaryModel.Writer(new DataOutputStream(bytes));
        for(String text:texts) {
            TextIntegrity.require(text,"valid fixture");writer.string(text);
            try(BufferedReader reader=new BufferedReader(TextIntegrity.utf8(new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8))))) {
                equal(text,reader.readLine(),"Standard UTF-8 round trip");
            }
            equal(text,NativeCandidateCodec.decode("abc","3\tL\t"+text,0).text,"Native record retains exact code points");
        }
        BinaryModel.Reader reader=new BinaryModel.Reader(new DataInputStream(new ByteArrayInputStream(bytes.toByteArray())));
        for(String text:texts)equal(text,reader.string(),"Binary model preserves UTF-16 including supplementary and combining text");
        int files=0;long lines=0;
        for(Path root:Arrays.asList(Paths.get("app/src/main/assets"),Paths.get("docs/play-publishing"))) {
            try(java.util.stream.Stream<Path> paths=Files.walk(root)) {
                for(Path path:(Iterable<Path>)paths.filter(Files::isRegularFile).filter(p->p.toString().endsWith(".tsv") || p.toString().endsWith(".txt"))::iterator) {
                    try(BufferedReader in=new BufferedReader(TextIntegrity.utf8(Files.newInputStream(path)))) {
                        String line;int number=0;
                        while((line=in.readLine())!=null) {number++;TextIntegrity.require(line,path+":"+number);}
                        lines+=number;files++;
                    }
                }
            }
        }
        System.out.println("PASS strict UTF-8 and Unicode audit: "+files+" source/note files, "+lines+" lines; font support is checked separately on Android");
    }
}
