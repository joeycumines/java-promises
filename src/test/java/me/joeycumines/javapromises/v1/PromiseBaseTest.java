package me.joeycumines.javapromises.v1;

import me.joeycumines.javapromises.core.MutatedStateException;
import me.joeycumines.javapromises.core.PendingValueException;
import me.joeycumines.javapromises.core.PromiseState;
import me.joeycumines.javapromises.core.SelfResolutionException;
import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.*;

public class PromiseBaseTest {
    @Test
    public void testGetValuePendingValueException() {
        PromiseBaseShell promise = new PromiseBaseShell();

        try {
            promise.getValue();
            Assert.fail("did not throw PendingValueException");
        } catch (PendingValueException e) {
            Assert.assertNotEquals(null, e);
        }
    }

    @Test
    public void testGetStateUnset() {
        PromiseBaseShell promise = new PromiseBaseShell();
        Assert.assertEquals(PromiseState.PENDING, promise.getState());
    }

    @Test
    public void testFinalizePendingIllegalArgumentException() {
        PromiseBaseShell promise = new PromiseBaseShell();

        try {
            promise.finalize(PromiseState.PENDING, null);
            Assert.fail("did not throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            Assert.assertNotEquals(null, e);
        }
    }

    @Test
    public void testFinalizeFulfill() {
        PromiseBaseShell promise = new PromiseBaseShell();
        Object value = new Object();

        promise.finalize(PromiseState.FULFILLED, value);

        Assert.assertEquals(PromiseState.FULFILLED, promise.getState());
        Assert.assertEquals(value, promise.getValue());
    }

    @Test
    public void testFinalizeFulfillNull() {
        PromiseBaseShell promise = new PromiseBaseShell();

        promise.finalize(PromiseState.FULFILLED, null);

        Assert.assertEquals(PromiseState.FULFILLED, promise.getState());
        Assert.assertEquals(null, promise.getValue());
    }

    @Test
    public void testFinalizeReject() {
        PromiseBaseShell promise = new PromiseBaseShell();
        Object value = new Exception();

        promise.finalize(PromiseState.REJECTED, value);

        Assert.assertEquals(PromiseState.REJECTED, promise.getState());
        Assert.assertEquals(value, promise.getValue());
    }

    @Test
    public void testFinalizeRejectNull() {
        PromiseBaseShell promise = new PromiseBaseShell();

        promise.finalize(PromiseState.REJECTED, null);

        Assert.assertEquals(PromiseState.REJECTED, promise.getState());
        Assert.assertEquals(null, promise.getValue());
    }

    @Test
    public void testFinalizeRejectIllegalArgumentException() {
        PromiseBaseShell promise = new PromiseBaseShell();
        Object value = new Object();

        try {
            promise.finalize(PromiseState.REJECTED, value);
            Assert.fail("did not throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            Assert.assertNotEquals(null, e);
        }

        Assert.assertEquals(PromiseState.PENDING, promise.getState());
    }

    @Test
    public void testFinalizeSelfResolutionException() {
        PromiseBaseShell promise = new PromiseBaseShell();

        try {
            promise.finalize(PromiseState.FULFILLED, promise);
            Assert.fail("did not throw SelfResolutionException");
        } catch (SelfResolutionException e) {
            Assert.assertNotEquals(null, e);
        }

        Assert.assertEquals(PromiseState.PENDING, promise.getState());
    }

    @Test
    public void testFinalizeMutatedStateException() {
        PromiseBaseShell promise = new PromiseBaseShell();
        Object value = new Object();

        promise.finalize(PromiseState.FULFILLED, value);

        try {
            promise.finalize(PromiseState.REJECTED, null);
            Assert.fail("did not throw MutatedStateException");
        } catch (MutatedStateException e) {
            Assert.assertNotEquals(null, e);
        }

        Assert.assertEquals(PromiseState.FULFILLED, promise.getState());
        Assert.assertEquals(value, promise.getValue());
    }
}
