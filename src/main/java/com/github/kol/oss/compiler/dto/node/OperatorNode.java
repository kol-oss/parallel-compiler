package com.github.kol.oss.compiler.dto.node;

public class OperatorNode extends Node {
    private Node left;
    private Node right;

    public OperatorNode(String value, Node left, Node right) {
        super(value);

        this.left = left;
        this.right = right;
    }

    public Node getLeft() {
        return left;
    }

    public void setLeft(Node left) {
        this.left = left;
    }

    public Node getRight() {
        return right;
    }

    public void setRight(Node right) {
        this.right = right;
    }
}