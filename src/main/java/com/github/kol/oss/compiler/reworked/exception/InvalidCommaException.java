package com.github.kol.oss.compiler.reworked.exception;

public class InvalidCommaException extends PositionedException {
    @Override
    public String getMessage() {
        return "Comma can not be placed outside the function parenthesis";
    }
}
