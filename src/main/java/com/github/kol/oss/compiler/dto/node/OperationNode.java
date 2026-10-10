package com.github.kol.oss.compiler.dto.node;

import com.github.kol.oss.compiler.dto.token.Token;

public class OperationNode extends TokenNode {
    private TokenNode left;
    private TokenNode right;

    public OperationNode(Token token, TokenNode left, TokenNode right) {
        super(token);

        this.left = left;
        this.right = right;
    }

    public TokenNode getLeft() {
        return left;
    }

    public void setLeft(TokenNode left) {
        this.left = left;
    }

    public TokenNode getRight() {
        return right;
    }

    public void setRight(TokenNode right) {
        this.right = right;
    }
}