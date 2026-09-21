package com.github.kol.oss.compiler;

import com.github.kol.oss.compiler.exception.ExceptionHandler;
import com.github.kol.oss.compiler.token.Token;

import java.util.List;

public class Main {
    public static void main(String[] args) {
        String expression = "7.111.01";

        ExceptionHandler exceptionHandler = new ExceptionHandler();

        Lexer lexer = new Lexer(exceptionHandler);
        List<Token> tokens = lexer.process(expression);

        Syntax syntax = new Syntax(exceptionHandler);
        syntax.process(tokens);

        exceptionHandler.printAndClear(expression);
    }
}
