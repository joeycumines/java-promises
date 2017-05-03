package me.joeycumines.javapromises.core;

/**
 * Thrown on the attempted resolution of a completed promise, that forms OR will form a circular reference.
 */
public class CircularResolutionException extends RuntimeException {
    private Promise<?> promise;

    /**
     * @param promise The promise which forms the circular reference.
     */
    public CircularResolutionException(Promise<?> promise) {
        super("[illegal operation] cannot resolve promise which creates a circular reference: " + promise.toString());

        this.promise = promise;
    }

    public Promise<?> getPromise() {
        return this.promise;
    }
}
