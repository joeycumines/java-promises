package me.joeycumines.javapromises.core;

import me.joeycumines.javapromises.v1.BlockingPromise;
import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Extend this class to test a Promise implementation.
 */
public abstract class PromiseTest {
    abstract protected PromiseFactory getFactory();

    /**
     * Promises that are pending on each other in a circular manner will simply wait forever, all WITHOUT blocking.
     */
    @Test
    public void testCircularReferencesUnresolved() {
        BlockingPromise one = new BlockingPromise(this.getFactory());
        Promise two = this.getFactory().resolve(one.getPromise());
        Promise three = this.getFactory().resolve(two);

        assertEquals(PromiseState.PENDING, one.getPromise().getState());
        assertEquals(PromiseState.PENDING, two.getState());
        assertEquals(PromiseState.PENDING, three.getState());

        one.resolve(three);

        assertEquals(PromiseState.PENDING, one.getPromise().getState());
        assertEquals(PromiseState.PENDING, two.getState());
        assertEquals(PromiseState.PENDING, three.getState());
    }

    /**
     * Promises that are resolved to each other in a circular manner will throw a CircularResolutionException.
     * <p>
     * 3 promises example.
     */
    @Test
    public void testCircularReferencesResolved3() {
        Promise one = mock(Promise.class);
        when(one.getState()).thenReturn(PromiseState.FULFILLED);

        Promise two = mock(Promise.class);
        when(two.getState()).thenReturn(PromiseState.FULFILLED);
        when(two.getValue()).thenReturn(one);

        Promise three = mock(Promise.class);
        when(three.getState()).thenReturn(PromiseState.FULFILLED);
        when(three.getValue()).thenReturn(two);

        when(one.getValue()).thenReturn(three);

        try {
            this.getFactory().resolve(one);
            fail();
        } catch (CircularResolutionException e) {
            assertNotNull(e);
        }
    }

    /**
     * Promises that are resolved to each other in a circular manner will throw a CircularResolutionException.
     * <p>
     * 2 promises example.
     */
    @Test
    public void testCircularReferencesResolved2() {
        Promise one = mock(Promise.class);
        when(one.getState()).thenReturn(PromiseState.FULFILLED);

        Promise two = mock(Promise.class);
        when(two.getState()).thenReturn(PromiseState.FULFILLED);
        when(two.getValue()).thenReturn(one);

        when(one.getValue()).thenReturn(two);

        try {
            this.getFactory().resolve(one);
            fail();
        } catch (CircularResolutionException e) {
            assertNotNull(e);
        }
    }

    /**
     * Promises that are resolved to each other in a circular manner will throw a CircularResolutionException.
     * <p>
     * 1 promises example.
     */
    @Test
    public void testCircularReferencesResolved1() {
        Promise one = mock(Promise.class);
        when(one.getState()).thenReturn(PromiseState.FULFILLED);
        when(one.getValue()).thenReturn(one);

        try {
            this.getFactory().resolve(one);
            fail();
        } catch (CircularResolutionException e) {
            assertNotNull(e);
        }
    }

    /**
     * Ensure you can resolve an object.
     */
    @Test
    public void testResolve() {
        Object value = new Object();

        Promise promise = this.getFactory().resolve(value);

        promise.sync();

        assertEquals(PromiseState.FULFILLED, promise.getState());
        assertEquals(value, promise.getValue());
    }

    @Test
    public void testResolveThen() {
        AtomicInteger counter = new AtomicInteger(0);
        Object value = new Object();

        Promise promise = this.getFactory().resolve(value)
                .then((r) -> {
                    assertEquals(value, r);
                    assertEquals(0, counter.getAndIncrement());
                    return r;
                });

        promise.sync();

        assertEquals(1, counter.get());
        assertEquals(PromiseState.FULFILLED, promise.getState());
        assertEquals(value, promise.getValue());
    }

    @Test
    public void testResolveThenExcept() {
        AtomicInteger counter = new AtomicInteger(0);
        Object value = new Object();

        Promise promise = this.getFactory().resolve(value)
                .then((r) -> {
                    assertEquals(value, r);
                    assertEquals(0, counter.getAndIncrement());
                    return r;
                })
                .except((e) -> {
                    counter.incrementAndGet();
                    return null;
                });

        promise.sync();

        assertEquals(1, counter.get());
        assertEquals(PromiseState.FULFILLED, promise.getState());
        assertEquals(value, promise.getValue());
    }

    @Test
    public void testResolveThenExceptWithThrown() {
        AtomicInteger counter = new AtomicInteger(0);
        RuntimeException exception1 = new RuntimeException();
        RuntimeException exception2 = new RuntimeException();
        Object value = new Object();

        Promise promise = this.getFactory().resolve(value)
                .then((r) -> {
                    assertEquals(value, r);
                    assertEquals(0, counter.getAndIncrement());
                    throw exception1;
                })
                .except((Function<Throwable, Promise>) (e) -> {
                    assertEquals(exception1, e);
                    assertEquals(1, counter.getAndIncrement());
                    return this.getFactory().reject(exception2);
                });

        promise.sync();

        assertEquals(2, counter.get());
        assertEquals(PromiseState.REJECTED, promise.getState());
        assertEquals(exception2, promise.getValue());
    }

    @Test
    public void testResolveThenExceptWithThrownCaught() {
        AtomicInteger counter = new AtomicInteger(0);
        RuntimeException exception = new RuntimeException();
        Object value = new Object();

        Promise promise = this.getFactory().resolve(value)
                .then((r) -> {
                    assertEquals(value, r);
                    assertEquals(0, counter.getAndIncrement());
                    throw exception;
                })
                .except((e) -> {
                    assertEquals(exception, e);
                    assertEquals(1, counter.getAndIncrement());
                    return e;
                });

        promise.sync();

        assertEquals(2, counter.get());
        assertEquals(PromiseState.FULFILLED, promise.getState());
        assertEquals(exception, promise.getValue());
    }

    @Test
    public void testThenReturnsNewPromise() {
        Promise promise = this.getFactory().resolve(true);
        assertNotEquals(promise, promise.then((r) -> null));
    }

    @Test
    public void testExceptReturnsNewPromise() {
        Promise promise = this.getFactory().resolve(true);
        assertNotEquals(promise, promise.except((r) -> null));
    }

    @Test
    public void testAlwaysReturnsNewPromise() {
        Promise promise = this.getFactory().resolve(true);
        assertNotEquals(promise, promise.always((r) -> null));
    }

    /**
     * Test that promises will resolve values of other promises, and will propagate state.
     * <p>
     * If promises are already resolved already they MUST be resolved WITHOUT needing to call sync.
     * <p>
     * Note that normally {@link Promise#getValue()} SHOULD NEVER return a promise.
     */
    @Test
    public void testResolvePromiseFulfilled() {
        Object object = new Object();

        Promise input = mock(Promise.class);
        when(input.getValue()).thenReturn(object);
        when(input.getState()).thenReturn(PromiseState.FULFILLED);

        Promise promise = this.getFactory().resolve(input);

        assertEquals(object, promise.getValue());
        assertEquals(PromiseState.FULFILLED, promise.getState());
    }

    /**
     * Test that promises will resolve values of other promises, and will propagate state.
     * <p>
     * If promises are already resolved already they MUST be resolved WITHOUT needing to call sync.
     * <p>
     * Note that normally {@link Promise#getValue()} SHOULD NEVER return a promise.
     */
    @Test
    public void testResolvePromiseRejected() {
        Exception exception = new Exception();

        Promise input = mock(Promise.class);
        when(input.getValue()).thenReturn(exception);
        when(input.getState()).thenReturn(PromiseState.REJECTED);

        Promise promise = this.getFactory().resolve(input);

        assertEquals(exception, promise.getValue());
        assertEquals(PromiseState.REJECTED, promise.getState());
    }

    /**
     * Test that promises will resolve values of other promises, as far as necessary.
     * <p>
     * Note that normally {@link Promise#getValue()} SHOULD NEVER return a promise.
     */
    @Test
    public void testResolvePromiseDeepFulfilled() {
        Function<Object, Promise> createPromise = (object) -> {
            Promise input = mock(Promise.class);
            when(input.getValue()).thenReturn(object);
            when(input.getState()).thenReturn(PromiseState.FULFILLED);

            return input;
        };

        Object object = new Object();
        Promise promise = this.getFactory().resolve(createPromise.apply(createPromise.apply(createPromise.apply(createPromise.apply(object)))));

        assertEquals(object, promise.getValue());
        assertEquals(PromiseState.FULFILLED, promise.getState());
    }

    /**
     * Test that promises will resolve values of other promises, as far as necessary.
     * <p>
     * Note that normally {@link Promise#getValue()} SHOULD NEVER return a promise.
     */
    @Test
    public void testResolvePromiseDeepRejected() {
        Function<Object, Promise> createPromise = (object) -> {
            Promise input = mock(Promise.class);
            when(input.getValue()).thenReturn(object);
            when(input.getState()).thenReturn(PromiseState.FULFILLED);

            return input;
        };

        Object object = new Exception();
        // we have to reject the last one
        Promise input = mock(Promise.class);
        when(input.getValue()).thenReturn(object);
        when(input.getState()).thenReturn(PromiseState.REJECTED);
        Promise promise = this.getFactory().resolve(createPromise.apply(createPromise.apply(createPromise.apply(createPromise.apply(input)))));

        assertEquals(object, promise.getValue());
        assertEquals(PromiseState.REJECTED, promise.getState());
    }

    /**
     * Malformed promises that do not return exceptions when rejected, should throw an exception when they are used as
     * a parameter for resolve.
     */
    @Test
    public void testResolvePromiseRejectedNonThrowableGivesException() {
        Object object = new Object();

        Promise input = mock(Promise.class);
        when(input.getValue()).thenReturn(object);
        when(input.getState()).thenReturn(PromiseState.REJECTED);

        try {
            this.getFactory().resolve(input);
            fail("needs to throw some form of Exception");
        } catch (Exception e) {
            assertNotNull(e);
        }
    }

    /**
     * Test the async resolution of promises that are waiting works as expected.
     * <p>
     * Snuck in some then / catch / always tests too.
     */
    @Test
    public void testResolvePromiseDeepWaitFulfilled() {
        BlockingPromise blocker = new BlockingPromise(this.getFactory());

        Promise promise1 = this.getFactory().resolve(blocker.getPromise());
        Promise promise2 = this.getFactory().resolve(promise1);

        assertEquals(PromiseState.PENDING, blocker.getPromise().getState());
        assertEquals(PromiseState.PENDING, promise1.getState());
        assertEquals(PromiseState.PENDING, promise2.getState());

        AtomicInteger counter = new AtomicInteger();
        Object object = new Object();

        ConcurrentLinkedQueue<Promise> queue = new ConcurrentLinkedQueue<Promise>();

        Consumer<Promise> setupPromise = (promise) -> {
            queue.offer(promise.then((value) -> {
                assertEquals(object, value);
                assertTrue(6 >= counter.incrementAndGet());
                return null;
            }));

            queue.offer(promise.except((e) -> {
                counter.incrementAndGet();
                return null;
            }));

            queue.offer(promise.always((value) -> {
                assertEquals(object, value);
                assertTrue(6 >= counter.incrementAndGet());
                return null;
            }));
        };

        setupPromise.accept(blocker.getPromise());
        setupPromise.accept(promise1);
        setupPromise.accept(promise2);

        assertEquals(PromiseState.PENDING, blocker.getPromise().getState());
        assertEquals(PromiseState.PENDING, promise1.getState());
        assertEquals(PromiseState.PENDING, promise2.getState());

        blocker.resolve(object);

        // wait for all the inner ones to resolve, for the counter
        queue.forEach(Promise::sync);

        assertEquals(PromiseState.FULFILLED, blocker.getPromise().getState());
        assertEquals(PromiseState.FULFILLED, promise1.getState());
        assertEquals(PromiseState.FULFILLED, promise2.getState());
        assertEquals(object, blocker.getPromise().getValue());
        assertEquals(object, promise1.getValue());
        assertEquals(object, promise2.getValue());

        assertEquals(6, counter.get());
    }

    /**
     * Test the async resolution of promises that are waiting works as expected.
     * <p>
     * Snuck in some then / catch / always tests too.
     */
    @Test
    public void testResolvePromiseDeepWaitRejected() {
        BlockingPromise blocker = new BlockingPromise(this.getFactory());

        Promise promise1 = this.getFactory().resolve(blocker.getPromise());
        Promise promise2 = this.getFactory().resolve(promise1);

        assertEquals(PromiseState.PENDING, blocker.getPromise().getState());
        assertEquals(PromiseState.PENDING, promise1.getState());
        assertEquals(PromiseState.PENDING, promise2.getState());

        AtomicInteger counter = new AtomicInteger();
        Throwable exception = new Exception();

        ConcurrentLinkedQueue<Promise> queue = new ConcurrentLinkedQueue<Promise>();

        Consumer<Promise> setupPromise = (promise) -> {
            queue.offer(promise.then((value) -> {
                counter.incrementAndGet();
                return null;
            }));

            queue.offer(promise.except((value) -> {
                assertEquals(exception, value);
                assertTrue(6 >= counter.incrementAndGet());
                return null;
            }));

            queue.offer(promise.always((value) -> {
                assertEquals(exception, value);
                assertTrue(6 >= counter.incrementAndGet());
                return null;
            }));
        };

        setupPromise.accept(blocker.getPromise());
        setupPromise.accept(promise1);
        setupPromise.accept(promise2);

        assertEquals(PromiseState.PENDING, blocker.getPromise().getState());
        assertEquals(PromiseState.PENDING, promise1.getState());
        assertEquals(PromiseState.PENDING, promise2.getState());

        blocker.reject(exception);

        // wait for all the inner ones to resolve, for the counter
        queue.forEach(Promise::sync);

        assertEquals(PromiseState.REJECTED, blocker.getPromise().getState());
        assertEquals(PromiseState.REJECTED, promise1.getState());
        assertEquals(PromiseState.REJECTED, promise2.getState());
        assertEquals(exception, blocker.getPromise().getValue());
        assertEquals(exception, promise1.getValue());
        assertEquals(exception, promise2.getValue());

        assertEquals(6, counter.get());
    }

    @Test
    public void testResolvePrimitive() {
        Promise promise = this.getFactory().resolve(22);
        assertEquals(22, promise.getValue());
        assertEquals(PromiseState.FULFILLED, promise.getState());
    }

    @Test
    public void testResolveException() {
        Exception exception = new Exception();
        Promise promise = this.getFactory().resolve(exception);
        assertEquals(exception, promise.getValue());
        assertEquals(PromiseState.FULFILLED, promise.getState());
    }

    /**
     * Reject must resolve immediately.
     */
    @Test
    public void testReject() {
        Throwable throwable = new Throwable();
        Promise promise = this.getFactory().reject(throwable);
        assertEquals(PromiseState.REJECTED, promise.getState());
        assertEquals(throwable, promise.getValue());
    }

    @Test
    public void testExcept() {
        AtomicInteger counter = new AtomicInteger();
        Object object = new Object();
        Exception exception = new Exception();
        Promise promise = this.getFactory().reject(exception)
                .except((e) -> {
                    assertEquals(exception, e);
                    counter.incrementAndGet();
                    return object;
                });

        promise.sync();

        assertEquals(object, promise.getValue());
        assertEquals(PromiseState.FULFILLED, promise.getState());
        assertEquals(1, counter.get());
    }

    @Test
    public void testExceptDoubled() {
        AtomicInteger counter = new AtomicInteger();
        Object object = new Object();
        Exception exception = new Exception();
        Promise promise = this.getFactory().reject(exception)
                .except((e) -> {
                    assertEquals(exception, e);
                    counter.incrementAndGet();
                    return object;
                })
                .except((e) -> {
                    counter.incrementAndGet();
                    return object;
                });

        promise.sync();

        assertEquals(object, promise.getValue());
        assertEquals(PromiseState.FULFILLED, promise.getState());
        assertEquals(1, counter.get());
    }

    @Test
    public void testExceptThrow() {
        AtomicInteger counter = new AtomicInteger();
        RuntimeException exception = new RuntimeException();
        Promise promise = this.getFactory().reject(exception)
                .except((e) -> {
                    assertEquals(exception, e);
                    counter.incrementAndGet();
                    throw (RuntimeException) e;
                });

        promise.sync();

        assertEquals(exception, promise.getValue());
        assertEquals(PromiseState.REJECTED, promise.getState());
        assertEquals(1, counter.get());
    }

    @Test
    public void testThenThrow() {
        AtomicInteger counter = new AtomicInteger();
        RuntimeException exception = new RuntimeException();
        Promise promise = this.getFactory().resolve(exception)
                .then((e) -> {
                    assertEquals(exception, e);
                    counter.incrementAndGet();
                    throw (RuntimeException) e;
                });

        promise.sync();

        assertEquals(exception, promise.getValue());
        assertEquals(PromiseState.REJECTED, promise.getState());
        assertEquals(1, counter.get());
    }

    @Test
    public void testAlwaysThrow() {
        AtomicInteger counter = new AtomicInteger();
        RuntimeException exception = new RuntimeException();
        Promise promise = this.getFactory().resolve(exception)
                .always((e) -> {
                    assertEquals(exception, e);
                    counter.incrementAndGet();
                    throw (RuntimeException) e;
                });

        promise.sync();

        assertEquals(exception, promise.getValue());
        assertEquals(PromiseState.REJECTED, promise.getState());
        assertEquals(1, counter.get());
    }

    @Test
    public void testExceptReturnException() {
        AtomicInteger counter = new AtomicInteger();
        Object object = new Object();
        Exception exception = new Exception();
        Promise promise = this.getFactory().reject(exception)
                .except((e) -> {
                    assertEquals(exception, e);
                    counter.incrementAndGet();
                    return exception;
                })
                .except((e) -> {
                    counter.incrementAndGet();
                    return object;
                });

        promise.sync();

        assertEquals(exception, promise.getValue());
        assertEquals(PromiseState.FULFILLED, promise.getState());
        assertEquals(1, counter.get());
    }

    @Test
    public void testExceptAfterThen() {
        AtomicInteger counter = new AtomicInteger();
        Object object = new Object();
        Exception exception = new Exception();
        Promise promise = this.getFactory().resolve(object)
                .then((r) -> {
                    assertEquals(object, r);
                    counter.incrementAndGet();
                    return exception;
                })
                .except((e) -> {
                    assertEquals(exception, e);
                    counter.incrementAndGet();
                    return null;
                });

        promise.sync();

        assertEquals(exception, promise.getValue());
        assertEquals(PromiseState.FULFILLED, promise.getState());
        assertEquals(1, counter.get());
    }

    @Test
    public void testExceptResolvesFulfilled() {
        AtomicInteger counter = new AtomicInteger();
        Object object = new Object();
        Exception exception = new Exception();
        Promise promise = this.getFactory().reject(exception)
                .except((e) -> {
                    assertEquals(exception, e);
                    counter.incrementAndGet();
                    return this.getFactory().resolve(object);
                });

        promise.sync();

        assertEquals(object, promise.getValue());
        assertEquals(PromiseState.FULFILLED, promise.getState());
        assertEquals(1, counter.get());
    }

    @Test
    public void testExceptResolvesRejected() {
        AtomicInteger counter = new AtomicInteger();
        Object object = new Object();
        Exception exception = new Exception();
        Exception exception1 = new Exception();

        Promise promise = this.getFactory().reject(exception)
                .except((e) -> {
                    assertEquals(exception, e);
                    counter.incrementAndGet();
                    return this.getFactory().reject(exception1);
                });

        promise.sync();

        assertEquals(exception1, promise.getValue());
        assertEquals(PromiseState.REJECTED, promise.getState());
        assertEquals(1, counter.get());
    }

    @Test
    public void testAlwaysResolvesFulfilled() {
        AtomicInteger counter = new AtomicInteger();
        Object object = new Object();
        Exception exception = new Exception();
        Promise promise = this.getFactory().reject(exception)
                .always((e) -> {
                    assertEquals(exception, e);
                    counter.incrementAndGet();
                    return this.getFactory().resolve(object);
                });

        promise.sync();

        assertEquals(object, promise.getValue());
        assertEquals(PromiseState.FULFILLED, promise.getState());
        assertEquals(1, counter.get());
    }

    @Test
    public void testAlwaysResolvesRejected() {
        AtomicInteger counter = new AtomicInteger();
        Object object = new Object();
        Exception exception = new Exception();
        Exception exception1 = new Exception();

        Promise promise = this.getFactory().reject(exception)
                .always((e) -> {
                    assertEquals(exception, e);
                    counter.incrementAndGet();
                    return this.getFactory().reject(exception1);
                });

        promise.sync();

        assertEquals(exception1, promise.getValue());
        assertEquals(PromiseState.REJECTED, promise.getState());
        assertEquals(1, counter.get());
    }

    @Test
    public void testThenResolvesFulfilled() {
        AtomicInteger counter = new AtomicInteger();
        Object object = new Object();
        Exception exception = new Exception();

        Promise promise = this.getFactory().resolve(exception)
                .then((e) -> {
                    assertEquals(exception, e);
                    counter.incrementAndGet();
                    return this.getFactory().resolve(object);
                });

        promise.sync();

        assertEquals(object, promise.getValue());
        assertEquals(PromiseState.FULFILLED, promise.getState());
        assertEquals(1, counter.get());
    }

    @Test
    public void testThenResolvesRejected() {
        AtomicInteger counter = new AtomicInteger();
        Object object = new Object();
        Exception exception = new Exception();
        Exception exception1 = new Exception();

        Promise promise = this.getFactory().resolve(exception)
                .then((e) -> {
                    assertEquals(exception, e);
                    counter.incrementAndGet();
                    return this.getFactory().reject(exception1);
                });

        promise.sync();

        assertEquals(exception1, promise.getValue());
        assertEquals(PromiseState.REJECTED, promise.getState());
        assertEquals(1, counter.get());
    }

    @Test
    public void testThen() {
        Promise promise = this.getFactory().resolve(3)
                .then((Function<Integer, Integer>) (r) -> r * 2);

        promise.sync();

        assertEquals(6, promise.getValue());
        assertEquals(PromiseState.FULFILLED, promise.getState());
    }

    @Test
    public void testThenCast() {
        AtomicInteger counter = new AtomicInteger();
        String string = "example";

        Promise promise = this.getFactory().resolve(string)
                .then((Function<String, Object>) (s) -> {
                    assertEquals(0, counter.getAndIncrement());
                    assertEquals("example", s);
                    return s + "_test";
                });

        promise.sync();

        assertEquals(1, counter.getAndIncrement());
        assertEquals(PromiseState.FULFILLED, promise.getState());
        assertEquals("example_test", promise.getValue());
    }

    /**
     * Anonymous functions that are cast incorrectly will resolve with an exception.
     */
    @Test
    public void testThenCastException() {
        String string = "example";

        // this will resolve a promise, but the then statement expects an integer
        Promise promise = this.getFactory().resolve(string)
                .then((Function<Integer, Object>) (s) -> {
                    return null;
                });

        promise.sync();

        assertEquals(PromiseState.REJECTED, promise.getState());
        assertTrue(promise.getValue() instanceof ClassCastException);
    }

    @Test
    public void testExceptCast() {
        AtomicInteger counter = new AtomicInteger();
        IOException exception = new IOException();
        Object object = new Object();

        Promise promise = this.getFactory().reject(exception)
                .except((Function<Exception, Object>) (e) -> {
                    assertEquals(0, counter.getAndIncrement());
                    assertEquals(exception, e);
                    return object;
                });

        promise.sync();

        assertEquals(1, counter.getAndIncrement());
        assertEquals(PromiseState.FULFILLED, promise.getState());
        assertEquals(object, promise.getValue());
    }

    /**
     * Anonymous functions that are cast incorrectly will resolve with an exception.
     */
    @Test
    public void testExceptCastException() {
        IOException exception = new IOException();

        // this will resolve a promise, but the then statement expects an integer
        Promise promise = this.getFactory().reject(exception)
                .except((Function<RuntimeException, Object>) (s) -> {
                    return null;
                });

        promise.sync();

        assertEquals(PromiseState.REJECTED, promise.getState());
        assertNotEquals(exception, promise.getValue());
        assertTrue(promise.getValue() instanceof ClassCastException);
    }

    @Test
    public void testAlwaysCast() {
        AtomicInteger counter = new AtomicInteger();
        String string = "example";

        Promise promise = this.getFactory().resolve(string)
                .always((Function<String, Object>) (s) -> {
                    assertEquals(0, counter.getAndIncrement());
                    return s + "_test";
                });

        promise.sync();

        assertEquals(1, counter.getAndIncrement());
        assertEquals(PromiseState.FULFILLED, promise.getState());
        assertEquals("example_test", promise.getValue());
    }

    /**
     * Anonymous functions that are cast incorrectly will resolve with an exception.
     */
    @Test
    public void testAlwaysCastException() {
        String string = "example";

        // this will resolve a promise, but the then statement expects an integer
        Promise promise = this.getFactory().resolve(string)
                .always((Function<Integer, Object>) (s) -> {
                    return null;
                });

        promise.sync();

        assertEquals(PromiseState.REJECTED, promise.getState());
        assertTrue(promise.getValue() instanceof ClassCastException);
    }

    @Test
    public void testAlways() {
        Promise promise = this.getFactory().resolve(1)
                .always((Function<Integer, Integer>)(r) -> r + 1)
                .always((Function<Integer, Integer>)(r) -> r + 1);

        promise.sync();

        assertEquals(3, promise.getValue());
        assertEquals(PromiseState.FULFILLED, promise.getState());
    }

    @Test
    public void testAlwaysFunctionsAsCatch() {
        Promise promise = this.getFactory().reject(new Throwable())
                .always((r) -> 1)
                .always((Function<Integer, Integer>)(r) -> r + 1)
                .always((Function<Integer, Integer>)(r) -> r + 1);

        promise.sync();

        assertEquals(3, promise.getValue());
        assertEquals(PromiseState.FULFILLED, promise.getState());
    }

    @Test
    public void testCreateResolveSleeper() {
        Promise promise = this.getFactory().create((resolve, reject) -> {
            resolve.accept(true);
        });

        try {
            TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException ignored) {
        }

        assertEquals(true, promise.getValue());
        assertEquals(PromiseState.FULFILLED, promise.getState());
    }

    @Test
    public void testCreateRejectSleeper() {
        Exception exception = new Exception();
        Promise promise = this.getFactory().create((resolve, reject) -> {
            reject.accept(exception);
        });

        try {
            TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException ignored) {
        }

        assertEquals(exception, promise.getValue());
        assertEquals(PromiseState.REJECTED, promise.getState());
    }

    /**
     * This test will fail if promise creation is not done async to the main thread.
     */
    @Test
    public void testSync() {
        Promise promise = this.getFactory().create((resolve, reject) -> {
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException ignored) {
            }
            resolve.accept(true);
        });

        assertEquals(PromiseState.PENDING, promise.getState());
        promise.sync();
        assertEquals(true, promise.getValue());
        assertEquals(PromiseState.FULFILLED, promise.getState());
    }

    @Test
    public void testSyncMultipleCallsNoProblem() {
        Promise promise = this.getFactory().resolve(true);
        promise.sync();
        promise.sync();
        promise.sync();
        promise.sync();

        assertEquals(true, promise.getValue());
        assertEquals(PromiseState.FULFILLED, promise.getState());
    }

    @Test
    public void testThenSync() {
        Promise promise = this.getFactory().create((resolve, reject) -> {
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException ignored) {
            }
            resolve.accept(true);
        });

        assertEquals(PromiseState.PENDING, promise.getState());
        assertEquals(true, promise.thenSync());
        assertEquals(PromiseState.FULFILLED, promise.getState());
    }

    @Test
    public void testThenSyncOpposite() {
        Exception exception = new Exception();
        Promise promise = this.getFactory().create((resolve, reject) -> {
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException ignored) {
            }
            reject.accept(exception);
        });

        assertEquals(PromiseState.PENDING, promise.getState());
        assertEquals(null, promise.thenSync());
        assertEquals(exception, promise.getValue());
        assertEquals(PromiseState.REJECTED, promise.getState());
    }

    @Test
    public void testThenSyncCast() {
        Promise promise = this.getFactory().create((resolve, reject) -> {
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException ignored) {
            }
            resolve.accept("big hello");
        });

        assertEquals(PromiseState.PENDING, promise.getState());

        String result = promise.thenSync(String.class);

        assertEquals("big hello", result);
        assertEquals(PromiseState.FULFILLED, promise.getState());
    }

    @Test
    public void testThenSyncCastOpposite() {
        Exception exception = new Exception();
        Promise promise = this.getFactory().create((resolve, reject) -> {
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException ignored) {
            }
            reject.accept(exception);
        });

        assertEquals(PromiseState.PENDING, promise.getState());

        String result = promise.thenSync(String.class);

        assertEquals(null, result);
        assertEquals(exception, promise.getValue());
        assertEquals(PromiseState.REJECTED, promise.getState());
    }

    @Test
    public void testExceptSync() {
        Exception exception = new Exception();
        Promise promise = this.getFactory().create((resolve, reject) -> {
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException ignored) {
            }
            reject.accept(exception);
        });

        assertEquals(PromiseState.PENDING, promise.getState());
        assertEquals(exception, promise.exceptSync());
        assertEquals(PromiseState.REJECTED, promise.getState());
    }

    @Test
    public void testExceptSyncOpposite() {
        Promise promise = this.getFactory().create((resolve, reject) -> {
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException ignored) {
            }
            resolve.accept(true);
        });

        assertEquals(PromiseState.PENDING, promise.getState());
        assertEquals(null, promise.exceptSync());
        assertEquals(true, promise.getValue());
        assertEquals(PromiseState.FULFILLED, promise.getState());
    }
}
