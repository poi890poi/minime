package dev.minime.core;

import dev.minime.testing.LanguageTimingInputs;
import java.nio.file.*;
import java.util.*;
import static dev.minime.core.Regression.*;

final class LanguageTimingRegression {
    static void run()throws Exception {
        Path asset=Paths.get("app/src/androidTest/assets/language-timing-inputs.tsv");
        yes(Arrays.equals(Files.readAllBytes(asset),Files.readAllBytes(Paths.get("docs/release-hardening/language-timing/inputs.tsv"))),"device corpus is byte-identical to frozen corpus");
        List<LanguageTimingInputs.Query> all=LanguageTimingInputs.read(Files.newInputStream(asset));
        equal(720,all.size(),"frozen input count");Set<String> visited=new HashSet<>();
        for(String mode:Arrays.asList("chinese","english","taiwanese_english","japanese_english")) {
            for(int shard=0;shard<LanguageTimingInputs.SHARDS;shard++) {
                List<LanguageTimingInputs.Query> slice=LanguageTimingInputs.shard(all,mode,shard);Map<String,Integer> counts=new HashMap<>();
                for(LanguageTimingInputs.Query q:slice) {
                    yes(visited.add(q.id),"each query occurs in exactly one shard");equal(mode,q.mode,"no cross-mode timing inputs");
                    counts.merge(q.source+"/"+q.condition,1,Integer::sum);
                }
                for(int n:counts.values())equal(4,n,"each complete 16-input stratum contributes four per shard");
            }
        }
        equal(all.size(),visited.size(),"all frozen queries reachable");
        for(int invalid:new int[]{-1,4}) {
            boolean failed=false;try{LanguageTimingInputs.shard(all,"chinese",invalid);}catch(IllegalArgumentException expected){failed=true;}
            yes(failed,"invalid shard fails before replay");
        }
        System.out.println("PASS labelled timing corpus: 720 unique queries partitioned into 16 mode-specific shards");
    }
}
