package me.joeycumines.javapromises.core;

/**
 * Thrown if something attempted to change the state of a promise more then once.
 */
public class MutatedStateException extends RuntimeException {
    private Promise<?> promise;
    private PromiseState stateOld;
    private PromiseState stateNew;

    public MutatedStateException(Promise<?> promise, PromiseState stateOld, PromiseState stateNew) {
        super("[illegal operation] a promise (" + promise.toString() + ") had it's state illegally changed from " + stateOld.toString() + " to " + stateNew.toString());

        this.promise = promise;
        this.stateOld = stateOld;
        this.stateNew = stateNew;
    }

    public Promise<?> getPromise() {
        return this.promise;
    }

    public PromiseState getStateOld() {
        return this.stateOld;
    }

    public PromiseState getStateNew() {
        return this.stateNew;
    }
}
