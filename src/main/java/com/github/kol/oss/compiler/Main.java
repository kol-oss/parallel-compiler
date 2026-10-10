package com.github.kol.oss.compiler;

import com.github.kol.oss.compiler.dto.node.TokenNode;
import com.github.kol.oss.compiler.dto.node.OperationNode;
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

            boolean isValid = exceptionHandler.isPresent();
            exceptionHandler.printAndClear(value);
            System.out.println();

            if (isValid) {
                continue;
            }

            TokenNode rawTree = treeProcessor.buildTree(tokens);
            OptimizationProcessor optimizer = new OptimizationProcessor(exceptionHandler);
            TokenNode optimizedAst = optimizer.process(rawTree);

            isValid = exceptionHandler.isPresent();
            exceptionHandler.printAndClear(value);
            System.out.println();

            if (isValid) {
                continue;
            }

            System.out.println("Дерево паралельної форми (ЯПФ):");
            printTree(optimizedAst, "", true);
        }
    }

    public static void printTree(TokenNode node, String indent, boolean isRight) {
        if (node == null) return;

        String val = node.getToken().value();
        System.out.println(indent + (isRight ? "└── " : "├── ") + val);

        if (node instanceof OperationNode op) {
            printTree(op.getLeft(), indent + (isRight ? "    " : "│   "), false);
            printTree(op.getRight(), indent + (isRight ? "    " : "│   "), true);
        }
    }
}
