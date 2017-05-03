package me.joeycumines.javapromises.v1;

import me.joeycumines.javapromises.core.PromiseFactory;
import me.joeycumines.javapromises.core.PromiseTest;
import org.junit.Test;

import java.util.function.Consumer;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class PromiseRunnableTest extends PromiseTest {
    @Override
    protected PromiseFactory getFactory() {
        return PromiseRunnableFactory.getInstance();
    }

    @Test
    public void testRunner() {
        PromiseRunnable<Object> promise = new PromiseRunnable<Object>();
        assertEquals(null, promise.getRunner());
        PromiseRunner runner = mock(PromiseRunner.class);
        promise.setRunner(runner);
        assertEquals(runner, promise.getRunner());
    }

    @Test
    public void testRunnerTwice() {
        PromiseRunnable<Object> promise = new PromiseRunnable<Object>();
        assertEquals(null, promise.getRunner());
        PromiseRunner runner = mock(PromiseRunner.class);
        promise.setRunner(runner);
        try {
            promise.setRunner(mock(PromiseRunner.class));
            fail();
        } catch (IllegalStateException e) {
            assertNotNull(e);
        }
        assertEquals(runner, promise.getRunner());
    }

    @Test
    public void testAction() {
        PromiseRunnable<Object> promise = new PromiseRunnable<Object>();
        assertNull(promise.getAction());
        Consumer<PromiseRunnable<? super Object>> action = (p) -> {
        };
        promise.setAction(action);
        assertEquals(action, promise.getAction());
    }

    @Test
    public void testActionTwice() {
        PromiseRunnable<Object> promise = new PromiseRunnable<Object>();
        assertNull(promise.getAction());
        Consumer<PromiseRunnable<? super Object>> action = (p) -> {
        };
        promise.setAction(action);
        try {
            promise.setAction(action);
            fail();
        } catch (IllegalStateException e) {
            assertNotNull(e);
        }
        assertEquals(action, promise.getAction());
    }

    @Test
    public void testSetRun() {
        PromiseRunnable<Object> promise = new PromiseRunnable<Object>();
        assertFalse(promise.isRun());
        promise.setRun();
        assertTrue(promise.isRun());
    }

    @Test
    public void testSetRunTwice() {
        PromiseRunnable<Object> promise = new PromiseRunnable<Object>();
        assertFalse(promise.isRun());
        promise.setRun();
        assertTrue(promise.isRun());
        promise.setRun();
        assertTrue(promise.isRun());
    }

    @Test
    public void testRunNoAction() {
        PromiseRunnable<Object> promise = new PromiseRunnable<Object>();
        PromiseRunner runner = mock(PromiseRunner.class);
        promise.setRunner(runner);

        try {
            promise.run();
            fail();
        } catch (RunPromiseException e) {
            assertNotNull(e);
        }

        verify(runner, never()).runPromise(any());
        assertFalse(promise.isRun());
    }

    @Test
    public void testRunNoRunner() {
        PromiseRunnable<Object> promise = new PromiseRunnable<Object>();
        PromiseRunner runner = mock(PromiseRunner.class);
        promise.setAction((p) -> {
        });

        try {
            promise.run();
            fail();
        } catch (RunPromiseException e) {
            assertNotNull(e);
        }

        verify(runner, never()).runPromise(any());
        assertFalse(promise.isRun());
    }

    @Test
    public void testRunAlreadyRun() {
        PromiseRunnable<Object> promise = new PromiseRunnable<Object>();
        PromiseRunner runner = mock(PromiseRunner.class);
        promise.setRunner(runner);
        promise.setAction((p) -> {
        });

        assertFalse(promise.isRun());
        promise.setRun();
        assertTrue(promise.isRun());

        try {
            promise.run();
            fail();
        } catch (RunPromiseException e) {
            assertNotNull(e);
        }

        verify(runner, never()).runPromise(any());
        assertTrue(promise.isRun());
    }

    @Test
    public void testRun() {
        PromiseRunnable<Object> promise = new PromiseRunnable<Object>();
        PromiseRunner runner = mock(PromiseRunner.class);
        promise.setRunner(runner);
        promise.setAction((p) -> {
        });

        promise.run();

        verify(runner, times(1)).runPromise(promise);
        assertTrue(promise.isRun());

        // try again, will fail
        try {
            promise.run();
            fail();
        } catch (RunPromiseException e) {
            assertNotNull(e);
        }
    }
}
