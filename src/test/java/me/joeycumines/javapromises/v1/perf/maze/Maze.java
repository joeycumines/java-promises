package me.joeycumines.javapromises.v1.perf.maze;

import java.util.Objects;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 * Scenario.
 *
 * - actually just a tree
 * - each node can have branches, which is where the runner can travel from, from there
 * - there is only one way to the end
 * - proof is determined by the list of ids travelled to success
 */
public class Maze {
    private final MazeNode start;
    private final MazeNode end;
    private final String solution;

    Maze(MazeNode start, MazeNode end, String solution) {
        Objects.requireNonNull(start);
        Objects.requireNonNull(end);
        Objects.requireNonNull(solution);
        this.start = start;
        this.end = end;
        this.solution = solution;
    }

    public MazeRunner start() {
        return new MazeRunner(start);
    }

    public boolean end(MazeRunner runner) {
        return runner.getNode() == end;
    }

    public String getSolution() {
        return this.solution;
    }

    private static void decorateNode(Supplier<String> nextId, MazeNode node, int breadth, int depth) {
        Objects.requireNonNull(node);

        if (0 >= depth || 0 >= breadth) {
            return;
        }

        MazeNode[] leaves = new MazeNode[breadth];

        for (int x = 0; x < breadth; x++) {
            leaves[x] = new MazeNode(nextId.get());
            decorateNode(nextId, leaves[x], breadth, depth - 1);
        }

        node.setBranches(leaves);
    }

    public static Maze generate(int breadth, int depth) {
        AtomicInteger idCounter = new AtomicInteger();
        Supplier<String> nextId = () -> Integer.toString(idCounter.incrementAndGet());

        MazeNode start = new MazeNode(nextId.get());

        // generate all links
        decorateNode(nextId, start, breadth, depth - 1);

        // decide on a solution at random
        Random rand = new Random();

        MazeRunner runner = new MazeRunner(start);
        MazeSolution solution = new MazeSolution();
        solution.note(runner);

        MazeNode end = null;

        while (true) {
            MazeRunner[] runnerArray = runner.next();

            if (0 == runnerArray.length) {
                end = runner.getNode();
                break;
            }

            // decide where we are going, and note that in the solution
            // rand.nextInt((max - min) + 1) + min
            runner = runnerArray[rand.nextInt(runnerArray.length)];
            solution.note(runner);
        }

        return new Maze(start, end, solution.get());
    }
}
