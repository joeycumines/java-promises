package me.joeycumines.javapromises.core;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Extend this class to test a {@link PromiseApi} implementation.
 */
public abstract class PromiseApiTest {
    abstract protected PromiseApi getApi();

    @Test
    public void testResolveFulfilledImmediately() {
        Promise<Object> input = this.getApi().fulfill(15);

        Promise<Number> output = this.getApi().resolve(input, Integer.class);

        assertEquals(PromiseState.FULFILLED, output.getState());
        assertEquals(15, output.thenSync().intValue());
        assertNull(output.exceptSync());
    }

    @Test
    public void testResolveRejectedImmediately() {
        Exception exception = new Exception();
        Promise<Object> input = this.getApi().reject(exception);

        Promise<Number> output = this.getApi().resolve(input, Integer.class);

        assertEquals(PromiseState.REJECTED, output.getState());
        assertNull(output.thenSync());
        assertEquals(exception, output.exceptSync());
    }

    @Test
    public void testResolvePending() {
        Promise<Object> input = this.getApi().create((fulfill, reject) -> {
        });

        Promise<String> output = this.getApi().resolve(input, String.class);

        assertEquals(PromiseState.PENDING, output.getState());
    }

    @Test
    public void testResolveValue() {
        Promise<String> promise = this.getApi().resolve("HELLO BOSS", String.class);
        assertEquals(PromiseState.FULFILLED, promise.getState());
        assertEquals("HELLO BOSS", promise.thenSync());
        assertEquals(null, promise.exceptSync());
    }

    @Test
    public void testResolveNull() {
        Promise<String> promise = this.getApi().resolve(null, String.class);
        assertEquals(PromiseState.FULFILLED, promise.getState());
        assertEquals(null, promise.thenSync());
        assertEquals(null, promise.exceptSync());
    }

    @Test
    public void testResolveException() {
        Exception exception = new Exception();
        Promise<Exception> promise = this.getApi().resolve(exception, Exception.class);
        assertEquals(PromiseState.FULFILLED, promise.getState());
        assertEquals(exception, promise.thenSync());
        assertEquals(null, promise.exceptSync());
    }

    @Test
    public void testResolveCastFailure() {
        try {
            this.getApi().resolve(4, String.class);
            fail();
        } catch (ClassCastException e) {
            assertNotNull(e);
        }
    }

    @Test
    public void testResolveFulfilled() {
        BlockingPromise<Integer> blocker = new BlockingPromise<>(this.getApi());
        Promise<Integer> promise = this.getApi().resolve(blocker.getPromise(), Integer.class);

        assertEquals(PromiseState.PENDING, promise.getState());

        blocker.fulfill(12319);

        assertEquals(12319, promise.thenSync().intValue());
        assertEquals(null, promise.exceptSync());
        assertEquals(PromiseState.FULFILLED, promise.getState());
    }

    @Test
    public void testResolveRejected() {
        BlockingPromise<Integer> blocker = new BlockingPromise<>(this.getApi());
        Promise<Integer> promise = this.getApi().resolve(blocker.getPromise(), Integer.class);

        assertEquals(PromiseState.PENDING, promise.getState());

        Exception exception = new Exception();
        blocker.reject(exception);

        assertEquals(null, promise.thenSync());
        assertEquals(exception, promise.exceptSync());
        assertEquals(PromiseState.REJECTED, promise.getState());
    }

    @Test
    public void testResolveNullType() {
        try {
            this.getApi().resolve(123, null);
            fail();
        } catch (NullPointerException e) {
            assertNotNull(e);
        }
    }

    @Test
    public void testResolveAssignable() {
        try {
            this.getApi().resolve(mock(Promise.class), Promise.class);
            fail();
        } catch (IllegalArgumentException e) {
            assertNotNull(e);
        }

        try {
            this.getApi().resolve(mock(PromiseDummy.class), PromiseDummy.class);
            fail();
        } catch (IllegalArgumentException e) {
            assertNotNull(e);
        }
    }

    @Test
    public void testResolveNestedTypedPendingStepped() {
        BlockingPromise<String> p1 = new BlockingPromise<>(this.getApi());
        BlockingPromise<Promise<String>> p2 = new BlockingPromise<>(this.getApi());
        BlockingPromise<Promise<Promise<String>>> p3 = new BlockingPromise<>(this.getApi());

        Promise<String> promise = this.getApi().resolve(p3.getPromise(), String.class);

        assertEquals(PromiseState.PENDING, promise.getState());

        p3.fulfill(p2.getPromise());
        assertEquals(PromiseState.PENDING, promise.getState());

        p2.fulfill(p1.getPromise());
        assertEquals(PromiseState.PENDING, promise.getState());

        p1.fulfill("HELLO!");

        assertEquals("HELLO!", promise.thenSync());
        assertEquals(null, promise.exceptSync());
        assertEquals(PromiseState.FULFILLED, promise.getState());
    }

    @Test
    public void testResolveNestedTypedPendingSteppedReject() {
        BlockingPromise<String> p1 = new BlockingPromise<>(this.getApi());
        BlockingPromise<Promise<String>> p2 = new BlockingPromise<>(this.getApi());
        BlockingPromise<Promise<Promise<String>>> p3 = new BlockingPromise<>(this.getApi());

        Promise<String> promise = this.getApi().resolve(p3.getPromise(), String.class);

        assertEquals(PromiseState.PENDING, promise.getState());

        p3.fulfill(p2.getPromise());
        assertEquals(PromiseState.PENDING, promise.getState());

        p2.fulfill(p1.getPromise());
        assertEquals(PromiseState.PENDING, promise.getState());

        Exception exception = new Exception();
        p1.reject(exception);

        assertEquals(null, promise.thenSync());
        assertEquals(exception, promise.exceptSync());
        assertEquals(PromiseState.REJECTED, promise.getState());
    }

    @Test
    public void testResolveNestedTypedPendingSteppedRejectMidway() {
        BlockingPromise<Promise<String>> p2 = new BlockingPromise<>(this.getApi());
        BlockingPromise<Promise<Promise<String>>> p3 = new BlockingPromise<>(this.getApi());

        Promise<String> promise = this.getApi().resolve(p3.getPromise(), String.class);

        assertEquals(PromiseState.PENDING, promise.getState());

        p3.fulfill(p2.getPromise());
        assertEquals(PromiseState.PENDING, promise.getState());

        Exception exception = new Exception();

        p2.reject(exception);

        assertEquals(null, promise.thenSync());
        assertEquals(exception, promise.exceptSync());
        assertEquals(PromiseState.REJECTED, promise.getState());
    }

    @Test
    public void testResolveNestedTypedStepped() {
        BlockingPromise<String> p1 = new BlockingPromise<>(this.getApi());
        BlockingPromise<Promise<String>> p2 = new BlockingPromise<>(this.getApi());
        BlockingPromise<Promise<Promise<String>>> p3 = new BlockingPromise<>(this.getApi());

        p3.fulfill(p2.getPromise());

        p2.fulfill(p1.getPromise());

        p1.fulfill("HELLO!");

        p1.getPromise().sync();
        p2.getPromise().sync();
        p3.getPromise().sync();

        Promise<String> promise = this.getApi().resolve(p3.getPromise(), String.class);
        assertEquals(PromiseState.FULFILLED, promise.getState());
        assertEquals("HELLO!", promise.thenSync());
        assertEquals(null, promise.exceptSync());
    }

    @Test
    public void testResolveNestedTypedSteppedReject() {
        BlockingPromise<String> p1 = new BlockingPromise<>(this.getApi());
        BlockingPromise<Promise<String>> p2 = new BlockingPromise<>(this.getApi());
        BlockingPromise<Promise<Promise<String>>> p3 = new BlockingPromise<>(this.getApi());

        p3.fulfill(p2.getPromise());

        p2.fulfill(p1.getPromise());

        Exception exception = new Exception();
        p1.reject(exception);

        p1.getPromise().sync();
        p2.getPromise().sync();
        p3.getPromise().sync();

        Promise<String> promise = this.getApi().resolve(p3.getPromise(), String.class);
        assertEquals(PromiseState.REJECTED, promise.getState());
        assertEquals(null, promise.thenSync());
        assertEquals(exception, promise.exceptSync());
    }

    @Test
    public void testResolveNestedTypedSteppedRejectMidway() {
        BlockingPromise<Promise<String>> p2 = new BlockingPromise<>(this.getApi());
        BlockingPromise<Promise<Promise<String>>> p3 = new BlockingPromise<>(this.getApi());

        p3.fulfill(p2.getPromise());
        Exception exception = new Exception();
        p2.reject(exception);

        p2.getPromise().sync();
        p3.getPromise().sync();

        Promise<String> promise = this.getApi().resolve(p3.getPromise(), String.class);
        assertEquals(PromiseState.REJECTED, promise.getState());
        assertEquals(null, promise.thenSync());
        assertEquals(exception, promise.exceptSync());
    }


    @Test
    public void testResolveCircularFulfilled1() {
        Promise p1 = mock(Promise.class);
        when(p1.getState()).thenReturn(PromiseState.FULFILLED);
        when(p1.thenSync()).thenReturn(p1);

        try {
            this.getApi().resolve(p1, Object.class);
            fail();
        } catch (CircularResolutionException e) {
            assertNotNull(e);
        }
    }

    @Test
    public void testResolveCircularFulfilled2() {
        BlockingPromise<Object> p1 = new BlockingPromise<>(this.getApi());
        BlockingPromise<Object> p2 = new BlockingPromise<>(this.getApi());

        p1.fulfill(p2.getPromise());
        p2.fulfill(p1.getPromise());

        p1.getPromise().sync();
        p2.getPromise().sync();

        try {
            this.getApi().resolve(p1.getPromise(), Object.class);
            fail();
        } catch (CircularResolutionException e) {
            assertNotNull(e);
        }
    }

    @Test
    public void testResolveCircularFulfilled5() {
        BlockingPromise<Object> p1 = new BlockingPromise<>(this.getApi());
        BlockingPromise<Object> p2 = new BlockingPromise<>(this.getApi());
        BlockingPromise<Object> p3 = new BlockingPromise<>(this.getApi());
        BlockingPromise<Object> p4 = new BlockingPromise<>(this.getApi());
        BlockingPromise<Object> p5 = new BlockingPromise<>(this.getApi());

        p1.fulfill(p5.getPromise());
        p2.fulfill(p1.getPromise());
        p3.fulfill(p2.getPromise());
        p4.fulfill(p3.getPromise());
        p5.fulfill(p4.getPromise());

        p1.getPromise().sync();
        p2.getPromise().sync();
        p3.getPromise().sync();
        p4.getPromise().sync();
        p5.getPromise().sync();

        try {
            this.getApi().resolve(p1.getPromise(), Object.class);
            fail();
        } catch (CircularResolutionException e) {
            assertNotNull(e);
        }
    }

    @Test
    public void testResolveCircular2() {
        BlockingPromise<Object> p1 = new BlockingPromise<>(this.getApi());
        BlockingPromise<Object> p2 = new BlockingPromise<>(this.getApi());

        p1.fulfill(p2.getPromise());

        Promise<Object> promise = this.getApi().resolve(p2.getPromise(), Object.class);
        assertEquals(PromiseState.PENDING, promise.getState());

        p2.fulfill(p1.getPromise());

        assertNotNull(promise.exceptSync());
        assertNull(promise.thenSync());
        assertTrue(promise.exceptSync() instanceof CircularResolutionException);
        assertEquals(PromiseState.REJECTED, promise.getState());
    }

    @Test
    public void testResolveCircular5() {
        BlockingPromise<Object> p1 = new BlockingPromise<>(this.getApi());
        BlockingPromise<Object> p2 = new BlockingPromise<>(this.getApi());
        BlockingPromise<Object> p3 = new BlockingPromise<>(this.getApi());
        BlockingPromise<Object> p4 = new BlockingPromise<>(this.getApi());
        BlockingPromise<Object> p5 = new BlockingPromise<>(this.getApi());

        p2.fulfill(p1.getPromise());
        p3.fulfill(p2.getPromise());
        p4.fulfill(p3.getPromise());
        p5.fulfill(p4.getPromise());

        Promise<Object> promise = this.getApi().resolve(p1.getPromise(), Object.class);
        assertEquals(PromiseState.PENDING, promise.getState());

        p1.fulfill(p5.getPromise());

        assertNotNull(promise.exceptSync());
        assertNull(promise.thenSync());
        assertTrue(promise.exceptSync() instanceof CircularResolutionException);
        assertEquals(PromiseState.REJECTED, promise.getState());
    }

    @Test
    public void testResolveAll() {
        PromiseApi api = spy(this.getApi());

        Integer[] intArray = {1, 2, 3, 4};
        List<Integer> intList = Arrays.asList(intArray);

        List<Promise<Number>> numberList = api.resolveAll(intList, Number.class);

        verify(api, times(1)).resolve(1, Number.class);
        verify(api, times(1)).resolve(2, Number.class);
        verify(api, times(1)).resolve(3, Number.class);
        verify(api, times(1)).resolve(4, Number.class);

        assertEquals(4, numberList.size());
        assertEquals(1, numberList.get(0).thenSync());
        assertEquals(2, numberList.get(1).thenSync());
        assertEquals(3, numberList.get(2).thenSync());
        assertEquals(4, numberList.get(3).thenSync());
    }

    @Test
    public void testResolveAllMulti() {
        ArrayList<Object> objectList = new ArrayList<>();

        Throwable throwable = new Throwable();

        objectList.add(13);
        objectList.add(this.getApi().fulfill(1512));
        objectList.add(false);
        objectList.add("cn");
        objectList.add(this.getApi().reject(throwable));
        objectList.add(this.getApi().create((fulfill, reject) -> {
        }));

        List<Promise<Object>> promiseList = this.getApi().resolveAll(objectList, Object.class);

        assertEquals(6, objectList.size());

        assertEquals(13, promiseList.get(0).thenSync());
        assertEquals(1512, promiseList.get(1).thenSync());
        assertEquals(false, promiseList.get(2).thenSync());
        assertEquals("cn", promiseList.get(3).thenSync());
        assertEquals(throwable, promiseList.get(4).exceptSync());
        assertEquals(PromiseState.PENDING, promiseList.get(5).getState());
    }

    @Test
    public void testAttemptNull() {
        Promise<Integer> promise = this.getApi().attempt((Supplier<Promise<Integer>>) () -> null);

        assertEquals(null, promise.thenSync());
        assertEquals(null, promise.exceptSync());
        assertEquals(PromiseState.FULFILLED, promise.getState());
    }

    @Test
    public void testAttemptThrow() {
        RuntimeException e = new RuntimeException();
        Promise<Integer> promise = this.getApi().attempt((Supplier<Promise<Integer>>) () -> {
            throw e;
        });

        assertEquals(null, promise.thenSync());
        assertEquals(e, promise.exceptSync());
        assertEquals(PromiseState.REJECTED, promise.getState());
    }

    @Test
    public void testAttemptFulfilled() {
        Promise<Integer> promise = this.getApi().attempt((Supplier<Promise<Integer>>) () -> this.getApi().fulfill(23));

        assertEquals(23, promise.thenSync().intValue());
        assertEquals(null, promise.exceptSync());
        assertEquals(PromiseState.FULFILLED, promise.getState());
    }

    @Test
    public void testAttemptRejected() {
        Throwable e = new Throwable();
        Promise<Integer> promise = this.getApi().attempt((Supplier<Promise<Integer>>) () -> this.getApi().reject(e));

        assertEquals(null, promise.thenSync());
        assertEquals(e, promise.exceptSync());
        assertEquals(PromiseState.REJECTED, promise.getState());
    }

    @Test
    public void testAttemptNullAction() {
        Supplier<Promise<Object>> supplier = null;
        try {
            //noinspection ConstantConditions
            this.getApi().attempt(supplier);
            fail();
        } catch (NullPointerException e) {
            assertNotNull(e);
        }
    }

    @Test
    public void testAttemptRunnableNull() {
        AtomicInteger counter = new AtomicInteger(0);
        Promise<?> promise = this.getApi().attempt((Runnable) counter::incrementAndGet);

        assertEquals(null, promise.thenSync());
        assertEquals(null, promise.exceptSync());
        assertEquals(PromiseState.FULFILLED, promise.getState());
        assertEquals(1, counter.get());
    }

    @Test
    public void testAttemptRunnableThrow() {
        RuntimeException e = new RuntimeException();
        Promise<?> promise = this.getApi().attempt((Runnable) () -> {
            throw e;
        });

        assertEquals(null, promise.thenSync());
        assertEquals(e, promise.exceptSync());
        assertEquals(PromiseState.REJECTED, promise.getState());
    }

    @Test
    public void testAttemptRunnableNullAction() {
        Runnable runnable = null;
        try {
            //noinspection ConstantConditions
            this.getApi().attempt(runnable);
            fail();
        } catch (NullPointerException e) {
            assertNotNull(e);
        }
    }


    @Test
    public void testEach() {
        Integer[] intArray = {1, 2, 3, 4};
        List<Integer> intList = Arrays.asList(intArray);

        Promise<List<String>> promise = this.getApi().each(intList, (value) -> this.getApi().fulfill(value.toString()));

        List<String> result = promise.thenSync();
        assertNull(promise.exceptSync());
        assertEquals(PromiseState.FULFILLED, promise.getState());

        assertEquals(4, result.size());
        assertEquals("1", result.get(0));
        assertEquals("2", result.get(1));
        assertEquals("3", result.get(2));
        assertEquals("4", result.get(3));
    }

    @Test
    public void testEachNull() {
        Integer[] intArray = {1, 2, 3, 4};
        List<Integer> intList = Arrays.asList(intArray);

        Promise<List<String>> promise = this.getApi().each(intList, (value) -> null);

        List<String> result = promise.thenSync();
        assertNull(promise.exceptSync());
        assertEquals(PromiseState.FULFILLED, promise.getState());

        assertEquals(4, result.size());
        assertEquals(null, result.get(0));
        assertEquals(null, result.get(1));
        assertEquals(null, result.get(2));
        assertEquals(null, result.get(3));
    }

    @Test
    public void testEachIsSerial() {
        Integer[] intArray = {1, 2, null, 3, 4};
        List<Integer> intList = Arrays.asList(intArray);

        AtomicInteger counter = new AtomicInteger(0);
        AtomicInteger counterNull = new AtomicInteger(0);

        Promise<List<Integer>> promise = this.getApi().each(intList, (value) -> {
            try {
                TimeUnit.MILLISECONDS.sleep(200);
            } catch (InterruptedException ignored) {
            }

            if (null == value) {
                counterNull.incrementAndGet();
                return null;
            }

            return this.getApi().fulfill(counter.incrementAndGet() * value);
        });

        assertEquals(PromiseState.PENDING, promise.getState());

        List<Integer> result = promise.thenSync();
        assertNull(promise.exceptSync());
        assertEquals(PromiseState.FULFILLED, promise.getState());

        assertEquals(5, result.size());
        assertEquals(1, result.get(0).intValue());
        assertEquals(4, result.get(1).intValue());
        assertEquals(null, result.get(2));
        assertEquals(9, result.get(3).intValue());
        assertEquals(16, result.get(4).intValue());

        assertEquals(4, counter.get());
        assertEquals(1, counterNull.get());
    }

    @Test
    public void testEachReject1() {
        Integer[] intArray = {1};
        List<Integer> intList = Arrays.asList(intArray);

        Throwable e = new Throwable();

        Promise<List<Integer>> promise = this.getApi().each(intList, (value) -> {
            return this.getApi().reject(e);
        });

        assertNull(promise.thenSync());
        assertEquals(e, promise.exceptSync());
        assertEquals(PromiseState.REJECTED, promise.getState());
    }

    @Test
    public void testEachRejectPartial() {
        Integer[] intArray = {1, 2, 3, 4};
        List<Integer> intList = Arrays.asList(intArray);

        AtomicInteger counter = new AtomicInteger(0);
        RuntimeException e = new RuntimeException();

        Promise<List<Integer>> promise = this.getApi().each(intList, (value) -> {
            if (3 == value) {
                return this.getApi().reject(e);
            }

            return this.getApi().fulfill(counter.incrementAndGet() * value);
        });

        assertNull(promise.thenSync());
        assertEquals(e, promise.exceptSync());
        assertEquals(PromiseState.REJECTED, promise.getState());

        assertEquals(2, counter.get());
    }

    @Test
    public void testEachRejectThrown() {
        Integer[] intArray = {1, 2, 3, 4};
        List<Integer> intList = Arrays.asList(intArray);

        AtomicInteger counter = new AtomicInteger(0);
        RuntimeException e = new RuntimeException();

        Promise<List<Integer>> promise = this.getApi().each(intList, (value) -> {
            if (3 == value) {
                throw e;
            }

            return this.getApi().fulfill(counter.incrementAndGet() * value);
        });

        assertNull(promise.thenSync());
        assertEquals(e, promise.exceptSync());
        assertEquals(PromiseState.REJECTED, promise.getState());

        assertEquals(2, counter.get());
    }

    @Test
    public void testEachEmpty() {
        @SuppressWarnings("MismatchedReadAndWriteOfArray") Integer[] intArray = {};
        List<Integer> intList = Arrays.asList(intArray);

        AtomicInteger counter = new AtomicInteger(0);

        Promise<List<Integer>> promise = this.getApi().each(intList, (value) -> {
            return this.getApi().fulfill(counter.incrementAndGet() * value);
        });

        List<Integer> result = promise.thenSync();
        assertNull(promise.exceptSync());
        assertEquals(PromiseState.FULFILLED, promise.getState());

        assertEquals(0, result.size());
        assertEquals(0, counter.get());
    }

    @Test
    public void testEachMultipleTypes() {
        Exception e = new Exception("Big hello!");
        Object[] objArray = {44, 2.23, "hello!", e};
        List<Object> objList = Arrays.asList(objArray);

        Promise<List<Object>> promise = this.getApi().each(objList, (value) -> this.getApi().fulfill(value));

        List<Object> result = promise.thenSync();
        assertNull(promise.exceptSync());
        assertEquals(PromiseState.FULFILLED, promise.getState());

        assertEquals(4, result.size());
        assertEquals("44", result.get(0).toString());
        assertEquals("2.23", result.get(1).toString());
        assertEquals("hello!", result.get(2).toString());
        assertEquals(e.toString(), result.get(3).toString());
    }

    @Test
    public void testEachNullPointerExceptionLeft() {
        try {
            this.getApi().each(null, (v) -> null);
            fail();
        } catch (NullPointerException e) {
            assertNotNull(e);
        }
    }

    @Test
    public void testEachNullPointerExceptionRight() {
        try {
            this.getApi().each(new ArrayList<Promise<Object>>(), null);
            fail();
        } catch (NullPointerException e) {
            assertNotNull(e);
        }
    }

    @Test
    public void testResolveAllNullPointerExceptionLeft() {
        try {
            this.getApi().resolveAll(null, Object.class);
            fail();
        } catch (NullPointerException e) {
            assertNotNull(e);
        }
    }

    @Test
    public void testResolveAllNullPointerExceptionRight() {
        try {
            this.getApi().resolveAll(new ArrayList<Promise<Object>>(), null);
            fail();
        } catch (NullPointerException e) {
            assertNotNull(e);
        }
    }

    @Test
    public void testRaceNull() {
        try {
            this.getApi().race(null);
            fail();
        } catch (NullPointerException e) {
            assertNotNull(e);
        }
    }

    @Test
    public void testRaceExitEarlyFulfill() {
        List<Promise<Object>> promiseList = new ArrayList<>();
        promiseList.add(this.getApi().fulfill(5));
        promiseList.add(this.getApi().reject(new Throwable()));

        promiseList.forEach(Promise::sync);

        Promise<Object> promise = this.getApi().race(promiseList);

        assertEquals(PromiseState.FULFILLED, promise.getState());
        assertEquals(5, promise.thenSync());
        assertEquals(null, promise.exceptSync());
    }

    @Test
    public void testRaceExitEarlyReject() {
        List<Promise<Object>> promiseList = new ArrayList<>();
        Throwable throwable = new Throwable();
        promiseList.add(this.getApi().reject(throwable));
        promiseList.add(this.getApi().fulfill(5));

        promiseList.forEach(Promise::sync);

        Promise<Object> promise = this.getApi().race(promiseList);

        assertEquals(PromiseState.REJECTED, promise.getState());
        assertEquals(null, promise.thenSync());
        assertEquals(throwable, promise.exceptSync());
    }

    @Test
    public void testRaceNullPromise() {
        List<Promise<Object>> promiseList = new ArrayList<>();
        promiseList.add(null);
        try {
            this.getApi().race(promiseList);
            fail();
        } catch (NullPointerException e) {
            assertNotNull(e);
        }
    }

    @Test
    public void testRaceFulfill() {
        BlockingPromise<Object> b1 = new BlockingPromise<>(this.getApi());
        BlockingPromise<Object> b2 = new BlockingPromise<>(this.getApi());
        BlockingPromise<Object> b3 = new BlockingPromise<>(this.getApi());

        List<Promise<Object>> promiseList = new ArrayList<>();
        promiseList.add(b1.getPromise());
        promiseList.add(b2.getPromise());
        promiseList.add(b3.getPromise());

        Promise<Object> promise = this.getApi().race(promiseList);

        assertEquals(PromiseState.PENDING, promise.getState());

        b2.fulfill(512);

        assertEquals(512, promise.thenSync());
        assertEquals(null, promise.exceptSync());
        assertEquals(PromiseState.FULFILLED, promise.getState());

        b1.reject(new Exception());
        b3.fulfill("no");

        assertEquals(512, promise.thenSync());
        assertEquals(null, promise.exceptSync());
        assertEquals(PromiseState.FULFILLED, promise.getState());
    }

    @Test
    public void testRaceReject() {
        BlockingPromise<Object> b1 = new BlockingPromise<>(this.getApi());
        BlockingPromise<Object> b2 = new BlockingPromise<>(this.getApi());
        BlockingPromise<Object> b3 = new BlockingPromise<>(this.getApi());

        List<Promise<Object>> promiseList = new ArrayList<>();
        promiseList.add(b1.getPromise());
        promiseList.add(b2.getPromise());
        promiseList.add(b3.getPromise());

        Promise<Object> promise = this.getApi().race(promiseList);

        assertEquals(PromiseState.PENDING, promise.getState());

        Exception e = new Exception();
        b2.reject(e);

        assertEquals(null, promise.thenSync());
        assertEquals(e, promise.exceptSync());
        assertEquals(PromiseState.REJECTED, promise.getState());

        b1.reject(new Exception());
        b3.fulfill("no");

        assertEquals(null, promise.thenSync());
        assertEquals(e, promise.exceptSync());
        assertEquals(PromiseState.REJECTED, promise.getState());
    }

    @Test
    public void testRaceEmptyIterable() {
        List<Promise<Object>> promiseList = new ArrayList<>();
        try {
            this.getApi().race(promiseList);
            fail();
        } catch (IllegalArgumentException e) {
            assertNotNull(e);
        }
    }

    @Test
    public void testAllNull() {
        try {
            this.getApi().all(null);
            fail();
        } catch (NullPointerException e) {
            assertNotNull(e);
        }
    }

    @Test
    public void testAllEmptyIterable() {
        List<Promise<Object>> promiseList = new ArrayList<>();
        try {
            this.getApi().all(promiseList);
            fail();
        } catch (IllegalArgumentException e) {
            assertNotNull(e);
        }
    }

    @Test
    public void testAllExitEarlyReject() {
        Exception e = new Exception();
        List<Promise<Object>> promiseList = new ArrayList<>();
        promiseList.add(this.getApi().fulfill(5));
        promiseList.add(this.getApi().reject(e));

        promiseList.forEach(Promise::sync);

        Promise<List<Object>> promise = this.getApi().all(promiseList);

        assertEquals(PromiseState.REJECTED, promise.getState());
        assertEquals(null, promise.thenSync());
        assertEquals(e, promise.exceptSync());
    }

    @Test
    public void testAllExitEarlyFulfill() {
        List<Promise<Object>> promiseList = new ArrayList<>();
        promiseList.add(this.getApi().fulfill(161));
        promiseList.add(this.getApi().fulfill(5));

        promiseList.forEach(Promise::sync);

        Promise<List<Object>> promise = this.getApi().all(promiseList);

        assertEquals(PromiseState.FULFILLED, promise.getState());
        assertEquals(null, promise.exceptSync());
        List<Object> result = promise.thenSync();
        assertEquals(2, result.size());
        assertEquals(161, result.get(0));
        assertEquals(5, result.get(1));
    }

    @Test
    public void testAllNullPromise() {
        List<Promise<Object>> promiseList = new ArrayList<>();
        promiseList.add(null);
        try {
            this.getApi().all(promiseList);
            fail();
        } catch (NullPointerException e) {
            assertNotNull(e);
        }
    }

    @Test
    public void testAllFulfill() {
        BlockingPromise<Object> b1 = new BlockingPromise<>(this.getApi());
        BlockingPromise<Object> b2 = new BlockingPromise<>(this.getApi());
        BlockingPromise<Object> b3 = new BlockingPromise<>(this.getApi());

        List<Promise<Object>> promiseList = new ArrayList<>();
        promiseList.add(b1.getPromise());
        promiseList.add(b2.getPromise());
        promiseList.add(b3.getPromise());

        Promise<List<Object>> promise = this.getApi().all(promiseList);

        assertEquals(PromiseState.PENDING, promise.getState());

        b2.fulfill("first to resolve");
        b2.getPromise().sync();
        try {
            TimeUnit.MILLISECONDS.sleep(400);
        } catch (InterruptedException ignored) {
            ;
        }

        assertEquals(PromiseState.PENDING, promise.getState());

        Object b1Value = new Object();
        b1.fulfill(b1Value);
        b1.getPromise().sync();
        try {
            TimeUnit.MILLISECONDS.sleep(400);
        } catch (InterruptedException ignored) {
            ;
        }

        assertEquals(PromiseState.PENDING, promise.getState());

        b3.fulfill(true);

        assertEquals(null, promise.exceptSync());
        assertEquals(PromiseState.FULFILLED, promise.getState());

        List<Object> result = promise.thenSync();

        assertEquals(3, result.size());
        assertEquals(b1Value, result.get(0));
        assertEquals("first to resolve", result.get(1));
        assertEquals(true, result.get(2));
    }

    @Test
    public void testAllFulfillPartial() {
        BlockingPromise<Object> blocker = new BlockingPromise<>(this.getApi());
        List<Promise<Object>> promiseList = new ArrayList<>();
        promiseList.add(this.getApi().fulfill("first"));
        promiseList.add(blocker.getPromise());

        Promise<List<Object>> promise = this.getApi().all(promiseList);

        assertEquals(PromiseState.PENDING, promise.getState());
        try {
            TimeUnit.MILLISECONDS.sleep(400);
        } catch (InterruptedException ignored) {
            ;
        }
        assertEquals(PromiseState.PENDING, promise.getState());

        blocker.fulfill(91241);

        assertEquals(null, promise.exceptSync());
        assertEquals(PromiseState.FULFILLED, promise.getState());

        List<Object> result = promise.thenSync();

        assertEquals(2, result.size());
        assertEquals("first", result.get(0));
        assertEquals(91241, result.get(1));
    }

    @Test
    public void testAllReject() {
        BlockingPromise<Object> b1 = new BlockingPromise<>(this.getApi());
        BlockingPromise<Object> b2 = new BlockingPromise<>(this.getApi());
        BlockingPromise<Object> b3 = new BlockingPromise<>(this.getApi());

        List<Promise<Object>> promiseList = new ArrayList<>();
        promiseList.add(b1.getPromise());
        promiseList.add(b2.getPromise());
        promiseList.add(b3.getPromise());

        Promise<List<Object>> promise = this.getApi().all(promiseList);

        assertEquals(PromiseState.PENDING, promise.getState());

        Throwable e = new Throwable();
        b3.reject(e);

        assertEquals(e, promise.exceptSync());
        assertEquals(null, promise.thenSync());
        assertEquals(PromiseState.REJECTED, promise.getState());

        b1.fulfill(1414);
        b1.getPromise().sync();
        b2.reject(new RuntimeException());
        b2.getPromise().sync();

        assertEquals(e, promise.exceptSync());
        assertEquals(null, promise.thenSync());
        assertEquals(PromiseState.REJECTED, promise.getState());
    }

    @Test
    public void testAllRejectPartial() {
        BlockingPromise<Object> blocker = new BlockingPromise<>(this.getApi());
        List<Promise<Object>> promiseList = new ArrayList<>();
        promiseList.add(this.getApi().fulfill("first"));
        promiseList.add(blocker.getPromise());

        Promise<List<Object>> promise = this.getApi().all(promiseList);

        Throwable e = new Throwable();
        blocker.reject(e);

        assertEquals(e, promise.exceptSync());
        assertEquals(null, promise.thenSync());
        assertEquals(PromiseState.REJECTED, promise.getState());
    }

    @Test
    public void testAllFulfillOne() {
        Promise<Integer> input = this.getApi().attempt(() -> {
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException ignored) {
            }
            return this.getApi().fulfill(152);
        });

        List<Promise<Integer>> promiseList = new ArrayList<>();
        promiseList.add(input);

        Promise<List<Number>> promise = this.getApi().all(promiseList);

        assertEquals(PromiseState.PENDING, promise.getState());
        assertEquals(null, promise.exceptSync());
        assertEquals(PromiseState.FULFILLED, promise.getState());

        List<Number> result = promise.thenSync();

        assertEquals(1, result.size());
        assertEquals(152, result.get(0).intValue());
    }

    @Test
    public void testAllRejectOne() {
        Throwable e = new Throwable();

        Promise<Integer> input = this.getApi().attempt(() -> {
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException ignored) {
            }
            return this.getApi().reject(e);
        });

        List<Promise<Integer>> promiseList = new ArrayList<>();
        promiseList.add(input);

        Promise<List<Number>> promise = this.getApi().all(promiseList);

        assertEquals(PromiseState.PENDING, promise.getState());
        assertEquals(e, promise.exceptSync());
        assertEquals(null, promise.thenSync());
        assertEquals(PromiseState.REJECTED, promise.getState());
    }

    @Test
    public void testAnyNull() {
        try {
            this.getApi().any(null);
            fail();
        } catch (NullPointerException e) {
            assertNotNull(e);
        }
    }

    @Test
    public void testAnyEmptyIterable() {
        List<Promise<Object>> promiseList = new ArrayList<>();
        try {
            this.getApi().any(promiseList);
            fail();
        } catch (IllegalArgumentException e) {
            assertNotNull(e);
        }
    }

    @Test
    public void testAnyExitEarlyReject() {
        Exception e1 = new Exception();
        Exception e2 = new Exception();
        List<Promise<Object>> promiseList = new ArrayList<>();
        promiseList.add(this.getApi().reject(e1));
        promiseList.add(this.getApi().reject(e2));

        promiseList.forEach(Promise::sync);

        Promise<Object> promise = this.getApi().any(promiseList);

        assertEquals(PromiseState.REJECTED, promise.getState());
        assertEquals(null, promise.thenSync());
        AggregateException e = (AggregateException) promise.exceptSync();
        assertNotNull(e);

        assertEquals(2, e.getExceptionList().size());
        assertEquals(e1, e.getExceptionList().get(0));
        assertEquals(e2, e.getExceptionList().get(1));
    }

    @Test
    public void testAnyExitEarlyFulfill() {
        List<Promise<Object>> promiseList = new ArrayList<>();
        promiseList.add(this.getApi().reject(new Throwable()));
        promiseList.add(this.getApi().fulfill(5));
        promiseList.add(this.getApi().fulfill("asdads"));

        promiseList.forEach(Promise::sync);

        Promise<Object> promise = this.getApi().any(promiseList);

        assertEquals(PromiseState.FULFILLED, promise.getState());
        assertEquals(null, promise.exceptSync());
        assertEquals(5, promise.thenSync());
    }

    @Test
    public void testAnyNullPromise() {
        List<Promise<Object>> promiseList = new ArrayList<>();
        promiseList.add(null);
        try {
            this.getApi().any(promiseList);
            fail();
        } catch (NullPointerException e) {
            assertNotNull(e);
        }
    }

    @Test
    public void testAnyFulfill() {
        BlockingPromise<Object> b1 = new BlockingPromise<>(this.getApi());
        BlockingPromise<Object> b2 = new BlockingPromise<>(this.getApi());
        BlockingPromise<Object> b3 = new BlockingPromise<>(this.getApi());

        List<Promise<Object>> promiseList = new ArrayList<>();
        promiseList.add(b1.getPromise());
        promiseList.add(b2.getPromise());
        promiseList.add(b3.getPromise());

        Promise<Object> promise = this.getApi().any(promiseList);

        assertEquals(PromiseState.PENDING, promise.getState());

        b2.fulfill("first to resolve");
        b2.getPromise().sync();
        try {
            TimeUnit.MILLISECONDS.sleep(400);
        } catch (InterruptedException ignored) {
            ;
        }

        assertEquals("first to resolve", promise.thenSync());
        assertEquals(null, promise.exceptSync());
        assertEquals(PromiseState.FULFILLED, promise.getState());

        Object b1Value = new Object();
        b1.fulfill(b1Value);
        b1.getPromise().sync();
        b3.fulfill(true);
        b3.getPromise().sync();

        assertEquals("first to resolve", promise.thenSync());
        assertEquals(null, promise.exceptSync());
        assertEquals(PromiseState.FULFILLED, promise.getState());
    }

    @Test
    public void testAnyFulfillPartial() {
        BlockingPromise<Object> blocker = new BlockingPromise<>(this.getApi());
        List<Promise<Object>> promiseList = new ArrayList<>();
        promiseList.add(blocker.getPromise());
        promiseList.add(this.getApi().fulfill("first"));

        Promise<Object> promise = this.getApi().any(promiseList);

        assertEquals("first", promise.thenSync());
        assertNull(promise.exceptSync());
        assertEquals(PromiseState.FULFILLED, promise.getState());
    }

    @Test
    public void testAnyReject() {
        BlockingPromise<Object> b1 = new BlockingPromise<>(this.getApi());
        BlockingPromise<Object> b2 = new BlockingPromise<>(this.getApi());
        BlockingPromise<Object> b3 = new BlockingPromise<>(this.getApi());

        List<Promise<Object>> promiseList = new ArrayList<>();
        promiseList.add(b1.getPromise());
        promiseList.add(b2.getPromise());
        promiseList.add(b3.getPromise());

        Promise<Object> promise = this.getApi().any(promiseList);

        assertEquals(PromiseState.PENDING, promise.getState());

        Throwable e1 = new Throwable();
        RuntimeException e2 = new RuntimeException();
        Exception e3 = new Exception();

        b3.reject(e3);
        b3.getPromise().sync();
        try {
            TimeUnit.MILLISECONDS.sleep(400);
        } catch (InterruptedException ignored) {
            ;
        }

        assertEquals(PromiseState.PENDING, promise.getState());

        b1.reject(e1);
        b1.getPromise().sync();
        try {
            TimeUnit.MILLISECONDS.sleep(400);
        } catch (InterruptedException ignored) {
            ;
        }

        assertEquals(PromiseState.PENDING, promise.getState());

        b2.reject(e2);

        assertNull(promise.thenSync());
        assertEquals(PromiseState.REJECTED, promise.getState());

        AggregateException e = (AggregateException) promise.exceptSync();
        assertNotNull(e);
        assertEquals(3, e.getExceptionList().size());
        assertEquals(e1, e.getExceptionList().get(0));
        assertEquals(e2, e.getExceptionList().get(1));
        assertEquals(e3, e.getExceptionList().get(2));
    }

    @Test
    public void testAnyRejectPartial() {
        BlockingPromise<Object> blocker = new BlockingPromise<>(this.getApi());
        List<Promise<Object>> promiseList = new ArrayList<>();

        promiseList.add(blocker.getPromise());
        promiseList.add(this.getApi().reject(new Exception()));

        Promise<Object> promise = this.getApi().any(promiseList);

        Throwable e = new Throwable();
        blocker.reject(e);

        assertNotNull(promise.exceptSync());
        assertEquals(null, promise.thenSync());
        assertEquals(PromiseState.REJECTED, promise.getState());
    }

    @Test
    public void testToCompletableFutureNull() {
        try {
            this.getApi().toCompletableFuture(null);
            fail();
        } catch (NullPointerException e) {
            assertNotNull(e);
        }
    }

    @Test
    public void testToCompletableFutureFulfilled() {
        CompletableFuture<Integer> future = this.getApi().toCompletableFuture(this.getApi().fulfill(5142));
        assertTrue(future.isDone());
        assertEquals(5142, future.join().intValue());
    }

    @Test
    public void testToCompletableFutureRejected() {
        Throwable throwable = new Throwable();
        CompletableFuture<Integer> future = this.getApi().toCompletableFuture(this.getApi().reject(throwable));
        assertTrue(future.isCompletedExceptionally());
        try {
            future.get();
            fail();
        } catch (ExecutionException e) {
            assertEquals(throwable, e.getCause());
        } catch (InterruptedException ignored) {
        }
    }

    @Test
    public void testToCompletableFutureFulfill() {
        BlockingPromise<Integer> blocker = new BlockingPromise<>(this.getApi());
        CompletableFuture<Number> future = this.getApi().toCompletableFuture(blocker.getPromise());

        assertFalse(future.isDone());

        blocker.fulfill(5142);

        assertEquals(5142, future.join().intValue());
        assertTrue(future.isDone());
    }

    @Test
    public void testToCompletableFutureReject() {
        Throwable throwable = new Throwable();
        BlockingPromise<Integer> blocker = new BlockingPromise<>(this.getApi());
        CompletableFuture<Number> future = this.getApi().toCompletableFuture(blocker.getPromise());

        assertFalse(future.isDone());

        blocker.reject(throwable);

        try {
            future.get();
            fail();
        } catch (ExecutionException e) {
            assertEquals(throwable, e.getCause());
        } catch (InterruptedException ignored) {
        }

        assertTrue(future.isCompletedExceptionally());
    }

    @Test
    public void testAnyVarargs() {
        Promise<Integer> promise;

        promise = this.getApi().any(this.getApi().fulfill(1));

        assertEquals(PromiseState.FULFILLED, promise.getState());
        assertEquals(1, promise.thenSync().intValue());


        promise = this.getApi().any(this.getApi().reject(new Exception()), this.getApi().fulfill(2));

        assertEquals(PromiseState.FULFILLED, promise.getState());
        assertEquals(2, promise.thenSync().intValue());


        promise = this.getApi().any(this.getApi().reject(new Exception()), this.getApi().reject(new Exception()), this.getApi().fulfill(3));

        assertEquals(PromiseState.FULFILLED, promise.getState());
        assertEquals(3, promise.thenSync().intValue());
    }

    @Test
    public void testAllVarargs() {
        Promise<List<Integer>> promise;

        promise = this.getApi().all(this.getApi().fulfill(1), this.getApi().fulfill(2), this.getApi().fulfill(3));

        assertEquals(PromiseState.FULFILLED, promise.getState());

        List<Integer> result = promise.thenSync();
        assertEquals(3, result.size());
        assertEquals(1, result.get(0).intValue());
        assertEquals(2, result.get(1).intValue());
        assertEquals(3, result.get(2).intValue());
    }

    @Test
    public void testRaceVarargs() {
        Promise<Integer> promise;

        promise = this.getApi().race(this.getApi().fulfill(1));

        assertEquals(PromiseState.FULFILLED, promise.getState());
        assertEquals(1, promise.thenSync().intValue());


        promise = this.getApi().race((new BlockingPromise<Integer>(this.getApi())).getPromise(), this.getApi().fulfill(2));

        assertEquals(PromiseState.FULFILLED, promise.getState());
        assertEquals(2, promise.thenSync().intValue());


        promise = this.getApi().race((new BlockingPromise<Integer>(this.getApi())).getPromise(), (new BlockingPromise<Integer>(this.getApi())).getPromise(), this.getApi().fulfill(3));

        assertEquals(PromiseState.FULFILLED, promise.getState());
        assertEquals(3, promise.thenSync().intValue());

        Exception e = new Exception();

        promise = this.getApi().race((new BlockingPromise<Integer>(this.getApi())).getPromise(), this.getApi().reject(e));

        assertEquals(PromiseState.REJECTED, promise.getState());
        assertEquals(e, promise.exceptSync());
    }

    abstract class PromiseDummy implements Promise {
    }
}
