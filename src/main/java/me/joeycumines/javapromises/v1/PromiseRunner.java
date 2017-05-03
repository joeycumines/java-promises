package me.joeycumines.javapromises.v1;

/**
 * A manager for executing promises, used internally by {@link PromiseRunnable}.
 * <p>
 * The purpose of this is to abstract how promises are run.
 * <p>
 * Promises that are created by chaining then and except calls will inherit the same PromiseRunner instance.
 */
public interface PromiseRunner {
    public <T> void runPromise(PromiseRunnable<T> promise);
}
