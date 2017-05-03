package me.joeycumines.javapromises.v1;

import me.joeycumines.javapromises.core.MutatedStateException;
import me.joeycumines.javapromises.core.Promise;
import me.joeycumines.javapromises.core.SelfResolutionException;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Implementation of PromiseBase to test underlying functionality, exposes reject, fulfill, resolve.
 */
public class PromiseBaseShell<T> extends PromiseBase<T> {
    @Override
    public PromiseBase<T> reject(Throwable exception) throws MutatedStateException, NullPointerException {
        return super.reject(exception);
    }

    @Override
    public PromiseBase<T> fulfill(T value) throws SelfResolutionException, MutatedStateException {
        return super.fulfill(value);
    }

    @Override
    public PromiseBase<T> resolve(Promise<? extends T> promise) throws SelfResolutionException, MutatedStateException {
        return super.resolve(promise);
    }

    @Override
    public <U> Promise<U> then(Function<? super T, Promise<? extends U>> callback) {
        return null;
    }

    @Override
    public <U> Promise<U> then(BiConsumer<? super T, Consumer<? super U>> callback) {
        return null;
    }

    @Override
    public Promise<T> except(Function<Throwable, Promise<? extends T>> callback) {
        return null;
    }

    @Override
    public Promise<T> except(BiConsumer<Throwable, Consumer<? super T>> callback) {
        return null;
    }

    @Override
    public <U> Promise<U> always(BiFunction<? super T, Throwable, Promise<? extends U>> callback) {
        return null;
    }
}
