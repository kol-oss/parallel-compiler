package com.github.kol.oss.compiler.exception;

public class InvalidContentException extends PositionedException {
    @Override
    public String getMessage() {
        return "Can not make empty parenthesis for non-functions";
    }
}
