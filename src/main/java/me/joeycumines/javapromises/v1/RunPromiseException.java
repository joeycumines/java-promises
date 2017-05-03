package me.joeycumines.javapromises.v1;

/**
 * Thrown if there was an error running a promise.
 */
public class RunPromiseException extends RuntimeException {
    private PromiseRunnable<?> promise;

    RunPromiseException(PromiseRunnable<?> promise, String message) {
        super("[runtime exception] " + message + ": " + promise.toString());

        this.promise = promise;
    }

    public PromiseRunnable<?> getPromise() {
        return this.promise;
    }
}
