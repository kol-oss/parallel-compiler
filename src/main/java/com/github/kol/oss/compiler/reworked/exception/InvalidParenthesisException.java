package com.github.kol.oss.compiler.reworked.exception;

public class InvalidParenthesisException extends PositionedException {
    private final int count;

    public InvalidParenthesisException(int count) {
        this.count = count;
    }

    @Override
    public String getMessage() {
        return count > 0 ? "More" : "Less" + " closing parenthesis required (" + count + ")";
    }
}
