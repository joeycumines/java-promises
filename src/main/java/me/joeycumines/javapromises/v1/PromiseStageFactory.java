package me.joeycumines.javapromises.v1;

import me.joeycumines.javapromises.core.*;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class PromiseStageFactory extends PromiseApi {
    private static PromiseStageFactory globalInstance;

    private final Executor executor;

    public PromiseStageFactory() {
        this.executor = null;
    }

    public PromiseStageFactory(Executor executor) {
        this.executor = executor;
    }

    @Override
    public <T> Promise<T> create(BiConsumer<Consumer<? super T>, Consumer<Throwable>> action) {
        Objects.requireNonNull(action);
        CompletableFuture<T> future = new CompletableFuture<>();

        Promise<T> promise = new PromiseStage<>(future, this.executor);

        Runnable task = () -> {
            try {
                action.accept(
                        (value) -> {
                            PromiseState state = promise.getState();
                            if (PromiseState.PENDING != state) {
                                throw new MutatedStateException(promise, state, PromiseState.FULFILLED);
                            }

                            if (value == promise) {
                                throw new SelfResolutionException(promise);
                            }

                            future.complete(value);
                        },
                        (exception) -> {
                            Objects.requireNonNull(exception);

                            PromiseState state = promise.getState();
                            if (PromiseState.PENDING != state) {
                                throw new MutatedStateException(promise, state, PromiseState.REJECTED);
                            }

                            future.completeExceptionally(exception);
                        }
                );
            } catch (Throwable e) {
                if (!future.isDone() && !future.isCompletedExceptionally() && !future.isCancelled()) {
                    future.completeExceptionally(e);
                }
            }
        };

        if (null == this.executor) {
            CompletableFuture.runAsync(task);
        } else {
            CompletableFuture.runAsync(task, this.executor);
        }

        return promise;
    }

    @Override
    public <T> Promise<T> reject(Throwable reason) {
        Objects.requireNonNull(reason);
        CompletableFuture<T> future = new CompletableFuture<>();
        future.completeExceptionally(reason);
        Promise<T> promise = new PromiseStage<>(future, this.executor);
        promise.sync();
        return promise;
    }

    @Override
    public <T> Promise<T> fulfill(T value) {
        Promise<T> promise = new PromiseStage<>(CompletableFuture.completedFuture(value), this.executor);
        promise.sync();
        return promise;
    }

    @Override
    public <T> Promise<T> wrap(Promise<? extends T> promise) {
        return PromiseStage.wrap(CompletableFuture.completedFuture(null), this.executor, promise);
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
