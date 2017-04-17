package me.joeycumines.javapromises.core;

/**
 * Thrown if something attempted to change the state more then once.
 */
public class MutatedStateException extends RuntimeException {
    public MutatedStateException(PromiseInterface promise, PromiseState stateOld, PromiseState stateNew) {
        super("[illegal operation] a promise (" + promise.toString() + ") had it's state illegally changed from " + stateOld.toString() + " to " + stateNew.toString());
    }
}
