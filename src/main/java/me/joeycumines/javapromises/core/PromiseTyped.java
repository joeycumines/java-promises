package me.joeycumines.javapromises.core;

import java.util.function.Function;

/**
 * A convenience, that allows you to simply add this to your Promise implementation as an interface, giving you the
 * ability to have strict typing on the return type of then. This design circumvents type erasure.
 * <p>
 * NOTE: Any changes to this INCLUDING documentation should also be done to Promise, where relevant.
 */
public interface PromiseTyped<T> {
    /**
     * Get this as a instance of Promise.
     * Note for implementations: implement both Promise and PromiseTyped, then just return this.
     *
     * @return This as a promise.
     */
    public Promise getPromise();

    /**
     * Get the current state of the promise.
     * <p>
     * Checking this manually is not a preferred pattern.
     *
     * @return The state of the promise.
     */
    public PromiseState getState();

    /**
     * Get the resolved value of the promise.
     * <p>
     * Checking this manually is not a preferred pattern.
     *
     * @return The value resolved by this promise.
     * @throws PendingValueException If this is called on a promise in a PENDING state.
     */
    public Object getValue() throws PendingValueException;

    /**
     * Specify a function to be run on successful resolution (fulfillment) of this promise.
     * <p>
     * The callback Function will be called with the value resolved.
     * <p>
     * This method generates a new promise, which will resolve with the value returned.
     * <p>
     * If the callback provided itself returns a promise, the callback promises result AND status will be propagated to
     * this promise (blocking this until the callback promise is itself resolved).
     *
     * @param callback The operation which will be performed if the promise resolves successfully.
     * @return A promise which will resolve after the previous promise AND any inner operations.
     */
    public Promise then(Function<T, Object> callback);

    /**
     * Specify a function to be run on failed resolution (rejection) of this promise.
     * <p>
     * The callback Function will be called with the value resolved.
     * <p>
     * This method generates a new promise, which will resolve with the value returned.
     * <p>
     * If the callback provided itself returns a promise, the callback promises result AND status will be propagated to
     * this promise (blocking this until the callback promise is itself resolved).
     *
     * @param callback Function\<Throwable, Object\> The operation which will be performed if the promise fails to resolve successfully.
     * @return A promise which will resolve after the previous promise AND any inner operations.
     */
    public Promise except(Function callback);

    /**
     * Specify a function to be run on ANY resolution (rejection OR fulfillment) of this promise.
     * <p>
     * The callback Function will be called with the value resolved.
     * <p>
     * This method generates a new promise, which will resolve with the value returned.
     * <p>
     * If the callback provided itself returns a promise, the callback promises result AND status will be propagated to
     * this promise (blocking this until the callback promise is itself resolved).
     *
     * @param callback Function\<Throwable, Object\> The operation which will be performed if the promise fails to resolve successfully.
     * @return A promise which will resolve after the previous promise AND any inner operations.
     */
    public Promise always(Function callback);

    /**
     * Calling this method will block the current thread until this is resolved.
     * <p>
     * NOTE: All responsibility for sane use of this method lies with the caller.
     */
    public void sync();

    /**
     * Calling this method will block until the promise is resolved, returning the value if it resolved with FULFILLED,
     * otherwise returning null.
     *
     * @return The resolved value, or null if REJECTED.
     */
    public T thenSync();

    /**
     * Calling this method will block until the promise is resolved, returning the value if it resolved with REJECTED,
     * otherwise returning null.
     *
     * @return The resolved value, or null if FULFILLED.
     */
    public Throwable exceptSync();
}
