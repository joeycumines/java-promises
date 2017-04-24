package me.joeycumines.javapromises.v1;

import me.joeycumines.javapromises.core.Promise;

/**
 * Thrown if there was an error running a promise.
 */
public class RunPromiseException extends RuntimeException {
    private Promise promise;

    RunPromiseException(Promise promise, String message) {
        super("[runtime exception] " + message + ": " + promise.toString());

        this.promise = promise;
    }

    public Promise getPromise() {
        return promise;
    }
}
