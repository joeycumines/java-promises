package me.joeycumines.javapromises.v1;

import me.joeycumines.javapromises.core.PromiseFactory;
import me.joeycumines.javapromises.core.PromiseFactoryTest;

public class PromiseStageFactoryTest extends PromiseFactoryTest {
    @Override
    protected PromiseFactory getFactory() {
        return PromiseStageFactory.getInstance();
    }
}
