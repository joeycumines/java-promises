package me.joeycumines.javapromises.v1.perf.maze;

import static org.junit.Assert.*;

public class MazeTester {
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
