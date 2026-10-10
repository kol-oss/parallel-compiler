package com.github.kol.oss.compiler;

import com.github.kol.oss.compiler.dto.node.OperationNode;
import com.github.kol.oss.compiler.dto.node.TokenNode;
import com.github.kol.oss.compiler.dto.token.Token;
import com.github.kol.oss.compiler.exception.ExceptionHandler;
import com.github.kol.oss.compiler.processor.LexicalProcessor;
import com.github.kol.oss.compiler.processor.OptimizationProcessor;
import com.github.kol.oss.compiler.processor.SyntaxProcessor;
import com.github.kol.oss.compiler.processor.TreeProcessor;

import java.util.List;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        ExceptionHandler exceptionHandler = new ExceptionHandler();

        LexicalProcessor lexicalProcessor = new LexicalProcessor(exceptionHandler);
        SyntaxProcessor syntaxProcessor = new SyntaxProcessor(exceptionHandler);

        TreeProcessor treeProcessor = new TreeProcessor();
        OptimizationProcessor optimizationProcessor = new OptimizationProcessor(exceptionHandler);

        Scanner scanner = new Scanner(System.in);
        boolean isTokenOutput = false;

        while (true) {
            System.out.print("Enter: ");
            String value = scanner.nextLine();

            // special commands (end to leave, token to display tokens parsing)
            if (value.equalsIgnoreCase("end"))
                break;
            else if (value.equalsIgnoreCase("token")) {
                isTokenOutput = !isTokenOutput;
                continue;
            } else if (value.isEmpty())
                continue;

            System.out.println("Step 1. Lexical and Syntax validation: STARTED");

            // lexical processing
            List<Token> tokens = lexicalProcessor.process(value);
            if (isTokenOutput)
                System.out.println(tokens);

            // syntax processing
            syntaxProcessor.process(tokens);
            if (exceptionHandler.printAndClear(value))
                continue;

            System.out.println("Step 1. Lexical and Syntax validation: PASSED");
            System.out.println();

            System.out.println("Step 2. Operators validation and Tree building: STARTED");

            // building of tree of operations
            TokenNode operatorTree = optimizationProcessor.process(treeProcessor.buildTree(tokens));

            if (exceptionHandler.printAndClear(value))
                continue;

            System.out.println("Step 2. Operators validation and Tree building: PASSED");
            displayTree(operatorTree, "", true);

            System.out.println();
        }
    }

    public static void displayTree(TokenNode node, String indent, boolean isRight) {
        if (node == null) return;

        String val = node.getToken().value();
        System.out.println(indent + (isRight ? "└── " : "├── ") + val);

        if (node instanceof OperationNode op) {
            displayTree(op.getLeft(), indent + (isRight ? "    " : "│   "), false);
            displayTree(op.getRight(), indent + (isRight ? "    " : "│   "), true);
        }
    }
}
