package me.joeycumines.javapromises.v1;

import me.joeycumines.javapromises.core.*;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.ArrayList;
import java.util.Vector;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.*;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class PromiseBaseTest {
    @Test
    public void testGetStateUnset() {
        PromiseBaseShell<Object> promise = new PromiseBaseShell<>();
        assertEquals(PromiseState.PENDING, promise.getState());
    }

    @Test
    public void testGetValueUnset() {
        PromiseBaseShell<Object> promise = new PromiseBaseShell<>();
        assertNull(promise.getValue());
    }

    @Test
    public void testGetExceptionUnset() {
        PromiseBaseShell<Object> promise = new PromiseBaseShell<>();
        assertNull(promise.getException());
    }

    @Test
    public void testFulfill() {
        PromiseBaseShell<Object> promise = new PromiseBaseShell<>();
        Object value = new Object();

        promise.fulfill(value);

        assertEquals(PromiseState.FULFILLED, promise.getState());
        assertEquals(value, promise.getValue());
        assertNull(promise.getException());
    }

    @Test
    public void testFulfillNull() {
        PromiseBaseShell<Object> promise = new PromiseBaseShell<>();

        promise.fulfill(null);

        assertEquals(PromiseState.FULFILLED, promise.getState());
        assertEquals(null, promise.getValue());
    }

    @Test
    public void testReject() {
        PromiseBaseShell<Object> promise = new PromiseBaseShell<>();
        Throwable value = new Throwable();

        promise.reject(value);

        assertEquals(PromiseState.REJECTED, promise.getState());
        assertEquals(value, promise.getException());
        assertNull(promise.getValue());
    }

    @Test
    public void testRejectNull() {
        PromiseBaseShell promise = new PromiseBaseShell();

        try {
            promise.reject(null);
            fail();
        } catch (NullPointerException e) {
            assertNotNull(e);
        }

        assertEquals(PromiseState.PENDING, promise.getState());
        assertNull(promise.getValue());
        assertNull(promise.getException());
    }

    @Test
    public void testFulfillSelfResolutionException() {
        PromiseBaseShell<Object> promise = new PromiseBaseShell<>();

        try {
            promise.fulfill(promise);
            fail("did not throw SelfResolutionException");
        } catch (SelfResolutionException e) {
            assertNotNull(e);
        }

        assertEquals(PromiseState.PENDING, promise.getState());
        assertNull(promise.getValue());
        assertNull(promise.getException());
    }

    @Test
    public void testFulfillMutatedStateException() {
        PromiseBaseShell<Object> promise = new PromiseBaseShell<>();
        Throwable value = new Throwable();

        promise.reject(value);

        try {
            promise.fulfill(null);
            fail("did not throw MutatedStateException");
        } catch (MutatedStateException e) {
            assertNotNull(e);
        }

        assertEquals(PromiseState.REJECTED, promise.getState());
        assertEquals(value, promise.getException());
        assertNull(promise.getValue());
    }

    @Test
    public void testRejectMutatedStateException() {
        PromiseBaseShell<Object> promise = new PromiseBaseShell<>();
        Object value = new Object();

        promise.fulfill(value);

        try {
            promise.reject(new RuntimeException());
            fail("did not throw MutatedStateException");
        } catch (MutatedStateException e) {
            assertNotNull(e);
        }

        assertEquals(PromiseState.FULFILLED, promise.getState());
        assertEquals(value, promise.getValue());
        assertNull(promise.getException());
    }

    @Test
    public void testResolveNull() {
        PromiseBaseShell<Object> promise = new PromiseBaseShell<>();

        promise.resolve(null);

        assertEquals(PromiseState.FULFILLED, promise.getState());
        assertNull(promise.getValue());
        assertNull(promise.getException());
        assertNull(promise.thenSync());
        assertNull(promise.exceptSync());
    }

    @Test
    public void testResolvePromiseRejected() {
        PromiseBaseShell<Object> promise = new PromiseBaseShell<>();

        Throwable e = new Throwable();

        @SuppressWarnings("unchecked") Promise<Integer> value = mock(Promise.class);
        when(value.getState()).thenReturn(PromiseState.REJECTED);
        when(value.exceptSync()).thenReturn(e);

        promise.resolve(value);

        verify(value, atLeastOnce()).getState();
        //noinspection ThrowableNotThrown
        verify(value, atLeastOnce()).exceptSync();

        assertEquals(PromiseState.REJECTED, promise.getState());
        assertNull(promise.getValue());
        assertEquals(e, promise.getException());
        assertNull(promise.thenSync());
        assertEquals(e, promise.exceptSync());
    }

    @Test
    public void testResolvePromiseFulfilled() {
        PromiseBaseShell<Object> promise = new PromiseBaseShell<>();

        Integer a = 23;

        @SuppressWarnings("unchecked") Promise<Integer> value = mock(Promise.class);
        when(value.getState()).thenReturn(PromiseState.FULFILLED);
        when(value.thenSync()).thenReturn(a);

        promise.resolve(value);

        verify(value, atLeastOnce()).getState();
        verify(value, atLeastOnce()).thenSync();

        assertEquals(PromiseState.FULFILLED, promise.getState());
        assertEquals(a, promise.getValue());
        assertNull(promise.getException());
        assertEquals(a, promise.thenSync());
        assertNull(promise.exceptSync());
    }

    @Test
    public void testResolvePromiseSelf() {
        PromiseBaseShell<Object> promise = new PromiseBaseShell<>();

        try {
            promise.resolve(promise);
            fail();
        } catch (SelfResolutionException e) {
            assertNotNull(e);
        }

        assertEquals(PromiseState.PENDING, promise.getState());
        assertNull(promise.getValue());
        assertNull(promise.getException());
    }

    @Test
    public void testFulfillPromiseSelf() {
        PromiseBaseShell<Object> promise = new PromiseBaseShell<>();

        try {
            promise.fulfill(promise);
            fail();
        } catch (SelfResolutionException e) {
            assertNotNull(e);
        }

        assertEquals(PromiseState.PENDING, promise.getState());
        assertNull(promise.getValue());
        assertNull(promise.getException());
    }

    @Test
    public void testFulfillMutatedStateExceptionDouble() {
        PromiseBaseShell<Object> promise = new PromiseBaseShell<>();
        Integer value = 1;

        promise.fulfill(value);

        try {
            promise.fulfill(value);
            fail("did not throw MutatedStateException");
        } catch (MutatedStateException e) {
            assertNotNull(e);
        }

        assertEquals(PromiseState.FULFILLED, promise.getState());
        assertEquals(value, promise.getValue());
        assertNull(promise.getException());
    }

    @Test
    public void testRejectMutatedStateExceptionDouble() {
        PromiseBaseShell<?> promise = new PromiseBaseShell<>();
        Throwable value = new Throwable();

        promise.reject(value);

        try {
            promise.reject(value);
            fail("did not throw MutatedStateException");
        } catch (MutatedStateException e) {
            assertNotEquals(null, e);
        }

        assertEquals(PromiseState.REJECTED, promise.getState());
        assertEquals(value, promise.getException());
        assertNull(promise.getValue());
    }

    @Test
    public void testResolvePromiseIsAsync() {
        PromiseBaseShell<Object> promise = new PromiseBaseShell<Object>();
        PromiseBaseShell<Object> value = new PromiseBaseShell<Object>();

        promise.resolve(value);

        assertEquals(PromiseState.PENDING, promise.getState());
        assertNull(promise.getValue());
        assertNull(promise.getException());
    }

    @Test
    public void testFulfillPromiseResolvesPromise() {
        Throwable e = new Throwable();
        @SuppressWarnings("unchecked") Promise<Object> value = mock(Promise.class);
        when(value.getState()).thenReturn(PromiseState.REJECTED);
        when(value.exceptSync()).thenReturn(e);

        @SuppressWarnings("unchecked") PromiseBaseShell<Promise<Object>> inner = mock(PromiseBaseShell.class);
        when(inner.getState()).thenReturn(PromiseState.FULFILLED);
        when(inner.thenSync()).thenReturn(value);

        PromiseBaseShell<Promise<Object>> promise = new PromiseBaseShell<>();

        promise.resolve(inner);

        assertEquals(PromiseState.FULFILLED, promise.getState());
        assertEquals(value, promise.getValue());
        assertNull(promise.getException());
        assertEquals(e, promise.thenSync().exceptSync());
    }

    @Test
    public void testResolvePromiseNested() {
        Throwable e = new Throwable();
        @SuppressWarnings("unchecked") Promise<Object> value = mock(Promise.class);
        when(value.getState()).thenReturn(PromiseState.REJECTED);
        when(value.exceptSync()).thenReturn(e);

        PromiseBaseShell<Promise<Object>> promise = new PromiseBaseShell<>();

        promise.fulfill(value);

        assertEquals(PromiseState.FULFILLED, promise.getState());
        assertEquals(value, promise.getValue());
        assertNull(promise.getException());
        assertEquals(e, promise.thenSync().exceptSync());
    }

    @Test
    public void testResolvePromiseFulfillAsync() {
        PromiseBaseShell<Object> inner = spy(new PromiseBaseShell<Object>());

        Object value = new Object();

        // make inner .always work by calling it's callback after waiting a second, and setting the values correctly
        // we also need to make it notifyAll, since we will have to wait, otherwise the test will exit

        doAnswer(new Answer<Object>() {
            @SuppressWarnings("unchecked")
            @Override
            public Object answer(InvocationOnMock invocation) {
                BiFunction callback = (BiFunction) invocation.getArguments()[0];

                // do the rest of this in a new thread
                Runnable runnable = () -> {
                    try {
                        TimeUnit.SECONDS.sleep(1);
                        inner.fulfill(value);
                        callback.apply(value, null);
                        synchronized (inner) {
                            inner.notifyAll();
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        fail();
                    }
                };

                Thread thread = new Thread(runnable);
                thread.start();

                return null;
            }
        }).when(inner).always(any());

        PromiseBaseShell<Object> promise = new PromiseBaseShell<Object>();

        // trigger the countdown until actually resolving
        promise.resolve(inner);

        // we should still be pending
        assertEquals(PromiseState.PENDING, promise.getState());

        // wait for some fuc
        synchronized (inner) {
            while (PromiseState.PENDING == inner.getState()) {
                try {
                    inner.wait();
                } catch (InterruptedException ignored) {
                }
            }
        }

        assertEquals(PromiseState.FULFILLED, promise.getState());
        assertEquals(value, promise.getValue());
    }

    @Test
    public void testResolvePromiseRejectAsync() {
        PromiseBaseShell<Object> inner = spy(new PromiseBaseShell<Object>());

        Throwable exception = new Throwable();

        // make inner .always work by calling it's callback after waiting a second, and setting the values correctly
        // we also need to make it notifyAll, since we will have to wait, otherwise the test will exit

        doAnswer(new Answer<Object>() {
            @SuppressWarnings("unchecked")
            @Override
            public Object answer(InvocationOnMock invocation) {
                BiFunction callback = (BiFunction) invocation.getArguments()[0];

                // do the rest of this in a new thread
                Runnable runnable = () -> {
                    try {
                        TimeUnit.SECONDS.sleep(1);
                        inner.reject(exception);
                        callback.apply(null, exception);
                        synchronized (inner) {
                            inner.notifyAll();
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        fail();
                    }
                };

                Thread thread = new Thread(runnable);
                thread.start();

                return null;
            }
        }).when(inner).always(any());

        PromiseBaseShell<Object> promise = new PromiseBaseShell<Object>();

        // trigger the countdown until actually resolving
        promise.resolve(inner);

        // we should still be pending
        assertEquals(PromiseState.PENDING, promise.getState());

        // wait for some fuc
        synchronized (inner) {
            while (PromiseState.PENDING == inner.getState()) {
                try {
                    inner.wait();
                } catch (InterruptedException ignored) {
                }
            }
        }

        assertEquals(PromiseState.REJECTED, promise.getState());
        assertEquals(exception, promise.getException());
    }

    /**
     * This test spawns multiple threads, that all attempt to finalize the value, and log their timestamps
     */
    @Test
    public void testRace() {
        PromiseBaseShell<Object> promise = new PromiseBaseShell<Object>();

        Vector<Long> resultList = new Vector<Long>();
        ArrayList<Thread> threadList = new ArrayList<Thread>();

        AtomicInteger totalSuccess = new AtomicInteger(0);
        totalSuccess.set(0);

        for (int x = 0; x < 10; x++) {
            Integer threadIndex = x;

            Runnable runnable = () -> {
                try {
                    TimeUnit.SECONDS.sleep(1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                // set the value to threadIndex,
                try {
                    promise.fulfill(threadIndex);
                    totalSuccess.addAndGet(1);
                } catch (MutatedStateException ignored) {
                }

                // update the time we took, and also notifyAll
                synchronized (resultList) {
                    resultList.set(threadIndex, System.currentTimeMillis());
                    resultList.notifyAll();
                }
            };

            resultList.add(null);
            threadList.add(new Thread(runnable));
        }

        Supplier<Boolean> isDone = () -> {
            for (Long result : resultList) {
                if (null == result) {
                    return false;
                }
            }
            return true;
        };

        // start all the threads
        threadList.forEach(Thread::start);

        // we have a sleep at the start, so it will be pending
        assertEquals(PromiseState.PENDING, promise.getState());

        // wait until we are done
        synchronized (resultList) {
            while (!isDone.get()) {
                try {
                    resultList.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        assertEquals(PromiseState.FULFILLED, promise.getState());
        assertNotNull(promise.getValue());

        // this is the thread that won
        int winner = (Integer) promise.getValue();

        // get the smallest value in resultList
        Long max = null;
        for (Long result : resultList) {
            assertNotNull(result);
            if (null == max || result < max) {
                max = result;
            }
        }

        // the winner must have the same time as the max
        assertEquals(max, resultList.get(winner));

        // check that we only successfully resolved one successfully
        assertEquals(1, totalSuccess.get());
    }

    /**
     * Implementation of PromiseBase to test underlying functionality, exposes reject, fulfill, resolve.
     */
    class PromiseBaseShell<T> extends PromiseBase<T> {
        @Override
        public PromiseBase<T> reject(Throwable exception) throws MutatedStateException, NullPointerException {
            return super.reject(exception);
        }

        @Override
        public PromiseBase<T> fulfill(T value) throws SelfResolutionException, MutatedStateException {
            return super.fulfill(value);
        }

        @Override
        public PromiseBase<T> resolve(Promise<? extends T> promise) throws SelfResolutionException, MutatedStateException {
            return super.resolve(promise);
        }

        @Override
        public <U> Promise<U> then(Function<? super T, ? extends Promise<? extends U>> callback) {
            return null;
        }

        @Override
        public <U> Promise<U> then(BiConsumer<? super T, Consumer<? super U>> callback) {
            return null;
        }

        @Override
        public Promise<T> except(Function<Throwable, ? extends Promise<? extends T>> callback) {
            return null;
        }

        @Override
        public Promise<T> except(BiConsumer<Throwable, Consumer<? super T>> callback) {
            return null;
        }

        @Override
        public <U> Promise<U> always(BiFunction<? super T, Throwable, ? extends Promise<? extends U>> callback) {
            return null;
        }
    }
}
