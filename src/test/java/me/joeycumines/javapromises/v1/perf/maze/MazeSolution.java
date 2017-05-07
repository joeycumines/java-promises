package me.joeycumines.javapromises.v1.perf.maze;

import java.util.Objects;

/**
 * Build a maze solution string. Shitty.
 */
public class MazeSolution {
    private String solution;

    public MazeSolution() {
        this("");
    }

    public MazeSolution(String solution) {
        this.solution = solution;
    }

    public MazeSolution note(MazeRunner runner) {
        Objects.requireNonNull(runner);
        this.solution += "|" + runner.getNodeId();
        return this;
    }

    public String get() {
        return this.solution;
    }

    public MazeSolution copy() {
        return new MazeSolution(this.solution);
    }
}
