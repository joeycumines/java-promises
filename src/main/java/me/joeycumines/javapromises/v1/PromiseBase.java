package me.joeycumines.javapromises.v1;

import me.joeycumines.javapromises.core.*;

import java.util.function.Function;

/**
 * A simple thread-safe implementation of promises sans then and except.
 * <p>
 * The protected method {@link #finalize()} should be used to set the state, by design (and part of the deliberate
 * constraints to improve logic and performance) it can only be called once, and has multiple checks to ensure sane
 * state of resolution.
 */
public abstract class PromiseBase implements PromiseInterface {
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
    protected void finalize(PromiseState state, Object value) throws IllegalArgumentException, MutatedStateException, SelfResolutionException {
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
            throw new SelfResolutionException((PromiseInterface) value);
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
    }

    protected void fulfill(Object value) {
        this.finalize(PromiseState.FULFILLED, value);
    }

    protected void reject(Exception value) {
        this.finalize(PromiseState.REJECTED, value);
    }

    protected void resolve(Object value) {
        if (null == value || !(value instanceof PromiseInterface)) {
            this.fulfill(value);
            return;
        }

        if (this == value) {
            throw new SelfResolutionException(this);
        }

        //noinspection ConstantConditions
        PromiseInterface promise = (PromiseInterface) value;

        // if the promise can be resolved immediately, do so
        PromiseState state = promise.getState();
        if (PromiseState.PENDING != state) {
            this.finalize(state, promise.getValue());
        }

//        // by design, we don't want to explicitly block this thread, so we can't just do this
//        promise.sync();
//        this.finalize(promise.getState(), promise.getValue());

        // will allow this thread to continue on
        promise.always((r) -> {
            this.finalize(promise.getState(), promise.getValue());
            return null;
        });
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
    public <T extends Exception> T exceptSync(Class<T> type) {
        return type.cast(this.exceptSync());
    }
}
