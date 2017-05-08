package me.joeycumines.javapromises.v1.perf.mather;

import java.util.Map;

import static org.junit.Assert.*;

public class MathRequester {
    private final Map.Entry<String, Double> data;
    private volatile long time;
    private volatile boolean complete;

    public MathRequester(Map.Entry<String, Double> data) {
        this.data = data;
        this.complete = false;
        this.time = 0;
    }

    public String request() {
        if (0 != this.time) {
            fail("single request only");
        }
        this.time = System.currentTimeMillis();
        return this.data.getKey();
    }

    public void respond(double response) {
        if (this.complete) {
            fail("single response only");
        }
        assertEquals(this.data.getValue(), response, 0.001);
        this.time = System.currentTimeMillis() - this.time;
        this.complete = true;
    }

    public boolean isComplete() {
        return this.complete;
    }

    public long getTime() {
        if (!this.complete) {
            fail("incomplete request cannot get time");
        }

        return this.time;
    }

    public MathRequester redo() {
        return new MathRequester(this.data);
    }
}
