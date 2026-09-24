package com.github.kol.oss.compiler;

import com.github.kol.oss.compiler.dto.Token;
import com.github.kol.oss.compiler.exception.ExceptionHandler;
import com.github.kol.oss.compiler.processor.LexicalProcessor;
import com.github.kol.oss.compiler.processor.SyntaxProcessor;

import java.util.List;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        ExceptionHandler exceptionHandler = new ExceptionHandler();
        LexicalProcessor lexicalProcessor = new LexicalProcessor(exceptionHandler);
        SyntaxProcessor syntaxProcessor = new SyntaxProcessor(exceptionHandler);

        Scanner scanner = new Scanner(System.in);
        boolean isTokenOutput = false;

        while (true) {
            System.out.print("Enter: ");
            String value = scanner.nextLine();
            if (value.equalsIgnoreCase("end"))
                break;
            else if (value.equalsIgnoreCase("token")) {
                isTokenOutput = !isTokenOutput;
                continue;
            } else if (value.isEmpty())
                continue;

            List<Token> tokens = lexicalProcessor.process(value);
            if (isTokenOutput)
                System.out.println(tokens);

            syntaxProcessor.process(tokens);

            exceptionHandler.printAndClear(value);
            System.out.println();
        }
    }
}
