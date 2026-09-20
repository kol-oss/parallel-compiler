package com.github.kol.oss.compiler;

import com.github.kol.oss.compiler.exception.LexicalException;
import com.github.kol.oss.compiler.exception.SyntaxException;
import com.github.kol.oss.compiler.lexica.LexicalAnalyzer;
import com.github.kol.oss.compiler.syntax.SyntaxAnalyzer;
import com.github.kol.oss.compiler.token.Token;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ExpressionChecker {
    private static final String ANSI_GREEN = "\u001B[32m";
    private static final String ANSI_RED = "\u001B[31m";
    private static final String ANSI_RESET = "\u001B[0m";

    private final LexicalAnalyzer lexicalAnalyzer;
    private final SyntaxAnalyzer syntaxAnalyzer;

    public ExpressionChecker(LexicalAnalyzer lexicalAnalyzer, SyntaxAnalyzer syntaxAnalyzer) {
        this.lexicalAnalyzer = lexicalAnalyzer;
        this.syntaxAnalyzer = syntaxAnalyzer;
    }

    public void validateAndVisualize(String expression) {
        List<Token> tokens = new ArrayList<>();
        try {
            tokens = lexicalAnalyzer.analyze(expression);
            syntaxAnalyzer.analyze(tokens);
        } catch (LexicalException exception) {
            List<Integer> errorIndexes = new ArrayList<>();
            List<String> errorMessages = new ArrayList<>();
            for (LexicalException childException : exception.getMessages()) {
                int position = childException.getPosition();

                errorIndexes.add(position);
                errorMessages.add("> (" + position + "): " + childException.getMessage());
            }

            StringBuilder errorExpression = new StringBuilder();
            for (int i = 0; i < expression.length(); i++) {
                char symbol = expression.charAt(i);
                if (errorIndexes.contains(i))
                    errorExpression.append(ANSI_RED).append(symbol).append(ANSI_RESET);
                else
                    errorExpression.append(symbol);
            }

            System.out.println(ANSI_RED + "Lexical validation failed: " + ANSI_RESET + errorExpression);
            errorMessages.forEach(System.out::println);
            return;
        } catch (SyntaxException exception) {
            System.out.println(tokens.stream().map(token -> token == exception.getToken() ? ANSI_RED + token.value() + ANSI_RESET : token.value()).collect(Collectors.joining(" ")));
            System.out.println(ANSI_RED + "Syntax error at token " + exception.getToken().value() + ": " + exception.getMessage() + ANSI_RESET);
            return;
        }

        System.out.println(ANSI_GREEN + "expression is valid" + ANSI_RESET);
    }
}
