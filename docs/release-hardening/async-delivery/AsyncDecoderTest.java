package dev.minime.ime;

import android.os.*;
import android.test.InstrumentationTestCase;
import dev.minime.core.*;
import java.io.StringReader;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@SuppressWarnings("deprecation")
public final class AsyncDecoderTest extends InstrumentationTestCase {
    private static final class DeliveryHandler extends Handler {
        final CountDownLatch enqueued=new CountDownLatch(1);
        volatile boolean asynchronous;
        DeliveryHandler(){super(Looper.getMainLooper());}
        @Override public boolean sendMessageAtTime(Message message,long uptimeMillis) {
            asynchronous=message.isAsynchronous();
            boolean sent=super.sendMessageAtTime(message,uptimeMillis);
            if(sent)enqueued.countDown();
            return sent;
        }
    }
    public void testResultIsAsynchronousOnOriginalMainLooper()throws Exception {
        DeliveryHandler handler=new DeliveryHandler();AsyncDecoder decoder=new AsyncDecoder(handler);
        CountDownLatch delivered=new CountDownLatch(1);AtomicInteger mainDeliveries=new AtomicInteger();
        try {
            getInstrumentation().runOnMainSync(()->decoder.query(null,"fixture",false,"",false,
                AddonDictionary.EMPTY,Collections.emptySet(),r->{
                    if(Looper.myLooper()==handler.getLooper())mainDeliveries.incrementAndGet();
                    delivered.countDown();
                }));
            assertTrue(delivered.await(5,TimeUnit.SECONDS));
            assertTrue(handler.asynchronous);assertEquals(1,mainDeliveries.get());
        } finally {getInstrumentation().runOnMainSync(decoder::close);}
    }
    public void testCloseRejectsAlreadyEnqueuedResult()throws Exception {
        DeliveryHandler handler=new DeliveryHandler();AsyncDecoder decoder=new AsyncDecoder(handler);
        AtomicInteger delivered=new AtomicInteger();
        try {
            getInstrumentation().runOnMainSync(()->{
                decoder.query(null,"fixture",false,"",false,AddonDictionary.EMPTY,
                    Collections.emptySet(),r->delivered.incrementAndGet());
                try {assertTrue(handler.enqueued.await(3,TimeUnit.SECONDS));}
                catch(InterruptedException failure){throw new AssertionError(failure);}
                decoder.close();
            });
            getInstrumentation().waitForIdleSync();
            assertEquals(0,delivered.get());assertEquals(1L,decoder.stats.staleDelivery.get());
        } finally {getInstrumentation().runOnMainSync(decoder::close);}
    }
    public void testCancellationBetweenWorkerAndMainDelivery()throws Exception {
        AddonDictionary words=AddonDictionary.read(new StringReader("japanese\tzavora\tかな\ttest\ttest\n"));
        AsyncDecoder decoder=new AsyncDecoder(new Handler(Looper.getMainLooper()));
        AtomicInteger obsolete=new AtomicInteger();CountDownLatch delivered=new CountDownLatch(1);
        try {
            getInstrumentation().runOnMainSync(()-> {
                decoder.query(null,"zavora",false,"",false,words,Collections.singleton("japanese"),r->obsolete.incrementAndGet());
                long deadline=SystemClock.uptimeMillis()+3000;
                // Hold main delivery until calculation has crossed the add-on stage.
                while(decoder.stats.calls[2].get()==0 && SystemClock.uptimeMillis()<deadline)SystemClock.sleep(1);
                assertEquals(1L,decoder.stats.calls[2].get());decoder.cancel();
                decoder.query(null,"zavora",false,"",false,words,Collections.singleton("japanese"),r->{
                    assertEquals("かな",r.get(0).text);delivered.countDown();
                });
            });
            assertTrue(delivered.await(5,TimeUnit.SECONDS));getInstrumentation().waitForIdleSync();
            assertEquals(0,obsolete.get());assertEquals(1L,decoder.stats.delivered.get());
        } finally {getInstrumentation().runOnMainSync(decoder::close);}
    }
    public void testBurstKeepsLatestAndCloseRejectsNewWork()throws Exception {
        AsyncDecoder decoder=new AsyncDecoder(new Handler(Looper.getMainLooper()));
        CountDownLatch delivered=new CountDownLatch(1);AtomicInteger stale=new AtomicInteger();
        try {
            getInstrumentation().runOnMainSync(()-> {
                for(int i=0;i<100;i++)decoder.query(null,"fixture",false,"",false,AddonDictionary.EMPTY,Collections.emptySet(),r->stale.incrementAndGet());
                decoder.query(null,"last",false,"",false,AddonDictionary.EMPTY,Collections.emptySet(),r->delivered.countDown());
            });
            assertTrue(delivered.await(5,TimeUnit.SECONDS));getInstrumentation().waitForIdleSync();assertEquals(0,stale.get());
            assertEquals(101L,decoder.stats.requests.get());assertEquals(1L,decoder.stats.delivered.get());
        } finally {getInstrumentation().runOnMainSync(decoder::close);}
        getInstrumentation().runOnMainSync(()->decoder.query(null,"closed",false,"",false,AddonDictionary.EMPTY,Collections.emptySet(),r->stale.incrementAndGet()));
        assertEquals(101L,decoder.stats.requests.get());assertEquals(0,stale.get());
    }
}
