package me.joeycumines.javapromises.v1;

import me.joeycumines.javapromises.core.*;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
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
public class PromiseRunnable<T> extends PromiseBase<T> {
    /**
     * The action that may be executed by this promise, using the runner.
     * <p>
     * The promise (single argument) provided will be this.
     * <p>
     * Can only be set (to something non-null) once.
     */
    private Consumer<PromiseRunnable<T>> action;

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
    private final ConcurrentLinkedQueue<PromiseRunnable<?>> subscriberQueue;

    public PromiseRunnable() {
        this(null, null);
    }

    public PromiseRunnable(PromiseRunner runner) {
        this(runner, null);
    }

    public PromiseRunnable(PromiseRunner runner, Consumer<PromiseRunnable<T>> action) {
        super();

        this.action = action;
        this.runner = runner;
        this.run = false;
        this.subscriberQueue = new ConcurrentLinkedQueue<PromiseRunnable<?>>();
    }

    public Consumer<PromiseRunnable<T>> getAction() {
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

    public PromiseRunnable<T> setAction(Consumer<PromiseRunnable<T>> action) throws IllegalStateException {
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

    public PromiseRunnable<T> setRunner(PromiseRunner runner) throws IllegalStateException {
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

    public PromiseRunnable<T> setRun() {
        if (this.run) {
            return this;
        }

        synchronized (this.lock) {
            this.run = true;
        }

        return this;
    }

    public PromiseRunnable<T> run() {
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

    @Override
    public PromiseRunnable<T> reject(Throwable exception) throws MutatedStateException, NullPointerException {
        super.reject(exception);
        this.broadcast();
        return this;
    }

    @Override
    public PromiseRunnable<T> fulfill(T value) throws SelfResolutionException, MutatedStateException {
        super.fulfill(value);
        this.broadcast();
        return this;
    }

    @Override
    public PromiseRunnable<T> resolve(Promise<? extends T> promise) throws SelfResolutionException, MutatedStateException {
        super.resolve(promise);
        this.broadcast();
        return this;
    }

    private void broadcast() {
        while (PromiseState.PENDING != this.getState() && !this.subscriberQueue.isEmpty()) {
            // no we will not be safe, if it's in the queue it needs to be in a waiting state
            PromiseRunnable<?> promise = this.subscriberQueue.poll();
            // we do need to be aware that we could have concurrent access though
            if (null != promise) {
                promise.run();
            }
        }
    }

    private <U> PromiseRunnable<U> subscribe(PromiseRunnable<U> promise) {
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
    public <U> Promise<U> then(Function<? super T, ? extends Promise<? extends U>> callback) {
        Consumer<PromiseRunnable<U>> action = (promise) -> {
            try {
                // inherit the exception if the parent (this) REJECTED, without running the callback
                if (PromiseState.REJECTED == this.getState()) {
                    promise.reject(this.getException());
                    return;
                }

                promise.resolve(callback.apply(this.getValue()));
            } catch (Throwable e) {
                promise.reject(e);
            }
        };

        return this.subscribe(new PromiseRunnable<>(this.getRunner(), action));
    }

    @Override
    public Promise<T> except(Function<Throwable, ? extends Promise<? extends T>> callback) {
        Consumer<PromiseRunnable<T>> action = (promise) -> {
            try {
                // use the same value as the parent if the parent FULFILLED
                if (PromiseState.FULFILLED == this.getState()) {
                    promise.fulfill(this.getValue());
                    return;
                }

                promise.resolve(callback.apply(this.getException()));
            } catch (Throwable e) {
                promise.reject(e);
            }
        };

        return this.subscribe(new PromiseRunnable<>(this.getRunner(), action));
    }

    @Override
    public <U> Promise<U> always(BiFunction<? super T, Throwable, ? extends Promise<? extends U>> callback) {
        Consumer<PromiseRunnable<U>> action = (promise) -> {
            try {
                promise.resolve(callback.apply(this.getValue(), this.getException()));
            } catch (Throwable e) {
                promise.reject(e);
            }
        };

        return this.subscribe(new PromiseRunnable<>(this.getRunner(), action));
    }

    @Override
    public <U> Promise<U> then(BiConsumer<? super T, Consumer<? super U>> callback) {
        Consumer<PromiseRunnable<U>> action = (promise) -> {
            try {
                // inherit the exception if the parent (this) REJECTED, without running the callback
                if (PromiseState.REJECTED == this.getState()) {
                    promise.reject(this.getException());
                    return;
                }

                // MAY resolve promise
                callback.accept(this.getValue(), promise::fulfill);

                // if promise isn't resolved yet, fulfill it with null
                if (PromiseState.PENDING == promise.getState()) {
                    promise.fulfill(null);
                }
            } catch (Throwable e) {
                promise.reject(e);
            }
        };

        return this.subscribe(new PromiseRunnable<>(this.getRunner(), action));
    }

    @Override
    public Promise<T> except(BiConsumer<Throwable, Consumer<? super T>> callback) {
        Consumer<PromiseRunnable<T>> action = (promise) -> {
            try {
                // use the same value as the parent if the parent FULFILLED
                if (PromiseState.FULFILLED == this.getState()) {
                    promise.fulfill(this.getValue());
                    return;
                }

                // MAY resolve promise
                callback.accept(this.getException(), promise::fulfill);

                // if promise isn't resolved yet, fulfill it with null
                if (PromiseState.PENDING == promise.getState()) {
                    promise.fulfill(null);
                }
            } catch (Throwable e) {
                promise.reject(e);
            }
        };

        return this.subscribe(new PromiseRunnable<>(this.getRunner(), action));
    }
}
