package me.joeycumines.javapromises.v1;

import org.junit.Test;

import java.util.function.Consumer;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class PromiseRunnableTest {
    @Test
    public void testRunner() {
        PromiseRunnable promise = new PromiseRunnable();
        assertEquals(null, promise.getRunner());
        PromiseRunner runner = mock(PromiseRunner.class);
        promise.setRunner(runner);
        assertEquals(runner, promise.getRunner());
    }

    @Test
    public void testRunnerTwice() {
        PromiseRunnable promise = new PromiseRunnable();
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
        PromiseRunnable promise = new PromiseRunnable();
        assertNull(promise.getAction());
        Consumer<PromiseRunnable> action = (p) -> {
        };
        promise.setAction(action);
        assertEquals(action, promise.getAction());
    }

    @Test
    public void testActionTwice() {
        PromiseRunnable promise = new PromiseRunnable();
        assertNull(promise.getAction());
        Consumer<PromiseRunnable> action = (p) -> {
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
        PromiseRunnable promise = new PromiseRunnable();
        assertFalse(promise.isRun());
        promise.setRun();
        assertTrue(promise.isRun());
    }

    @Test
    public void testSetRunTwice() {
        PromiseRunnable promise = new PromiseRunnable();
        assertFalse(promise.isRun());
        promise.setRun();
        assertTrue(promise.isRun());
        promise.setRun();
        assertTrue(promise.isRun());
    }

    @Test
    public void testRunNoAction() {
        PromiseRunnable promise = new PromiseRunnable();
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
        PromiseRunnable promise = new PromiseRunnable();
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
        PromiseRunnable promise = new PromiseRunnable();
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
        PromiseRunnable promise = new PromiseRunnable();
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
