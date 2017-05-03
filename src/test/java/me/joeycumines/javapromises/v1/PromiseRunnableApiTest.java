package me.joeycumines.javapromises.v1;

import me.joeycumines.javapromises.core.PromiseApi;
import me.joeycumines.javapromises.core.PromiseApiTest;

public class PromiseRunnableApiTest extends PromiseApiTest {
    @Override
    protected PromiseApi getApi() {
        return PromiseRunnableFactory.getInstance();
    }
}
