package me.joeycumines.javapromises.v1;

/**
 * A manager for executing promises, used internally by Promise, created because I wanted to encapsulate the creation of
 * threads.
 * <p>
 * The purpose of this is to abstract how promises are run.
 * <p>
 * Promises that are created by chaining then and except calls will inherit the same PromiseRunner instance.
 */
public interface PromiseRunner {
    public void runPromise(Promise promise);
}
