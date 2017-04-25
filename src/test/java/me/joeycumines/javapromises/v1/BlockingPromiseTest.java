package me.joeycumines.javapromises.v1;

import me.joeycumines.javapromises.core.PromiseState;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Cbf mocking this out, it uses PromiseRunnableFactory.
 */
public class BlockingPromiseTest {
    @Test
    public void testGetPromise() {
        BlockingPromise blocker = new BlockingPromise(PromiseRunnableFactory.getInstance());
        assertNotNull(blocker.getPromise());
        assertEquals(PromiseState.PENDING, blocker.getPromise().getState());
    }

    @Test
    public void testResolve() {
        BlockingPromise blocker = new BlockingPromise(PromiseRunnableFactory.getInstance());
        Object value = new Object();
        blocker.resolve(value);
        assertEquals(value, blocker.getPromise().thenSync());
    }

    @Test
    public void testReject() {
        BlockingPromise blocker = new BlockingPromise(PromiseRunnableFactory.getInstance());
        Throwable value = new Throwable();
        blocker.reject(value);
        assertEquals(value, blocker.getPromise().exceptSync());
    }
}
