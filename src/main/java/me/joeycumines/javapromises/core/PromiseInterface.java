package me.joeycumines.javapromises.core;

import java.util.function.Function;

/**
 * A "Promise", the core concept of this class is based on the JavaScript ES6 promise implementation, with adaptions
 * where necessary or where it made more sense.
 * <p>
 * JavaScript and Java are very different languages, and therefore in practice this implementation is likely very naive.
 * <p>
 * The primary goal that this library hopes to achieve is to improve developer QoL by improving flexibility "out of the
 * box", with minimal additional complexity, when writing code or libraries that require asynchronous (Multi-Threaded)
 * operations.
 * <p>
 * Usage: Promises should implement this interface in full, in whatever manner suits the application.
 * Implementations must support chaining, and must support anything that correctly implements this interface, as results
 * from the return statement within then and except callbacks.
 * <p>
 * Read on to learn more, and good luck! You are probably going to need it.
 */
public interface PromiseInterface {
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
    public Object getValue();

    /**
     * Specify a function to be run on successful resolution of this promise.
     * <p>
     * The callback Function will be called with the value resolved.
     * <p>
     * This method generates a new promise, which will resolve with the value returned.
     * <p>
     * If the callback provided itself returns a promise, the callback promises result AND status will be propagated to
     * this promise (blocking this until the callback promise is itself resolved).
     *
     * @param callback Function\<Object, Object\> The operation which will be performed if the promise resolves successfully.
     * @return A promise which will resolve after the previous promise AND any inner operations.
     */
    public PromiseInterface then(Function callback);

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
     * @param callback Function\<Exception, Object\> The operation which will be performed if the promise fails to resolve successfully.
     * @return A promise which will resolve after the previous promise AND any inner operations.
     */
    public PromiseInterface except(Function callback);

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
    public Object thenSync();

    /**
     * Functions the same as {@link #thenSync()} but with an inbuilt type cast.
     *
     * @param type The type to cast to.
     */
    public <T> T thenSync(Class<T> type);

    /**
     * Calling this method will block until the promise is resolved, returning the value if it resolved with REJECTED,
     * otherwise returning null.
     *
     * @return The resolved value, or null if FULFILLED.
     */
    public Exception exceptSync();

    /**
     * Functions the same as {@link #exceptSync()} but with an inbuilt type cast.
     *
     * @param type The type to cast to.
     */
    public <T extends Exception> T exceptSync(Class<T> type);
}
