package me.joeycumines.javapromises.core;

import org.junit.Test;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public abstract class PromiseTest {
    /**
     * Create a promise. Implement this.
     *
     * @param action The task to perform asynchronously.
     * @return A new promise.
     */
    protected abstract Promise create(BiConsumer<Consumer<Object>, Consumer<Object>> action);
}
