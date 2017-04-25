package me.joeycumines.javapromises.v1;

import me.joeycumines.javapromises.core.Promise;
import me.joeycumines.javapromises.core.PromiseFactory;

import java.util.function.Consumer;

/**
 * Wraps a Promise created using PromiseFactory, so that we can resolve a promise after the fact.
 */
public class BlockingPromise {
    private Promise promise;
    private Consumer<Object> _resolve;
    private Consumer<Throwable> _reject;
    private final Object lock;

    public BlockingPromise(PromiseFactory factory) {
        this.lock = new Object();

        synchronized (this.lock) {
            this.promise = factory.create((resolve, reject) -> {
                synchronized (this.lock) {
                    this._resolve = resolve;
                    this._reject = reject;

                    this.lock.notifyAll();
                }
            });

            while (null == this._resolve || null == this._reject) {
                try {
                    this.lock.wait();
                } catch (InterruptedException ignored) {
                }
            }
        }
    }

    public Promise getPromise() {
        return promise;
    }

    public Promise resolve(Object value) {
        this._resolve.accept(value);
        return this.promise;
    }

    public Promise reject(Throwable value) {
        this._reject.accept(value);
        return this.promise;
    }
}
