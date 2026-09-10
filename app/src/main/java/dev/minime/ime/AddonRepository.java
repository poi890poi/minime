package dev.minime.ime;

import android.content.*;
import dev.minime.core.AddonDictionary;
import dev.minime.core.PairedForms;
import dev.minime.core.JapaneseBasics;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;

/** Optional immutable assets load once, off the input thread. */
final class AddonRepository {
    private static final Map<String,CompletableFuture<AddonDictionary>> cache=new HashMap<>();
    /** Compatibility for whole-corpus diagnostics; production requests active packs. */
    static CompletableFuture<AddonDictionary> load(Context context) {
        return load(context,new HashSet<>(Arrays.asList("taiwan","poj","japanese")));
    }
    static CompletableFuture<AddonDictionary> load(Context context,Set<String> requested) {
        List<CompletableFuture<AddonDictionary>> parts=new ArrayList<>();
        for(String pack:new TreeSet<>(requested))parts.add(pack(context,pack));
        return CompletableFuture.allOf(parts.toArray(new CompletableFuture<?>[0])).thenApply(ignored->{
            AddonDictionary result=AddonDictionary.EMPTY;
            for(CompletableFuture<AddonDictionary> part:parts)result=AddonDictionary.combine(result,part.join());
            return result;
        });
    }
    static synchronized void retainEnabled(Set<String> enabled) {cache.keySet().retainAll(enabled);}
    static synchronized Set<String> cachedPacks() {return new HashSet<>(cache.keySet());}
    private static synchronized CompletableFuture<AddonDictionary> pack(Context context,String pack) {
        if(!Arrays.asList("taiwan","poj","japanese","geography").contains(pack))throw new IllegalArgumentException("Unknown dictionary pack");
        CompletableFuture<AddonDictionary> future=cache.get(pack);
        if(future==null || future.isCompletedExceptionally()) {
            Context app=context.getApplicationContext();
            future=CompletableFuture.supplyAsync(()-> {
                PairedForms pairs=PairedForms.EMPTY;
                try {if(pack.equals("poj"))pairs=PairedForms.read(new InputStreamReader(app.getAssets().open("paired-forms.tsv"),StandardCharsets.UTF_8));}
                catch(IOException e) {android.util.Log.w("MinIME","Paired forms unavailable",e);}
                try {
                    String asset=pack.equals("geography")?"geography.tsv":"addon-"+pack+".tsv";
                    AddonDictionary words=AddonDictionary.read(new InputStreamReader(app.getAssets().open(asset),StandardCharsets.UTF_8),pairs);
                    if(!pack.equals("japanese"))return words;
                    try {return AddonDictionary.withJapaneseBasics(words,JapaneseBasics.read(new InputStreamReader(app.getAssets().open("japanese-basic.tsv"),StandardCharsets.UTF_8)));}
                    catch(IOException e) {android.util.Log.w("MinIME","Japanese characters unavailable",e);return words;}
                }
                catch(IOException e) {throw new CompletionException(e);}
            });
            cache.put(pack,future);
        }
        return future;
    }
    static CompletableFuture<AddonDictionary> geography(Context context) {return pack(context,"geography");}
    /** Source inspection streams rows without building language indexes. */
    static InputStream openWords(Context context)throws IOException {
        List<InputStream> streams=new ArrayList<>();
        try {
            for(String pack:Arrays.asList("taiwan","poj","japanese"))streams.add(context.getAssets().open("addon-"+pack+".tsv"));
            return new SequenceInputStream(Collections.enumeration(streams));
        } catch(IOException failure) {for(InputStream stream:streams)try {stream.close();}catch(IOException ignored){}throw failure;}
    }
    static Set<String> enabled(Context context) {
        SharedPreferences settings=context.getSharedPreferences("settings",Context.MODE_PRIVATE);
        Set<String> enabled=new HashSet<>();
        for(String pack:Arrays.asList("taiwan","japanese","poj","geography"))if(settings.getBoolean("addon_"+pack,false))enabled.add(pack);
        return enabled;
    }
}
