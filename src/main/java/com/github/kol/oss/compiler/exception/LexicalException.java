package com.github.kol.oss.compiler.exception;

public class LexicalException extends RuntimeException {
    private final int position;

    public LexicalException(int position, String message) {
        super(message);

        this.position = position;
    }

    public int getPosition() {
        return position;
    }
}
