package me.joeycumines.javapromises.core;

import org.junit.Test;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import static org.junit.Assert.*;

/**
 * Extend this class to test a {@link Promise} implementation.
 * <p>
 * It requires a {@link PromiseFactory} instance to work, so you will have to implement that as well. You probably want
 * to extend {@link PromiseApi} as the base, and implement {@link PromiseApiTest}, as well.
 */
public abstract class PromiseTest {
    abstract protected PromiseFactory getFactory();

    /**
     * <b>Test {@link Promise#getState()}</b>
     * <p>
     * --------------------------------------------
     * <p>
     * <b>Get the current state of the promise.</b>
     * <p>
     * A promise's initial state is {@code PENDING}, will change <b>at most</b> once, to either resolved states,
     * {@code FULFILLED} or {@code REJECTED}.
     * <p>
     * {@code @return The state of the promise.}
     */
    @Test
    public void testGetState() {
        assertEquals(PromiseState.PENDING, this.getFactory().create((fulfill, reject) -> {
        }).getState());
        assertEquals(PromiseState.FULFILLED, this.getFactory().fulfill(new Object()).getState());
        assertEquals(PromiseState.REJECTED, this.getFactory().reject(new Throwable()).getState());
    }

    /**
     * <b>Test {@link Promise#then(Function)}</b>
     * <p>
     * --------------------------------------------
     * <p>
     * If the callback returns a {@code null} value, then the returned promise will resolve as {@code FULFILLED} with
     * value {@code null}.
     */
    @Test
    public void testThenCallbackReturnNull() {
        Promise<Integer> promise = this.getFactory().fulfill(42);
        assertEquals(42, promise.thenSync().intValue());
        promise = promise.then((value) -> {
            return null;
        });
        assertNull(promise.thenSync());
        assertNull(promise.exceptSync());
        assertEquals(PromiseState.FULFILLED, promise.getState());
    }

    /**
     * <b>Test {@link Promise#except(Function)}</b>
     * <p>
     * --------------------------------------------
     * <p>
     * If the callback returns a {@code null} value, then the returned promise will resolve as {@code FULFILLED} with
     * value {@code null}.
     */
    @Test
    public void testExceptCallbackReturnNull() {
        Throwable exception = new Throwable();
        Promise<Integer> promise = this.getFactory().reject(exception);
        assertEquals(exception, promise.exceptSync());
        promise = promise.except((value) -> {
            return null;
        });
        assertNull(promise.thenSync());
        assertNull(promise.exceptSync());
        assertEquals(PromiseState.FULFILLED, promise.getState());
    }

    /**
     * <b>Test {@link Promise#except(Function)} and {@link Promise#except(BiConsumer)}</b>
     * <p>
     * --------------------------------------------
     * <p>
     * If {@code this} resolved with the {@code FULFILLED} state, then the returned promise will reflect the state and
     * value of {@code this}, and the <b>callback will not be run</b>.
     */
    @Test
    public void testExceptCallbackDoesNothingOnFulfill() {
        Promise<Integer> promise = this.getFactory().fulfill(42);
        assertEquals(42, promise.thenSync().intValue());

        promise = promise.except((e) -> {
            throw new RuntimeException();
        });
        assertEquals(42, promise.thenSync().intValue());

        promise = promise.except((e, fulfill) -> {
            throw new RuntimeException();
        });
        assertEquals(42, promise.thenSync().intValue());

        promise = promise.except((e) -> {
            throw new RuntimeException();
        });
        assertEquals(42, promise.thenSync().intValue());

        promise = promise.except((e, fulfill) -> {
            throw new RuntimeException();
        });
        assertEquals(42, promise.thenSync().intValue());

        promise = promise.then((value) -> {
            return null;
        });
        assertNull(promise.thenSync());
        assertNull(promise.exceptSync());
        assertEquals(PromiseState.FULFILLED, promise.getState());
    }

    /**
     * <b>Test {@link Promise#then(Function)} and {@link Promise#then(BiConsumer)}</b>
     * <p>
     * --------------------------------------------
     * <p>
     * If {@code this} resolved with the {@code REJECTED} state, then the returned promise will reflect the state and
     * value of {@code this}, and the <b>callback will not be run</b>.
     */
    @Test
    public void testThenCallbackDoesNothingOnReject() {
        Throwable exception = new Throwable();
        Promise<Integer> promise = this.getFactory().reject(exception);
        assertEquals(exception, promise.exceptSync());

        promise = promise.then((r) -> this.getFactory().fulfill(5));
        assertEquals(exception, promise.exceptSync());

        promise = promise.then((r, fulfill) -> fulfill.accept(5));
        assertEquals(exception, promise.exceptSync());

        promise = promise.then((r) -> this.getFactory().fulfill(5));
        assertEquals(exception, promise.exceptSync());

        promise = promise.then((r, fulfill) -> fulfill.accept(5));
        assertEquals(exception, promise.exceptSync());

        promise = promise.except((value) -> {
            return null;
        });
        assertNull(promise.thenSync());
        assertNull(promise.exceptSync());
        assertEquals(PromiseState.FULFILLED, promise.getState());
    }

    @Test
    public void testLargeChainOfThen() {
        AtomicInteger counter = new AtomicInteger();
        Promise<Integer> promise = this.getFactory().fulfill(0);

        for (int x = 0; x < 50; x++) {
            Promise<Integer> next = promise.then((r, fulfill) -> {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException ignored) {
                }
                fulfill.accept(r + 1);
            });
            assertNotEquals(promise, next);
            promise = next;
            counter.incrementAndGet();
        }

        assertEquals(PromiseState.PENDING, promise.getState());
        assertEquals(50, promise.thenSync().intValue());
        assertEquals(50, counter.get());
        assertEquals(PromiseState.FULFILLED, promise.getState());
    }

    @Test
    public void testAlwaysThrowInnerException() {
        RuntimeException exception = new RuntimeException();
        Promise<Integer> promise = this.getFactory().fulfill(67);
        assertEquals(67, promise.thenSync().intValue());
        promise = promise.always((r, e) -> {
            assertEquals(67, r.intValue());
            assertEquals(null, e);
            throw exception;
        });
        assertEquals(exception, promise.exceptSync());
        assertNull(promise.thenSync());
        assertEquals(PromiseState.REJECTED, promise.getState());
    }

    @Test
    public void testAlwaysReturnFulfilled() {
        Promise<Integer> promise = this.getFactory().reject(new Throwable())
                .always((r, e) -> {
                    assertNotNull(e);
                    assertNull(r);

                    return this.getFactory().fulfill(125);
                });

        assertEquals(125, promise.thenSync().intValue());
        assertNull(promise.exceptSync());
        assertEquals(PromiseState.FULFILLED, promise.getState());
    }

    @Test
    public void testAlwaysReturnRejected() {
        Exception exception = new Exception();

        Promise<Integer> promise = this.getFactory().reject(new Throwable())
                .always((r, e) -> {
                    assertNotNull(e);
                    assertNull(r);

                    return this.getFactory().reject(exception);
                });

        assertEquals(exception, promise.exceptSync());
        assertNull(promise.thenSync());
        assertEquals(PromiseState.REJECTED, promise.getState());
    }

    @Test
    public void testAlwaysReturnPending() {
        BlockingPromise<Integer> blocker = new BlockingPromise<>(this.getFactory());

        Promise<Integer> promise = this.getFactory().reject(new Throwable())
                .always((r, e) -> {
                    assertNotNull(e);
                    assertNull(r);

                    return blocker.getPromise();
                });

        assertEquals(PromiseState.PENDING, promise.getState());

        blocker.fulfill(125);

        assertEquals(125, promise.thenSync().intValue());
        assertNull(promise.exceptSync());
        assertEquals(PromiseState.FULFILLED, promise.getState());
    }

    @Test
    public void testLogicalExceptionHandling() {
        BlockingPromise<Integer> blocker = new BlockingPromise<>(this.getFactory());

        Promise<Integer> promise = this.getFactory().reject(new Throwable())
                .always((r, e) -> {
                    assertNotNull(e);
                    assertNull(r);

                    return blocker.getPromise();
                });

        assertEquals(PromiseState.PENDING, promise.getState());

        AtomicInteger counter = new AtomicInteger();

        RuntimeException exception = new RuntimeException();

        promise = promise
                .then((r, fulfill) -> counter.incrementAndGet())
                .then((r, fulfill) -> counter.incrementAndGet())
                .then((r, fulfill) -> counter.incrementAndGet())
                .always((r, e) -> this.getFactory().reject(exception))
                .then((r, fulfill) -> counter.incrementAndGet())
                .except((e) -> {
                    assertEquals(exception, e);
                    return this.getFactory().fulfill(3);
                })
                .except((e, fulfill) -> counter.incrementAndGet())
                .then((r, fulfill) -> fulfill.accept((Integer) r + 2))
                .except((e, fulfill) -> counter.incrementAndGet())
                .except((e, fulfill) -> counter.incrementAndGet())
                .always((r, e) -> {
                    assertNull(e);
                    assertNotNull(r);
                    return this.getFactory().fulfill((Integer) r + 1);
                });

        assertEquals(PromiseState.PENDING, promise.getState());

        blocker.reject(new Throwable());

        assertEquals(6, promise.thenSync().intValue());
        assertEquals(null, promise.exceptSync());
        assertEquals(PromiseState.FULFILLED, promise.getState());

        assertEquals(0, counter.get());
    }

    @Test
    public void testChainTypesThenFulfillSuccess() {
        Promise<Integer> promise = this.getFactory().fulfill(4)
                .then((value, fulfill) -> fulfill.accept(value * 2));
        assertEquals(8, promise.thenSync().intValue());
        assertNull(promise.exceptSync());
        assertEquals(PromiseState.FULFILLED, promise.getState());
    }

    @Test
    public void testChainTypesThenFulfillSuccessNoCall() {
        Promise<Integer> promise = this.getFactory().fulfill(4)
                .then((value, fulfill) -> {
                });
        assertEquals(null, promise.thenSync());
        assertNull(promise.exceptSync());
        assertEquals(PromiseState.FULFILLED, promise.getState());
    }

    @Test
    public void testChainTypesThenFulfillDifferentType() {
        Promise<String> promise = this.getFactory().fulfill(4)
                .then((value, fulfill) -> fulfill.accept("hello"));
        assertEquals("hello", promise.thenSync());
        assertNull(promise.exceptSync());
        assertEquals(PromiseState.FULFILLED, promise.getState());
    }

    @Test
    public void testThenFulfillThrowException() {
        RuntimeException e = new RuntimeException();
        Promise<Integer> promise = this.getFactory().fulfill(4)
                .then((value, fulfill) -> {
                    throw e;
                });
        assertEquals(null, promise.thenSync());
        assertEquals(e, promise.exceptSync());
        assertEquals(PromiseState.REJECTED, promise.getState());
    }

    @Test
    public void testExceptFulfillThrowException() {
        RuntimeException e = new RuntimeException();
        Promise<Object> promise = this.getFactory().reject(new Throwable())
                .except((value, fulfill) -> {
                    throw e;
                });
        assertEquals(null, promise.thenSync());
        assertEquals(e, promise.exceptSync());
        assertEquals(PromiseState.REJECTED, promise.getState());
    }

    @Test
    public void testThenFulfillThrowExceptionAfterFulfill() {
        RuntimeException e = new RuntimeException();
        Promise<String> promise = this.getFactory().fulfill(4)
                .then((value, fulfill) -> {
                    fulfill.accept("actual");
                    throw e;
                });
        assertEquals("actual", promise.thenSync());
        assertEquals(null, promise.exceptSync());
        assertEquals(PromiseState.FULFILLED, promise.getState());
    }

    @Test
    public void testExceptFulfillThrowExceptionAfterFulfill() {
        RuntimeException e = new RuntimeException();
        Promise<String> promise = this.getFactory().reject(new RuntimeException());
        promise = promise.except((value, fulfill) -> {
            fulfill.accept("actual");
            throw e;
        });
        assertEquals("actual", promise.thenSync());
        assertEquals(null, promise.exceptSync());
        assertEquals(PromiseState.FULFILLED, promise.getState());
    }

    @Test
    public void testExceptFulfillNoCall() {
        RuntimeException e = new RuntimeException();
        Promise<String> promise = this.getFactory().reject(e);
        promise = promise.except((exception, fulfill) -> {
        });
        assertEquals(null, promise.thenSync());
        assertEquals(null, promise.exceptSync());
        assertEquals(PromiseState.FULFILLED, promise.getState());
    }

    @Test
    public void testExceptFulfillWithSubtype() {
        RuntimeException e = new RuntimeException();
        Promise<Number> promise = this.getFactory().reject(e);
        promise = promise.except((exception, fulfill) -> {
            //noinspection UnnecessaryBoxing
            fulfill.accept(new Integer(4));
        });
        assertEquals(4, promise.thenSync());
        assertEquals(null, promise.exceptSync());
        assertEquals(PromiseState.FULFILLED, promise.getState());
    }

    @Test
    public void testThenFulfillNoCallback() {
        RuntimeException e = new RuntimeException();
        Promise<Object> promise = this.getFactory().reject(e)
                .then((value, fulfill) -> fulfill.accept(null));
        assertEquals(null, promise.thenSync());
        assertEquals(e, promise.exceptSync());
        assertEquals(PromiseState.REJECTED, promise.getState());
    }

    @Test
    public void testExceptFulfillNoCallback() {
        Object value = new Object();
        Promise<Object> promise = this.getFactory().fulfill(value)
                .except((e, fulfill) -> fulfill.accept(null));
        assertEquals(value, promise.thenSync());
        assertEquals(null, promise.exceptSync());
        assertEquals(PromiseState.FULFILLED, promise.getState());
    }

    @Test
    public void testThenFulfilled() {
        Object value = new Object();
        Promise<Object> promise = this.getFactory().fulfill(null)
                .then((r) -> this.getFactory().fulfill(value));
        assertEquals(value, promise.thenSync());
        assertEquals(null, promise.exceptSync());
        assertEquals(PromiseState.FULFILLED, promise.getState());
    }

    @Test
    public void testThenRejected() {
        RuntimeException value = new RuntimeException();
        Promise<Object> promise = this.getFactory().fulfill(null)
                .then((r) -> this.getFactory().reject(value));
        assertEquals(null, promise.thenSync());
        assertEquals(value, promise.exceptSync());
        assertEquals(PromiseState.REJECTED, promise.getState());
    }

    @Test
    public void testThenNoCallback() {
        RuntimeException value = new RuntimeException();
        Promise<Object> promise = this.getFactory().reject(value)
                .then((r) -> this.getFactory().fulfill(null));
        assertEquals(null, promise.thenSync());
        assertEquals(value, promise.exceptSync());
        assertEquals(PromiseState.REJECTED, promise.getState());
    }

    @Test
    public void testExceptFulfilled() {
        Object value = new Object();
        Promise<Object> promise = this.getFactory().reject(new Throwable())
                .except((r) -> this.getFactory().fulfill(value));
        assertEquals(value, promise.thenSync());
        assertEquals(null, promise.exceptSync());
        assertEquals(PromiseState.FULFILLED, promise.getState());
    }

    @Test
    public void testExceptRejected() {
        RuntimeException value = new RuntimeException();
        Promise<Object> promise = this.getFactory().reject(new Throwable())
                .except((r) -> this.getFactory().reject(value));
        assertEquals(null, promise.thenSync());
        assertEquals(value, promise.exceptSync());
        assertEquals(PromiseState.REJECTED, promise.getState());
    }

    @Test
    public void testExceptNoCallback() {
        Object value = new Object();
        Promise<Object> promise = this.getFactory().fulfill(value)
                .except((r) -> this.getFactory().fulfill(null));
        assertEquals(value, promise.thenSync());
        assertEquals(null, promise.exceptSync());
        assertEquals(PromiseState.FULFILLED, promise.getState());
    }

    @Test
    public void testAlwaysFulfilled() {
        Object value = new Object();
        Promise<Object> promise = this.getFactory().reject(new Throwable())
                .always((r, e) -> this.getFactory().fulfill(value));
        assertEquals(value, promise.thenSync());
        assertEquals(null, promise.exceptSync());
        assertEquals(PromiseState.FULFILLED, promise.getState());
    }

    @Test
    public void testAlwaysRejected() {
        RuntimeException value = new RuntimeException();
        Promise<Object> promise = this.getFactory().fulfill(null)
                .always((r, e) -> this.getFactory().reject(value));
        assertEquals(null, promise.thenSync());
        assertEquals(value, promise.exceptSync());
        assertEquals(PromiseState.REJECTED, promise.getState());
    }

    /**
     * Test that the input from a rejected promise is correct, for all chaining methods.
     */
    @Test
    public void testInputRejected() {
        AtomicInteger compare = new AtomicInteger();
        AtomicInteger counter = new AtomicInteger();
        Consumer<Integer> check = (times) -> {
            assertEquals(compare.addAndGet(times), counter.get());
        };

        RuntimeException exception = new RuntimeException();
        Promise<Object> input = this.getFactory().reject(exception);

        // then does nothing
        input.then((r) -> {
            counter.incrementAndGet();
            return null;
        }).sync();
        check.accept(0);

        input.then((r, fulfill) -> {
            counter.incrementAndGet();
        }).sync();
        check.accept(0);

        // except has the correct
        input.except((e) -> {
            if (e == exception) {
                counter.incrementAndGet();
            }
            return null;
        }).sync();
        check.accept(1);

        input.except((e, fulfill) -> {
            if (e == exception) {
                counter.incrementAndGet();
            }
        }).sync();
        check.accept(1);

        // always has the correct
        input.always((value, e) -> {
            if (value == null && e == exception) {
                counter.incrementAndGet();
            }
            return null;
        }).sync();
        check.accept(1);
    }

    /**
     * Test that the input from a fulfilled promise is correct, for all chaining methods.
     */
    @Test
    public void testInputFulfilled() {
        AtomicInteger compare = new AtomicInteger();
        AtomicInteger counter = new AtomicInteger();
        Consumer<Integer> check = (times) -> {
            assertEquals(compare.addAndGet(times), counter.get());
        };

        Object value = new Object();
        Promise<Object> input = this.getFactory().fulfill(value);

        // then has the correct
        input.then((r) -> {
            if (value == r) {
                counter.incrementAndGet();
            }
            return null;
        }).sync();
        check.accept(1);

        input.then((r, fulfill) -> {
            if (value == r) {
                counter.incrementAndGet();
            }
        }).sync();
        check.accept(1);

        // except does nothing
        input.except((e) -> {
            counter.incrementAndGet();
            return null;
        }).sync();
        check.accept(0);

        input.except((e, fulfill) -> {
            counter.incrementAndGet();
        }).sync();
        check.accept(0);

        // always has the correct
        input.always((r, e) -> {
            if (r == value && e == null) {
                counter.incrementAndGet();
            }
            return null;
        }).sync();
        check.accept(1);
    }
}
