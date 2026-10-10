package com.github.kol.oss.compiler.exception;

public class ZeroDivisionException extends PositionedException {
    @Override
    public String getMessage() {
        return "This arithmetic operation is not allowed (division by zero)";
    }
}
