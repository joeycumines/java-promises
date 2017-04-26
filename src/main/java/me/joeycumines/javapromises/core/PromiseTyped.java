package me.joeycumines.javapromises.core;

import java.util.function.Function;

/**
 * A convenience, that allows you to simply add this to your Promise implementation as an interface, giving you the
 * ability to have strict typing on the return type of then. This design circumvents type erasure.
 * <p>
 * NOTE: Any changes to this should also be made to Promise, where relevant.
 */
public interface PromiseTyped<T> {
    /**
     * Get this as a promise.
     * Implementations MUST return this, which means that they must implement both Promise and PromiseTyped.
     *
     * @return This as a Promise.
     */
    public Promise getPromise();

    /**
     * @see Promise#getState() Identical implementation.
     */
    public PromiseState getState();

    /**
     * @see Promise#getValue() Identical implementation.
     */
    public Object getValue() throws PendingValueException;

    /**
     * @see Promise#then(Function) Identical implementation, sans the typed argument.
     */
    public Promise then(Function<T, Object> callback);

    /**
     * @see Promise#except(Function) Identical implementation.
     */
    public Promise except(Function callback);

    /**
     * @see Promise#always(Function) Identical implementation.
     */
    public Promise always(Function callback);

    /**
     * @see Promise#sync() Identical implementation.
     */
    public void sync();

    /**
     * @see Promise#thenSync() Identical implementation, sans the automatically typed return value.
     */
    public T thenSync();

    /**
     * @see Promise#exceptSync() Identical implementation.
     */
    public Throwable exceptSync();
}
