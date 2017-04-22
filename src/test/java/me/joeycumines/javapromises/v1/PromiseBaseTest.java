package me.joeycumines.javapromises.v1;

import me.joeycumines.javapromises.core.*;
import org.junit.Assert;
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
            Assert.fail("did not throw PendingValueException");
        } catch (PendingValueException e) {
            Assert.assertNotEquals(null, e);
        }
    }

    @Test
    public void testGetStateUnset() {
        PromiseBaseShell promise = new PromiseBaseShell();
        Assert.assertEquals(PromiseState.PENDING, promise.getState());
    }

    @Test
    public void testFinalizePendingIllegalArgumentException() {
        PromiseBaseShell promise = new PromiseBaseShell();

        try {
            promise.finalize(PromiseState.PENDING, null);
            Assert.fail("did not throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            Assert.assertNotEquals(null, e);
        }
    }

    @Test
    public void testFinalizeFulfill() {
        PromiseBaseShell promise = new PromiseBaseShell();
        Object value = new Object();

        promise.finalize(PromiseState.FULFILLED, value);

        Assert.assertEquals(PromiseState.FULFILLED, promise.getState());
        Assert.assertEquals(value, promise.getValue());
    }

    @Test
    public void testFinalizeFulfillNull() {
        PromiseBaseShell promise = new PromiseBaseShell();

        promise.finalize(PromiseState.FULFILLED, null);

        Assert.assertEquals(PromiseState.FULFILLED, promise.getState());
        Assert.assertEquals(null, promise.getValue());
    }

    @Test
    public void testFinalizeReject() {
        PromiseBaseShell promise = new PromiseBaseShell();
        Object value = new Exception();

        promise.finalize(PromiseState.REJECTED, value);

        Assert.assertEquals(PromiseState.REJECTED, promise.getState());
        Assert.assertEquals(value, promise.getValue());
    }

    @Test
    public void testFinalizeRejectNull() {
        PromiseBaseShell promise = new PromiseBaseShell();

        promise.finalize(PromiseState.REJECTED, null);

        Assert.assertEquals(PromiseState.REJECTED, promise.getState());
        Assert.assertEquals(null, promise.getValue());
    }

    @Test
    public void testFinalizeRejectIllegalArgumentException() {
        PromiseBaseShell promise = new PromiseBaseShell();
        Object value = new Object();

        try {
            promise.finalize(PromiseState.REJECTED, value);
            Assert.fail("did not throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            Assert.assertNotEquals(null, e);
        }

        Assert.assertEquals(PromiseState.PENDING, promise.getState());
    }

    @Test
    public void testFinalizeSelfResolutionException() {
        PromiseBaseShell promise = new PromiseBaseShell();

        try {
            promise.finalize(PromiseState.FULFILLED, promise);
            Assert.fail("did not throw SelfResolutionException");
        } catch (SelfResolutionException e) {
            Assert.assertNotEquals(null, e);
        }

        Assert.assertEquals(PromiseState.PENDING, promise.getState());
    }

    @Test
    public void testFinalizeMutatedStateException() {
        PromiseBaseShell promise = new PromiseBaseShell();
        Object value = new Object();

        promise.finalize(PromiseState.FULFILLED, value);

        try {
            promise.finalize(PromiseState.REJECTED, null);
            Assert.fail("did not throw MutatedStateException");
        } catch (MutatedStateException e) {
            Assert.assertNotEquals(null, e);
        }

        Assert.assertEquals(PromiseState.FULFILLED, promise.getState());
        Assert.assertEquals(value, promise.getValue());
    }

    @Test
    public void testFulfill() {
        PromiseBaseShell promise = new PromiseBaseShell();
        Object value = new Object();

        promise.fulfill(value);

        Assert.assertEquals(PromiseState.FULFILLED, promise.getState());
        Assert.assertEquals(value, promise.getValue());
    }

    @Test
    public void testFulfillNull() {
        PromiseBaseShell promise = new PromiseBaseShell();

        promise.fulfill(null);

        Assert.assertEquals(PromiseState.FULFILLED, promise.getState());
        Assert.assertEquals(null, promise.getValue());
    }

    @Test
    public void testReject() {
        PromiseBaseShell promise = new PromiseBaseShell();
        Exception value = new Exception();

        promise.reject(value);

        Assert.assertEquals(PromiseState.REJECTED, promise.getState());
        Assert.assertEquals(value, promise.getValue());
    }

    @Test
    public void testRejectNull() {
        PromiseBaseShell promise = new PromiseBaseShell();

        promise.reject(null);

        Assert.assertEquals(PromiseState.REJECTED, promise.getState());
        Assert.assertEquals(null, promise.getValue());
    }

    @Test
    public void testFulfillSelfResolutionException() {
        PromiseBaseShell promise = new PromiseBaseShell();

        try {
            promise.fulfill(promise);
            Assert.fail("did not throw SelfResolutionException");
        } catch (SelfResolutionException e) {
            Assert.assertNotEquals(null, e);
        }

        Assert.assertEquals(PromiseState.PENDING, promise.getState());
    }

    @Test
    public void testFulfillMutatedStateException() {
        PromiseBaseShell promise = new PromiseBaseShell();
        Exception value = new Exception();

        promise.finalize(PromiseState.REJECTED, value);

        try {
            promise.fulfill(null);
            Assert.fail("did not throw MutatedStateException");
        } catch (MutatedStateException e) {
            Assert.assertNotEquals(null, e);
        }

        Assert.assertEquals(PromiseState.REJECTED, promise.getState());
        Assert.assertEquals(value, promise.getValue());
    }

    @Test
    public void testRejectMutatedStateException() {
        PromiseBaseShell promise = new PromiseBaseShell();
        Object value = new Object();

        promise.finalize(PromiseState.FULFILLED, value);

        try {
            promise.reject(null);
            Assert.fail("did not throw MutatedStateException");
        } catch (MutatedStateException e) {
            Assert.assertNotEquals(null, e);
        }

        Assert.assertEquals(PromiseState.FULFILLED, promise.getState());
        Assert.assertEquals(value, promise.getValue());
    }

    @Test
    public void testFulfillMutatedStateExceptionDouble() {
        PromiseBaseShell promise = new PromiseBaseShell();
        Object value = new Object();

        promise.finalize(PromiseState.FULFILLED, value);

        try {
            promise.fulfill(null);
            Assert.fail("did not throw MutatedStateException");
        } catch (MutatedStateException e) {
            Assert.assertNotEquals(null, e);
        }

        Assert.assertEquals(PromiseState.FULFILLED, promise.getState());
        Assert.assertEquals(value, promise.getValue());
    }

    @Test
    public void testRejectMutatedStateExceptionDouble() {
        PromiseBaseShell promise = new PromiseBaseShell();
        Exception value = new Exception();

        promise.reject(value);

        try {
            promise.reject(null);
            Assert.fail("did not throw MutatedStateException");
        } catch (MutatedStateException e) {
            Assert.assertNotEquals(null, e);
        }

        Assert.assertEquals(PromiseState.REJECTED, promise.getState());
        Assert.assertEquals(value, promise.getValue());
    }

    @Test
    public void testResolve() {
        PromiseBaseShell promise = new PromiseBaseShell();
        Object value = new Object();

        promise.resolve(value);

        Assert.assertEquals(PromiseState.FULFILLED, promise.getState());
        Assert.assertEquals(value, promise.getValue());
    }

    @Test
    public void testResolveExceptionIsFulfilled() {
        PromiseBaseShell promise = new PromiseBaseShell();
        Object value = new Exception();

        promise.resolve(value);

        Assert.assertEquals(PromiseState.FULFILLED, promise.getState());
        Assert.assertEquals(value, promise.getValue());
    }

    @Test
    public void testResolvePromiseIsAsync() {
        PromiseBaseShell promise = new PromiseBaseShell();
        Object value = new PromiseBaseShell();

        promise.resolve(value);

        Assert.assertEquals(PromiseState.PENDING, promise.getState());
    }

    @Test
    public void testResolvePromiseSelfResolution() {
        PromiseBaseShell promise = new PromiseBaseShell();

        try {
            promise.resolve(promise);
            Assert.fail("did not throw SelfResolutionException");
        } catch (SelfResolutionException e) {
            Assert.assertNotEquals(null, e);
        }

        Assert.assertEquals(PromiseState.PENDING, promise.getState());
    }

    @Test
    public void testResolvePromiseFulfill() {
        PromiseInterface inner = mock(PromiseInterface.class);
        Object value = new Object();

        when(inner.getState()).thenReturn(PromiseState.FULFILLED);
        when(inner.getValue()).thenReturn(value);

        PromiseBaseShell promise = new PromiseBaseShell();
        promise.resolve(inner);

        Assert.assertEquals(PromiseState.FULFILLED, promise.getState());
        Assert.assertEquals(value, promise.getValue());
    }

    @Test
    public void testResolvePromiseReject() {
        PromiseInterface inner = mock(PromiseInterface.class);
        Object value = new Exception();

        when(inner.getState()).thenReturn(PromiseState.REJECTED);
        when(inner.getValue()).thenReturn(value);

        PromiseBaseShell promise = new PromiseBaseShell();
        promise.resolve(inner);

        Assert.assertEquals(PromiseState.REJECTED, promise.getState());
        Assert.assertEquals(value, promise.getValue());
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
                        inner.fulfill(value);
                        callback.apply(value);
                        synchronized (inner) {
                            inner.notifyAll();
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        Assert.fail();
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
        Assert.assertEquals(PromiseState.PENDING, promise.getState());

        // wait for some fuc
        synchronized (inner) {
            while (PromiseState.PENDING == inner.getState()) {
                try {
                    inner.wait();
                } catch (InterruptedException ignored) {
                }
            }
        }

        Assert.assertEquals(PromiseState.FULFILLED, promise.getState());
        Assert.assertEquals(value, promise.getValue());
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
                        Assert.fail();
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
        Assert.assertEquals(PromiseState.PENDING, promise.getState());

        // wait for some fuc
        synchronized (inner) {
            while (PromiseState.PENDING == inner.getState()) {
                try {
                    inner.wait();
                } catch (InterruptedException ignored) {
                }
            }
        }

        Assert.assertEquals(PromiseState.REJECTED, promise.getState());
        Assert.assertEquals(value, promise.getValue());
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
                    promise.fulfill(threadIndex);
                    totalSuccess.addAndGet(1);
                } catch (MutatedStateException ignored) {
                }

                // update the time we took, and also notifyAll
                synchronized (resultList) {
                    resultList.set(threadIndex, System.nanoTime());
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
        Assert.assertEquals(PromiseState.PENDING, promise.getState());

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

        Assert.assertEquals(PromiseState.FULFILLED, promise.getState());
        Assert.assertNotNull(promise.getValue());

        // this is the thread that won
        int winner = (Integer) promise.getValue();

        // get the index of the smallest value
        Long max = null;
        int index = -1;
        int x = 0;
        for (Long result : resultList) {
            Assert.assertNotNull(result);
            if (null == max || result < max) {
                max = result;
                index = x;
            }
            x++;
        }

        // the winner must be the one with the smallest time
        Assert.assertEquals(index, winner);

        // check that we only successfully resolved one successfully
        Assert.assertEquals(1, totalSuccess.get());
    }
}
