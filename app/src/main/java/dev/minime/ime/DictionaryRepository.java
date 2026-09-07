package dev.minime.ime;

import android.content.Context;
import dev.minime.core.PhoneticDictionary;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;

final class DictionaryRepository {
    private static CompletableFuture<PhoneticDictionary> future;
    static synchronized CompletableFuture<PhoneticDictionary> load(Context context) {
        if(future==null) {
            Context app=context.getApplicationContext();
            future=CompletableFuture.supplyAsync(() -> {
                try { return PhoneticDictionary.readBinary(app.getAssets().open("model.bin")); }
                catch(IOException e) { throw new CompletionException(e); }
            });
        }
        return future;
    }
    private static Reader reader(Context c,String name) throws IOException { return new InputStreamReader(c.getAssets().open(name),StandardCharsets.UTF_8); }
}
