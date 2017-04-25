package me.joeycumines.javapromises.v1;

import me.joeycumines.javapromises.core.*;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.ArrayList;
import java.util.Vector;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class PromiseBaseTest {
    @Test
    public void testGetValuePendingValueException() {
        PromiseBaseShell promise = new PromiseBaseShell();

        try {
            promise.getValue();
            fail("did not throw PendingValueException");
        } catch (PendingValueException e) {
            assertNotEquals(null, e);
        }
    }

    @Test
    public void testGetStateUnset() {
        PromiseBaseShell promise = new PromiseBaseShell();
        assertEquals(PromiseState.PENDING, promise.getState());
    }

    @Test
    public void testFinalizePendingIllegalArgumentException() {
        PromiseBaseShell promise = new PromiseBaseShell();

        try {
            promise.finalize(PromiseState.PENDING, null);
            fail("did not throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertNotEquals(null, e);
        }
    }

    @Test
    public void testFinalizeFulfill() {
        PromiseBaseShell promise = new PromiseBaseShell();
        Object value = new Object();

        promise.finalize(PromiseState.FULFILLED, value);

        assertEquals(PromiseState.FULFILLED, promise.getState());
        assertEquals(value, promise.getValue());
    }

    @Test
    public void testFinalizeFulfillNull() {
        PromiseBaseShell promise = new PromiseBaseShell();

        promise.finalize(PromiseState.FULFILLED, null);

        assertEquals(PromiseState.FULFILLED, promise.getState());
        assertEquals(null, promise.getValue());
    }

    @Test
    public void testFinalizeReject() {
        PromiseBaseShell promise = new PromiseBaseShell();
        Object value = new Exception();

        promise.finalize(PromiseState.REJECTED, value);

        assertEquals(PromiseState.REJECTED, promise.getState());
        assertEquals(value, promise.getValue());
    }

    @Test
    public void testFinalizeRejectNull() {
        PromiseBaseShell promise = new PromiseBaseShell();

        promise.finalize(PromiseState.REJECTED, null);

        assertEquals(PromiseState.REJECTED, promise.getState());
        assertEquals(null, promise.getValue());
    }

    @Test
    public void testFinalizeRejectIllegalArgumentException() {
        PromiseBaseShell promise = new PromiseBaseShell();
        Object value = new Object();

        try {
            promise.finalize(PromiseState.REJECTED, value);
            fail("did not throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertNotEquals(null, e);
        }

        assertEquals(PromiseState.PENDING, promise.getState());
    }

    @Test
    public void testFinalizeSelfResolutionException() {
        PromiseBaseShell promise = new PromiseBaseShell();

        try {
            promise.finalize(PromiseState.FULFILLED, promise);
            fail("did not throw SelfResolutionException");
        } catch (SelfResolutionException e) {
            assertNotEquals(null, e);
        }

        assertEquals(PromiseState.PENDING, promise.getState());
    }

    @Test
    public void testFinalizeMutatedStateException() {
        PromiseBaseShell promise = new PromiseBaseShell();
        Object value = new Object();

        promise.finalize(PromiseState.FULFILLED, value);

        try {
            promise.finalize(PromiseState.REJECTED, null);
            fail("did not throw MutatedStateException");
        } catch (MutatedStateException e) {
            assertNotEquals(null, e);
        }

        assertEquals(PromiseState.FULFILLED, promise.getState());
        assertEquals(value, promise.getValue());
    }

    @Test
    public void testFulfill() {
        PromiseBaseShell promise = new PromiseBaseShell();
        Object value = new Object();

        promise.finalize(PromiseState.FULFILLED, value);

        assertEquals(PromiseState.FULFILLED, promise.getState());
        assertEquals(value, promise.getValue());
    }

    @Test
    public void testFulfillNull() {
        PromiseBaseShell promise = new PromiseBaseShell();

        promise.finalize(PromiseState.FULFILLED, null);

        assertEquals(PromiseState.FULFILLED, promise.getState());
        assertEquals(null, promise.getValue());
    }

    @Test
    public void testReject() {
        PromiseBaseShell promise = new PromiseBaseShell();
        Exception value = new Exception();

        promise.reject(value);

        assertEquals(PromiseState.REJECTED, promise.getState());
        assertEquals(value, promise.getValue());
    }

    @Test
    public void testRejectNull() {
        PromiseBaseShell promise = new PromiseBaseShell();

        promise.reject(null);

        assertEquals(PromiseState.REJECTED, promise.getState());
        assertEquals(null, promise.getValue());
    }

    @Test
    public void testFulfillSelfResolutionException() {
        PromiseBaseShell promise = new PromiseBaseShell();

        try {
            promise.finalize(PromiseState.FULFILLED, promise);
            fail("did not throw SelfResolutionException");
        } catch (SelfResolutionException e) {
            assertNotEquals(null, e);
        }

        assertEquals(PromiseState.PENDING, promise.getState());
    }

    @Test
    public void testFulfillMutatedStateException() {
        PromiseBaseShell promise = new PromiseBaseShell();
        Exception value = new Exception();

        promise.finalize(PromiseState.REJECTED, value);

        try {
            promise.finalize(PromiseState.FULFILLED, null);
            fail("did not throw MutatedStateException");
        } catch (MutatedStateException e) {
            assertNotEquals(null, e);
        }

        assertEquals(PromiseState.REJECTED, promise.getState());
        assertEquals(value, promise.getValue());
    }

    @Test
    public void testRejectMutatedStateException() {
        PromiseBaseShell promise = new PromiseBaseShell();
        Object value = new Object();

        promise.finalize(PromiseState.FULFILLED, value);

        try {
            promise.reject(null);
            fail("did not throw MutatedStateException");
        } catch (MutatedStateException e) {
            assertNotEquals(null, e);
        }

        assertEquals(PromiseState.FULFILLED, promise.getState());
        assertEquals(value, promise.getValue());
    }

    @Test
    public void testFulfillMutatedStateExceptionDouble() {
        PromiseBaseShell promise = new PromiseBaseShell();
        Object value = new Object();

        promise.finalize(PromiseState.FULFILLED, value);

        try {
            promise.finalize(PromiseState.FULFILLED, null);
            fail("did not throw MutatedStateException");
        } catch (MutatedStateException e) {
            assertNotEquals(null, e);
        }

        assertEquals(PromiseState.FULFILLED, promise.getState());
        assertEquals(value, promise.getValue());
    }

    @Test
    public void testRejectMutatedStateExceptionDouble() {
        PromiseBaseShell promise = new PromiseBaseShell();
        Exception value = new Exception();

        promise.reject(value);

        try {
            promise.reject(null);
            fail("did not throw MutatedStateException");
        } catch (MutatedStateException e) {
            assertNotEquals(null, e);
        }

        assertEquals(PromiseState.REJECTED, promise.getState());
        assertEquals(value, promise.getValue());
    }

    @Test
    public void testResolve() {
        PromiseBaseShell promise = new PromiseBaseShell();
        Object value = new Object();

        promise.resolve(value);

        assertEquals(PromiseState.FULFILLED, promise.getState());
        assertEquals(value, promise.getValue());
    }

    @Test
    public void testResolveExceptionIsFulfilled() {
        PromiseBaseShell promise = new PromiseBaseShell();
        Object value = new Exception();

        promise.resolve(value);

        assertEquals(PromiseState.FULFILLED, promise.getState());
        assertEquals(value, promise.getValue());
    }

    @Test
    public void testResolvePromiseIsAsync() {
        PromiseBaseShell promise = new PromiseBaseShell();
        Object value = new PromiseBaseShell();

        promise.resolve(value);

        assertEquals(PromiseState.PENDING, promise.getState());
    }

    @Test
    public void testResolvePromiseSelfResolution() {
        PromiseBaseShell promise = new PromiseBaseShell();

        try {
            promise.resolve(promise);
            fail("did not throw SelfResolutionException");
        } catch (SelfResolutionException e) {
            assertNotEquals(null, e);
        }

        assertEquals(PromiseState.PENDING, promise.getState());
    }

    @Test
    public void testResolvePromiseFulfill() {
        Promise inner = mock(Promise.class);
        Object value = new Object();

        when(inner.getState()).thenReturn(PromiseState.FULFILLED);
        when(inner.getValue()).thenReturn(value);

        PromiseBaseShell promise = new PromiseBaseShell();
        promise.resolve(inner);

        assertEquals(PromiseState.FULFILLED, promise.getState());
        assertEquals(value, promise.getValue());
    }

    @Test
    public void testResolvePromiseReject() {
        Promise inner = mock(Promise.class);
        Object value = new Exception();

        when(inner.getState()).thenReturn(PromiseState.REJECTED);
        when(inner.getValue()).thenReturn(value);

        PromiseBaseShell promise = new PromiseBaseShell();
        promise.resolve(inner);

        assertEquals(PromiseState.REJECTED, promise.getState());
        assertEquals(value, promise.getValue());
    }

    @Test
    public void testResolvePromiseFulfillAsync() {
        PromiseBaseShell inner = spy(new PromiseBaseShell());

        Object value = new Object();

        // make inner .always work by calling it's callback after waiting a second, and setting the values correctly
        // we also need to make it notifyAll, since we will have to wait, otherwise the test will exit

        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) {
                Function<Object, Object> callback = (Function<Object, Object>) invocation.getArguments()[0];

                // do the rest of this in a new thread
                Runnable runnable = () -> {
                    try {
                        TimeUnit.SECONDS.sleep(1);
                        inner.finalize(PromiseState.FULFILLED, value);
                        callback.apply(value);
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
        }).when(inner).always(any(Function.class));

        PromiseBaseShell promise = new PromiseBaseShell();

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
        PromiseBaseShell inner = spy(new PromiseBaseShell());

        Exception value = new Exception();

        // make inner .always work by calling it's callback after waiting a second, and setting the values correctly
        // we also need to make it notifyAll, since we will have to wait, otherwise the test will exit

        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) {
                Function<Object, Object> callback = (Function<Object, Object>) invocation.getArguments()[0];

                // do the rest of this in a new thread
                Runnable runnable = () -> {
                    try {
                        TimeUnit.SECONDS.sleep(1);
                        inner.reject(value);
                        callback.apply(value);
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
        }).when(inner).always(any(Function.class));

        PromiseBaseShell promise = new PromiseBaseShell();

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
        assertEquals(value, promise.getValue());
    }

    /**
     * This test spawns multiple threads, that all attempt to finalize the value, and log their timestamps
     */
    @Test
    public void testRace() {
        PromiseBaseShell promise = new PromiseBaseShell();

        Vector<Long> resultList = new Vector<Long>();
        ArrayList<Thread> threadList = new ArrayList<Thread>();

        AtomicInteger totalSuccess = new AtomicInteger();
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
                    promise.finalize(PromiseState.FULFILLED, threadIndex);
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
}
