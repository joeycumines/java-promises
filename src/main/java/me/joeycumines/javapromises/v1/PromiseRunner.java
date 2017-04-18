package me.joeycumines.javapromises.v1;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Simple singleton example of how a promise runner might work.
 */
public class PromiseRunner implements PromiseRunnerInterface {
    private static PromiseRunner singletonInstance;

    private final ExecutorService executor;

    private PromiseRunner() {
        this.executor = Executors.newCachedThreadPool();
    }

    /**
     * @return A singleton PromiseRunner (thread safe).
     */
    public static PromiseRunner getInstance() {
        // double checked locking
        if (null == singletonInstance) {
            synchronized (PromiseRunner.class) {
                if (null == singletonInstance) {
                    singletonInstance = new PromiseRunner();
                }
            }

        }

        return singletonInstance;
    }

    @Override
    public void runPromise(Promise promise) {
        this.executor.submit(() -> {
            promise.getAction().accept(promise);
        });
    }
}
