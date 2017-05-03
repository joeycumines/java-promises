package me.joeycumines.javapromises.core;

import me.joeycumines.javapromises.v1.PromiseRunnableFactory;
import org.junit.Test;

import java.util.function.BiConsumer;
import java.util.function.Function;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Extend this class to test a {@link PromiseApi} implementation.
 */
public abstract class PromiseApiTest {
    abstract protected PromiseApi getApi();
}
