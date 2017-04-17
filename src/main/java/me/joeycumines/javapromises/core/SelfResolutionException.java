package me.joeycumines.javapromises.core;

/**
 * An exception that is triggered if a promise attempts to resolve to itself.
 */
public class SelfResolutionException extends RuntimeException {
    SelfResolutionException(PromiseInterface promise) {
        super("[illegal operation] a promise resolved to itself: " + promise.toString());
    }
}
