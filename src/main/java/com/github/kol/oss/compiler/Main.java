package com.github.kol.oss.compiler;

import com.github.kol.oss.compiler.lexica.LexicalAnalyzer;
import com.github.kol.oss.compiler.syntax.SyntaxAnalyzer;

import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        LexicalAnalyzer lexicalAnalyzer = new LexicalAnalyzer();
        SyntaxAnalyzer syntaxAnalyzer = new SyntaxAnalyzer();

        ExpressionChecker expressionChecker = new ExpressionChecker(lexicalAnalyzer, syntaxAnalyzer);

        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.print("Enter expression or command: ");

            String expression = scanner.nextLine();
            if (expression.equalsIgnoreCase("end"))
                return;

            if (expression.equalsIgnoreCase("debug")) {
                boolean isDebug = lexicalAnalyzer.toggleDebug();
                System.out.println("debug " + (isDebug ? "enabled" : "disabled"));
                continue;
            }

            expressionChecker.validateAndVisualize(expression);
        }
    }
}
