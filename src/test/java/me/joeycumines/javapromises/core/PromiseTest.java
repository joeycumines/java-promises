package me.joeycumines.javapromises.core;

import me.joeycumines.javapromises.v1.BlockingPromise;
import org.junit.Test;

import static org.junit.Assert.*;

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
}
