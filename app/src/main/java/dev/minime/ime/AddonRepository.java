package dev.minime.ime;

import android.content.*;
import dev.minime.core.AddonDictionary;
import dev.minime.core.PairedForms;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;

/** Optional immutable assets load once, off the input thread. */
final class AddonRepository {
    private static CompletableFuture<AddonDictionary> future,geography;
    static synchronized CompletableFuture<AddonDictionary> load(Context context) {
        if(future==null) {
            Context app=context.getApplicationContext();
            future=CompletableFuture.supplyAsync(()-> {
                PairedForms pairs=PairedForms.EMPTY;
                try {pairs=PairedForms.read(new InputStreamReader(app.getAssets().open("paired-forms.tsv"),StandardCharsets.UTF_8));}
                catch(IOException e) {android.util.Log.w("MinIME","Paired forms unavailable",e);}
                try {return AddonDictionary.read(new InputStreamReader(app.getAssets().open("addons.tsv"),StandardCharsets.UTF_8),pairs);}
                catch(IOException e) {throw new CompletionException(e);}
            });
        }
        return future;
    }
    static synchronized CompletableFuture<AddonDictionary> geography(Context context) {
        if(geography==null) {
            Context app=context.getApplicationContext();
            geography=CompletableFuture.supplyAsync(()-> {
                try {return AddonDictionary.read(new InputStreamReader(app.getAssets().open("geography.tsv"),StandardCharsets.UTF_8));}
                catch(IOException e) {throw new CompletionException(e);}
            });
        }return geography;
    }
    static Set<String> enabled(Context context) {
        SharedPreferences settings=context.getSharedPreferences("settings",Context.MODE_PRIVATE);
        Set<String> enabled=new HashSet<>();
        for(String pack:Arrays.asList("taiwan","japanese","poj","geography"))if(settings.getBoolean("addon_"+pack,false))enabled.add(pack);
        return enabled;
    }
}
