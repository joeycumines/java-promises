package me.joeycumines.javapromises.v1;

import me.joeycumines.javapromises.core.MutatedStateException;
import me.joeycumines.javapromises.core.Promise;
import me.joeycumines.javapromises.core.PromiseState;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * This Promise implementation supports the wrapping of the Java 8 interface,
 * {@link java.util.concurrent.CompletionStage}. As the callee controls how new dependant Promise objects are created,
 * said implementation will also control the threading, etc, of dependant stages.
 * <p>
 * The promise state and value is retrieved by way of callback, meaning that, provided the {@link CompletionStage}
 * implementation is correct, this promise will always be resolved before resolving promises registered with then, etc.
 * <p>
 * This class is provided more as a way to enable inter-operation with other libraries, then as a recommended way to
 * use promises.
 * <p>
 * The thread-safe implementation of state is implemented by {@link PromiseBase}.
 */
public class PromiseStage<T> extends PromiseBase<T> {
    protected final CompletionStage<T> stage;
    protected final Executor executor;

    public PromiseStage(CompletionStage<T> stage) {
        this(stage, null);
    }

    public PromiseStage(CompletionStage<T> stage, Executor executor) {
        this(stage, executor, null);
    }

    /**
     * @param stage        The completion stage to base this promise off.
     * @param executor     The executor if desired, will be inherited by all chained promises.
     * @param resultSource ONLY FOR INTERNAL USE.
     */
    private PromiseStage(CompletionStage<T> stage, Executor executor, Promise<? extends T> resultSource) {
        super();

        Objects.requireNonNull(stage);

        this.executor = executor;

        // handle resolving another promise as the result source THIS REQUIRES STAGE TO BE COMPLETE, OR LOGIC WILL BORK
        if (null != resultSource) {
            this.stage = stage;
            this.resolve(resultSource);
            return;
        }

        // this means we should always be resolved BEFORE we trigger any callbacks
        this.stage = stage.whenComplete((value, throwable) -> {
            if (PromiseState.PENDING != this.getState()) {
                throw new MutatedStateException(this, this.getState(), null == throwable ? PromiseState.FULFILLED : PromiseState.REJECTED);
            }

            if (null != throwable) {
                if (throwable instanceof CompletionException) {
                    CompletionException a = (CompletionException) throwable;
                    this.reject(a.getCause());
                    return;
                }

                this.reject(throwable);
                return;
            }

            this.fulfill(value);
        });
    }

    public CompletionStage<T> getStage() {
        return this.stage;
    }

    public Executor getExecutor() {
        return this.executor;
    }

    @Override
    public <U> Promise<U> then(Function<? super T, ? extends Promise<? extends U>> callback) {
        // thenComposeAsync(Function<? super T,? extends CompletionStage<U>> fn, Executor executor)
        // inner returns a CompletableFuture - triggered after inner promise
        CompletableFuture<U> future = new CompletableFuture<>();

        Function<? super T, ? extends CompletionStage<U>> fn = (value) -> {
            Promise<? extends U> promise = callback.apply(value);

            if (null == promise || PromiseState.FULFILLED == promise.getState()) {
                future.complete(null == promise ? null : promise.thenSync());
            } else if (PromiseState.REJECTED == promise.getState()) {
                future.completeExceptionally(promise.exceptSync());
            } else {
                promise.always((r, e) -> {
                    if (null != e) {
                        future.completeExceptionally(e);
                        return null;
                    }

                    future.complete(r);
                    return null;
                });
            }

            return future;
        };

        if (null == this.getExecutor()) {
            return new PromiseStage<>(this.getStage().thenComposeAsync(fn));
        }

        return new PromiseStage<>(this.getStage().thenComposeAsync(fn, this.getExecutor()), this.getExecutor());
    }

    @Override
    public <U> Promise<U> then(BiConsumer<? super T, Consumer<? super U>> callback) {
        // thenApplyAsync(Function<? super T,? extends U> fn)
        Function<? super T, ? extends U> fn = (value) -> {
            Holder<U> result = new Holder<>();
            try {
                callback.accept(value, result::setValue);
            } catch (RuntimeException innerException) {
                if (!result.checkFlag()) {
                    throw innerException;
                }
            }
            return result.getValue();
        };

        if (null == this.getExecutor()) {
            return new PromiseStage<>(this.getStage().thenApplyAsync(fn));
        }

        return new PromiseStage<>(this.getStage().thenApplyAsync(fn, this.getExecutor()), this.getExecutor());
    }

    @Override
    public Promise<T> except(Function<Throwable, ? extends Promise<? extends T>> callback) {
        // CompletionStage<T> exceptionally(Function<Throwable,? extends T> fn)
        // set an exception, to indicate if it completed exceptionally
        // thenComposeAsync(Function<? super T,? extends CompletionStage<U>> fn, Executor executor)

        Holder<Throwable> exception = new Holder<>();

        CompletionStage<T> stage = this.getStage()
                .exceptionally((e) -> {
                    if (e instanceof CompletionException) {
                        CompletionException a = (CompletionException) e;
                        e = a.getCause();
                    }

                    exception.setValue(e);

                    return null;
                });

        CompletableFuture<T> future = new CompletableFuture<>();

        Function<? super T, CompletionStage<T>> fn = (v) -> {
            Throwable ex = exception.getValue();

            if (null == ex) {
                // it was not an exceptional completion
                return this.getStage();
            }

            Promise<? extends T> promise = callback.apply(ex);

            if (null == promise || PromiseState.FULFILLED == promise.getState()) {
                future.complete(null == promise ? null : promise.thenSync());
            } else if (PromiseState.REJECTED == promise.getState()) {
                future.completeExceptionally(promise.exceptSync());
            } else {
                promise.always((r, e) -> {
                    if (null != e) {
                        future.completeExceptionally(e);
                        return null;
                    }

                    future.complete(r);
                    return null;
                });
            }

            return future;
        };

        if (null == this.getExecutor()) {
            return new PromiseStage<>(stage.thenComposeAsync(fn));
        }

        return new PromiseStage<>(stage.thenComposeAsync(fn, this.getExecutor()), this.getExecutor());
    }

    @Override
    public Promise<T> except(BiConsumer<Throwable, Consumer<? super T>> callback) {
        Holder<Throwable> exception = new Holder<>();

        CompletionStage<T> stage = this.getStage()
                .exceptionally((e) -> {
                    if (e instanceof CompletionException) {
                        CompletionException a = (CompletionException) e;
                        e = a.getCause();
                    }

                    exception.setValue(e);

                    return null;
                });

        Function<? super T, ? extends T> fn = (value) -> {
            Throwable e = exception.getValue();

            if (null == e) {
                return value;
            }

            Holder<T> result = new Holder<>();
            try {
                callback.accept(e, result::setValue);
            } catch (RuntimeException innerException) {
                if (!result.checkFlag()) {
                    throw innerException;
                }
            }
            return result.getValue();
        };

        if (null == this.getExecutor()) {
            return new PromiseStage<>(stage.thenApplyAsync(fn));
        }

        return new PromiseStage<>(stage.thenApplyAsync(fn, this.getExecutor()), this.getExecutor());
    }

    @Override
    public <U> Promise<U> always(BiFunction<? super T, Throwable, ? extends Promise<? extends U>> callback) {
        // CompletionStage<T> exceptionally(Function<Throwable,? extends T> fn)
        // set an exception, that is only checked if the value for the next one is null
        // thenComposeAsync(Function<? super T,? extends CompletionStage<U>> fn, Executor executor)
        Holder<Throwable> exception = new Holder<>();

        CompletionStage<T> stage = this.getStage()
                .exceptionally((e) -> {
                    if (e instanceof CompletionException) {
                        CompletionException a = (CompletionException) e;
                        e = a.getCause();
                    }

                    exception.setValue(e);

                    return null;
                });

        CompletableFuture<U> future = new CompletableFuture<>();

        Function<? super T, CompletionStage<U>> fn = (value) -> {
            Throwable ex = exception.getValue();

            Promise<? extends U> promise = callback.apply(value, ex);

            if (null == promise || PromiseState.FULFILLED == promise.getState()) {
                future.complete(null == promise ? null : promise.thenSync());
            } else if (PromiseState.REJECTED == promise.getState()) {
                future.completeExceptionally(promise.exceptSync());
            } else {
                promise.always((r, e) -> {
                    if (null != e) {
                        future.completeExceptionally(e);
                        return null;
                    }

                    future.complete(r);
                    return null;
                });
            }

            return future;
        };

        if (null == this.getExecutor()) {
            return new PromiseStage<>(stage.thenComposeAsync(fn));
        }

        return new PromiseStage<>(stage.thenComposeAsync(fn, this.getExecutor()), this.getExecutor());
    }

    /**
     * Use a <b>completed</b> {@link CompletionStage} as a base, create a new {@link PromiseStage}, that will resolve
     * with the same state and value as the provided {@link Promise}.
     * <p>
     * A null stage or promise will result in a {@link NullPointerException}.
     * <p>
     * If the stage provided is not completed, and will never complete, then this thread will hang.
     *
     * @param stage    A SUCCESSFULLY COMPLETED completion stage.
     * @param executor The executor to create the new {@link PromiseStage} with. Can be null.
     * @param promise  The promise to wrap.
     * @param <T>      The type of the returned promise.
     * @return A new promise that will resolve the same as the provided one that is an instance of {@link PromiseStage}.
     */
    public static <T> Promise<T> wrap(CompletionStage<T> stage, Executor executor, Promise<? extends T> promise) {
        Objects.requireNonNull(stage);
        Objects.requireNonNull(promise);

        // if we don't sync with the stage it will be less obvious to debug, then clauses will never run instead
        PromiseStage<T> base = new PromiseStage<>(stage, executor);
        base.sync();

        // exit early with immediate resolution if we can
        if (PromiseState.PENDING != promise.getState()) {
            return new PromiseStage<>(stage, executor, promise);
        }

        return base.then((v) -> promise);
    }

    class Holder<U> {
        private U value;
        private boolean flag;

        Holder() {
            this(null);
        }

        Holder(U value) {
            synchronized (this) {
                setValue(value);
                checkFlag();
            }
        }

        synchronized U getValue() {
            return this.value;
        }

        synchronized void setValue(U value) {
            this.value = value;
            this.flag = true;
        }

        synchronized boolean checkFlag() {
            boolean f = this.flag;
            this.flag = false;
            return f;
        }
    }
}
