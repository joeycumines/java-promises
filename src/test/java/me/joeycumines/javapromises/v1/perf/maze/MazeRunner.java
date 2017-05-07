package me.joeycumines.javapromises.v1.perf.maze;

import java.util.Objects;

/**
 * Actor.
 *
 * - handles moving only
 */
public class MazeRunner {
    private final MazeNode node;

    MazeRunner(MazeNode node) {
        Objects.requireNonNull(node);
        this.node = node;
    }

    public MazeRunner[] next() {
        MazeRunner[] runnerList = new MazeRunner[this.node.getBranches().length];
        for (int x = 0; x < this.node.getBranches().length; x++) {
            runnerList[x] = new MazeRunner(this.node.getBranches()[x]);
        }
        return runnerList;
    }

    String getNodeId() {
        return this.node.getId();
    }

    MazeNode getNode() {
        return node;
    }
}
