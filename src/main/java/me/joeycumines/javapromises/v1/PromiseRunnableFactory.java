package me.joeycumines.javapromises.v1;

import me.joeycumines.javapromises.core.PromiseApi;
import me.joeycumines.javapromises.core.PromiseFactory;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * A factory for creating PromiseRunnable instances.
 * <p>
 * Default promise runner is SimpleRunner.
 */
public class PromiseRunnableFactory extends PromiseApi implements PromiseFactory {
    private static PromiseRunnableFactory globalInstance;

    private final PromiseRunner runner;

    public PromiseRunnableFactory(PromiseRunner runner) {
        if (null == runner) {
            throw new IllegalArgumentException("the runner cannot be null");
        }

        this.runner = runner;
    }

    @Override
    public PromiseRunnable create(BiConsumer<Consumer<Object>, Consumer<Throwable>> action) {
        return PromiseRunnable.create()
                .setRunner(this.runner)
                .setAction((promise) -> action.accept(promise::resolve, promise::reject))
                .run();
    }

    /**
     * Create a new finalized promise, with the state REJECTED.
     *
     * @param value The value this will reject with.
     * @return A new REJECTED promise.
     */
    @Override
    public PromiseRunnable reject(Throwable value) {
        return PromiseRunnable.create()
                .setRunner(this.runner)
                .setRun()
                .reject(value);
    }

    @Override
    public PromiseRunnable resolve(Object value) {
        return null;
    }

    /**
     * @return A global PromiseRunnableFactory (thread safe).
     */
    public static PromiseRunnableFactory getInstance() {
        // double checked locking
        if (null == globalInstance) {
            synchronized (PromiseRunnableFactory.class) {
                if (null == globalInstance) {
                    globalInstance = new PromiseRunnableFactory(SimpleRunner.getInstance());
                }
            }

        }

        return globalInstance;
    }
}
