package me.joeycumines.javapromises.v1;

import org.junit.Test;

import java.util.function.Consumer;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class PromiseTest {
    @Test
    public void testRunner() {
        Promise promise = new Promise();
        assertEquals(null, promise.getRunner());
        PromiseRunnerInterface runner = mock(PromiseRunnerInterface.class);
        promise.setRunner(runner);
        assertEquals(runner, promise.getRunner());
    }

    @Test
    public void testRunnerTwice() {
        Promise promise = new Promise();
        assertEquals(null, promise.getRunner());
        PromiseRunnerInterface runner = mock(PromiseRunnerInterface.class);
        promise.setRunner(runner);
        try {
            promise.setRunner(mock(PromiseRunnerInterface.class));
            fail();
        } catch (IllegalStateException e) {
            assertNotNull(e);
        }
        assertEquals(runner, promise.getRunner());
    }

    @Test
    public void testAction() {
        Promise promise = new Promise();
        assertNull(promise.getAction());
        Consumer<Promise> action = (p) -> {
        };
        promise.setAction(action);
        assertEquals(action, promise.getAction());
    }

    @Test
    public void testActionTwice() {
        Promise promise = new Promise();
        assertNull(promise.getAction());
        Consumer<Promise> action = (p) -> {
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
        Promise promise = new Promise();
        assertFalse(promise.isRun());
        promise.setRun();
        assertTrue(promise.isRun());
    }

    @Test
    public void testSetRunTwice() {
        Promise promise = new Promise();
        assertFalse(promise.isRun());
        promise.setRun();
        assertTrue(promise.isRun());
        promise.setRun();
        assertTrue(promise.isRun());
    }

    @Test
    public void testRun() {

    }
}
