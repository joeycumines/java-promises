package me.joeycumines.javapromises.v1;

import me.joeycumines.javapromises.core.PromiseFactory;
import me.joeycumines.javapromises.core.PromiseFactoryTest;

import static org.junit.Assert.*;

public class PromiseRunnableFactoryTest extends PromiseFactoryTest {
    @Override
    protected PromiseFactory getFactory() {
        return PromiseRunnableFactory.getInstance();
    }
}
