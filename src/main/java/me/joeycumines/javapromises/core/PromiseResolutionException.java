package me.joeycumines.javapromises.core;

/**
 * An exception that is triggered if a promise attempts to resolve to itself, or some other promise (logic error).
 */
public class PromiseResolutionException extends RuntimeException {
    private PromiseInterface promise;

    public PromiseResolutionException(PromiseInterface promise) {
        super("[illegal operation] a promise resolved to another promise: " + promise.toString());

        this.promise = promise;
    }

    public PromiseInterface getPromise() {
        return promise;
    }
}
