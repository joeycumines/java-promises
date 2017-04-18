package me.joeycumines.javapromises.v1;

import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

public class PromiseTest {
    @Test
    public void testExample() throws Exception {
        PromiseRunnerInterface runner = PromiseRunner.getInstance();

        Promise p1 = new Promise((promise) -> {
            try {
                TimeUnit.SECONDS.sleep(2);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            promise.resolve("hey b0ss");
        }, runner);

        p1.run();

        for (int x = 0; x < 10; x++) {
            System.out.print(p1.getState());
        }

        System.out.println(p1.thenSync() + " - " + p1.exceptSync());
    }
}