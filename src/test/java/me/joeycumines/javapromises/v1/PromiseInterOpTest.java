package me.joeycumines.javapromises.v1;

import me.joeycumines.javapromises.core.PromiseFactory;
import me.joeycumines.javapromises.core.PromiseTest;

import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Test that multiple promise implementations play nice.
 * <p>
 * This thing is dumb but hey it didn't cost me any time to write.
 */
public class PromiseInterOpTest extends PromiseTest {
    private static final PromiseFactory[] FACTORY_LIST = new PromiseFactory[]{
            PromiseRunnableFactory.getInstance(),
            PromiseStageFactory.getInstance()
    };

    private static final AtomicInteger COUNTER = new AtomicInteger(0);

    private static final Random RAND = new Random();

    private static synchronized PromiseFactory next() {
        COUNTER.set(RAND.nextInt(FACTORY_LIST.length));
        return FACTORY_LIST[COUNTER.get()];
    }

    @Override
    protected PromiseFactory getFactory() {
        return next();
    }
}
