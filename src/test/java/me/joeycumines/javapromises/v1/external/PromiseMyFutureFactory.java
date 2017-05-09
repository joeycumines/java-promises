package me.joeycumines.javapromises.v1.external;

import me.joeycumines.javapromises.core.*;
import me.joeycumines.javapromises.v1.PromiseStage;
import org.inferred.cjp39.j8stages.MyFuture;

import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * https://github.com/chrisalice/j8stages
 */
public class PromiseMyFutureFactory extends PromiseApi {
    private static PromiseMyFutureFactory globalInstance;
    private final Executor executor;

    public PromiseMyFutureFactory(Executor executor) {
        this.executor = executor;
    }

    @Override
    public <T> Promise<T> create(BiConsumer<Consumer<? super T>, Consumer<Throwable>> action) {
        Objects.requireNonNull(action);

        MyFuture<T> future = new MyFuture<>();

        Promise<T> promise = new PromiseStage<>(future, this.executor);

        this.executor.execute(() -> {
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
        });

        return promise;
    }

    @Override
    public <T> Promise<T> reject(Throwable reason) {
        Objects.requireNonNull(reason);
        MyFuture<T> future = new MyFuture<>();
        future.completeExceptionally(reason);
        Promise<T> promise = new PromiseStage<>(future, this.executor);
        promise.sync();
        return promise;
    }

    @Override
    public <T> Promise<T> fulfill(T value) {
        Promise<T> promise = new PromiseStage<>(MyFuture.completed(value), this.executor);
        promise.sync();
        return promise;
    }

    @Override
    public <T> Promise<T> wrap(Promise<? extends T> promise) {
        return PromiseStage.wrap(MyFuture.completed(null), this.executor, promise);
    }

    /**
     * @return A global PromiseMyFutureFactory (thread safe).
     */
    public static PromiseMyFutureFactory getInstance() {
        // double checked locking
        if (null == globalInstance) {
            synchronized (PromiseMyFutureFactory.class) {
                if (null == globalInstance) {
                    globalInstance = new PromiseMyFutureFactory(Executors.newCachedThreadPool());
                }
            }

        }

        return globalInstance;
    }
}
