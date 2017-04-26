package me.joeycumines.javapromises.core;

import me.joeycumines.javapromises.v1.BlockingPromise;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;
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
     * Resolve an object.
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
                    fail();
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
                .except((Function<Throwable, Promise>)(e) -> {
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
     * Test that promises will resolve values of other promises.
     */
    @Test
    public void testResolvePromise() {

    }

    /**
     * Test that promises will resolve values of other promises, as far as necessary.
     */
    @Test
    public void testResolvePromiseDeep() {

    }

    @Test
    public void testResolvePrimitive() {

    }

    @Test
    public void testResolveException() {

    }

    @Test
    public void testReject() {

    }

    @Test
    public void testExcept() {

    }

    @Test
    public void testExceptDoubled() {

    }

    @Test
    public void testExceptRethrow() {

    }

    @Test
    public void testExceptReturnException() {

    }

    @Test
    public void testExceptAfterThen() {

    }

    @Test
    public void testExceptAfterExcept() {

    }

    @Test
    public void testExceptResolves() {

    }

    @Test
    public void testThen() {

    }

    @Test
    public void testThenThrow() {

    }

    @Test
    public void testThenReturnException() {

    }

    @Test
    public void testThenAfterThen() {

    }

    @Test
    public void testThenAfterExcept() {

    }

    @Test
    public void testThenResolves() {

    }

    @Test
    public void testAlways() {

    }

    @Test
    public void testAlwaysFunctionsAsCatch() {

    }

    @Test
    public void testAlwaysThrow() {

    }

    @Test
    public void testSync() {

    }

    @Test
    public void testSyncResolved() {

    }

    @Test
    public void testThenSync() {

    }

    @Test
    public void testExceptSync() {

    }
}
