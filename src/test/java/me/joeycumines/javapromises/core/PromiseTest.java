package me.joeycumines.javapromises.core;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static org.junit.Assert.*;

/**
 * Extend this class to test a {@link Promise} implementation.
 * <p>
 * It requires a {@link PromiseFactory} instance to work, so you will have to implement that as well. You probably want
 * to extend {@link PromiseApi} as the base, and implement {@link PromiseApiTest}, as well.
 */
public abstract class PromiseTest {
    abstract protected PromiseFactory getFactory();

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
