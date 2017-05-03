package me.joeycumines.javapromises.v1;

import me.joeycumines.javapromises.core.PromiseFactory;
import me.joeycumines.javapromises.core.PromiseTest;

import static org.junit.Assert.*;

public class PromiseStageFactoryTest extends PromiseTest {

    @Override
    protected PromiseFactory getFactory() {
        return PromiseStageFactory.getInstance();
    }
}
