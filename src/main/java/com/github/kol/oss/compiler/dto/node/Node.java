package com.github.kol.oss.compiler.dto.node;

public abstract class Node {
    private final String value;
    private int level;

    public Node(String value) {
        this.value = value;
    }

    public Node(String value, int level) {
        this.value = value;
        this.level = level;
    }

    public String getValue() {
        return value;
    }

    public int getLevel() {
        return this.level;
    }

    public void setLevel(int level) {
        this.level = level;
    }
}