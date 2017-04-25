package me.joeycumines.javapromises.core;

import org.junit.Test;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Extend this class to test a PromiseApi implementation.
 */
public abstract class PromiseApiTest {
    abstract protected PromiseApi getApi();
}
