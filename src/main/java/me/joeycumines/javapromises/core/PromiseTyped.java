package me.joeycumines.javapromises.core;

import java.util.function.Function;

/**
 * A convenience, that allows you to simply add this to your Promise implementation as an interface, giving you the
 * ability to have strict typing on the return type of then. This design circumvents type erasure.
 * <p>
 * NOTE: Any changes to this should also be made to Promise, where relevant.
 */
public interface PromiseTyped<T> {
    public Promise getPromise();

    public PromiseState getState();
    public Object getValue() throws PendingValueException;

    public Promise then(Function<T, Object> callback);
    public Promise except(Function callback);
    public Promise always(Function callback);

    public void sync();
    public T thenSync();
    public Throwable exceptSync();
}
