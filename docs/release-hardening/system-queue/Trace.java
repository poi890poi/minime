package android.os;
import java.util.*;
public final class Trace {
    static final Set<String> active=new HashSet<>();static int depth;
    public static boolean isEnabled(){return true;}
    public static void beginAsyncSection(String n,int id){if(!active.add(n+id))throw new AssertionError("duplicate");}
    public static void endAsyncSection(String n,int id){if(!active.remove(n+id))throw new AssertionError("unmatched");}
    public static void beginSection(String n){depth++;}
    public static void endSection(){if(--depth<0)throw new AssertionError("unmatched");}
    public static void assertIdle(){if(depth!=0 || !active.isEmpty())throw new AssertionError("unbalanced");}
}
