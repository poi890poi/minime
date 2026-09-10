package dev.minime.ime;

import android.test.AndroidTestCase;
import dev.minime.core.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("deprecation")
public final class AddonRepositoryTest extends AndroidTestCase {
    public void testIndependentColdLoadsWarmReuseAndDisable()throws Exception {
        AddonRepository.retainEnabled(Collections.emptySet());
        for(String pack:Arrays.asList("taiwan","japanese","poj")) {
            Set<String> scope=Collections.singleton(pack);AddonRepository.retainEnabled(scope);
            long start=System.nanoTime();AddonDictionary cold=AddonRepository.load(getContext(),scope).get(60,TimeUnit.SECONDS);
            long coldNs=System.nanoTime()-start;assertEquals(scope,AddonRepository.cachedPacks());
            start=System.nanoTime();AddonDictionary warm=AddonRepository.load(getContext(),scope).get(1,TimeUnit.SECONDS);
            long warmNs=System.nanoTime()-start;assertSame(cold,warm);
            String[] probe=AddonTestData.probe(getContext(),pack);
            assertFalse(cold.lookup(probe[0],scope).isEmpty());
            for(String excluded:Arrays.asList("taiwan","japanese","poj"))if(!excluded.equals(pack))
                assertTrue(cold.lookup(probe[0],Collections.singleton(excluded)).isEmpty());
            android.util.Log.i("MinIMEPackLoading","pack="+pack+" cold_ms="+coldNs/1e6+" warm_ms="+warmNs/1e6);
        }
        AddonRepository.retainEnabled(Collections.emptySet());assertTrue(AddonRepository.cachedPacks().isEmpty());
        assertSame(AddonDictionary.EMPTY,AddonRepository.load(getContext(),Collections.emptySet()).get());
    }
}
