package me.joeycumines.javapromises.v1;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Simple singleton example of how a promise runner might work.
 */
public class SimpleRunner implements PromiseRunner {
    private static SimpleRunner singletonInstance;

    private final ExecutorService executor;

    private SimpleRunner() {
        this.executor = Executors.newCachedThreadPool();
    }

    /**
     * @return A singleton SimpleRunner (thread safe).
     */
    public static SimpleRunner getInstance() {
        // double checked locking
        if (null == singletonInstance) {
            synchronized (SimpleRunner.class) {
                if (null == singletonInstance) {
                    singletonInstance = new SimpleRunner();
                }
            }

        }

        return singletonInstance;
    }

    @Override
    public void runPromise(PromiseRunnable promise) {
        this.executor.submit(() -> {
            promise.getAction().accept(promise);
        });
    }
}
