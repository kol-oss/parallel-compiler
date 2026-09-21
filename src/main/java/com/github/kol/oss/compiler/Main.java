package com.github.kol.oss.compiler;

import com.github.kol.oss.compiler.exception.ExceptionHandler;
import com.github.kol.oss.compiler.processor.LexicalProcessor;
import com.github.kol.oss.compiler.processor.SyntaxProcessor;
import com.github.kol.oss.compiler.dto.Token;

import java.util.List;

public class Main {
    public static void main(String[] args) {
        String expression = "7.111.01";

        ExceptionHandler exceptionHandler = new ExceptionHandler();

        LexicalProcessor lexicalProcessor = new LexicalProcessor(exceptionHandler);
        List<Token> tokens = lexicalProcessor.process(expression);

        SyntaxProcessor syntaxProcessor = new SyntaxProcessor(exceptionHandler);
        syntaxProcessor.process(tokens);

        exceptionHandler.printAndClear(expression);
    }
}
