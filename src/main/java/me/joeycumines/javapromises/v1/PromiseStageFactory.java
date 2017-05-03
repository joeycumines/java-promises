package me.joeycumines.javapromises.v1;

import me.joeycumines.javapromises.core.Promise;
import me.joeycumines.javapromises.core.PromiseApi;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class PromiseStageFactory extends PromiseApi {
    private static PromiseStageFactory globalInstance;

    private final Executor executor;

    public PromiseStageFactory(Executor executor) {
        this.executor = executor;
    }

    @Override
    public <T> Promise<T> create(BiConsumer<Consumer<? super T>, Consumer<Throwable>> action) {
        CompletableFuture<T> future = new CompletableFuture<>();

        if (null == this.executor) {
            CompletableFuture.runAsync(() -> {
                action.accept(future::complete, future::completeExceptionally);
            });
        } else {
            CompletableFuture.runAsync(() -> {
                action.accept(future::complete, future::completeExceptionally);
            }, this.executor);
        }

        return new PromiseStage<>(future, this.executor);
    }

    @Override
    public <T> Promise<T> reject(Throwable reason) {
        CompletableFuture<T> future = new CompletableFuture<>();
        future.completeExceptionally(reason);
        return new PromiseStage<>(future, this.executor);
    }

    @Override
    public <T> Promise<T> fulfill(T value) {
        return new PromiseStage<>(CompletableFuture.completedFuture(value), this.executor);
    }

    @Override
    public <T> Promise<T> wrap(Promise<? extends T> promise) {
        // TODO: implement this
        return null;
    }

    /**
     * @return A global PromiseStageFactory (thread safe).
     */
    public static PromiseStageFactory getInstance() {
        // double checked locking
        if (null == globalInstance) {
            synchronized (PromiseStageFactory.class) {
                if (null == globalInstance) {
                    globalInstance = new PromiseStageFactory(Executors.newCachedThreadPool());
                }
            }

        }

        return globalInstance;
    }
}
