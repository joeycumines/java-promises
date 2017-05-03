package me.joeycumines.javapromises.core;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * A "promise", the core concept of this class is based on the JavaScript ES6 promise implementation, with adaptions
 * where necessary or where it made more sense. This interface has been optimised with the goal of a thin but extremely
 * flexible, yet type safe and minimally opinionated implementation.
 * <p>
 * This library hopes to improve developer QoL by improving flexibility "out of the box", with minimal additional
 * complexity, when writing code or libraries that require asynchronous operation. It is deliberately simplistic, in
 * contrast to the somewhat similar, native Java 8 {@link java.util.concurrent.CompletionStage} implementation.
 * <p>
 * A promise must have a state {@link PromiseState}, and can be resolved at most <b>once</b>. A {@code PENDING} promise
 * is considered unresolved, and may transition to either {@code FULFILLED}, on success, resolving with a value of
 * either type {@code T} or {@code null}, OR it may resolve as {@code REJECTED} on failure, with a non-{@code null}
 * {@link Throwable}.
 * <p>
 * When a promise changes from it's initial {@code PENDING} state, it <b>resolves</b>, which will cause it to either
 * <b>fulfill</b> or <b>reject</b>, into either states {@code FULFILLED} or {@code REJECTED}, respectively. Naming a
 * method or callback parameter "fulfill" implies it is an action that <b>can</b> be taken that will <b>always</b>
 * resolve the promise as {@code FULFILLED}, unless an error occurs. The name "reject" follows the same guidelines, and
 * the name "resolve" <b>should</b> have the possibility of performing either action, but <b>may</b> always fulfill.
 * <p>
 * When a promise's <b>value</b> is referred to, within this interface, it refers to <b>either</b> the {@code FULFILLED}
 * value, returned by {@link #thenSync()}, for example, OR, if the promise {@code REJECTED}, it refers to a non-null
 * {@link Throwable}, which could be accessed via {@link #exceptSync()}. A {@code PENDING} promise has no value.
 * <p>
 * Promises should implement this interface in full, in whatever manner suits the application, in addition to a
 * corresponding {@link PromiseFactory}, which can be coupled with extending {@link PromiseApi}. Doing this will allow
 * the implementer to simply pass a factory/api instance into the provided abstract {@code PromiseTest},
 * {@code PromiseFactoryTest}, and {@code PromiseApiTest} classes, and run the provided JUnit 4 tests.
 * <p>
 * Beyond these guidelines there are no restrictions. It should be noted that this interface does not guarantee the
 * execution order of (promise) state associated asynchronous code, like {@link java.util.concurrent.CompletionStage},
 * and unlike the <a href="https://promisesaplus.com/">JavaScript A+ promise specification</a> (e.g. then callbacks).
 * <p>
 * All methods that are part of this interface <b>must</b> be thread-safe.
 * <p>
 * Note: It is part of the contract of this interface that no method may block unless the method name ends in "sync", OR
 * due to an INTERNAL lock in place to allow thread-safety.
 * <p>
 * <b>If a {@code null} object argument is passed to any method, then a {@link NullPointerException} will be thrown.</b>
 */
public interface Promise<T> {
    /**
     * Get the current state of the promise.
     * <p>
     * A promise's initial state is {@code PENDING}, will change <b>at most</b> once, to either resolved states,
     * {@code FULFILLED} or {@code REJECTED}.
     *
     * @return The state of the promise.
     */
    PromiseState getState();

    /**
     * Specify a callback to be run on successful resolution {@code FULFILLED} of this, and return a new promise,
     * that will resolve with the same state and value as the callback's returned promise, <b>after</b> the callback
     * promise resolves. The input parameter will be the {@code FULFILLED} value of {@code this}.
     * <p>
     * The callback will be run as soon as possible (but not inline) if {@code this} is already {@code FULFILLED}.
     * <p>
     * If the callback returns a {@code null} value, then the returned promise will resolve as {@code FULFILLED} with
     * value {@code null}.
     * <p>
     * If an exception is thrown within the callback, then the returned promise will be {@code REJECTED}, with that
     * exception.
     * <p>
     * If {@code this} resolved with the {@code REJECTED} state, then the returned promise will reflect the state and
     * value of {@code this}, and the <b>callback will not be run</b>.
     *
     * @param callback The operation which will be performed if {@code this} resolves successfully.
     * @param <U>      The return type of the new promise, dictated by the callback promise's return type.
     * @return A promise which will resolve after the previous promise(s) AND any inner operations.
     * @throws NullPointerException If callback is null.
     */
    <U> Promise<U> then(Function<? super T, Promise<? extends U>> callback);

    /**
     * Specify a callback to be run on successful resolution {@code FULFILLED} of this, and return a new promise,
     * that will fulfill with the value accepted within the callback, into it's second argument, a {@link Consumer}.
     * <p>
     * The callback will be run as soon as possible (but not inline) if {@code this} is already {@code FULFILLED}.
     * <p>
     * If no call is made to this second argument <b>within the callback</b>, then the resolved value of the returned
     * promise will be {@code null}, and the state {@code FULFILLED}.
     * <p>
     * If {@code this} resolves as {@code REJECTED} <b>callback will not be called</b>, and the returned promise will
     * reject with the same {@link Throwable}.
     * <p>
     * If an exception is thrown within the callback, then the returned promise will be {@code REJECTED} with that
     * exception, <b>provided that it has not already resolved</b>.
     * <p>
     * For example:
     * <pre>
     *     <code>
     *         Promise&lt;Integer&gt; input = // some async operation which eventually fulfills with int(5)
     *         Promise&lt;Integer&gt; output = input.then((inputValue, fulfill) -> fulfill.accept(inputValue + 10));
     *         // will output 15
     *         System.out.println(output.thenSync());
     *     </code>
     * </pre>
     *
     * @param callback The operation which will be performed if the promise resolves successfully.
     * @param <U>      The return type of the new promise.
     * @return A promise which will resolve after the previous promise AND any inner operations.
     * @throws NullPointerException If callback is null.
     */
    <U> Promise<U> then(BiConsumer<? super T, Consumer<? super U>> callback);

    /**
     * Specify a callback to be run if this resolves with a failed state {@code REJECTED}, and return a new promise,
     * that will resolve with the same state and value as the callback's returned promise, <b>after</b> the callback
     * promise resolves. The input parameter will be the {@code REJECTED} value of {@code this} (a {@link Throwable}).
     * <p>
     * To preserve type safety, the returned type of promise within the callback is restricted to things which can be
     * cast to the type of {@code T}, unlike {@link #then(Function)}, which can allow any type, due to the fact that the
     * no-callback case will only ever result in a {@link Throwable}.
     * <p>
     * The callback will be run as soon as possible (but not inline) if {@code this} is already {@code REJECTED}.
     * <p>
     * If the callback returns a {@code null} value, then the returned promise will resolve as {@code FULFILLED} with
     * value {@code null}.
     * <p>
     * If an exception is thrown within the callback, then the returned promise will be {@code REJECTED}, with that
     * exception.
     * <p>
     * If {@code this} resolved with the {@code FULFILLED} state, then the returned promise will reflect the state and
     * value of {@code this}, and the <b>callback will not be run</b>.
     *
     * @param callback The operation which will be performed if the promise resolves exceptionally.
     * @return A promise which will resolve after the previous promise(s) AND any inner operations.
     * @throws NullPointerException If callback is null.
     */
    Promise<T> except(Function<Throwable, Promise<? extends T>> callback);

    /**
     * Specify a callback to be run if this resolves with a failed state {@code REJECTED}, and return a new promise,
     * that will fulfill with the value accepted within the callback, into it's second argument, a {@link Consumer}.
     * <p>
     * To preserve type safety, the type of fulfillment value within the callback is restricted, to things which can be
     * cast to the type of {@code T}, unlike {@link #then(BiConsumer)}, which can allow any type, due to the fact that
     * the no-callback case will only ever result in a {@link Throwable}.
     * <p>
     * The callback will be run as soon as possible (but not inline) if {@code this} is already {@code REJECTED}.
     * <p>
     * If no call is made to this second argument <b>within the callback</b>, then the resolved value of the returned
     * promise will be {@code null}, and the state {@code FULFILLED}.
     * <p>
     * If {@code this} resolves as {@code FULFILLED} <b>callback will not be called</b>, and the returned promise will
     * fulfill with the same value.
     * <p>
     * If an exception is thrown within the callback, then the returned promise will be {@code REJECTED} with that
     * exception, <b>provided that it has not already resolved</b>.
     * <p>
     * For example:
     * <pre>
     *     <code>
     *         Promise&lt;Object&gt; input = // some async operation which eventually rejects with an exception
     *         Promise&lt;Object&gt; output = input.except((exception, fulfill) -> fulfill.accept(exception));
     *         // will output the string representation of the FULFILLED exception originally REJECTED by input
     *         System.out.println(output.thenSync());
     *     </code>
     * </pre>
     *
     * @param callback The operation which will be performed if the promise resolves exceptionally.
     * @return A promise which will resolve after the previous promise AND any inner operations.
     * @throws NullPointerException If callback is null.
     */
    Promise<T> except(BiConsumer<Throwable, Consumer<? super T>> callback);

    /**
     * Specify a callback that will always run on resolution, or as soon as possible if {@code this} is already in
     * a resolved state. Returns a new promise, that will resolve with the same state and value as the callback's return
     * value (a promise), <b>after</b> the returned promise resolves.
     * <p>
     * The right input parameter will be the {@code REJECTED} exception (not {@code null}), if {@code this} rejected,
     * otherwise it can be assumed that the state was {@code FULFILLED}, and the left input parameter will be set, if
     * the fulfillment value was non-null.
     * <p>
     * The callback will be run as soon as possible (but not inline) if {@code this} is already resolved.
     * <p>
     * If the callback returns a {@code null} value, then the returned promise will resolve as {@code FULFILLED} with
     * value {@code null}.
     * <p>
     * If an exception is thrown within the callback, then the returned promise will be {@code REJECTED}, with that
     * exception.
     *
     * @param callback The operation to perform when the promise resolves.
     * @param <U>      The return type of the new promise, dictated by the callback promise's return type.
     * @return A promise which will resolve after the previous promise AND any inner operations.
     * @throws NullPointerException If callback is null.
     */
    <U> Promise<U> always(BiFunction<? super T, Throwable, Promise<? extends U>> callback);

    /**
     * Calling this method will block the current thread until {@code this} is resolved (<b>not</b> {@code PENDING}).
     * <p>
     * All responsibility for sane use of this method lies with the caller.
     */
    void sync();

    /**
     * Calling this method will block until the promise is resolved, returning a value if it resolved with
     * {@code FULFILLED}, otherwise returning {@code null}.
     *
     * @return The resolved value, or {@code null} if {@code REJECTED}.
     * @see #sync()
     */
    T thenSync();

    /**
     * Calling this method will block until the promise is resolved, returning a value if it resolved with
     * {@code REJECTED}, otherwise returning {@code null}.
     *
     * @return The reason for rejection, or {@code null} if {@code FULFILLED}.
     * @see #sync()
     */
    Throwable exceptSync();
}
