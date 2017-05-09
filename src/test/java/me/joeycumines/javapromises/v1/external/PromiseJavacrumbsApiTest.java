package me.joeycumines.javapromises.v1.external;

import me.joeycumines.javapromises.core.PromiseApi;
import me.joeycumines.javapromises.core.PromiseApiTest;

public class PromiseJavacrumbsApiTest extends PromiseApiTest {
    @Override
    protected PromiseApi getApi() {
        return PromiseJavacrumbsFactory.getInstance();
    }
}
