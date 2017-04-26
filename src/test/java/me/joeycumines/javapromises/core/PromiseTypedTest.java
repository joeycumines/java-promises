package me.joeycumines.javapromises.core;

import org.junit.Test;

import java.util.function.BiConsumer;

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
}
