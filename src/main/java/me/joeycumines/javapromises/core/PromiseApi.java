package me.joeycumines.javapromises.core;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Helpers for the creation and use promises. It is not required to use this implementation, but it is designed to work
 * with <b>any</b> promise implementation.
 * <p>
 * Note: This class is designed to work with any promise implementation, make a concrete class extending this.
 */
public abstract class PromiseApi implements PromiseFactory {
    /**
     * Given an iterable of one or more promises, return a promise, that will resolve with a {@link List} containing the
     * fulfillment values of each input promise, provided every promise resolves successfully. <b>The returned promise
     * will reject with same reason as the first (if any) input promise that rejects.</b> This will not wait on any
     * other promises, and is as such useful for situations requiring early-exit behaviour.
     * <p>
     * If <b>any</b> of the input promises are already rejected, then the <b>first</b> encountered rejected promise
     * (using a iterator), will have it's rejection reason propagated to the returned promise.
     * <p>
     * <b>Unlike the JS promise spec, an empty iterable WILL throw an {@link IllegalArgumentException}.</b>
     * <p>
     * Any null argument encountered will result in a {@link NullPointerException} (MAY not be triggered by an input
     * containing a null value IF and only if there is a rejected promise encountered BEFORE the null value).
     *
     * @param promiseIterable The promises to resolve. Must not be null, and must contain no null values.
     * @param <T>             The type of the promise to return.
     * @return A promise that will resolve successfully <b>only</b> if all inputs do, with their values in a list.
     * @throws NullPointerException     If the promiseIterable parameter, or any of it's values, are null.*
     * @throws IllegalArgumentException If the iterable was empty.
     */
    public <T> Promise<List<T>> all(Iterable<? extends Promise<? extends T>> promiseIterable) throws NullPointerException, IllegalArgumentException {
        Objects.requireNonNull(promiseIterable);

        Iterator<? extends Promise<? extends T>> iterator = promiseIterable.iterator();

        // we require at least one value to work with
        if (!iterator.hasNext()) {
            throw new IllegalArgumentException("the provided iterable cannot be empty");
        }

        // we store the promises as we validate them, so they can be guaranteed not null
        List<Promise<? extends T>> promiseList = new ArrayList<>();

        // validate the input, and see if we can exit early

        boolean fulfilled = true;

        while (iterator.hasNext()) {
            Promise<? extends T> promise = iterator.next();
            Objects.requireNonNull(promise);

            PromiseState state = promise.getState();

            // early reject clause - no need to continue, also guarantees order
            if (PromiseState.REJECTED == state) {
                return this.reject(promise.exceptSync());
            }

            // keep track of any pending, so we can identify if they are all fulfilled
            if (PromiseState.FULFILLED != state) {
                fulfilled = false;
            }

            promiseList.add(promise);
        }

        // early exit - they are all fulfilled
        if (fulfilled) {
            List<T> resultList = new ArrayList<>();
            promiseList.forEach((promise) -> resultList.add(promise.thenSync()));

            return this.fulfill(resultList);
        }

        // we have to do this if we want to support early resolution
        BlockingPromise<List<T>> blocker = new BlockingPromise<>(this);

        // keep track of how many things are completed
        AtomicInteger counter = new AtomicInteger(0);

        // every time a promise completes successfully
        Runnable complete = () -> {
            // increment counter - and if we haven't completed everything, we can exit for now
            if (counter.incrementAndGet() != promiseList.size()) {
                return;
            }

            // we are all done, build a result set and return

            List<T> resultList = new ArrayList<>();
            promiseList.forEach((promise) -> resultList.add(promise.thenSync()));

            blocker.fulfill(resultList);
        };

        // uses the validated input, register callbacks for each promise
        promiseList.forEach((promise) -> promise.always((r, e) -> {
            // nothing to do if we already resolved
            if (PromiseState.PENDING != blocker.getPromise().getState()) {
                return null;
            }

            // reject the returned promise
            if (null != e) {
                blocker.reject(e);
                return null;
            }

            // will fulfill if we can
            complete.run();
            return null;
        }));

        return blocker.getPromise();
    }

    /**
     * Given an iterable of one or more promises, return a promise, that will resolve with the same state and value as
     * the <b>first</b> one that resolves. This exit-early behaviour is useful in a number of cases.
     * <p>
     * If <b>any</b> of the provided promises are already resolved, then the <b>first</b> encountered promise (using an
     * iterator) will be used to determine the state and value of the returned promise.
     * <p>
     * <b>Unlike the JS promise spec, an empty iterable WILL throw an {@link IllegalArgumentException}.</b>
     * <p>
     * Any null argument encountered will result in a {@link NullPointerException} (MAY not be triggered by an input
     * containing a null value IF and only if there is a resolve promise encountered BEFORE the null value).
     *
     * @param promiseIterable The promises to race. Must not be null, and must contain no null values.
     * @param <T>             The type of the promise to return.
     * @return A promise that will resolve with the same state and value as the <b>first</b> input promise to resolve.
     * @throws NullPointerException     If the promiseIterable parameter, or any of it's values, are null.*
     * @throws IllegalArgumentException If the iterable was empty.
     */
    public <T> Promise<T> race(Iterable<? extends Promise<? extends T>> promiseIterable) throws NullPointerException, IllegalArgumentException {
        Objects.requireNonNull(promiseIterable);

        Iterator<? extends Promise<? extends T>> iterator = promiseIterable.iterator();

        // we require at least one value to work with
        if (!iterator.hasNext()) {
            throw new IllegalArgumentException("the provided iterable cannot be empty");
        }

        // we store the promises as we validate them, so they can be guaranteed not null
        List<Promise<? extends T>> promiseList = new ArrayList<>();

        // validate the input, and see if we can exit early
        while (iterator.hasNext()) {
            Promise<? extends T> promise = iterator.next();
            Objects.requireNonNull(promise);

            PromiseState promiseState = promise.getState();

            // synchronous exit-early clauses
            if (PromiseState.FULFILLED == promiseState) {
                return this.fulfill(promise.thenSync());
            } else if (PromiseState.REJECTED == promiseState) {
                return this.reject(promise.exceptSync());
            }

            promiseList.add(promise);
        }

        // we have to do this if we want to support early resolution
        BlockingPromise<T> blocker = new BlockingPromise<>(this);

        // uses the validated input, register callbacks for each promise
        promiseList.forEach((promise) -> promise.always((r, e) -> {
            // nothing to do if we already resolved
            if (PromiseState.PENDING != blocker.getPromise().getState()) {
                return null;
            }

            if (null != e) {
                blocker.reject(e);
                return null;
            }

            blocker.fulfill(r);
            return null;
        }));

        return blocker.getPromise();
    }

    /**
     * Given an iterable of one or more promises, return a promise, that will fulfill with the value of the <b>first</b>
     * of the input promises that fulfills. If <b>all</b> of the promises reject, then the returned promise will reject
     * with an {@link AggregateException}, which can be used to retrieve the reasons why it rejected.
     * <p>
     * If any of the provided promises are already fulfilled, then the <b>first iterated</b> promise will have it's
     * fulfillment value propagated. If they are all rejected, then this will reject immediately.
     * <p>
     * Any null argument encountered will result in a {@link NullPointerException} (MAY not be triggered by an input
     * containing a null value IF and only if there is a fulfilled promise encountered BEFORE the null value).
     *
     * @param promiseIterable The promises to watch. Must not be null, and must contain no null values.
     * @param <T>             The type of promise to return.
     * @return A new promise, that will fulfill the same as the first input to do so, or reject when <b>all</b> do.
     * @throws NullPointerException     If the promiseIterable parameter, or any of it's values, are null.*
     * @throws IllegalArgumentException If the iterable was empty.
     */
    public <T> Promise<T> any(Iterable<? extends Promise<? extends T>> promiseIterable) throws NullPointerException, IllegalArgumentException {
        Objects.requireNonNull(promiseIterable);

        Iterator<? extends Promise<? extends T>> iterator = promiseIterable.iterator();

        // we require at least one value to work with
        if (!iterator.hasNext()) {
            throw new IllegalArgumentException("the provided iterable cannot be empty");
        }

        // we store the promises as we validate them, so they can be guaranteed not null
        List<Promise<? extends T>> promiseList = new ArrayList<>();

        boolean rejected = true;

        // validate the input, and see if we can exit early
        while (iterator.hasNext()) {
            Promise<? extends T> promise = iterator.next();
            Objects.requireNonNull(promise);

            PromiseState promiseState = promise.getState();

            // synchronous exit-early clause
            if (PromiseState.FULFILLED == promiseState) {
                return this.fulfill(promise.thenSync());
            }

            // if any are pending still we can keep going
            if (PromiseState.REJECTED != promiseState) {
                rejected = false;
            }

            promiseList.add(promise);
        }

        // if we were actually all rejected we can do so immediately
        if (rejected) {
            List<Throwable> exceptionList = new ArrayList<>();
            promiseList.forEach((promise) -> exceptionList.add(promise.exceptSync()));
            return this.reject(new AggregateException(exceptionList));
        }

        // we have to do this if we want to support early resolution
        BlockingPromise<T> blocker = new BlockingPromise<>(this);

        // keep track of how many we have rejected
        AtomicInteger countRejected = new AtomicInteger(0);

        promiseList.forEach((promise) -> promise.always((r, e) -> {
            // nothing to do if we already resolved
            if (PromiseState.PENDING != blocker.getPromise().getState()) {
                return null;
            }

            if (null != e) {
                if (countRejected.incrementAndGet() == promiseList.size()) {
                    List<Throwable> exceptionList = new ArrayList<>();
                    promiseList.forEach((p) -> exceptionList.add(p.exceptSync()));
                    blocker.reject(new AggregateException(exceptionList));
                }

                return null;
            }

            blocker.fulfill(r);

            return null;
        }));

        return blocker.getPromise();
    }

    /**
     * Given an iterable, perform an asynchronous action on each element <b>serially</b>, similar to
     * {@link Iterable#forEach(Consumer)}. These actions are chained together using the promise returned from each
     * call to {@code action}, which takes the iterated value. <b>The next element will not be iterated until / unless
     * the promise returned by the previous action call resolves successfully.</b> A null return value from the action
     * is allowed, it will be treated as a null fulfillment.
     * <p>
     * If all actions complete successfully, the returned {@link Promise} will fulfill with a {@link List}, containing
     * an element for each element iterated, in order, from the {@code inputIterable}. The elements will be set with
     * the fulfillment values of the promises returned by the corresponding action call.
     * <p>
     * If any action returns a rejected promise or throws any exceptions, then the returned promise will resolve as
     * {@code REJECTED}, with the same exception.
     * <p>
     * It should be noted that the iterable is walked <b>before</b> the async operations are performed, in order to
     * improve thread safety (the values are shallow copied locally).
     * <p>
     * Any null parameters will result in a {@link NullPointerException}.
     *
     * @param inputIterable The source of the values to iterate.
     * @param action        The action to perform on each value.
     * @param <T>           The type of the input values.
     * @param <U>           The type of the output values.
     * @return A new promise, that will resolve with a {@link List} of promises
     * @throws NullPointerException If any parameters are null.
     */
    public <T, U> Promise<List<U>> each(Iterable<? extends T> inputIterable, Function<? super T, ? extends Promise<? extends U>> action) throws NullPointerException {
        Objects.requireNonNull(inputIterable);
        Objects.requireNonNull(action);

        // make a copy of the iterable for concurrent access
        List<T> inputList = new ArrayList<>();
        inputIterable.forEach(inputList::add);
        inputList = Collections.synchronizedList(inputList);

        // make an output list as well
        List<Promise<U>> outputList = Collections.synchronizedList(new ArrayList<>());

        return this.eachRecursive(inputList, action, new AtomicInteger(0), outputList);
    }

    /**
     * Perform an action, and return a promise that will resolve with rejected, if the action throws an exception.
     * <p>
     * If the action returns a non null promise, then the returned promise will reflect the same state and value.
     *
     * @param action The action to attempt.
     * @param <T>    The type of the promise returned.
     * @return A new promise, that will resolve successfully, provided the action completes + does not return rejected.
     * @throws NullPointerException If the action is null.
     */
    public <T> Promise<T> attempt(Supplier<? extends Promise<? extends T>> action) throws NullPointerException {
        Objects.requireNonNull(action);
        return this.fulfill(null).then((r) -> action.get());
    }

    /**
     * Perform an action, and return a promise that will resolve with rejected, if the action throws an exception.
     * The returned promise will fulfill with a null value on success, or reject with the thrown exception on failure.
     * <p>
     * If you need a return type use {@link #attempt(Supplier)}.
     *
     * @param action The action to perform.
     * @return A promise that will resolve successfully if the provided action does not throw an exception.
     * @throws NullPointerException If the action is null.
     * @see #attempt(Supplier) If you need a typed promise.
     */
    public Promise<?> attempt(Runnable action) throws NullPointerException {
        Objects.requireNonNull(action);

        return this.fulfill(null).then((r) -> {
            action.run();
            return null;
        });
    }

    /**
     * Convert a promise to a {@link CompletableFuture}. The reverse of this conversion can be performed by instancing
     * a new {@link me.joeycumines.javapromises.v1.PromiseStage}.
     *
     * @param promise The promise to convert.
     * @param <T>     The type of the returned {@link CompletableFuture}.
     * @return A new {@link CompletableFuture} that will resolve with the same state as the input promise.
     * @throws NullPointerException If the input promise is null.
     */
    public <T> CompletableFuture<T> toCompletableFuture(Promise<? extends T> promise) throws NullPointerException {
        Objects.requireNonNull(promise);

        CompletableFuture<T> future = new CompletableFuture<>();
        PromiseState state = promise.getState();

        if (PromiseState.FULFILLED == state) {
            future.complete(promise.thenSync());
        } else if (PromiseState.REJECTED == state) {
            future.completeExceptionally(promise.exceptSync());
        } else {
            promise.always((r, e) -> {
                if (null != e) {
                    future.completeExceptionally(e);
                    return null;
                }

                future.complete(r);
                return null;
            });
        }

        return future;
    }

    /**
     * Create a new promise that will fulfill or reject, if given a promise and based on it's state, otherwise simply
     * fulfilling with the value given, provided it can be cast to the given type.
     * <p>
     * This method supports the resolution of multiple promises, that are chained together to an unknown depth.
     * <p>
     * This method uses a provided type parameter to provide type safety. If at all possible it is, however, recommended
     * to simply chain promises in a inherently type safe way.
     * <p>
     * If the provided value is not a promise, or it and <b>any chained promise(s) are ALL resolved</b> this MUST happen
     * synchronously, however if any are {@code PENDING} then the returned promise will resolve AFTER value, <b>and any
     * children</b>.
     * <p>
     * NOTE: This method is RECURSIVE, like the A+ Promise spec, and MUST throw a {@link CircularResolutionException} in
     * the event that the provided promise is already resolved, and is part of circularly linked resolved promises.
     * <p>
     * If {@code Promise.class} or a subclass is used as the type, an {@link IllegalArgumentException} will be thrown.
     * <p>
     * If {@code type} is null, a {@link NullPointerException} will be thrown.
     *
     * @param value The value to resolve.
     * @param type  The type of the promise that will be resolved, from the input value.
     * @return A new promise, that will resolve AFTER the provided promise. MAY be created by this PromiseFactory.
     * @throws CircularResolutionException If the provided promise will form a loop.
     * @throws IllegalArgumentException    If the type was {@code Promise.class}.
     * @throws NullPointerException        If the type was {@code null}.
     */
    public <T> Promise<T> resolve(Object value, Class<? extends T> type) throws CircularResolutionException, IllegalArgumentException, NullPointerException {
        Objects.requireNonNull(type);

        if (Promise.class.isAssignableFrom(type)) {
            throw new IllegalArgumentException("the type " + type.toString() + " is not a valid target type, Promise can be assigned from it");
        }

        // if we do not have a promise, we just try to cast the value
        if (!(null != value && value instanceof Promise)) {
            return this.fulfill(type.cast(value));
        }

        // resolve the promise if we can, could throw a CircularResolutionException
        value = this.resolvePromise((Promise<?>) value);

        // if we didn't resolve a promise, we are done, try the cast, and return a fulfilled promise with that value
        if (!(null != value && value instanceof Promise)) {
            return this.fulfill(type.cast(value));
        }

        Promise<?> promise = (Promise<?>) value;

        // if the promise REJECTED we can resolve that value in a new promise (prevents any type cast issues etc)
        if (PromiseState.REJECTED == promise.getState()) {
            return this.reject(promise.exceptSync());
        }

        // the state here should ALWAYS be PENDING, promises fulfilled with other promises + all others are handled

        return promise.always((r, e) -> this.resolve(promise, type));
    }

    /**
     * Copy an iterable into a new {@link ArrayList}, performing {@link #resolve(Object, Class)} on each element.
     * <p>
     * Any null parameters will result in a {@link NullPointerException}.
     *
     * @param inputIterable An iterable of items to resolve.
     * @param <T>           The target type of the items.
     * @return A new {@link ArrayList} containing the resolved promises (possibly pending).
     * @throws NullPointerException If either input is null.
     */
    public <T> ArrayList<Promise<T>> resolveAll(Iterable<?> inputIterable, Class<? extends T> type) throws NullPointerException {
        Objects.requireNonNull(inputIterable);
        Objects.requireNonNull(type);

        ArrayList<Promise<T>> outputList = new ArrayList<>();
        inputIterable.forEach((input) -> outputList.add(this.resolve(input, type)));
        return outputList;
    }

    /**
     * Find the deepest value possible, in a chain of promises. This method is synchronous, and will return any
     * unresolved promise, and will return {@code REJECTED} promises (over the exception itself), if that is the
     * deepest possible value.
     *
     * @param promise The starting promise.
     * @return The resolved value. If the value is a promise, we were waiting or rejected.
     * @throws CircularResolutionException If the input promise has circular references (throws on input promise).
     */
    private Object resolvePromise(Promise<?> promise) throws CircularResolutionException {
        Function<Promise<?>, Object> cannotContinue = (endPromise) -> {
            // return value will be a promise, if it was PENDING or REJECTED that we stopped on
            if (PromiseState.FULFILLED != endPromise.getState()) {
                return endPromise;
            }

            Object value = endPromise.thenSync();

            // if we resolved with ourselves, this was an old edge case, but just in case
            if (value == endPromise) {
                throw new CircularResolutionException(promise);
            }

            // resolve to the value of the end promise; it should NEVER be a promise
            return value;
        };

        // Floyd's cycle-finding algorithm - http://stackoverflow.com/a/2663147

        Promise<?> tortoise = promise;
        Promise<?> hare = promise;

        while (true) {
            Promise<?> step = null;

            // one step for tortoise
            step = this.next(tortoise);
            if (step == tortoise) {
                // exit: no next steps were found, we stayed on the same step
                return cannotContinue.apply(step);
            }
            tortoise = step;

            // two steps for hare
            step = this.next(hare);
            if (step == hare) {
                // exit: no next steps were found, we stayed on the same step
                return cannotContinue.apply(step);
            }
            hare = step;
            step = this.next(hare);
            if (step == hare) {
                // exit: no next steps were found, we stayed on the same step
                return cannotContinue.apply(step);
            }
            hare = step;

            // if they ever meet we have a circular reference
            if (tortoise == hare) {
                throw new CircularResolutionException(promise);
            }
        }
    }

    /**
     * Get the next promise in a chain.
     * <p>
     * If there is no next promise, or it cannot be reached yet, then the input promise will be returned.
     *
     * @param promise Input promise, must not be null.
     * @return The next promise, resolved from the input, or the same input promise if there was no next.
     * @throws NullPointerException If input promise is null.
     */
    private Promise<?> next(Promise<?> promise) throws NullPointerException {
        Objects.requireNonNull(promise);

        if (PromiseState.FULFILLED != promise.getState()) {
            return promise;
        }

        Object value = promise.thenSync();

        if (!(null != value && value instanceof Promise)) {
            return promise;
        }

        return (Promise<?>) value;
    }

    /**
     * @see #each(Iterable, Function) for details on what this is implementing.
     */
    private <T, U> Promise<List<U>> eachRecursive(List<T> inputList,
                                                  Function<? super T, ? extends Promise<? extends U>> action,
                                                  AtomicInteger index,
                                                  List<Promise<U>> outputList) {
        if (index.get() >= inputList.size()) {
            List<U> resultList = new ArrayList<>();
            outputList.forEach((promise) -> resultList.add(promise.thenSync()));
            return this.fulfill(resultList);
        }

        T value = inputList.get(index.getAndIncrement());

        Promise<U> promise = this.attempt(() -> action.apply(value));

        outputList.add(promise);

        return promise.then((r) -> this.eachRecursive(inputList, action, index, outputList));
    }
}
