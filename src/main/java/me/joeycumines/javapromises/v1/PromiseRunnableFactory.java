package me.joeycumines.javapromises.v1;

import me.joeycumines.javapromises.core.Promise;
import me.joeycumines.javapromises.core.PromiseApi;

import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * A factory for creating {@link PromiseRunnable} instances.
 * <p>
 * Default promise runner (for the global instance) is {@link ExecutorRunner}.
 */
public class PromiseRunnableFactory extends PromiseApi {
    private static PromiseRunnableFactory globalInstance;

    private final PromiseRunner runner;

    public PromiseRunnableFactory(PromiseRunner runner) {
        Objects.requireNonNull(runner);
        this.runner = runner;
    }

    @Override
    public <T> Promise<T> create(BiConsumer<Consumer<? super T>, Consumer<Throwable>> action) {
        Objects.requireNonNull(action);

        return (new PromiseRunnable<T>())
                .setRunner(this.runner)
                .setAction((promise) -> {
                    try {
                        action.accept(promise::fulfill, promise::reject);
                    } catch (Throwable e) {
                        promise.reject(e);
                    }
                })
                .run();
    }

    @Override
    public <T> Promise<T> reject(Throwable reason) {
        return (new PromiseRunnable<T>())
                .setRunner(this.runner)
                .setRun()
                .reject(reason);
    }

    @Override
    public <T> Promise<T> fulfill(T value) {
        return (new PromiseRunnable<T>())
                .setRunner(this.runner)
                .setRun()
                .fulfill(value);
    }

    @Override
    public <T> Promise<T> wrap(Promise<? extends T> promise) {
        return (new PromiseRunnable<T>())
                .setRunner(this.runner)
                .setRun()
                .resolve(promise);
    }

    /**
     * @return A global PromiseRunnableFactory (thread safe).
     */
    public static PromiseRunnableFactory getInstance() {
        // double checked locking
        if (null == globalInstance) {
            synchronized (PromiseRunnableFactory.class) {
                if (null == globalInstance) {
                    globalInstance = new PromiseRunnableFactory(ExecutorRunner.getInstance());
                }
            }

        }

        return globalInstance;
    }
}
