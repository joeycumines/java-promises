package me.joeycumines.javapromises.v1.external;

import me.joeycumines.javapromises.core.PromiseFactory;
import me.joeycumines.javapromises.core.PromiseFactoryTest;

public class PromiseMyFutureFactoryTest extends PromiseFactoryTest {
    @Override
    protected PromiseFactory getFactory() {
        return PromiseMyFutureFactory.getInstance();
    }
}
