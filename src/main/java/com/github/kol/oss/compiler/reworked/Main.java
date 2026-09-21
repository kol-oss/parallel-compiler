package com.github.kol.oss.compiler.reworked;

import com.github.kol.oss.compiler.reworked.exception.ExceptionHandler;
import com.github.kol.oss.compiler.reworked.token.Token;

import java.util.List;

public class Main {
    public static void main(String[] args) {
        String expression = "0.71.11";

        ExceptionHandler exceptionHandler = new ExceptionHandler();

        Lexer lexer = new Lexer(exceptionHandler);
        List<Token> tokens = lexer.process(expression);

        Syntax syntax = new Syntax(exceptionHandler);
        syntax.process(tokens);

        exceptionHandler.printAndClear(expression);
    }
}
