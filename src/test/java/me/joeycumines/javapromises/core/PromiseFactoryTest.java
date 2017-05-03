package me.joeycumines.javapromises.core;

import org.junit.Test;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

import static org.junit.Assert.*;

/**
 * Extend this class to test a {@link PromiseFactory} implementation.
 */
public abstract class PromiseFactoryTest {
    abstract protected PromiseFactory getFactory();

    /**
     * <b>Test {@link PromiseFactory#create(BiConsumer)}</b>
     * <p>
     * --------------------------------------------
     * <p>
     * <b>Creates a promise, and executes it asynchronously.</b>
     * <p>
     * The action parameter format is {@code (fulfill, reject) -> // stuff}.
     * <p>
     * The first call to either fulfill or reject will be the resolved value. They may throw exceptions for subsequent
     * calls. <b>If no call is made within action, then the state of the promise <b>must</b> be {@code PENDING} immediately
     * after action completes.</b> The implementation of {@link BlockingPromise} takes
     * advantage of this behaviour.
     * <p>
     * Any {@code Throwable throwable} that is thrown, within the action, will be the equivalent of calling
     * {@code reject.accept(throwable)}.
     * <p>
     * Calling the reject parameter, within the action, with a null value, will cause a {@link NullPointerException} to
     * be thrown internally, which will cause the returned promise to resolve as {@code REJECTED}, with that exception.
     * <p>
     * A null action parameter will cause a {@link NullPointerException} to be thrown.
     * <p>
     * {@code @param action The task to perform asynchronously.}
     * <p>
     * {@code @param <T>    The type the promise will resolve with.}
     * <p>
     * {@code @return A new promise.}
     * <p>
     * {@code @throws NullPointerException If the action is null.}
     */
    @Test
    public void testCreateCase1CreatesPromiseAsync() {
        AtomicInteger counter = new AtomicInteger();

        Promise<Object> promise = this.getFactory().create((fulfill, reject) -> {
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException ignored) {
            }
            counter.incrementAndGet();

            synchronized (counter) {
                counter.notify();
            }
        });

        assertNotNull(promise);
        assertEquals(PromiseState.PENDING, promise.getState());

        assertEquals(0, counter.get());

        synchronized (counter) {
            while (0 == counter.get()) {
                try {
                    counter.wait();
                } catch (InterruptedException ignored) {
                }
            }
        }

        assertEquals(1, counter.get());
        assertEquals(PromiseState.PENDING, promise.getState());
    }

    /**
     * <b>Test {@link PromiseFactory#create(BiConsumer)}</b>
     * <p>
     * --------------------------------------------
     * <p>
     * Creates a promise, and executes it asynchronously.
     * <p>
     * <b>The action parameter format is {@code (fulfill, reject) -> // stuff}.
     * <p>
     * The first call to either fulfill or reject will be the resolved value. They may throw exceptions for subsequent
     * calls. If no call is made within action, then the state of the promise <b>must</b> be {@code PENDING} immediately
     * after action completes. The implementation of {@link BlockingPromise} takes
     * advantage of this behaviour.</b>
     * <p>
     * Any {@code Throwable throwable} that is thrown, within the action, will be the equivalent of calling
     * {@code reject.accept(throwable)}.
     * <p>
     * Calling the reject parameter, within the action, with a null value, will cause a {@link NullPointerException} to
     * be thrown internally, which will cause the returned promise to resolve as {@code REJECTED}, with that exception.
     * <p>
     * A null action parameter will cause a {@link NullPointerException} to be thrown.
     * <p>
     * {@code @param action The task to perform asynchronously.}
     * <p>
     * {@code @param <T>    The type the promise will resolve with.}
     * <p>
     * {@code @return A new promise.}
     * <p>
     * {@code @throws NullPointerException If the action is null.}
     */
    @Test
    public void testCreateCase2Fulfill() {
        AtomicInteger counter = new AtomicInteger();

        Object value = new Object();

        Promise<Object> promise = this.getFactory().create((fulfill, reject) -> {
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException ignored) {
            }

            fulfill.accept(value);

            counter.incrementAndGet();

            synchronized (counter) {
                counter.notify();
            }
        });

        assertNotNull(promise);
        assertEquals(PromiseState.PENDING, promise.getState());

        assertEquals(0, counter.get());

        synchronized (counter) {
            while (0 == counter.get()) {
                try {
                    counter.wait();
                } catch (InterruptedException ignored) {
                }
            }
        }

        assertEquals(1, counter.get());
        assertEquals(PromiseState.FULFILLED, promise.getState());
        assertEquals(value, promise.thenSync());
        assertEquals(null, promise.exceptSync());
    }

    /**
     * <b>Test {@link PromiseFactory#create(BiConsumer)}</b>
     * <p>
     * --------------------------------------------
     * <p>
     * Creates a promise, and executes it asynchronously.
     * <p>
     * <b>The action parameter format is {@code (fulfill, reject) -> // stuff}.
     * <p>
     * The first call to either fulfill or reject will be the resolved value. They may throw exceptions for subsequent
     * calls. If no call is made within action, then the state of the promise <b>must</b> be {@code PENDING} immediately
     * after action completes. The implementation of {@link BlockingPromise} takes
     * advantage of this behaviour.</b>
     * <p>
     * Any {@code Throwable throwable} that is thrown, within the action, will be the equivalent of calling
     * {@code reject.accept(throwable)}.
     * <p>
     * Calling the reject parameter, within the action, with a null value, will cause a {@link NullPointerException} to
     * be thrown internally, which will cause the returned promise to resolve as {@code REJECTED}, with that exception.
     * <p>
     * A null action parameter will cause a {@link NullPointerException} to be thrown.
     * <p>
     * {@code @param action The task to perform asynchronously.}
     * <p>
     * {@code @param <T>    The type the promise will resolve with.}
     * <p>
     * {@code @return A new promise.}
     * <p>
     * {@code @throws NullPointerException If the action is null.}
     */
    @Test
    public void testCreateCase3Reject() {
        AtomicInteger counter = new AtomicInteger();

        Throwable exception = new Throwable();

        Promise<Object> promise = this.getFactory().create((fulfill, reject) -> {
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException ignored) {
            }

            reject.accept(exception);

            counter.incrementAndGet();

            synchronized (counter) {
                counter.notify();
            }
        });

        assertNotNull(promise);
        assertEquals(PromiseState.PENDING, promise.getState());

        assertEquals(0, counter.get());

        synchronized (counter) {
            while (0 == counter.get()) {
                try {
                    counter.wait();
                } catch (InterruptedException ignored) {
                }
            }
        }

        assertEquals(1, counter.get());
        assertEquals(PromiseState.REJECTED, promise.getState());
        assertEquals(null, promise.thenSync());
        assertEquals(exception, promise.exceptSync());
    }

    /**
     * <b>Test {@link PromiseFactory#create(BiConsumer)}</b>
     * <p>
     * --------------------------------------------
     * <p>
     * Creates a promise, and executes it asynchronously.
     * <p>
     * <b>The action parameter format is {@code (fulfill, reject) -> // stuff}.
     * <p>
     * The first call to either fulfill or reject will be the resolved value. They may throw exceptions for subsequent
     * calls. If no call is made within action, then the state of the promise <b>must</b> be {@code PENDING} immediately
     * after action completes. The implementation of {@link BlockingPromise} takes
     * advantage of this behaviour.</b>
     * <p>
     * Any {@code Throwable throwable} that is thrown, within the action, will be the equivalent of calling
     * {@code reject.accept(throwable)}.
     * <p>
     * Calling the reject parameter, within the action, with a null value, will cause a {@link NullPointerException} to
     * be thrown internally, which will cause the returned promise to resolve as {@code REJECTED}, with that exception.
     * <p>
     * A null action parameter will cause a {@link NullPointerException} to be thrown.
     * <p>
     * {@code @param action The task to perform asynchronously.}
     * <p>
     * {@code @param <T>    The type the promise will resolve with.}
     * <p>
     * {@code @return A new promise.}
     * <p>
     * {@code @throws NullPointerException If the action is null.}
     */
    @Test
    public void testCreateCase4FulfillThenBoth() {
        AtomicInteger counter = new AtomicInteger();

        Object value = new Object();

        Promise<Object> promise = this.getFactory().create((fulfill, reject) -> {
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException ignored) {
            }

            fulfill.accept(value);

            try {
                fulfill.accept(new Object());
            } catch (Throwable ignored) {
            }

            try {
                reject.accept(new Throwable());
            } catch (Throwable ignored) {
            }

            counter.incrementAndGet();

            synchronized (counter) {
                counter.notify();
            }
        });

        assertNotNull(promise);
        assertEquals(PromiseState.PENDING, promise.getState());

        assertEquals(0, counter.get());

        synchronized (counter) {
            while (0 == counter.get()) {
                try {
                    counter.wait();
                } catch (InterruptedException ignored) {
                }
            }
        }

        assertEquals(1, counter.get());
        assertEquals(PromiseState.FULFILLED, promise.getState());
        assertEquals(value, promise.thenSync());
        assertEquals(null, promise.exceptSync());
    }

    /**
     * <b>Test {@link PromiseFactory#create(BiConsumer)}</b>
     * <p>
     * --------------------------------------------
     * <p>
     * Creates a promise, and executes it asynchronously.
     * <p>
     * <b>The action parameter format is {@code (fulfill, reject) -> // stuff}.
     * <p>
     * The first call to either fulfill or reject will be the resolved value. They may throw exceptions for subsequent
     * calls. If no call is made within action, then the state of the promise <b>must</b> be {@code PENDING} immediately
     * after action completes. The implementation of {@link BlockingPromise} takes
     * advantage of this behaviour.</b>
     * <p>
     * Any {@code Throwable throwable} that is thrown, within the action, will be the equivalent of calling
     * {@code reject.accept(throwable)}.
     * <p>
     * Calling the reject parameter, within the action, with a null value, will cause a {@link NullPointerException} to
     * be thrown internally, which will cause the returned promise to resolve as {@code REJECTED}, with that exception.
     * <p>
     * A null action parameter will cause a {@link NullPointerException} to be thrown.
     * <p>
     * {@code @param action The task to perform asynchronously.}
     * <p>
     * {@code @param <T>    The type the promise will resolve with.}
     * <p>
     * {@code @return A new promise.}
     * <p>
     * {@code @throws NullPointerException If the action is null.}
     */
    @Test
    public void testCreateCase5RejectThenBoth() {
        AtomicInteger counter = new AtomicInteger();

        Throwable exception = new Throwable();

        Promise<Object> promise = this.getFactory().create((fulfill, reject) -> {
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException ignored) {
            }

            reject.accept(exception);

            try {
                fulfill.accept(new Object());
            } catch (Throwable ignored) {
            }

            try {
                reject.accept(new Throwable());
            } catch (Throwable ignored) {
            }

            counter.incrementAndGet();

            synchronized (counter) {
                counter.notify();
            }
        });

        assertNotNull(promise);
        assertEquals(PromiseState.PENDING, promise.getState());

        assertEquals(0, counter.get());

        synchronized (counter) {
            while (0 == counter.get()) {
                try {
                    counter.wait();
                } catch (InterruptedException ignored) {
                }
            }
        }

        assertEquals(1, counter.get());
        assertEquals(PromiseState.REJECTED, promise.getState());
        assertEquals(null, promise.thenSync());
        assertEquals(exception, promise.exceptSync());
    }

    //00000

    /**
     * <b>Test {@link PromiseFactory#create(BiConsumer)}</b>
     * <p>
     * --------------------------------------------
     * <p>
     * Creates a promise, and executes it asynchronously.
     * <p>
     * <b>The action parameter format is {@code (fulfill, reject) -> // stuff}.
     * <p>
     * The first call to either fulfill or reject will be the resolved value. They may throw exceptions for subsequent
     * calls. If no call is made within action, then the state of the promise <b>must</b> be {@code PENDING} immediately
     * after action completes. The implementation of {@link BlockingPromise} takes
     * advantage of this behaviour.</b>
     * <p>
     * Any {@code Throwable throwable} that is thrown, within the action, will be the equivalent of calling
     * {@code reject.accept(throwable)}.
     * <p>
     * Calling the reject parameter, within the action, with a null value, will cause a {@link NullPointerException} to
     * be thrown internally, which will cause the returned promise to resolve as {@code REJECTED}, with that exception.
     * <p>
     * A null action parameter will cause a {@link NullPointerException} to be thrown.
     * <p>
     * {@code @param action The task to perform asynchronously.}
     * <p>
     * {@code @param <T>    The type the promise will resolve with.}
     * <p>
     * {@code @return A new promise.}
     * <p>
     * {@code @throws NullPointerException If the action is null.}
     */
    @Test
    public void testCreateCase6FulfillNull() {
        AtomicInteger counter = new AtomicInteger();

        Promise<Object> promise = this.getFactory().create((fulfill, reject) -> {
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException ignored) {
            }

            fulfill.accept(null);

            counter.incrementAndGet();

            synchronized (counter) {
                counter.notify();
            }
        });

        assertNotNull(promise);
        assertEquals(PromiseState.PENDING, promise.getState());

        assertEquals(0, counter.get());

        synchronized (counter) {
            while (0 == counter.get()) {
                try {
                    counter.wait();
                } catch (InterruptedException ignored) {
                }
            }
        }

        assertEquals(1, counter.get());
        assertEquals(PromiseState.FULFILLED, promise.getState());
        assertEquals(null, promise.thenSync());
        assertEquals(null, promise.exceptSync());
    }

    /**
     * <b>Test {@link PromiseFactory#create(BiConsumer)}</b>
     * <p>
     * --------------------------------------------
     * <p>
     * Creates a promise, and executes it asynchronously.
     * <p>
     * The action parameter format is {@code (fulfill, reject) -> // stuff}.
     * <p>
     * The first call to either fulfill or reject will be the resolved value. They may throw exceptions for subsequent
     * calls. If no call is made within action, then the state of the promise <b>must</b> be {@code PENDING} immediately
     * after action completes. The implementation of {@link BlockingPromise} takes
     * advantage of this behaviour.
     * <p>
     * Any {@code Throwable throwable} that is thrown, within the action, will be the equivalent of calling
     * {@code reject.accept(throwable)}.
     * <p>
     * <b>Calling the reject parameter, within the action, with a null value, will cause a {@link NullPointerException} to
     * be thrown internally, which will cause the returned promise to resolve as {@code REJECTED}, with that exception.</b>
     * <p>
     * A null action parameter will cause a {@link NullPointerException} to be thrown.
     * <p>
     * {@code @param action The task to perform asynchronously.}
     * <p>
     * {@code @param <T>    The type the promise will resolve with.}
     * <p>
     * {@code @return A new promise.}
     * <p>
     * {@code @throws NullPointerException If the action is null.}
     */
    @Test
    public void testCreateCase7RejectNull() {
        AtomicInteger counter = new AtomicInteger();

        Promise<Object> promise = this.getFactory().create((fulfill, reject) -> {
            counter.incrementAndGet();

            reject.accept(null);

            // should not be called
            counter.incrementAndGet();

        });

        try {
            TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException ignored) {
        }

        promise.sync();

        assertEquals(1, counter.get());
        assertEquals(PromiseState.REJECTED, promise.getState());
        assertEquals(null, promise.thenSync());
        assertNotNull(promise.exceptSync());
        assertTrue(promise.exceptSync() instanceof NullPointerException);
    }

    /**
     * <b>Test {@link PromiseFactory#create(BiConsumer)}</b>
     * <p>
     * --------------------------------------------
     * <p>
     * Creates a promise, and executes it asynchronously.
     * <p>
     * The action parameter format is {@code (fulfill, reject) -> // stuff}.
     * <p>
     * The first call to either fulfill or reject will be the resolved value. They may throw exceptions for subsequent
     * calls. If no call is made within action, then the state of the promise <b>must</b> be {@code PENDING} immediately
     * after action completes. The implementation of {@link BlockingPromise} takes
     * advantage of this behaviour.
     * <p>
     * <b>Any {@code Throwable throwable} that is thrown, within the action, will be the equivalent of calling
     * {@code reject.accept(throwable)}.</b>
     * <p>
     * Calling the reject parameter, within the action, with a null value, will cause a {@link NullPointerException} to
     * be thrown internally, which will cause the returned promise to resolve as {@code REJECTED}, with that exception.
     * <p>
     * A null action parameter will cause a {@link NullPointerException} to be thrown.
     * <p>
     * {@code @param action The task to perform asynchronously.}
     * <p>
     * {@code @param <T>    The type the promise will resolve with.}
     * <p>
     * {@code @return A new promise.}
     * <p>
     * {@code @throws NullPointerException If the action is null.}
     */
    @Test
    public void testCreateCase8ThrowInternally() {
        AtomicInteger counter = new AtomicInteger();

        RuntimeException exception = new RuntimeException();

        Promise<Object> promise = this.getFactory().create((fulfill, reject) -> {
            counter.incrementAndGet();

            throw exception;
        });

        try {
            TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException ignored) {
        }

        promise.sync();

        assertEquals(1, counter.get());
        assertEquals(PromiseState.REJECTED, promise.getState());
        assertEquals(null, promise.thenSync());
        assertEquals(exception, promise.exceptSync());
    }

    /**
     * <b>Test {@link PromiseFactory#create(BiConsumer)}</b>
     * <p>
     * --------------------------------------------
     * <p>
     * Creates a promise, and executes it asynchronously.
     * <p>
     * The action parameter format is {@code (fulfill, reject) -> // stuff}.
     * <p>
     * The first call to either fulfill or reject will be the resolved value. They may throw exceptions for subsequent
     * calls. If no call is made within action, then the state of the promise <b>must</b> be {@code PENDING} immediately
     * after action completes. The implementation of {@link BlockingPromise} takes
     * advantage of this behaviour.
     * <p>
     * Any {@code Throwable throwable} that is thrown, within the action, will be the equivalent of calling
     * {@code reject.accept(throwable)}.
     * <p>
     * Calling the reject parameter, within the action, with a null value, will cause a {@link NullPointerException} to
     * be thrown internally, which will cause the returned promise to resolve as {@code REJECTED}, with that exception.
     * <p>
     * <b>A null action parameter will cause a {@link NullPointerException} to be thrown.</b>
     * <p>
     * {@code @param action The task to perform asynchronously.}
     * <p>
     * {@code @param <T>    The type the promise will resolve with.}
     * <p>
     * {@code @return A new promise.}
     * <p>
     * {@code @throws NullPointerException If the action is null.}
     */
    @Test
    public void testCreateCase9NoActionThrows() {
        try {
            this.getFactory().create(null);
            fail("did not throw exception");
        } catch (NullPointerException e) {
            assertNotNull(e);
        }
    }

    /**
     * <b>Test {@link PromiseFactory#create(BiConsumer)}</b>
     * <p>
     * --------------------------------------------
     * <p>
     * Creates a promise, and executes it asynchronously.
     * <p>
     * The action parameter format is {@code (fulfill, reject) -> // stuff}.
     * <p>
     * The first call to either fulfill or reject will be the resolved value. They may throw exceptions for subsequent
     * calls. If no call is made within action, then the state of the promise <b>must</b> be {@code PENDING} immediately
     * after action completes. The implementation of {@link BlockingPromise} takes
     * advantage of this behaviour.
     * <p>
     * <b>Any {@code Throwable throwable} that is thrown, within the action, will be the equivalent of calling
     * {@code reject.accept(throwable)}.</b>
     * <p>
     * Calling the reject parameter, within the action, with a null value, will cause a {@link NullPointerException} to
     * be thrown internally, which will cause the returned promise to resolve as {@code REJECTED}, with that exception.
     * <p>
     * A null action parameter will cause a {@link NullPointerException} to be thrown.
     * <p>
     * {@code @param action The task to perform asynchronously.}
     * <p>
     * {@code @param <T>    The type the promise will resolve with.}
     * <p>
     * {@code @return A new promise.}
     * <p>
     * {@code @throws NullPointerException If the action is null.}
     */
    @Test
    public void testCreateCase10ThrowInternallyAfterFulfill() {
        AtomicInteger counter = new AtomicInteger();

        RuntimeException exception = new RuntimeException();

        Promise<Integer> promise = this.getFactory().create((fulfill, reject) -> {
            counter.incrementAndGet();

            fulfill.accept(42);

            throw exception;
        });

        try {
            TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException ignored) {
        }

        promise.sync();

        assertEquals(1, counter.get());
        assertEquals(PromiseState.FULFILLED, promise.getState());
        assertEquals(42, promise.thenSync().intValue());
        assertEquals(null, promise.exceptSync());
    }

    /**
     * <b>Test {@link PromiseFactory#reject(Throwable)}</b>
     * <p>
     * --------------------------------------------
     * <p>
     * <b>Create a new {@code REJECTED} promise, with a provided reason for rejection.</b>
     * <p>
     * This MUST happen synchronously (the returned Promise MUST be {@code REJECTED}, not {@code PENDING} to reject
     * asynchronously).
     * <p>
     * A {@code null} reason will result in a {@link NullPointerException} being thrown.
     * <p>
     * {@code @param reason The value this will reject with.}
     * {@code @return A new promise.}
     * {@code @throws NullPointerException If the reason is null.}
     */
    @Test
    public void testRejectCase1Rejects() {
        Throwable exception = new Throwable();

        Promise<Integer> promise = this.getFactory().reject(exception);

        assertNotNull(promise);
        assertEquals(exception, promise.exceptSync());
        assertEquals(null, promise.thenSync());
        assertEquals(PromiseState.REJECTED, promise.getState());
    }

    /**
     * <b>Test {@link PromiseFactory#reject(Throwable)}</b>
     * <p>
     * --------------------------------------------
     * <p>
     * Create a new {@code REJECTED} promise, with a provided reason for rejection.
     * <p>
     * <b>This MUST happen synchronously (the returned Promise MUST be {@code REJECTED}, not {@code PENDING} to reject
     * asynchronously).</b>
     * <p>
     * A {@code null} reason will result in a {@link NullPointerException} being thrown.
     * <p>
     * {@code @param reason The value this will reject with.}
     * {@code @return A new promise.}
     * {@code @throws NullPointerException If the reason is null.}
     */
    @Test
    public void testRejectCase2IsSynchronous() {
        Throwable exception = new Throwable();

        Promise<Integer> promise = this.getFactory().reject(exception);

        assertEquals(PromiseState.REJECTED, promise.getState());

        assertEquals(exception, promise.exceptSync());
        assertEquals(null, promise.thenSync());
    }

    /**
     * <b>Test {@link PromiseFactory#reject(Throwable)}</b>
     * <p>
     * --------------------------------------------
     * <p>
     * Create a new {@code REJECTED} promise, with a provided reason for rejection.
     * <p>
     * This MUST happen synchronously (the returned Promise MUST be {@code REJECTED}, not {@code PENDING} to reject
     * asynchronously).
     * <p>
     * <b>A {@code null} reason will result in a {@link NullPointerException} being thrown.</b>
     * <p>
     * {@code @param reason The value this will reject with.}
     * {@code @return A new promise.}
     * {@code @throws NullPointerException If the reason is null.}
     */
    @Test
    public void testRejectCase3NullReason() {
        try {
            this.getFactory().reject(null);
            fail("did not throw exception");
        } catch (NullPointerException e) {
            assertNotNull(e);
        }
    }

    /**
     * <b>Test {@link PromiseFactory#fulfill(Object)}</b>
     * <p>
     * --------------------------------------------
     * <p>
     * <b>Create a new promise that is {@code FULFILLED} with the provided value.</b>
     * <p>
     * This MUST happen synchronously (the returned Promise MUST be {@code FULFILLED}, not {@code PENDING} to fulfill
     * asynchronously).
     * <p>
     * {@code @param value The value to fulfill.}
     * {@code @param <T>   The type of the value the promise resolved.}
     * {@code @return A new promise.}
     */
    @Test
    public void testFulfillCase1Fulfilled() {
        Promise<Integer> promise = this.getFactory().fulfill(5);

        assertNotNull(promise);
        assertEquals(5, promise.thenSync().intValue());
        assertEquals(null, promise.exceptSync());
        assertEquals(PromiseState.FULFILLED, promise.getState());
    }

    /**
     * <b>Test {@link PromiseFactory#fulfill(Object)}</b>
     * <p>
     * --------------------------------------------
     * <p>
     * Create a new promise that is {@code FULFILLED} with the provided value.
     * <p>
     * <b>This MUST happen synchronously (the returned Promise MUST be {@code FULFILLED}, not {@code PENDING} to fulfill
     * asynchronously).</b>
     * <p>
     * {@code @param value The value to fulfill.}
     * {@code @param <T>   The type of the value the promise resolved.}
     * {@code @return A new promise.}
     */
    @Test
    public void testFulfillCase2Synchronous() {
        Promise<String> promise = this.getFactory().fulfill("value s");

        assertEquals(PromiseState.FULFILLED, promise.getState());

        assertEquals("value s", promise.thenSync());
        assertEquals(null, promise.exceptSync());
    }

    /**
     * <b>Test {@link PromiseFactory#fulfill(Object)}, ensure null value works.</b>
     * <p>
     * --------------------------------------------
     * <p>
     * Create a new promise that is {@code FULFILLED} with the provided value.
     * <p>
     * This MUST happen synchronously (the returned Promise MUST be {@code FULFILLED}, not {@code PENDING} to fulfill
     * asynchronously).
     * <p>
     * {@code @param value The value to fulfill.}
     * {@code @param <T>   The type of the value the promise resolved.}
     * {@code @return A new promise.}
     */
    @Test
    public void testFulfillCase3NullValue() {
        Promise<Integer> promise = this.getFactory().fulfill(null);

        assertNotNull(promise);
        assertEquals(null, promise.thenSync());
        assertEquals(null, promise.exceptSync());
        assertEquals(PromiseState.FULFILLED, promise.getState());
    }

    /**
     * Ensure that {@link BlockingPromise} works with this factory.
     */
    @Test
    public void testBlockingPromiseGetPromise() {
        BlockingPromise blocker = new BlockingPromise(this.getFactory());
        assertNotNull(blocker.getPromise());
        assertEquals(PromiseState.PENDING, blocker.getPromise().getState());
    }

    /**
     * Ensure that {@link BlockingPromise} works with this factory.
     */
    @Test
    public void testBlockingPromiseResolve() {
        BlockingPromise<Object> blocker = new BlockingPromise<Object>(this.getFactory());
        Object value = new Object();
        blocker.fulfill(value);
        assertEquals(value, blocker.getPromise().thenSync());
    }

    /**
     * Ensure that {@link BlockingPromise} works with this factory.
     */
    @Test
    public void testBlockingPromiseReject() {
        BlockingPromise blocker = new BlockingPromise(this.getFactory());
        Throwable value = new Throwable();
        blocker.reject(value);
        assertEquals(value, blocker.getPromise().exceptSync());
    }

    /**
     * Ensure that {@link BlockingPromise} works with this factory.
     */
    @Test
    public void testBlockingPromiseResolveTypes() {
        BlockingPromise<String> blocker = new BlockingPromise<String>(this.getFactory());
        assertNotNull(blocker.getPromise());
        assertEquals(PromiseState.PENDING, blocker.getPromise().getState());

        blocker.fulfill("yes");

        Promise<? extends String> promise = blocker.getPromise();
        assertEquals("yes no?", promise.thenSync() + " no?");
        assertNull(promise.exceptSync());
        assertEquals(PromiseState.FULFILLED, promise.getState());
        assertEquals("yes", promise.thenSync());
    }
}
