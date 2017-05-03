package me.joeycumines.javapromises.v1;

import me.joeycumines.javapromises.core.Promise;

import java.util.concurrent.CompletionStage;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * This Promise implementation supports the wrapping of the Java 8 interface,
 * {@link java.util.concurrent.CompletionStage}. As the callee controls how new dependant Promise objects are created,
 * said implementation will also control the threading, etc, of dependant stages.
 * <p>
 * The promise state and value is retrieved by way of callback, meaning that, provided the CompletionStage
 * implementation is correct, this promise will always be resolved before resolving promises registered with then, etc.
 * <p>
 * This class is provided more as a way to enable inter-operation with other libraries, then as a recommended way to
 * use promises.
 * <p>
 * The thread-safe implementation of state is implemented by {@link PromiseBase}.
 */
public class PromiseStage<T> extends PromiseBase {
    protected final CompletionStage<T> stage;

    public PromiseStage(CompletionStage<T> stage) {
        super();
        if (null == stage) {
            throw new IllegalArgumentException("stage cannot be null");
        }

        // this means we should always be resolved BEFORE we trigger any callbacks
        this.stage = stage.whenComplete((value, throwable) -> {
            if (null != throwable) {
                this.reject(throwable);
                return;
            }

            //this.resolve(value);
        });
    }

    public CompletionStage<T> getStage() {
        return this.stage;
    }

    @Override
    public Promise then(Function callback) {
        return null;
    }

    @Override
    public Promise then(BiConsumer callback) {
        return null;
    }

    @Override
    public Promise except(Function callback) {
        return null;
    }

    @Override
    public Promise except(BiConsumer callback) {
        return null;
    }

    @Override
    public Promise always(BiFunction callback) {
        return null;
    }
}
