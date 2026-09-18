package com.github.kol.oss.compiler;

import com.github.kol.oss.compiler.lexer.LexicalAnalyzer;
import com.github.kol.oss.compiler.syntax.SyntaxAnalyzer;

import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        LexicalAnalyzer lexicalAnalyzer = new LexicalAnalyzer();
        SyntaxAnalyzer syntaxAnalyzer = new SyntaxAnalyzer();

        ExpressionChecker expressionChecker = new ExpressionChecker(lexicalAnalyzer, syntaxAnalyzer);

        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.print("Enter expression: ");

            String expression = scanner.nextLine();
            if (expression.equalsIgnoreCase("end"))
                return;

            expressionChecker.validateAndVisualize(expression);
        }
    }
}
