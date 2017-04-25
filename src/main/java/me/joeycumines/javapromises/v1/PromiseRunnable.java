package me.joeycumines.javapromises.v1;

import me.joeycumines.javapromises.core.*;

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
 * - neither action nor runner is required IF you are not calling {@link #run()}, and will manage the state externally
 * - executing actions are handled by the PromiseRunner, implement that however you wish
 */
public class PromiseRunnable extends PromiseBase {
    private static final Function<PromiseState, Boolean> ONLY_FULFILLED = (state) -> PromiseState.FULFILLED == state;
    private static final Function<PromiseState, Boolean> ONLY_REJECTED = (state) -> PromiseState.REJECTED == state;
    private static final Function<PromiseState, Boolean> ANY_FINALIZED = (state) -> PromiseState.PENDING != state;

    /**
     * The action that may be executed by this promise, using the runner.
     * <p>
     * The promise (single argument) provided will be this.
     * <p>
     * Can only be set (to something non-null) once.
     */
    private Consumer<PromiseRunnable> action;

    /*
     * A handler instance that provides a way to "run" this promise (this.action).
     *
     * Can only be set (to something non-null) once.
     */
    private PromiseRunner runner;

    /**
     * Has this promise been run yet.
     * <p>
     * Promises can only be run once, therefore run can only be marked so once.
     */
    private boolean run;

    /**
     * A list of promises that will be run on finalization of this.
     */
    private final ConcurrentLinkedQueue<PromiseRunnable> subscriberQueue;

    public PromiseRunnable() {
        this(null, null);
    }

    public PromiseRunnable(PromiseRunner runner) {
        this(runner, null);
    }

    public PromiseRunnable(PromiseRunner runner, Consumer<PromiseRunnable> action) {
        super();

        this.action = action;
        this.runner = runner;
        this.run = false;
        this.subscriberQueue = new ConcurrentLinkedQueue<PromiseRunnable>();
    }

    public Consumer<PromiseRunnable> getAction() {
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

    public PromiseRunnable setAction(Consumer<PromiseRunnable> action) throws IllegalStateException {
        this.assureActionNotSet();

        synchronized (this.lock) {
            this.assureActionNotSet();

            this.action = action;
        }

        return this;
    }

    public PromiseRunner getRunner() {
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

    public PromiseRunnable setRunner(PromiseRunner runner) throws IllegalStateException {
        this.assureRunnerNotSet();

        synchronized (this.lock) {
            this.assureRunnerNotSet();

            this.runner = runner;
        }

        return this;
    }

    public boolean isRun() {
        if (this.run) {
            return true;
        }

        synchronized (this.lock) {
            return this.run;
        }
    }

    public PromiseRunnable setRun() {
        if (this.run) {
            return this;
        }

        synchronized (this.lock) {
            this.run = true;
        }

        return this;
    }

    public PromiseRunnable run() {
        synchronized (this.lock) {
            if (null == this.runner) {
                throw new RunPromiseException(this, "no runner was provided");
            }

            if (null == this.action) {
                throw new RunPromiseException(this, "no action was provided");
            }

            if (this.run) {
                throw new RunPromiseException(this, "the promise was already run");
            }

            this.run = true;

            this.runner.runPromise(this);
        }

        return this;
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
    protected PromiseRunnable finalize(PromiseState state, Object value) throws IllegalArgumentException, MutatedStateException, SelfResolutionException {
        // protected > public
        super.finalize(state, value);

        // by now the state of this promise is actually finalized, so we can deal with (potentially) additional threads
        this.broadcast();

        return this;
    }

    @Override
    public PromiseRunnable reject(Exception value) {
        // protected > public
        super.reject(value);

        return this;
    }

    @Override
    public PromiseRunnable resolve(Object value) {
        // protected > public
        super.resolve(value);

        return this;
    }

    /**
     * Generate a promise, that will have it's state set, and it's result determined, by a callback triggered based on a
     * condition, otherwise if the condition fails, then it will inherit the same result as us via the subscriber queue.
     *
     * @param callback  The callback to be executed, the value returned can be a Promise.
     * @param condition The condition to trigger if the callback will be called, based on the state.
     * @return A new promise, that will be automatically run after this resolves.
     */
    private Promise build(Function callback, Function<PromiseState, Boolean> condition) {
        // create the action
        Consumer<PromiseRunnable> action = (promise) -> {
            try {
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
            } catch (Exception e) {
                promise.reject(e);
            }
        };

        // build a promise which inherits our runner
        PromiseRunnable promise = new PromiseRunnable(this.getRunner(), action);

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
    public Promise then(Function callback) {
        return this.build(callback, ONLY_FULFILLED);
    }

    @Override
    public Promise except(Function callback) {
        return this.build(callback, ONLY_REJECTED);
    }

    @Override
    public Promise always(Function callback) {
        return this.build(callback, ANY_FINALIZED);
    }

    /**
     * Constructor shorthand, use the fluid-style setters + the run method.
     *
     * @return PromiseRunnable
     */
    public static PromiseRunnable create() {
        return new PromiseRunnable();
    }
}