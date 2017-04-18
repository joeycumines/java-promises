package me.joeycumines.javapromises.v1;

import me.joeycumines.javapromises.core.MutatedStateException;
import me.joeycumines.javapromises.core.PromiseInterface;
import me.joeycumines.javapromises.core.SelfResolutionException;
import me.joeycumines.javapromises.core.PromiseState;

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
    private static final Function<PromiseState, Boolean> ONLY_FULFILLED = (state) -> PromiseState.FULFILLED == state;
    private static final Function<PromiseState, Boolean> ONLY_REJECTED = (state) -> PromiseState.REJECTED == state;

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

    /**
     * This should only ever be used internally, when we have definitely already resolved.
     */
    private void broadcast() {
        while (!this.subscriberQueue.isEmpty()) {
            // no we will not be safe, if it's in the queue it needs to be in a waiting state
            this.subscriberQueue.poll().run();
        }
    }

    @Override
    public void finalize(PromiseState state, Object value) throws IllegalArgumentException, MutatedStateException, SelfResolutionException {
        super.finalize(state, value);

        // by now the state of this promise is actually finalized, so we can deal with (potentially) additional threads
        this.broadcast();
    }

    public void fulfill(Object value) {
        this.finalize(PromiseState.FULFILLED, value);
    }

    public void reject(Exception value) {
        this.finalize(PromiseState.REJECTED, value);
    }

    public void resolve(Object value) {
        if (null == value || !(value instanceof PromiseInterface)) {
            this.fulfill(value);
            return;
        }

        if (this == value) {
            throw new SelfResolutionException(this);
        }

        //noinspection ConstantConditions
        PromiseInterface promise = (PromiseInterface) value;
        promise.sync();
        this.finalize(promise.getState(), promise.getValue());
    }

    /**
     * Generate a promise, that will have it's state set, and it's result determined, by a callback triggered based on a
     * condition, otherwise if the condition fails, then it will inherit the same result as us via the subscriber queue.
     *
     * @param callback  The callback to be executed, the value returned can be a PromiseInterface.
     * @param condition The condition to trigger if the callback will be called, based on the state.
     * @return A new promise, that will be automatically run after this resolves.
     */
    private PromiseInterface build(Function callback, Function<PromiseState, Boolean> condition) {
        // create the action
        Consumer<Promise> action = (promise) -> {
            // load the parent's (this) finalized values, to be fed into and built on for promise
            PromiseState state = this.getState();
            Object value = this.getValue();

            if (condition.apply(state)) {
                //noinspection unchecked
                promise.resolve(callback.apply(value));
                return;
            }

            // we didn't meet the conditions to trigger the callback, just inherit the state
            promise.finalize(state, value);
        };

        // build a promise which inherits our runner
        Promise promise = new Promise(action, this.getRunner());

        // add this new promise as a subscriber
        this.subscriberQueue.offer(promise);

        // if we are actually already done, trigger another broadcast so our subscribers get notified
        if (PromiseState.PENDING != this.getState()) {
            this.broadcast();
        }
        // otherwise the promise will be notified in due time, after this resolves

        return promise;
    }

    @Override
    public PromiseInterface then(Function callback) {
        return this.build(callback, ONLY_FULFILLED);
    }

    @Override
    public PromiseInterface except(Function callback) {
        return this.build(callback, ONLY_REJECTED);
    }
}
