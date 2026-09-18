package dev.minime.core;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.zip.GZIPInputStream;

/** Broad stored-phrase and prefix-access audit. Targets never enter decoding. */
public final class ChineseRecoveryEvaluation {
    static final class Editor implements CompositionEngine.Editor {
        String text="",composing="";
        public void composing(String s){composing=s;}public void commit(String s){text+=s;composing="";}
        public void delete(){}public void enter(){}public void finish(){}
    }
    static boolean glyph(Candidate c){return !c.literal && c.text.codePointCount(0,c.text.length())==1;}
    static CompositionEngine session(Editor editor,PhoneticDictionary dictionary,AddonDictionary addons,Set<String> enabled,String raw,List<Candidate> found) {
        CompositionEngine c=new CompositionEngine(editor,Learning.NONE);c.dictionary(dictionary);c.start(false,false,false,false);c.addons(addons,enabled);
        List<Runnable> replies=new ArrayList<>();c.decoder((model,r,z,context,done)->replies.add(()->done.accept(found)),()->{});
        raw.codePoints().forEach(c::type);if(!replies.isEmpty())replies.get(replies.size()-1).run();return c;
    }
    public static void main(String[] args)throws Exception {
        Path assets=Paths.get("app/src/main/assets");
        PhoneticDictionary d=PhoneticDictionary.load(Files.newBufferedReader(args.length>4?Paths.get(args[4]):assets.resolve("zh_tw.tsv")),Files.newBufferedReader(assets.resolve("en_us.tsv")),Files.newBufferedReader(assets.resolve("syllables.tsv")),Files.newBufferedReader(assets.resolve("context.tsv")));
        d.englishSpelling(Files.newBufferedReader(assets.resolve("en_spelling.tsv")));
        boolean nativeMode=args.length>2&&args[2].equals("native"),packs=args.length>3&&args[3].equals("packs");
        AddonDictionary addons=packs?AddonDictionary.combine(AddonDictionary.read(Files.newBufferedReader(assets.resolve("addons.tsv"))),AddonDictionary.read(Files.newBufferedReader(assets.resolve("geography.tsv")))):AddonDictionary.EMPTY;
        Set<String> enabled=packs?new HashSet<>(Arrays.asList("taiwan","geography")):Collections.emptySet();
        Map<String,List<Candidate>> cache=new HashMap<>();Map<String,Long> timings=new HashMap<>();
        try(DesktopEvaluation.Native nativeRime=nativeMode?new DesktopEvaluation.Native("artifacts/desktop-rime.exe",".tools/rime-evaluation/msvc/dist/lib/rime.dll","app/src/main/rimeAssets/rime","artifacts/chinese-recovery-native-user"):null;
            BufferedReader in=new BufferedReader(new InputStreamReader(new GZIPInputStream(Files.newInputStream(Paths.get(args[0]))),StandardCharsets.UTF_8));BufferedWriter out=Files.newBufferedWriter(Paths.get(args[1]))) {
            out.write("genre\tid\tcondition\traw\ttarget\tglyphs\tcandidates\tfirst_glyph_rank\ttarget_rank\tspace\tfirst_consumed\tselect_ok\tlookup_ns\toutputs\n");in.readLine();String line;int count=0;
            while((line=in.readLine())!=null) {
                String[] p=line.split("\t",-1);String raw=p[3],target=p[4];
                if(!cache.containsKey(raw)) {long started=System.nanoTime();List<Candidate> values=d.convert(raw,false);if(nativeMode)values=CandidateMerge.merge(nativeRime.query(raw),values);cache.put(raw,values);timings.put(raw,System.nanoTime()-started);}
                List<Candidate> found=cache.get(raw);Editor editor=new Editor();CompositionEngine c=session(editor,d,addons,enabled,raw,found);
                int firstRank=0,targetRank=0,firstEnd=0;Candidate first=null;List<String> outputs=new ArrayList<>();
                for(int i=1;i<c.candidates().size();i++) {
                    Candidate v=c.candidates().get(i);outputs.add(v.text+":"+v.consumed);
                    if(v.composed)throw new AssertionError("Unattested construction: "+raw);
                    if(v.text.equals(target)&&v.consumed==0)targetRank=i;
                    if(firstRank==0 && glyph(v) && v.consumed>0 && v.consumed<raw.length()) {firstRank=i;firstEnd=v.consumed;first=v;}
                }
                String space=c.candidates().isEmpty()?raw:c.candidates().get(c.preferred()).text;
                if(!c.candidates().isEmpty() && c.candidates().get(c.preferred()).consumed>0 && c.candidates().get(c.preferred()).consumed<raw.length())throw new AssertionError("Partial Space: "+raw);
                Candidate accepted=c.candidates().get(c.preferred());c.space();
                if(!editor.text.equals(accepted.text+(accepted.literal?" ":"")) || !c.raw().isEmpty() || !editor.composing.isEmpty())throw new AssertionError("Actual Space differs from displayed default: "+raw);
                boolean valid=true;
                if(first!=null) {
                    editor=new Editor();c=session(editor,d,addons,enabled,raw,found);
                    c.selectCandidate(first,c.compositionId());String suffix=raw.substring(first.consumed).replaceFirst("^'+","");
                    valid=editor.text.equals(first.text)&&c.raw().equals(suffix)&&editor.composing.equals(suffix);
                    if(!valid)throw new AssertionError("Lost raw suffix: "+raw);
                }
                out.write(line+"\t"+outputs.size()+"\t"+firstRank+"\t"+targetRank+"\t"+space+"\t"+firstEnd+"\t"+valid+"\t"+timings.get(raw)+"\t"+String.join("|",outputs)+"\n");
                if(++count%5000==0) {out.flush();System.out.println("Recovery episodes "+count+" unique queries "+cache.size());}
            }
        }
    }
}
