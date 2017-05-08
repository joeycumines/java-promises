package me.joeycumines.javapromises.v1;

import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Directly execute {@link PromiseRunnable} instances (promise callbacks, etc) with a provided executor.
 * <p>
 * There is a lazily loaded global instance available for basic use cases, see {@link #getInstance()} for more details.
 */
public class ExecutorRunner implements PromiseRunner {
    private static ExecutorRunner globalInstance;

    private final Executor executor;

    public ExecutorRunner(Executor executor) {
        Objects.requireNonNull(executor);
        this.executor = executor;
    }

    /**
     * Create a new global (singleton, though you can instance others like it manually) executor runner using a new
     * cached thread pool. Due to the various caveats surrounding that particular executor it is not intended for use in
     * a high-concurrency environment with tasks triggered externally (for example, on a web server).
     *
     * @return A global ExecutorRunner (thread safe).
     */
    public static ExecutorRunner getInstance() {
        // double checked locking
        if (null == globalInstance) {
            synchronized (ExecutorRunner.class) {
                if (null == globalInstance) {
                    globalInstance = new ExecutorRunner(Executors.newCachedThreadPool());
                }
            }
        }

        return globalInstance;
    }

    @Override
    public <T> void runPromise(PromiseRunnable<T> promise) {
        this.executor.execute(() -> promise.getAction().accept(promise));
    }
}
