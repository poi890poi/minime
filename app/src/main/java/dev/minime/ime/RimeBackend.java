package dev.minime.ime;

import android.content.Context;
import dev.minime.core.Candidate;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

/** Pinned offline Rime model. Sessions never learn or receive editor surroundings. */
final class RimeBackend {
    private static CompletableFuture<Boolean> loading;
    private static volatile boolean available;
    static synchronized CompletableFuture<Boolean> load(Context context) {
        if(loading==null) {
            Context app=context.getApplicationContext();
            loading=CompletableFuture.supplyAsync(()-> {
                try {
                    System.loadLibrary("minime_rime");
                    String version;
                    try(InputStream in=app.getAssets().open("rime/bundle-id.txt")) {
                        ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buffer=new byte[128];int n;
                        while((n=in.read(buffer))!=-1)out.write(buffer,0,n);
                        version=out.toString("UTF-8").trim();
                    }
                    if(!version.matches("[a-f0-9]{64}"))throw new IOException("Invalid model identity");
                    File base=new File(app.getNoBackupFilesDir(),"rime/"+version);
                    File shared=new File(base,"shared"),user=new File(base,"user");
                    if(!shared.isDirectory() && !shared.mkdirs())throw new IOException("Cannot prepare model");
                    if(!user.isDirectory() && !user.mkdirs())throw new IOException("Cannot prepare session directory");
                    for(String name:app.getAssets().list("rime")) {
                        File target=new File(shared,name);
                        if(target.isFile())continue;
                        File temporary=new File(shared,name+".tmp");
                        try(InputStream in=app.getAssets().open("rime/"+name);OutputStream out=new FileOutputStream(temporary)) {
                            byte[] buffer=new byte[32768];int n;while((n=in.read(buffer))!=-1)out.write(buffer,0,n);
                        }
                        if(!temporary.renameTo(target))throw new IOException("Cannot install model");
                    }
                    available=initialize(shared.getAbsolutePath(),user.getAbsolutePath());
                } catch(IOException | UnsatisfiedLinkError | SecurityException e) { available=false; }
                return available;
            });
        }
        return loading;
    }
    static List<Candidate> convert(String raw) {
        if(!available || !raw.matches("[a-zv]+(?:'[a-zv]+)*") || raw.length()>96)return Collections.emptyList();
        String[] words=query(raw);List<Candidate> result=new ArrayList<>();
        for(int i=0;i<words.length;i++)result.add(new Candidate(words[i],false,100-i));
        return result;
    }
    private static native boolean initialize(String shared,String user);
    private static native String[] query(String raw);
}
