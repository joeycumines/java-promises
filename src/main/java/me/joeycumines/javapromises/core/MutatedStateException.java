package me.joeycumines.javapromises.core;

/**
 * Thrown if something attempted to change the state more then once.
 */
public class MutatedStateException extends RuntimeException {
    private Promise promise;
    private PromiseState stateOld;
    private PromiseState stateNew;

    public MutatedStateException(Promise promise, PromiseState stateOld, PromiseState stateNew) {
        super("[illegal operation] a promise (" + promise.toString() + ") had it's state illegally changed from " + stateOld.toString() + " to " + stateNew.toString());

        this.promise = promise;
        this.stateOld = stateOld;
        this.stateNew = stateNew;
    }

    public Promise getPromise() {
        return promise;
    }

    public PromiseState getStateOld() {
        return stateOld;
    }

    public PromiseState getStateNew() {
        return stateNew;
    }
}
