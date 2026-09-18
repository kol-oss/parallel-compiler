package com.github.kol.oss.compiler;

import com.github.kol.oss.compiler.exception.LexicalException;
import com.github.kol.oss.compiler.exception.SyntaxException;
import com.github.kol.oss.compiler.lexer.LexicalAnalyzer;
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
            int position = exception.getPosition();

            System.out.println(expression.substring(0, position) + ANSI_RED + expression.charAt(position) + ANSI_RESET + expression.substring(position + 1));
            System.out.println(ANSI_RED + "Lexical error at position " + exception.getPosition() + ": " + exception.getMessage() + ANSI_RESET);
            return;
        } catch (SyntaxException exception) {
            System.out.println(tokens.stream().map(token -> token == exception.getToken() ? ANSI_RED + token.value() + ANSI_RESET : token.value()).collect(Collectors.joining(" ")));
            System.out.println(ANSI_RED + "Syntax error at token " + exception.getToken().value() + ": " + exception.getMessage() + ANSI_RESET);
            return;
        }

        System.out.println(ANSI_GREEN + "expression is valid" + ANSI_RESET);
    }
}
