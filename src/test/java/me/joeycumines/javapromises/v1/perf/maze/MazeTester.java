package me.joeycumines.javapromises.v1.perf.maze;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

public class MazeTester {
    private static final RuntimeException RUNTIME_EXCEPTION = new RuntimeException();
    private final Maze maze;

    public MazeTester(int breadth, int depth) {
        this.maze = Maze.generate(breadth, depth);
        assertEquals(depth, this.maze.getSolution().length() - this.maze.getSolution().replace("|", "").length());
    }

    private String solveSingleThreaded(MazeSolution solution, MazeRunner runner) {
        // we are trying to traverse runner
        solution = solution.copy().note(runner);

        if (this.maze.end(runner)) {
            return solution.get();
        }

        MazeRunner[] nextRunnerArray = runner.next();

        if (0 == nextRunnerArray.length) {
            return null;
        }

        for (MazeRunner nextRunner : nextRunnerArray) {
            String s = this.solveSingleThreaded(solution, nextRunner);

            if (null != s) {
                return s;
            }
        }

        return null;
    }

    private void solveMultiThreaded(Executor executor, AtomicReference<String> result, MazeSolution solution, MazeRunner runner) {
        MazeSolution nextSol = solution.copy().note(runner);

        if (this.maze.end(runner)) {
            result.set(nextSol.get());
            synchronized (result) {
                result.notify();
                return;
            }
        }

        if (null != result.get()) {
            return;
        }

        MazeRunner[] nextRunnerArray = runner.next();

        if (0 == nextRunnerArray.length) {
            return;
        }

        for (MazeRunner nextRunner : nextRunnerArray) {
            executor.execute(() -> this.solveMultiThreaded(executor, result, nextSol, nextRunner));
        }
    }

    private CompletableFuture<String> solveUsingCompletableFuture(MazeSolution solution, MazeRunner runner) {
        MazeSolution sol = solution.copy().note(runner);

        if (maze.end(runner)) {
            return CompletableFuture.completedFuture(sol.get());
        }

        MazeRunner[] nextRunnerArray = runner.next();

        if (0 == nextRunnerArray.length) {
            CompletableFuture<String> future = new CompletableFuture<>();
            future.completeExceptionally(RUNTIME_EXCEPTION);
            return future;
        }

        // complete if any of the options complete
        CompletableFuture<String> future = new CompletableFuture<>();

        for (MazeRunner nextRunner : nextRunnerArray) {
            // it would probably run quicker if we had this outside the loop BUT this is the same as the promise test
            CompletableFuture.runAsync(() -> {
                this.solveUsingCompletableFuture(sol, nextRunner)
                        .thenAccept(future::complete);
            });
        }

        return future;
    }

    public Long solveUsingCompletableFuture() {
        Long time = System.currentTimeMillis();

        String solution = this.solveUsingCompletableFuture(new MazeSolution(), this.maze.start()).join();

        time = System.currentTimeMillis() - time;
        assertEquals(this.maze.getSolution(), solution);
        return time;
    }

    public Long solveMultiThreaded() {
        AtomicReference<String> result = new AtomicReference<>();
        Executor executor = ForkJoinPool.commonPool();
        result.set(null);

        Long time = System.currentTimeMillis();

        this.solveMultiThreaded(executor, result, new MazeSolution(), this.maze.start());

        synchronized (result) {
            while (null == result.get()) {
                try {
                    result.wait();
                } catch (InterruptedException ignored) {
                }
            }
        }

        time = System.currentTimeMillis() - time;
        assertEquals(this.maze.getSolution(), result.get());
        return time;
    }

    public Long solveSingleThreaded() {
        Long time = System.currentTimeMillis();
        String solution = this.solveSingleThreaded(new MazeSolution(), this.maze.start());
        time = System.currentTimeMillis() - time;
        assertEquals(this.maze.getSolution(), solution);
        return time;
    }

    public Maze getMaze() {
        return this.maze;
    }
}
