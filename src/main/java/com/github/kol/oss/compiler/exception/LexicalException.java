package com.github.kol.oss.compiler.exception;

import java.util.List;

public class LexicalException extends RuntimeException {
    private int position = 0;
    private List<LexicalException> messages;

    public LexicalException(List<LexicalException> messages) {
        this.messages = messages;
    }

    public LexicalException(int position, String message) {
        super(message);

        this.position = position;
    }

    public int getPosition() {
        return position;
    }

    public List<LexicalException> getMessages() {
        return messages;
    }
}
