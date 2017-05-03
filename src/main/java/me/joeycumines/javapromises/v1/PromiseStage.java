package me.joeycumines.javapromises.v1;

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
        super();

        Objects.requireNonNull(stage);

        // this means we should always be resolved BEFORE we trigger any callbacks
        this.stage = stage.whenComplete((value, throwable) -> {
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

        this.executor = executor;
    }

    public CompletionStage<T> getStage() {
        return this.stage;
    }

    public Executor getExecutor() {
        return this.executor;
    }

    /**
     * Specify a callback to be run on successful resolution {@code FULFILLED} of this, and return a new promise,
     * that will resolve with the same state and value as the callback's returned promise, <b>after</b> the callback
     * promise resolves. The input parameter will be the {@code FULFILLED} value of {@code this}.
     * <p>
     * The callback will be run as soon as possible (but not inline) if {@code this} is already {@code FULFILLED}.
     * <p>
     * If the callback returns a {@code null} value, then the returned promise will resolve as {@code FULFILLED} with
     * value {@code null}.
     * <p>
     * If an exception is thrown within the callback, then the returned promise will be {@code REJECTED}, with that
     * exception.
     * <p>
     * If {@code this} resolved with the {@code REJECTED} state, then the returned promise will reflect the state and
     * value of {@code this}, and the <b>callback will not be run</b>.
     *
     * @param callback The operation which will be performed if {@code this} resolves successfully.
     * @return A promise which will resolve after the previous promise(s) AND any inner operations.
     * @throws NullPointerException If callback is null.
     */
    @Override
    public <U> Promise<U> then(Function<? super T, Promise<? extends U>> callback) {
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

    /**
     * Specify a callback to be run on successful resolution {@code FULFILLED} of this, and return a new promise,
     * that will fulfill with the value accepted within the callback, into it's second argument, a {@link Consumer}.
     * <p>
     * The callback will be run as soon as possible (but not inline) if {@code this} is already {@code FULFILLED}.
     * <p>
     * If no call is made to this second argument <b>within the callback</b>, then the resolved value of the returned
     * promise will be {@code null}, and the state {@code FULFILLED}.
     * <p>
     * If {@code this} resolves as {@code REJECTED} <b>callback will not be called</b>, and the returned promise will
     * reject with the same {@link Throwable}.
     * <p>
     * If an exception is thrown within the callback, then the returned promise will be {@code REJECTED} with that
     * exception, <b>provided that it has not already resolved</b>.
     * <p>
     * For example:
     * <pre>
     *     <code>
     *         Promise&lt;Integer&gt; input = // some async operation which eventually fulfills with int(5)
     *         Promise&lt;Integer&gt; output = input.then((inputValue, fulfill) -> fulfill.accept(inputValue + 10));
     *         // will output 15
     *         System.out.println(output.thenSync());
     *     </code>
     * </pre>
     *
     * @param callback The operation which will be performed if the promise resolves successfully.
     * @return A promise which will resolve after the previous promise AND any inner operations.
     * @throws NullPointerException If callback is null.
     */
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

    /**
     * Specify a callback to be run if this resolves with a failed state {@code REJECTED}, and return a new promise,
     * that will resolve with the same state and value as the callback's returned promise, <b>after</b> the callback
     * promise resolves. The input parameter will be the {@code REJECTED} value of {@code this} (a {@link Throwable}).
     * <p>
     * To preserve type safety, the returned type of promise within the callback is restricted to things which can be
     * cast to the type of {@code T}, unlike {@link #then(Function)}, which can allow any type, due to the fact that the
     * no-callback case will only ever result in a {@link Throwable}.
     * <p>
     * The callback will be run as soon as possible (but not inline) if {@code this} is already {@code REJECTED}.
     * <p>
     * If the callback returns a {@code null} value, then the returned promise will resolve as {@code FULFILLED} with
     * value {@code null}.
     * <p>
     * If an exception is thrown within the callback, then the returned promise will be {@code REJECTED}, with that
     * exception.
     * <p>
     * If {@code this} resolved with the {@code FULFILLED} state, then the returned promise will reflect the state and
     * value of {@code this}, and the <b>callback will not be run</b>.
     *
     * @param callback The operation which will be performed if the promise resolves exceptionally.
     * @return A promise which will resolve after the previous promise(s) AND any inner operations.
     * @throws NullPointerException If callback is null.
     */
    @Override
    public Promise<T> except(Function<Throwable, Promise<? extends T>> callback) {
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

    /**
     * Specify a callback to be run if this resolves with a failed state {@code REJECTED}, and return a new promise,
     * that will fulfill with the value accepted within the callback, into it's second argument, a {@link Consumer}.
     * <p>
     * To preserve type safety, the type of fulfillment value within the callback is restricted, to things which can be
     * cast to the type of {@code T}, unlike {@link #then(BiConsumer)}, which can allow any type, due to the fact that
     * the no-callback case will only ever result in a {@link Throwable}.
     * <p>
     * The callback will be run as soon as possible (but not inline) if {@code this} is already {@code REJECTED}.
     * <p>
     * If no call is made to this second argument <b>within the callback</b>, then the resolved value of the returned
     * promise will be {@code null}, and the state {@code FULFILLED}.
     * <p>
     * If {@code this} resolves as {@code FULFILLED} <b>callback will not be called</b>, and the returned promise will
     * fulfill with the same value.
     * <p>
     * If an exception is thrown within the callback, then the returned promise will be {@code REJECTED} with that
     * exception, <b>provided that it has not already resolved</b>.
     * <p>
     * For example:
     * <pre>
     *     <code>
     *         Promise&lt;Object&gt; input = // some async operation which eventually rejects with an exception
     *         Promise&lt;Object&gt; output = input.except((exception, fulfill) -> fulfill.accept(exception));
     *         // will output the string representation of the FULFILLED exception originally REJECTED by input
     *         System.out.println(output.thenSync());
     *     </code>
     * </pre>
     *
     * @param callback The operation which will be performed if the promise resolves exceptionally.
     * @return A promise which will resolve after the previous promise AND any inner operations.
     * @throws NullPointerException If callback is null.
     */
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

    /**
     * Specify a callback that will always run on resolution, or as soon as possible if {@code this} is already in
     * a resolved state. Returns a new promise, that will resolve with the same state and value as the callback's return
     * value (a promise), <b>after</b> the returned promise resolves.
     * <p>
     * The right input parameter will be the {@code REJECTED} exception (not {@code null}), if {@code this} rejected,
     * otherwise it can be assumed that the state was {@code FULFILLED}, and the left input parameter will be set, if
     * the fulfillment value was non-null.
     * <p>
     * The callback will be run as soon as possible (but not inline) if {@code this} is already resolved.
     * <p>
     * If the callback returns a {@code null} value, then the returned promise will resolve as {@code FULFILLED} with
     * value {@code null}.
     * <p>
     * If an exception is thrown within the callback, then the returned promise will be {@code REJECTED}, with that
     * exception.
     *
     * @param callback The operation to perform when the promise resolves.
     * @return A promise which will resolve after the previous promise AND any inner operations.
     * @throws NullPointerException If callback is null.
     */
    @Override
    public <U> Promise<U> always(BiFunction<? super T, Throwable, Promise<? extends U>> callback) {
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
