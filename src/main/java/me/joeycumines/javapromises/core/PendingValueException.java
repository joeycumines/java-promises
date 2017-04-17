package me.joeycumines.javapromises.core;

/**
 * Thrown when getValue is called on a promise which is pending.
 */
public class PendingValueException extends RuntimeException {
    private PromiseInterface promise;

    public PendingValueException(PromiseInterface promise) {
        super("[illegal operation] the value of an unresolved promise was accessed: " + promise.toString());

        this.promise = promise;
    }

    public PromiseInterface getPromise() {
        return promise;
    }
}
