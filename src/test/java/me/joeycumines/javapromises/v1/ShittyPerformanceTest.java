package me.joeycumines.javapromises.v1;

import me.joeycumines.javapromises.core.*;
import me.joeycumines.javapromises.v1.perf.mather.MathRequester;
import me.joeycumines.javapromises.v1.perf.mather.Mather;
import me.joeycumines.javapromises.v1.perf.maze.MazeRunner;
import me.joeycumines.javapromises.v1.perf.maze.MazeSolution;
import me.joeycumines.javapromises.v1.perf.maze.MazeTester;
import org.junit.Test;

import java.lang.management.ManagementFactory;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.junit.Assert.*;

public class ShittyPerformanceTest {
    private static RuntimeException RUNTIME_EXCEPTION = new RuntimeException();

    @Test
    public void testApiPerformance() {
        List<Map.Entry<String, PromiseApi>> apiList = new ArrayList<>();
        apiList.add(new AbstractMap.SimpleEntry<>("1_RUNNABLE", PromiseRunnableFactory.getInstance()));
        apiList.add(new AbstractMap.SimpleEntry<>("2_RUNNABLE_FORK_JOIN_COMMON", new PromiseRunnableFactory(new ExecutorRunner(ForkJoinPool.commonPool()))));
        apiList.add(new AbstractMap.SimpleEntry<>("3_STAGE", PromiseStageFactory.getInstance()));
        apiList.add(new AbstractMap.SimpleEntry<>("4_STAGE_DEFAULT_EXECUTOR", new PromiseStageFactory()));

        Collections.shuffle(apiList);

        PromiseApi coreApi = PromiseStageFactory.getInstance();

        Supplier<Long> time = System::currentTimeMillis;

        Function<Function<PromiseApi, Promise<?>>, Promise<List<Map.Entry<String, Long>>>> runner = (test) -> {
            // call each of the tests at the same time, and add them to an array
            BlockingPromise<?> blocker = new BlockingPromise<>(coreApi);
//            List<Promise<Long>> apiTest = new ArrayList<>();
//
//            apiList.forEach((api) -> apiTest.add(blocker.getPromise().then((v) -> {
//                // this bit is all api-specific
//                return test.apply(api.getValue()).then((v2, fulfill) -> fulfill.accept(time.get()));
//            })));

            Holder<Long> started = new Holder<>();

            // build the result, it starts on the blocker, then moves through each API sequentially
            Promise<List<Map.Entry<String, Long>>> result = blocker.getPromise()
                    .then((v) -> {
                        return coreApi.each(apiList, (Map.Entry<String, PromiseApi> api) -> {
                            // this bit is all api-specific
                            System.out.println("  + started testing " + api.getKey());
                            started.setValue(time.get());
                            return test.apply(api.getValue()).<Long>then((v2, fulfill) -> {
                                fulfill.accept(time.get() - started.getValue());
                                System.out.println("  - stopped testing " + api.getKey());
                            });
                        });
                    })
                    .then((timeList, fulfill) -> {
                        List<Map.Entry<String, Long>> resultList = new ArrayList<>();

                        for (int x = 0; x < apiList.size(); x++) {
                            resultList.add(new AbstractMap.SimpleEntry<>(apiList.get(x).getKey(), timeList.get(x)));
                        }

                        fulfill.accept(resultList);
                    });

            // actually start the test
            blocker.fulfill(null);
            return result;
        };

        BiConsumer<String, Function<PromiseApi, Promise<?>>> consoleTest = (name, test) -> {
            System.out.println("RUNNING: " + name);
            Throwable throwable = runner.apply(test)
                    .then((resultList, f) -> {
                        System.out.println("COMPLETED: " + name);
                        this.sortByValue(resultList);
                        int place = 1;
                        for (Map.Entry<String, Long> result : resultList) {
                            System.out.println("#" + (place++) + " " + result.getKey() + " ( " + result.getValue() + " ms )");
                        }
                    })
                    .exceptSync();

            if (null != throwable) {
                System.out.println("FAILED: " + name);
                throwable.printStackTrace();
                fail(throwable.getMessage());
            }
        };

        BiConsumer<Integer, Integer> testMaze = (breadth, depth) -> {
            System.out.println("--generating maze for " + breadth + "(breadth) x " + depth + "(depth)--");
            MazeTester mazeTester = new MazeTester(breadth, depth);
            System.out.println("--maze generation complete--");
            System.out.println("[control] maze solution single threaded took (ms): " + mazeTester.solveSingleThreaded());
            System.out.println("[control] maze solution multi threaded took (ms): " + mazeTester.solveMultiThreaded());
            System.out.println("[control] maze solution using CompletableFuture took (ms): " + mazeTester.solveUsingCompletableFuture());

            consoleTest.accept("maze test", (api) -> this.testMaze(api, mazeTester, new MazeSolution(), mazeTester.getMaze().start())
                    .then((String solution) -> {
                        assertEquals(mazeTester.getMaze().getSolution(), solution);
                        //System.out.println(api.getClass().getName() + " got :");
                        //System.out.println(solution);
                        return null;
                    }));
        };

        AtomicBoolean done = new AtomicBoolean(false);
        Thread threadCounter = new Thread(() -> {
            while (!done.get()) {
                long t = System.currentTimeMillis();
                long c = 0;
                long i = 0;

                for (int x = 0; x < 5; x++) {
                    i++;
                    c += ManagementFactory.getThreadMXBean().getThreadCount();

                    synchronized (done) {
                        try {
                            done.wait(100);
                        } catch (InterruptedException ignored) {
                        }
                    }
                    if (done.get()) {
                        break;
                    }
                }

                t = System.currentTimeMillis() - t;
                long count = c / i;
                System.out.println("--------average thread count last " + t + " ms : " + count);
            }
        });

        BiConsumer<Integer, Integer> testRequestResponse = (multi, size) -> {
            System.out.println("-- Running the Request-Response test for difficulty" + multi + " and " + size + " connections");

            System.out.println("-- generating requests");
            List<MathRequester> requestList = new ArrayList<>();

            for (int x = 0; x < size; x++) {
                requestList.add(new MathRequester(Mather.getInstance().genRandomEquation1(multi)));
            }

            System.out.println("-- requests generated");

            Supplier<List<MathRequester>> getRequestList = () -> {
                List<MathRequester> list = new ArrayList<>();
                requestList.forEach((request) -> list.add(request.redo()));
                return Collections.synchronizedList(list);
            };

            Function<List<MathRequester>, Long> getAverageResponse = (list) -> {
                long t = 0;
                long c = 0;

                for (MathRequester request : list) {
                    t += request.getTime();
                    c++;
                }

                if (0 == c) {
                    return -1L;
                }

                return t / c;
            };

            // see how long each request would take if there was no overhead
            Runnable singleThreadControl = () -> {
                List<MathRequester> list = getRequestList.get();

                long t = System.currentTimeMillis();

                for (MathRequester request : list) {
                    request.respond(Mather.getInstance().eval(request.request()));
                }

                t = System.currentTimeMillis() - t;

                System.out.println("[control] on average the actual work should take (ms): " + getAverageResponse.apply(list));
                System.out.println("[control] a single thread served all requests sequentially in (ms): " + t);
            };

            Runnable completableFutureControl = () -> {
                List<MathRequester> list = getRequestList.get();

                long t = System.currentTimeMillis();

                AtomicReference<CompletableFuture<?>> future = new AtomicReference<>(CompletableFuture.completedFuture(null));

                list.forEach((request) -> {
                    CompletableFuture<?> f = CompletableFuture.runAsync(() -> {
                        request.respond(Mather.getInstance().eval(request.request()));
                    });

                    future.set(CompletableFuture.allOf(future.get(), f));
                });

                future.get().join();

                t = System.currentTimeMillis() - t;

                System.out.println("[control] for CompletableFuture on average the work took (ms): " + getAverageResponse.apply(list));
                System.out.println("[control] for CompletableFuture the total time spent was (ms): " + t);
            };


            HashMap<PromiseApi, List<MathRequester>> apiTodoMap = new HashMap<>();
            apiList.forEach((apiPair) -> apiTodoMap.put(apiPair.getValue(), getRequestList.get()));

            Function<PromiseApi, Promise<?>> testPromises = (api) -> {
                List<MathRequester> list = apiTodoMap.get(api);

                List<Promise<MathRequester>> workerList = new ArrayList<>();

                list.forEach((request) -> {
                    workerList.add(api.create((fulfill, reject) -> {
                        request.respond(Mather.getInstance().eval(request.request()));

                        fulfill.accept(request);
                    }));
                });

                return api.all(workerList);
//                AtomicReference<Promise<?>> promise = new AtomicReference<>(api.fulfill(null));
//
//                list.forEach((request) -> {
//                    Promise<?> p = api.create((fulfill, reject) -> {
//                        request.respond(Mather.getInstance().eval(request.request()));
//                        fulfill.accept(null);
//                    });
//                    promise.set(promise.get().then((r) -> p));
//                });
//
//                return promise.get();
            };

            singleThreadControl.run();
            completableFutureControl.run();
            consoleTest.accept("MathRequester test", testPromises);

            for (Map.Entry<PromiseApi, List<MathRequester>> entry : apiTodoMap.entrySet()) {
                AtomicReference<String> name = new AtomicReference<>();
                apiList.forEach((apiPair) -> {
                    if (apiPair.getValue() == entry.getKey()) {
                        name.set(apiPair.getKey());
                    }
                });
                System.out.println("[result] MathRequester time for " + name.get() + " was (ms): " + getAverageResponse.apply(entry.getValue()));
            }
        };

//        threadCounter.start();
//
//        consoleTest.accept("sample test", this::testSample);
//
//        testMaze.accept(2, 6);
//        testMaze.accept(5, 6);
        testMaze.accept(10, 6);
//        testMaze.accept(4, 9);
        testMaze.accept(4, 11);
//        testMaze.accept(2, 21);
//
//        testRequestResponse.accept(100000, 100);
        testRequestResponse.accept(10000, 1000);
//        testRequestResponse.accept(10000, 10000);
        testRequestResponse.accept(100, 100000);

        done.set(true);
        synchronized (done) {
            done.notifyAll();
        }
    }

    /**
     * Move the next step(s), return a promise that will race.
     *
     * @see MazeTester#solveSingleThreaded(MazeSolution, MazeRunner)
     */
    private Promise<String> testMaze(PromiseApi api, MazeTester mazeTester, MazeSolution solution, MazeRunner runner) {
        // we are trying to traverse runner
        solution = solution.copy().note(runner);

        if (mazeTester.getMaze().end(runner)) {
            return api.fulfill(solution.get());
        }

        MazeRunner[] nextRunnerArray = runner.next();

        if (0 == nextRunnerArray.length) {
            return api.reject(RUNTIME_EXCEPTION);
        }

        List<Promise<String>> promiseList = new ArrayList<>();

        for (MazeRunner nextRunner : nextRunnerArray) {
            promiseList.add(this.testMaze(api, mazeTester, solution, nextRunner));
        }

        return api.any(promiseList);
    }

    private Promise<?> testSample(PromiseApi api) {
        return api.attempt(() -> {
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
    }

    private <K, V extends Comparable<? super V>> void sortByValue(List<Map.Entry<K, V>> list) {
        list.sort(Comparator.comparing(o -> (o.getValue())));
    }

    class Holder<U> {
        private U value;
        private boolean flag;

        Holder() {
            this(null);
        }

        Holder(U value) {
            synchronized (this) {
                setValue(value);
                checkFlag();
            }
        }

        synchronized U getValue() {
            return this.value;
        }

        synchronized void setValue(U value) {
            this.value = value;
            this.flag = true;
        }

        synchronized boolean checkFlag() {
            boolean f = this.flag;
            this.flag = false;
            return f;
        }
    }
}
