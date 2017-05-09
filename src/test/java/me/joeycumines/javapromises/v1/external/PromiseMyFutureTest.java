package me.joeycumines.javapromises.v1.external;

import me.joeycumines.javapromises.core.PromiseFactory;
import me.joeycumines.javapromises.core.PromiseTest;

public class PromiseMyFutureTest extends PromiseTest {
    @Override
    protected PromiseFactory getFactory() {
        return PromiseMyFutureFactory.getInstance();
    }
}
