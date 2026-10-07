package com.github.kol.oss.compiler.parser;

public class OperandNode implements TreeNode {
    private String value;
    private int level = 0;

    public OperandNode(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Override
    public int getLevel() {
        return level;
    }

    @Override
    public void setLevel(int level) {
        this.level = level;
    }
}