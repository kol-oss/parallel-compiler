package com.github.kol.oss.compiler.reworked;

import com.github.kol.oss.compiler.reworked.exception.ExceptionHandler;

public class Main {
    public static void main(String[] args) {
        String expression = "a + ";

        ExceptionHandler exceptionHandler = new ExceptionHandler();
        Lexer lexer = new Lexer(exceptionHandler);

        lexer.process(expression);
        exceptionHandler.visualize(expression);
    }
}
