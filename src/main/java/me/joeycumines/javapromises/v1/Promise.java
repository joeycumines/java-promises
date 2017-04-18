package me.joeycumines.javapromises.v1;

import me.joeycumines.javapromises.core.PromiseInterface;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * First-pass promise implementation.
 * <p>
 * The focus was maximum flexibility, with the ability to extend or implement promises for various use cases.
 * <p>
 * The most complex part of this implementation is the action property which is explained below:
 * - action is used to simplify and provide safety for all paths
 * - action will only be executed AT MOST once, either manually, or part of internal logic
 * - action is not required
 * - executing actions are handled by the PromiseRunnerInterface
 */
public class Promise extends PromiseBase {
    /**
     * The action that may be executed by this promise, using the runner.
     * <p>
     * The promise (single argument) provided will be this.
     * <p>
     * Can only be set (to something non-null) once.
     */
    private Consumer<Promise> action;

    /*
     * A handler instance that provides a way to "run" this promise (this.action).
     *
     * Can only be set (to something non-null) once.
     */
    private PromiseRunnerInterface runner;

    /**
     * Has this promise been run yet.
     * <p>
     * Promises can only be run once, therefore run can only be marked so once.
     */
    private boolean run;

    /**
     * A list of promises that will be run on finalization of this.
     */
    private final ConcurrentLinkedQueue<Promise> subscriberQueue;

    public Promise() {
        this(null, null);
    }

    public Promise(Consumer<Promise> action) {
        this(action, null);
    }

    public Promise(Consumer<Promise> action, PromiseRunnerInterface runner) {
        super();

        this.action = action;
        this.runner = runner;
        this.run = false;
        this.subscriberQueue = new ConcurrentLinkedQueue<Promise>();
    }

    public Consumer<Promise> getAction() {
        if (null != this.action) {
            return this.action;
        }

        synchronized (this.lock) {
            return this.action;
        }
    }

    private void assureActionNotSet() {
        if (null != this.action) {
            throw new IllegalStateException("[immutable] the action has already been set to something NN once, cannot be set again");
        }
    }

    public void setAction(Consumer<Promise> action) throws IllegalStateException {
        this.assureActionNotSet();

        synchronized (this.lock) {
            this.assureActionNotSet();

            this.action = action;
        }
    }

    public PromiseRunnerInterface getRunner() {
        if (null != this.runner) {
            return this.runner;
        }

        synchronized (this.lock) {
            return runner;
        }
    }

    private void assureRunnerNotSet() {
        if (null != this.runner) {
            throw new IllegalStateException("[immutable] the runner has already been set to something NN once, cannot be set again");
        }
    }

    public void setRunner(PromiseRunnerInterface runner) throws IllegalStateException {
        this.assureRunnerNotSet();

        synchronized (this.lock) {
            this.assureRunnerNotSet();

            this.runner = runner;
        }
    }

    public boolean isRun() {
        if (this.run) {
            return true;
        }

        synchronized (this.lock) {
            return this.run;
        }
    }

    public void setRun() {
        if (this.run) {
            return;
        }

        synchronized (this.lock) {
            this.run = true;
        }
    }

    public void run() {
        synchronized (this.lock) {
            if (null == this.runner) {
                throw new RunPromiseException(this, "no runner was provided");
            }

            if (null == this.action) {
                throw new RunPromiseException(this, "no action was provided");
            }

            this.runner.runPromise(this);
        }
    }

    public ConcurrentLinkedQueue<Promise> getSubscriberQueue() {
        return this.subscriberQueue;
    }

    @Override
    public PromiseInterface then(Function callback) {
        return null;
    }

    @Override
    public PromiseInterface except(Function callback) {
        return null;
    }
}
