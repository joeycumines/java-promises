package me.joeycumines.javapromises.core;

import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Extend this class to test a PromiseTyped implementation.
 */
public abstract class PromiseTypedTest {
    abstract protected PromiseFactory getFactory();

    /**
     * Must implement both Promise and PromiseTyped.
     */
    @Test
    public void testImplementsBothCreate() {
        PromiseTyped promise = (PromiseTyped) this.getFactory().create((resolve, reject) -> {
            resolve.accept(true);
        });

        assertNotNull(promise);
        assertEquals(promise, promise.getPromise());

        promise.sync();
        assertEquals(true, promise.getValue());
        assertEquals(PromiseState.FULFILLED, promise.getState());
    }

    /**
     * Must implement both Promise and PromiseTyped.
     */
    @Test
    public void testImplementsBothResolve() {
        PromiseTyped promise = (PromiseTyped) this.getFactory().resolve(true);

        assertNotNull(promise);
        assertEquals(promise, promise.getPromise());

        assertEquals(true, promise.getValue());
        assertEquals(PromiseState.FULFILLED, promise.getState());
    }

    /**
     * Must implement both Promise and PromiseTyped.
     */
    @Test
    public void testImplementsBothReject() {
        Exception exception = new Exception();
        PromiseTyped promise = (PromiseTyped) this.getFactory().reject(exception);

        assertNotNull(promise);
        assertEquals(promise, promise.getPromise());

        assertEquals(exception, promise.getValue());
        assertEquals(PromiseState.REJECTED, promise.getState());
    }

    @Test
    public void testThen() {
        @SuppressWarnings("unchecked") PromiseTyped<String> first = (PromiseTyped<String>) this.getFactory().resolve("hello");

        Promise second = first.then((r) -> {
            // this is unchecked, magical
            return r + " world";
        });

        second.sync();

        assertEquals("hello world", second.getValue());
        assertEquals(PromiseState.FULFILLED, second.getState());

        assertEquals("hello", first.getValue());
        assertEquals(PromiseState.FULFILLED, first.getState());
    }

    @Test
    public void testThenWrongType() {
        // this bit is fine, works as a normal promise
        Object object = new Object();
        @SuppressWarnings("unchecked") PromiseTyped<String> first = (PromiseTyped<String>) this.getFactory().resolve(object);

        assertEquals(PromiseState.FULFILLED, first.getState());
        assertEquals(object, first.getValue());

        // calling then will result in the promise being resolved with an error within the then (a cast error)
        Promise second = first.then((r) -> true);

        second.sync();

        assertEquals(PromiseState.REJECTED, second.getState());
        assertTrue(second.getValue() instanceof ClassCastException);
    }

    @Test
    public void testThenSync() {
        @SuppressWarnings("unchecked") PromiseTyped<String> first = (PromiseTyped<String>) this.getFactory().resolve("hello");
        String value = first.thenSync();
        value += " world";
        assertEquals("hello world", value);
        assertEquals(PromiseState.FULFILLED, first.getState());
    }

    @Test
    public void testThenSyncWrongType() {
        // this bit is fine, works as a normal promise
        Object object = new Object();
        @SuppressWarnings("unchecked") PromiseTyped<String> first = (PromiseTyped<String>) this.getFactory().resolve(object);

        assertEquals(PromiseState.FULFILLED, first.getState());
        assertEquals(object, first.getValue());

        try {
            // note, we have to assign the value here or it wont try to cast, apparently
            String value = first.thenSync();
            fail();
        } catch (ClassCastException e) {
            assertNotNull(e);
        }
    }
}
