package me.joeycumines.javapromises.v1;

import me.joeycumines.javapromises.core.*;

import java.util.function.Function;

/**
 * A simple thread-safe implementation of promises sans then and except.
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

    protected void finalize(PromiseState state, Object value) throws IllegalArgumentException, MutatedStateException, PromiseResolutionException {
        // if we are trying to set it to pending OR we are not pending, that's a paddling
        if (PromiseState.PENDING == state || PromiseState.PENDING != this.state) {
            throw new MutatedStateException(this, this.state, state);
        }

        // if we are trying to set our to another promise, that's a paddling
        if (null != value && value instanceof PromiseInterface) {
            throw new PromiseResolutionException((PromiseInterface) value);
        }

        // if we are trying to reject with something not an exception, that's a paddling
        if (PromiseState.REJECTED == state && !(null == value || value instanceof Exception)) {
            throw new IllegalArgumentException("a value was provided for rejection that was not an exception or null");
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

            // setup a notify then wait

            Function notify = (r) -> {
                this.lock.notify();
                return null;
            };

            // do both, since only one will be triggered
            this.then(notify);
            this.except(notify);

            while (PromiseState.PENDING != this.state) {
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
