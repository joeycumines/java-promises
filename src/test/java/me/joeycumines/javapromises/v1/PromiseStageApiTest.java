package me.joeycumines.javapromises.v1;

import me.joeycumines.javapromises.core.PromiseApi;
import me.joeycumines.javapromises.core.PromiseApiTest;

public class PromiseStageApiTest extends PromiseApiTest {
    @Override
    protected PromiseApi getApi() {
        return PromiseStageFactory.getInstance();
    }
}
