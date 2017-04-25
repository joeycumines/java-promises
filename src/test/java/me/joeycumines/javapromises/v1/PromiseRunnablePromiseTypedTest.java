package me.joeycumines.javapromises.v1;

import me.joeycumines.javapromises.core.PromiseFactory;
import me.joeycumines.javapromises.core.PromiseTypedTest;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Test PromiseRunnable and it's factory in default configuration against the PromiseTyped interface.
 */
public class PromiseRunnablePromiseTypedTest extends PromiseTypedTest {
    @Override
    protected PromiseRunnableFactory getFactory() {
        return PromiseRunnableFactory.getInstance();
    }
}
