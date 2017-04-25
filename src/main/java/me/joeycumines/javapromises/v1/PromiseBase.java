package me.joeycumines.javapromises.v1;

import me.joeycumines.javapromises.core.*;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * A simple thread-safe implementation of promises sans then and except.
 * <p>
 * The protected method {@link #finalize()} should be used to set the state, by design (and part of the deliberate
 * constraints to improve logic and performance) it can only be called once, and has multiple checks to ensure sane
 * state of resolution.
 */
public abstract class PromiseBase implements Promise, PromiseTyped {
    /**
     * Use this as a lock against which you can synchronize the state.
     */
    protected final Object lock;

    private PromiseState state;
    private Object value;

    public PromiseBase() {
        this.lock = new Object();
        this.state = PromiseState.PENDING;
        this.value = null;
    }

    @Override
    public PromiseState getState() {
        // if the state is finalized we can just grab it without locking
        if (PromiseState.PENDING != this.state) {
            return this.state;
        }

        synchronized (this.lock) {
            return this.state;
        }
    }

    @Override
    public Object getValue() throws PendingValueException {
        // if the state is finalized we can just grab it without locking
        if (PromiseState.PENDING != this.state) {
            return this.value;
        }

        synchronized (this.lock) {
            // if we are trying to access the value and it hasn't been set yet, that's a paddling
            if (PromiseState.PENDING == this.state) {
                throw new PendingValueException(this);
            }

            return this.value;
        }
    }

    /**
     * Finalize the promise.
     * This method can only be called once, and contains checks to ensure sane state.
     * Thread safe.
     *
     * @param state Must be either REJECTED or FULFILLED.
     * @param value Nullable, must be an exception (REJECTED) or any object (FULFILLED).
     * @throws IllegalArgumentException If you try to reject with something that is not an exception.
     * @throws MutatedStateException    If you try to resolve an already resolved promise, or resolve with pending.
     * @throws SelfResolutionException  If you try to resolve this.
     */
    protected PromiseBase finalize(PromiseState state, Object value) throws IllegalArgumentException, MutatedStateException, SelfResolutionException {
        // if we are trying to set it to pending that's a paddling
        if (PromiseState.PENDING == state) {
            throw new IllegalArgumentException("the state PENDING is not a finalized state");
        }

        // if we are trying to reject with something not an exception, that's a paddling
        if (PromiseState.REJECTED == state && !(null == value || value instanceof Exception)) {
            throw new IllegalArgumentException("a value was provided for rejection that was not an exception or null");
        }

        // if we are trying to resolve to ourselves, that's a paddling
        if (null != value && this == value) {
            throw new SelfResolutionException((Promise) value);
        }

        // if we are not already  pending, that's a paddling
        if (PromiseState.PENDING != this.state) {
            throw new MutatedStateException(this, this.state, state);
        }

        // we have to do some double checked locking though, state may have actually been finalized
        synchronized (this.lock) {
            // state is actually finalized, more paddling
            if (PromiseState.PENDING != this.state) {
                throw new MutatedStateException(this, this.state, state);
            }

            // we are sure we have something that isn't finalized already
            this.value = value;
            this.state = state;
        }

        return this;
    }

    protected PromiseBase reject(Exception value) {
        this.finalize(PromiseState.REJECTED, value);

        return this;
    }

    /**
     * Recursively resolve a promise.
     * This is private because of the complications associated with null values, and because we only need one entry
     * point for resolve anyway (overloading gives us nothing extra dev experience wise).
     *
     * @param promise The promise to resolve CANNOT BE NULL.
     */
    private void resolvePromise(Promise promise) throws IllegalArgumentException, MutatedStateException, CircularResolutionException {
        if (null == promise) {
            throw new IllegalArgumentException("the promise to resolve was null");
        }

        // we cannot resolve ourselves
        if (this == promise) {
            throw new SelfResolutionException(this);
        }

        // see how far we can get; walk until we can finalize, get blocked by PENDING, or we fail due to circular refs
        // Floyd's cycle-finding algorithm - http://stackoverflow.com/a/2663147

        Promise tortoise = this;
        Promise hare = this;

        // get the next promise in a chain of FULFILLED promises
        // if it returns the input unmodified, then it can be considered a failed step
        Function<Promise, Promise> next = (p) -> {
            // exit early guard to deal with the start case
            if (p == this) {
                return promise;
            }

            // grab the next resolved one
            return PromiseBase.next(p);
        };

        // if we failed to resolve the promise this time, call this before exiting, it will figure things out
        Consumer<Promise> failure = (p) -> {
            // if we are still waiting, trigger this again after it stops PENDING
            if (PromiseState.PENDING == p.getState()) {
                p.always((r) -> {
                    this.resolvePromise(promise);
                    return null;
                });

                return;
            }

            // resolve to the state of the child; it should NEVER be a promise
            this.finalize(p.getState(), p.getValue());
        };

        while (true) {
            Promise step = null;

            // one step for tortoise
            step = next.apply(tortoise);
            if (step == tortoise) {
                // exit: no next steps were found, we stayed on the same step
                failure.accept(step);
                return;
            }
            tortoise = step;

            // two steps for hare
            step = next.apply(hare);
            if (step == hare) {
                // exit: no next steps were found, we stayed on the same step
                failure.accept(step);
                return;
            }
            hare = step;
            step = next.apply(hare);
            if (step == hare) {
                // exit: no next steps were found, we stayed on the same step
                failure.accept(step);
                return;
            }
            hare = step;

            // if they ever meet we have a circular reference
            if (tortoise == hare) {
                throw new CircularResolutionException(promise);
            }
        }
    }

    /**
     * Resolve this with either a value (which will always fulfill) OR the value of another promise, after it resolves.
     * <p>
     * The check on the promise is done via instanceof, this is so that if we have say something cast with PromiseTyped,
     * that also implements Promise, it still works as expected.
     *
     * @param value A value OR a NN promise, that will be resolved recursively.
     * @return this
     * @throws SelfResolutionException If value == this
     * @throws MutatedStateException   If the state was already finalized.
     */
    protected PromiseBase resolve(Object value) throws SelfResolutionException, MutatedStateException {
        if (value instanceof Promise) {
            this.resolvePromise((Promise) value);
            return this;
        }

        this.finalize(PromiseState.FULFILLED, value);

        return this;
    }

    @Override
    public void sync() {
        // if we are already done we can simply continue
        if (PromiseState.PENDING != this.state) {
            return;
        }

        synchronized (this.lock) {
            // and again
            if (PromiseState.PENDING != this.state) {
                return;
            }

            // setup a notifyAll
            this.always((r) -> {
                synchronized (this.lock) {
                    this.lock.notifyAll();
                    return null;
                }
            });

            // setup a wait
            while (PromiseState.PENDING == this.state) {
                try {
                    this.lock.wait();
                } catch (InterruptedException ignored) {
                }
            }
        }
    }

    @Override
    public Object thenSync() {
        this.sync();

        if (PromiseState.FULFILLED != this.getState()) {
            return null;
        }

        return this.getValue();
    }

    @Override
    public <T> T thenSync(Class<T> type) {
        return type.cast(this.thenSync());
    }

    @Override
    public Exception exceptSync() {
        this.sync();

        if (PromiseState.REJECTED != this.getState()) {
            return null;
        }

        return (Exception) this.getValue();
    }

    @Override
    public Promise getPromise() {
        return this;
    }

    /**
     * Get the next promise in a chain.
     * <p>
     * If there is no next promise, or it cannot be reached, then the input promise will be returned, for performance
     * reasons.
     *
     * @param promise Input promise, must not be null.
     * @return The next promise, resolved from the input, or the same input promise if there was no next.
     * @throws IllegalArgumentException If input promise is null.
     */
    private static Promise next(Promise promise) throws IllegalArgumentException {
        if (null == promise) {
            throw new IllegalArgumentException("the input promise cannot be null");
        }

        // getting the value will throw an exception if it is PENDING
        if (PromiseState.FULFILLED != promise.getState()) {
            return promise;
        }

        Object value = promise.getValue();

        if (!(value instanceof Promise)) {
            return promise;
        }

        return (Promise) value;
    }
}
