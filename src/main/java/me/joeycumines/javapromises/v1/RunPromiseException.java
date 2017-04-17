package me.joeycumines.javapromises.v1;

import me.joeycumines.javapromises.core.PromiseInterface;

/**
 * Thrown if there was an error running a promise.
 */
public class RunPromiseException extends RuntimeException {
    private PromiseInterface promise;

    RunPromiseException(PromiseInterface promise, String message) {
        super("[runtime exception] " + message + ": " + promise.toString());

        this.promise = promise;
    }

    public PromiseInterface getPromise() {
        return promise;
    }
}
