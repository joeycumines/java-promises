package me.joeycumines.javapromises.v1.perf.maze;

import java.util.Objects;

class MazeNode {
    private MazeNode[] branches;
    private final String id;

    MazeNode(String id) {
        Objects.requireNonNull(id);
        this.branches = new MazeNode[0];
        this.id = id;
    }

    MazeNode[] getBranches() {
        return branches;
    }

    MazeNode setBranches(MazeNode[] branches) {
        Objects.requireNonNull(branches);
        this.branches = branches;
        return this;
    }

    String getId() {
        return id;
    }
}
