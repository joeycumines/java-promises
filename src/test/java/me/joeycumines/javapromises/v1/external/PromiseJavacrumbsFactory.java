package me.joeycumines.javapromises.v1.external;

import me.joeycumines.javapromises.core.*;
import me.joeycumines.javapromises.v1.PromiseStage;
import net.javacrumbs.completionstage.CompletableCompletionStage;
import net.javacrumbs.completionstage.CompletionStageFactory;

import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * https://github.com/lukas-krecan/completion-stage
 */
public class PromiseJavacrumbsFactory extends PromiseApi {
    private static PromiseJavacrumbsFactory globalInstance;
    private final Executor executor;
    private final CompletionStageFactory factory;

    public PromiseJavacrumbsFactory(Executor executor) {
        this.executor = executor;
        this.factory = new CompletionStageFactory(this.executor);
    }

    @Override
    public <T> Promise<T> create(BiConsumer<Consumer<? super T>, Consumer<Throwable>> action) {
        Objects.requireNonNull(action);

        CompletableCompletionStage<T> stage = this.factory.createCompletionStage();

        Promise<T> promise = new PromiseStage<>(stage, this.executor);

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

                            stage.complete(value);
                        },
                        (exception) -> {
                            Objects.requireNonNull(exception);

                            PromiseState state = promise.getState();
                            if (PromiseState.PENDING != state) {
                                throw new MutatedStateException(promise, state, PromiseState.REJECTED);
                            }

                            stage.completeExceptionally(exception);
                        }
                );
            } catch (Throwable e) {
                stage.completeExceptionally(e);
            }
        });

        return promise;
    }

    @Override
    public <T> Promise<T> reject(Throwable reason) {
        Objects.requireNonNull(reason);
        CompletableCompletionStage<T> stage = this.factory.createCompletionStage();
        stage.completeExceptionally(reason);
        Promise<T> promise = new PromiseStage<>(stage, this.executor);
        promise.sync();
        return promise;
    }

    @Override
    public <T> Promise<T> fulfill(T value) {
        Promise<T> promise = new PromiseStage<>(this.factory.completedStage(value), this.executor);
        promise.sync();
        return promise;
    }

    @Override
    public <T> Promise<T> wrap(Promise<? extends T> promise) {
        return PromiseStage.wrap(this.factory.completedStage(null), this.executor, promise);
    }

    /**
     * @return A global PromiseJavacrumbsFactory (thread safe).
     */
    public static PromiseJavacrumbsFactory getInstance() {
        // double checked locking
        if (null == globalInstance) {
            synchronized (PromiseJavacrumbsFactory.class) {
                if (null == globalInstance) {
                    globalInstance = new PromiseJavacrumbsFactory(Executors.newCachedThreadPool());
                }
            }

        }

        return globalInstance;
    }
}
