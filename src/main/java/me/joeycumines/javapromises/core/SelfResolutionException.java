package me.joeycumines.javapromises.core;

/**
 * An exception that is triggered if a promise attempts to resolve to itself.
 */
public class SelfResolutionException extends RuntimeException {
    private Promise promise;

    public SelfResolutionException(Promise promise) {
        super("[illegal operation] a promise resolved to itself: " + promise.toString());

        this.promise = promise;
    }

    public Promise getPromise() {
        return promise;
    }
}
