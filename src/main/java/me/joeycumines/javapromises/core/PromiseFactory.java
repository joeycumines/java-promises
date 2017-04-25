package me.joeycumines.javapromises.core;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Create promises in completion states, or allow them to be run asynchronously.
 * This interface just provides a way to pass around ways to instance Promise objects.
 */
public interface PromiseFactory {
    /**
     * Create a promise, and executes it asynchronously.
     *
     * The action parameter format is (resolve, reject) -> // stuff
     *
     * @param action The task to perform asynchronously.
     * @return A new promise.
     */
    public Promise create(BiConsumer<Consumer<Object>, Consumer<Exception>> action);

    /**
     * Create a new REJECTED promise.
     *
     * @param value The value this will reject with.
     * @return A new promise.
     */
    public Promise reject(Exception value);

    /**
     * Create a new promise that will fulfill or reject, if given a promise and based on it's state, otherwise simply
     * fulfilling with the value given.
     *
     * Note that this method is RECURSIVE, like the A+ Promise spec.
     *
     * @param value The value this will finalized, promises will have their state propagated.
     * @return A new promise.
     */
    public Promise resolve(Object value);
}
