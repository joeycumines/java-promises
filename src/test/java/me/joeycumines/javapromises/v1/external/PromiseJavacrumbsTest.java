package me.joeycumines.javapromises.v1.external;

import me.joeycumines.javapromises.core.Promise;
import me.joeycumines.javapromises.core.PromiseFactory;
import me.joeycumines.javapromises.core.PromiseState;
import me.joeycumines.javapromises.core.PromiseTest;
import me.joeycumines.javapromises.v1.PromiseStage;
import me.joeycumines.javapromises.v1.PromiseStageFactory;
import net.javacrumbs.completionstage.CompletableCompletionStage;
import org.junit.Test;

import static org.junit.Assert.*;

public class PromiseJavacrumbsTest extends PromiseTest {
    @Override
    protected PromiseFactory getFactory() {
        return PromiseJavacrumbsFactory.getInstance();
    }

    @Test
    public void testCompletionStagesDontLeakCompletableFutures() {
        Promise<Object> promise = this.getFactory().fulfill(1);

        if (!(promise instanceof PromiseStage)) {
            fail();
        }

        PromiseStage<Object> promiseStage = (PromiseStage<Object>) promise;

        assertTrue(promiseStage.getStage() instanceof CompletableCompletionStage);

        promise = promiseStage.then((value) -> PromiseStageFactory.getInstance().create((fulfill, reject) -> {
            fulfill.accept((Integer) value + 3);
        }));

        if (!(promise instanceof PromiseStage)) {
            fail();
        }

        promiseStage = (PromiseStage<Object>) promise;

        assertTrue(promiseStage.getStage() instanceof CompletableCompletionStage);

        assertEquals(4, promiseStage.thenSync());
        assertNull(promiseStage.exceptSync());
        assertEquals(PromiseState.FULFILLED, promiseStage.getState());
    }
}
