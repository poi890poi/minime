package dev.minime.ime;

import android.content.Context;
import dev.minime.core.JapaneseConversion;
import java.io.*;
import java.util.*;

/** Only compiled with -PjapaneseEvaluation=true. No model in the ordinary APK. */
final class JapaneseNativeTestProvider implements JapaneseConversion.Provider,AutoCloseable {
    private long handle;
    final long loadNs;
    JapaneseNativeTestProvider(Context tests,Context app)throws IOException {
        File directory=new File(app.getCacheDir(),"japanese-evaluation");if(!directory.isDirectory() && !directory.mkdirs())throw new IOException("Cannot create test model directory");
        for(String name:new String[]{"libminime_japanese_test.so","yomi_termid.louds","tango.louds","token_array.bin","pos_table.bin","connection_single_column.bin"}) {
            File file=new File(directory,name);
            if(file.exists() && !file.setWritable(true))throw new IOException("Cannot refresh test asset");
            try(InputStream in=tests.getAssets().open("japanese-evaluation/"+name);OutputStream out=new FileOutputStream(file)) {
                byte[] buffer=new byte[65536];int n;while((n=in.read(buffer))>=0)out.write(buffer,0,n);
            }
        }
        File library=new File(directory,"libminime_japanese_test.so");library.setReadOnly();System.load(library.getAbsolutePath());
        long at=System.nanoTime();handle=create(directory.getAbsolutePath());loadNs=System.nanoTime()-at;
        if(handle==0)throw new IOException("Native model unavailable");
    }
    public synchronized List<String> convert(String kana) {if(handle==0)throw new IllegalStateException("Closed provider");return Arrays.asList(query(handle,kana));}
    synchronized long nativeNs(){return elapsed(handle);}
    public synchronized void close(){if(handle!=0){destroy(handle);handle=0;}}
    private static native long create(String directory);
    private static native String[] query(long handle,String kana);
    private static native long elapsed(long handle);
    private static native void destroy(long handle);
}
