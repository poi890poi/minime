package dev.minime.core;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.zip.GZIPInputStream;

/** New source-derived longer-input conditions; expected text never enters decoding. */
public final class PrefixAccessEvaluation {
    private static boolean prefix(Candidate c,String raw){return !c.literal&&c.consumed>0&&c.consumed<raw.length();}
    public static void main(String[] args)throws Exception {
        Path assets=Paths.get("app/src/main/assets");
        PhoneticDictionary d=PhoneticDictionary.load(Files.newBufferedReader(assets.resolve("zh_tw.tsv")),Files.newBufferedReader(assets.resolve("en_us.tsv")),Files.newBufferedReader(assets.resolve("syllables.tsv")),Files.newBufferedReader(assets.resolve("context.tsv")));
        d.englishSpelling(Files.newBufferedReader(assets.resolve("en_spelling.tsv")));
        Map<String,List<SuggestionCoverageAudit.Entry>> source=new HashMap<>();
        for(String line:Files.readAllLines(assets.resolve("zh_tw.tsv"))) {
            SuggestionCoverageAudit.Entry e=new SuggestionCoverageAudit.Entry(line.split("\t"));source.computeIfAbsent(e.text,k->new ArrayList<>()).add(e);
        }
        try(BufferedReader in=new BufferedReader(new InputStreamReader(new GZIPInputStream(Files.newInputStream(Paths.get(args[0]))),StandardCharsets.UTF_8));BufferedWriter out=Files.newBufferedWriter(Paths.get(args[1]))) {
            out.write(in.readLine()+"\tlookup_ns\tdecoder_rank\tcomposition_rank\tprefix_count\tspace\twhole_choices\tfirst_glyphs\tpreview\n");
            String line;int count=0,checked=0,selected=0;
            while((line=in.readLine())!=null) {
                String[] p=line.split("\t",-1);String raw=p[6],target=p[2],suffix=p[8];
                long at=System.nanoTime();List<Candidate> found=d.convert(raw,false);long ns=System.nanoTime()-at;
                int decoderRank=0,compositionRank=0,prefixCount=0;
                for(int i=0;i<found.size();i++) {
                    Candidate c=found.get(i);
                    if(c.composed)throw new AssertionError("Constructed output");
                    if(prefix(c,raw)&&c.text.codePointCount(0,c.text.length())>1) {
                        if(source.getOrDefault(c.text,Collections.emptyList()).stream().noneMatch(e->SuggestionCoverageAudit.matches(raw.substring(0,c.consumed),e.units)))throw new AssertionError("Invalid source span: "+raw+"/"+c.text);
                        prefixCount++;checked++;
                    }
                    if(prefix(c,raw)&&c.text.equals(target)&&raw.substring(c.consumed).replaceFirst("^'+","").equals(suffix))decoderRank=i+1;
                }
                ChineseRecoveryEvaluation.Editor editor=new ChineseRecoveryEvaluation.Editor();
                CompositionEngine engine=ChineseRecoveryEvaluation.session(editor,d,AddonDictionary.EMPTY,Collections.emptySet(),raw,found);
                List<String> whole=new ArrayList<>(),glyphs=new ArrayList<>(),preview=new ArrayList<>();Candidate chosen=null;
                for(int i=1;i<engine.candidates().size();i++) {
                    Candidate c=engine.candidates().get(i);
                    if(!prefix(c,raw))whole.add(c.text);
                    if(prefix(c,raw)&&c.text.codePointCount(0,c.text.length())==1&&glyphs.size()<2)glyphs.add(c.text+":"+c.consumed);
                    if(i<=8)preview.add(c.text+":"+c.consumed);
                    if(prefix(c,raw)&&c.text.equals(target)&&raw.substring(c.consumed).replaceFirst("^'+","").equals(suffix)){compositionRank=i;chosen=c;}
                }
                Candidate accepted=engine.candidates().get(engine.preferred());
                if(prefix(accepted,raw))throw new AssertionError("Prefix owns Space");
                String space=accepted.text;
                if(chosen!=null) {
                    long version=engine.compositionId();engine.selectCandidate(chosen,version);
                    if(!editor.text.equals(target)||!engine.raw().equals(suffix))throw new AssertionError("Prefix selection lost suffix");
                    engine.selectCandidate(chosen,version);
                    if(!editor.text.equals(target)||!engine.raw().equals(suffix))throw new AssertionError("Stale prefix accepted");
                    selected++;
                }
                out.write(line+"\t"+ns+"\t"+decoderRank+"\t"+compositionRank+"\t"+prefixCount+"\t"+space+"\t"+String.join("|",whole)+"\t"+String.join("|",glyphs)+"\t"+String.join("|",preview)+"\n");
                if(++count%5000==0){out.flush();System.out.println("Prefix episodes "+count);}
            }
            System.out.println("PASS "+count+" episodes, "+checked+" independently checked source spans, "+selected+" actual target-prefix selections; retrieval ranks are separate metrics.");
        }
    }
}
