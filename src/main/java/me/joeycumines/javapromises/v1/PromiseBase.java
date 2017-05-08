package me.joeycumines.javapromises.v1;

import me.joeycumines.javapromises.core.*;

import java.util.Objects;

/**
 * A simple thread-safe {@link Promise} implementation sans then, except, and always.
 */
public abstract class PromiseBase<T> implements Promise<T> {
    /**
     * Use this as a lock against which you can synchronize the state. Don't you dare expose it.
     */
    protected final Object lock;

    private PromiseState state;
    private volatile T value;
    private volatile Throwable exception;

    public PromiseBase() {
        this.lock = new Object();
        this.state = PromiseState.PENDING;
        this.value = null;
        this.exception = null;
    }

    @Override
    public PromiseState getState() {
        if (PromiseState.PENDING != this.state) {
            return this.state;
        }

        synchronized (this.lock) {
            return this.state;
        }
    }

    protected T getValue() {
        if (PromiseState.PENDING != this.state) {
            return this.value;
        }

        synchronized (this.lock) {
            return this.value;
        }
    }

    protected Throwable getException() {
        if (PromiseState.PENDING != this.state) {
            return this.exception;
        }

        synchronized (this.lock) {
            return this.exception;
        }
    }

    protected PromiseBase<T> reject(Throwable exception) throws MutatedStateException, NullPointerException {
        Objects.requireNonNull(exception);

        if (PromiseState.PENDING != this.state) {
            throw new MutatedStateException(this, this.state, PromiseState.REJECTED);
        }

        synchronized (this.lock) {
            if (PromiseState.PENDING != this.state) {
                throw new MutatedStateException(this, this.state, PromiseState.REJECTED);
            }

            this.exception = exception;
            this.state = PromiseState.REJECTED;
        }

        return this;
    }

    protected PromiseBase<T> fulfill(T value) throws SelfResolutionException, MutatedStateException {
        if (this == value) {
            throw new SelfResolutionException(this);
        }

        if (PromiseState.PENDING != this.state) {
            throw new MutatedStateException(this, this.state, PromiseState.FULFILLED);
        }

        synchronized (this.lock) {
            if (PromiseState.PENDING != this.state) {
                throw new MutatedStateException(this, this.state, PromiseState.FULFILLED);
            }

            this.value = value;
            this.state = PromiseState.FULFILLED;
        }

        return this;
    }

    /**
     * Resolve this promise with the same value/exception and state as another promise. Resolving null will fulfill
     * this with null.
     * <p>
     * Note that this may happen <b>asynchronously</b>, subsequent calls to resolve <b>may fail silently</b>.
     * Take this into consideration <b>especially if exposing this method externally</b>.
     * <p>
     * Circular references are not checked, but we do take care not to directly resolve this.
     *
     * @param promise The promise to resolve.
     * @return This promise.
     * @throws SelfResolutionException If promise is this, or the promise is resolved to this.
     * @throws MutatedStateException   If this was already resolved, at the time of this call. Not triggered immediately if promise is PENDING.
     */
    protected PromiseBase<T> resolve(Promise<? extends T> promise) throws SelfResolutionException, MutatedStateException {
        if (null == promise) {
            return this.fulfill(null);
        }

        if (this == promise) {
            throw new SelfResolutionException(this);
        }

        PromiseState state = promise.getState();

        if (PromiseState.REJECTED == state) {
            return this.reject(promise.exceptSync());
        }

        if (PromiseState.FULFILLED == state) {
            return this.fulfill(promise.thenSync());
        }

        promise.always((r, e) -> {
            this.resolve(promise);
            return null;
        });

        return this;
    }

    @Override
    public void sync() {
        if (PromiseState.PENDING != this.state) {
            return;
        }

        synchronized (this.lock) {
            if (PromiseState.PENDING != this.state) {
                return;
            }

            // setup a notifyAll
            this.always((value, exception) -> {
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
    public T thenSync() {
        this.sync();
        return this.getValue();
    }

    @Override
    public Throwable exceptSync() {
        this.sync();
        return this.getException();
    }
}
