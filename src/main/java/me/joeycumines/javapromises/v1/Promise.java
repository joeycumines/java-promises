package me.joeycumines.javapromises.v1;

import me.joeycumines.javapromises.core.PromiseState;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

/**
 * First-pass promise implementation.
 *
 * The focus was maximum flexibility, with the ability to extend or implement promises for various use cases.
 *
 * The most complex part of this implementation is the action property which is explained below:
 * - action is used to simplify and provide safety for all paths
 * - action will only be executed AT MOST once, either manually, or part of internal logic
 * - action is not required
 * - executing actions are handled by the PromiseRunnerInterface
 */
public class Promise {
    /**
     * The action that may be executed by this promise.
     *
     * Not required.
     */
    private Consumer<Promise> action;

    /*
     * A handler instance that provides a way to "run" this promise (this.action).
     *
     * Not required.
     */
    private PromiseRunnerInterface runner;

    /**
     * Has this promise been run yet. Promises can only be run once.
     *
     * Internal use only.
     */
    private boolean run;

    private Object value;

    private PromiseState state;

    private ConcurrentLinkedQueue<Promise> subscriberList;

    protected void ass() {

    }

    public Promise(Consumer<Promise> action) {
    }
}
