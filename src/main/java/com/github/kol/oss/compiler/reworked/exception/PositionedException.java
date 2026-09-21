package com.github.kol.oss.compiler.reworked.exception;

public class PositionedException extends RuntimeException {
    private int index;

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }
}
