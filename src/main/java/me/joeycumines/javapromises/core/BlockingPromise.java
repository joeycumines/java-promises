package me.joeycumines.javapromises.core;

import java.util.function.Consumer;

/**
 * Wraps a {@link Promise} created using {@link PromiseFactory}, so that we can resolve a promise after the fact.
 * <p>
 * On construction it will block the current thread until the promise action is run, so that the {@code fulfill} and
 * {@code reject} methods can be accessed safely.
 * <p>
 * This class is tested as part of {@link PromiseFactory}, in the abstract PromiseFactoryTest class.
 */
public class BlockingPromise<T> {
    private Promise<T> promise;
    private Consumer<? super T> _fulfill;
    private Consumer<Throwable> _reject;
    private final Object lock;

    public BlockingPromise(PromiseFactory factory) {
        this.lock = new Object();

        synchronized (this.lock) {
            this.promise = factory.create((resolve, reject) -> {
                synchronized (this.lock) {
                    this._fulfill = resolve;
                    this._reject = reject;

                    this.lock.notifyAll();
                }
            });

            while (null == this._fulfill || null == this._reject) {
                try {
                    this.lock.wait();
                } catch (InterruptedException ignored) {
                }
            }
        }
    }

    public Promise<T> getPromise() {
        return this.promise;
    }

    public Promise<T> fulfill(T value) {
        this._fulfill.accept(value);
        return this.promise;
    }

    public Promise<T> reject(Throwable value) {
        this._reject.accept(value);
        return this.promise;
    }
}
