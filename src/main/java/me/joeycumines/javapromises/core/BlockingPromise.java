package me.joeycumines.javapromises.core;

import java.util.function.Consumer;

/**
 * Wraps a {@link Promise} created using {@link PromiseFactory}, so that we can resolve a promise after the fact.
 * <p>
 * This class is tested as part of {@link PromiseFactory}, in the abstract PromiseFactoryTest class.
 */
public class BlockingPromise<T> {
    private final Promise<T> promise;
    private volatile Consumer<? super T> _fulfill;
    private volatile Consumer<Throwable> _reject;
    private final Object lock;

    public BlockingPromise(PromiseFactory factory) {
        this.lock = this;

        this.promise = factory.create((fulfill, reject) -> {
            synchronized (this.lock) {
                this._fulfill = fulfill;
                this._reject = reject;

                this.lock.notifyAll();
            }
        });
    }

    public Promise<T> getPromise() {
        return this.promise;
    }

    public Promise<T> fulfill(T value) {
        if (null == this._fulfill) {
            synchronized (this.lock) {
                while (null == this._fulfill) {
                    try {
                        this.lock.wait();
                    } catch (InterruptedException ignored) {
                    }
                }
            }
        }

        this._fulfill.accept(value);
        return this.promise;
    }

    public Promise<T> reject(Throwable value) {
        if (null == this._reject) {
            synchronized (this.lock) {
                while (null == this._reject) {
                    try {
                        this.lock.wait();
                    } catch (InterruptedException ignored) {
                    }
                }
            }
        }

        this._reject.accept(value);
        return this.promise;
    }
}
