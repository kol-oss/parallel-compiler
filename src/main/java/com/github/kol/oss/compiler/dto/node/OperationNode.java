package com.github.kol.oss.compiler.dto.node;

import com.github.kol.oss.compiler.dto.token.Token;

public class OperationNode extends TokenNode {
    private final TokenNode left;
    private final TokenNode right;

    public OperationNode(Token token, TokenNode left, TokenNode right) {
        super(token, 1 + left.getWeight() + right.getWeight());

        this.left = left;
        this.right = right;
    }

    public TokenNode getLeft() {
        return left;
    }

    public TokenNode getRight() {
        return right;
    }
}