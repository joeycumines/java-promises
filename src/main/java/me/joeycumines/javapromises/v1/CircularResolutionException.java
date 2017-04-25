package me.joeycumines.javapromises.v1;

import me.joeycumines.javapromises.core.Promise;

/**
 * If we tried to resolve a repeating loop of promises.
 */
public class CircularResolutionException extends RuntimeException {
    private Promise promise;

    CircularResolutionException(Promise promise) {
        super("[runtime exception] cannot resolve promise which creates a circular reference: " + promise.toString());

        this.promise = promise;
    }

    public Promise getPromise() {
        return promise;
    }
}
