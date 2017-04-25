package me.joeycumines.javapromises.core;

import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Extend this class to test a PromiseTyped implementation.
 */
public abstract class PromiseTypedTest {
    abstract protected PromiseFactory getFactory();
}
