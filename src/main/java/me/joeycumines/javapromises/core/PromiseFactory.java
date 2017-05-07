package me.joeycumines.javapromises.core;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Create promises in completion states, or allow them to be run asynchronously.
 * <p>
 * This interface provides a way to pass around ways to instance Promise objects, and can be considered the core API.
 */
public interface PromiseFactory {
    /**
     * Creates a promise, and executes it asynchronously.
     * <p>
     * The action parameter format is {@code (fulfill, reject) -> // stuff}.
     * <p>
     * The first call to either fulfill or reject will be the resolved value. Both of these methods <b>must</b> throw a
     * {@link MutatedStateException} for any subsequent calls. If no call is made within action, then the state of the
     * promise <b>must</b> be {@code PENDING} immediately after action completes.
     * The implementation of {@link BlockingPromise} takes advantage of this behaviour.
     * <p>
     * If fulfill is used to attempt to <b>resolve a promise with itself</b>, it <b>must</b> throw a
     * {@link SelfResolutionException}.
     * <p>
     * Any {@code Throwable throwable} that is thrown, within the action, will be the equivalent of calling
     * {@code reject.accept(throwable)}.
     * <p>
     * Calling the reject parameter, within the action, with a null value, will cause a {@link NullPointerException} to
     * be thrown internally, which will cause the returned promise to resolve as {@code REJECTED}, with that exception.
     * <p>
     * A null action parameter will cause a {@link NullPointerException} to be thrown.
     *
     * @param action The task to perform asynchronously.
     * @param <T>    The type the promise will resolve with.
     * @return A new promise.
     * @throws NullPointerException If the action is null.
     */
    <T> Promise<T> create(BiConsumer<Consumer<? super T>, Consumer<Throwable>> action);

    /**
     * Create a new {@code REJECTED} promise, with a provided reason for rejection.
     * <p>
     * This MUST happen synchronously (the returned Promise MUST be {@code REJECTED}, not {@code PENDING} to reject
     * asynchronously).
     * <p>
     * A {@code null} reason will result in a {@link NullPointerException} being thrown.
     *
     * @param reason The value this will reject with.
     * @return A new promise.
     * @throws NullPointerException If the reason is null.
     */
    <T> Promise<T> reject(Throwable reason);

    /**
     * Create a new promise that is {@code FULFILLED} with the provided value.
     * <p>
     * This MUST happen synchronously (the returned Promise MUST be {@code FULFILLED}, not {@code PENDING} to fulfill
     * asynchronously).
     *
     * @param value The value to fulfill.
     * @param <T>   The type of the value the promise resolved.
     * @return A new promise.
     */
    <T> Promise<T> fulfill(T value);

    /**
     * Return a promise created by this {@link PromiseFactory}, from any given promise, that will resolve in the same
     * way.
     * <p>
     * Performs a similar but tangential function to {@link PromiseApi#resolve(Object, Class)}, some use cases will
     * require one or the other, and some will require both.
     * <p>
     * This method is provided as a convenience, for cases when you need to ensure certain behaviour, due to differences
     * in promise implementations.
     * <p>
     * NOTE: The returned promise <b>must</b> be resolved <b>not pending</b>, if the promise to wrap was already
     * resolved.
     *
     * @param promise The promise you want to wrap.
     * @param <T>     The type of the promise.
     * @return A new promise.
     */
    <T> Promise<T> wrap(Promise<? extends T> promise);
}
