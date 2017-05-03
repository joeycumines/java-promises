package me.joeycumines.javapromises.core;

import java.util.Objects;
import java.util.function.Function;

/**
 * Helpers for the creation and use promises.
 * <p>
 * Note: This class is designed to work with any promise implementation, make a concrete class extending this.
 */
public abstract class PromiseApi implements PromiseFactory {
    // attempt
    // each
    // all
    // race

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
}
