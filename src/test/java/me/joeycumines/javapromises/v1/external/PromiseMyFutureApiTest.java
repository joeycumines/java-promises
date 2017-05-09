package me.joeycumines.javapromises.v1.external;

import me.joeycumines.javapromises.core.PromiseApi;
import me.joeycumines.javapromises.core.PromiseApiTest;

public class PromiseMyFutureApiTest extends PromiseApiTest {
    @Override
    protected PromiseApi getApi() {
        return PromiseMyFutureFactory.getInstance();
    }
}
