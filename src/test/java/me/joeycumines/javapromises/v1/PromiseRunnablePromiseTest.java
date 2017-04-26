package me.joeycumines.javapromises.v1;

import me.joeycumines.javapromises.core.CircularResolutionException;
import me.joeycumines.javapromises.core.PromiseState;
import me.joeycumines.javapromises.core.PromiseTest;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Test PromiseRunnable and it's factory in default configuration against the Promise interface.
 */
public class PromiseRunnablePromiseTest extends PromiseTest {
    @Override
    protected PromiseRunnableFactory getFactory() {
        return PromiseRunnableFactory.getInstance();
    }

    @Test
    public void testChainPromisesRejectDirect() {
        PromiseRunnable start = this.getFactory().create((resolve, reject) -> {
        });
        PromiseRunnable end = this.getFactory().resolve(this.getFactory().resolve(this.getFactory().resolve(this.getFactory().resolve(start))));

        assertEquals(PromiseState.PENDING, start.getState());
        assertEquals(PromiseState.PENDING, end.getState());

        Throwable e = new Throwable();
        start.reject(e);

        assertEquals(e, end.exceptSync());
        assertEquals(e, start.exceptSync());

        assertEquals(PromiseState.REJECTED, end.getState());
        assertEquals(e, end.getValue());
        assertEquals(PromiseState.REJECTED, start.getState());
        assertEquals(e, start.getValue());
    }

    @Test
    public void testChainPromisesFulfillDirect() {
        PromiseRunnable start = this.getFactory().create((resolve, reject) -> {
        });
        PromiseRunnable end = this.getFactory().resolve(this.getFactory().resolve(this.getFactory().resolve(this.getFactory().resolve(start))));

        assertEquals(PromiseState.PENDING, start.getState());
        assertEquals(PromiseState.PENDING, end.getState());

        Object value = new Object();
        start.resolve(value);

        assertEquals(value, end.thenSync());
        assertEquals(value, start.thenSync());

        assertEquals(PromiseState.FULFILLED, end.getState());
        assertEquals(value, end.getValue());
        assertEquals(PromiseState.FULFILLED, start.getState());
        assertEquals(value, start.getValue());
    }

    @Test
    public void testChainPromisesCircularDirectWaitForever() {
        PromiseRunnable start = this.getFactory().create((resolve, reject) -> {
        });
        PromiseRunnable end = this.getFactory().resolve(this.getFactory().resolve(this.getFactory().resolve(this.getFactory().resolve(start))));

        assertEquals(PromiseState.PENDING, start.getState());
        assertEquals(PromiseState.PENDING, end.getState());

        // attempt to resolve start with end, since there is no result that will ever appear, it will wait forever
        start.resolve(end);

        assertEquals(PromiseState.PENDING, start.getState());
        assertEquals(PromiseState.PENDING, end.getState());
    }

    @Test
    public void testChainPromisesCircularDirect() {
        PromiseBaseShell one = new PromiseBaseShell();
        PromiseBaseShell two = (new PromiseBaseShell()).finalize(PromiseState.FULFILLED, one);
        PromiseBaseShell three = (new PromiseBaseShell()).finalize(PromiseState.FULFILLED, two);

        one.finalize(PromiseState.FULFILLED, three);

        // one, two, three, now represent a circular promise state

        PromiseRunnable start = this.getFactory().create((resolve, reject) -> {
        });

        try {
            start.resolve(one);
            fail();
        } catch (CircularResolutionException e) {
            assertNotNull(e);
        }

        assertEquals(PromiseState.PENDING, start.getState());
    }
}
