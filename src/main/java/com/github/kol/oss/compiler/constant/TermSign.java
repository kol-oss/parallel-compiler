package com.github.kol.oss.compiler.constant;

public enum TermSign {
    POSITIVE,
    NEGATIVE;

    public TermSign opposite() {
        return this == POSITIVE ? NEGATIVE : POSITIVE;
    }

    public boolean isPositive() {
        return this == POSITIVE;
    }
}