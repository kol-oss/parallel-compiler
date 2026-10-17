package com.github.kol.oss.compiler;

import com.github.kol.oss.compiler.dto.node.FunctionNode;
import com.github.kol.oss.compiler.dto.node.OperationNode;
import com.github.kol.oss.compiler.dto.node.TokenNode;
import com.github.kol.oss.compiler.dto.token.Token;
import com.github.kol.oss.compiler.exception.ExceptionHandler;
import com.github.kol.oss.compiler.processor.CommutativeProcessor;
import com.github.kol.oss.compiler.processor.LexicalProcessor;
import com.github.kol.oss.compiler.processor.OptimizationProcessor;
import com.github.kol.oss.compiler.processor.SyntaxProcessor;
import com.github.kol.oss.compiler.processor.TreeProcessor;
import com.github.kol.oss.compiler.utils.ExpressionPrinter;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class Main {
    public static void main(String[] args) {
        // ensure box-drawing symbols render correctly regardless of the console code page
        System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out), true, StandardCharsets.UTF_8));

        ExceptionHandler exceptionHandler = new ExceptionHandler();

        LexicalProcessor lexicalProcessor = new LexicalProcessor(exceptionHandler);
        SyntaxProcessor syntaxProcessor = new SyntaxProcessor(exceptionHandler);

        TreeProcessor treeProcessor = new TreeProcessor();
        OptimizationProcessor optimizationProcessor = new OptimizationProcessor(exceptionHandler);
        CommutativeProcessor commutativeProcessor = new CommutativeProcessor();

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

            System.out.println("Step 2. Optimization: STARTED");

            // build the raw tree, then optimize it (logs every applied optimization)
            optimizationProcessor.getOptimizations().clear();
            TokenNode optimizedTree = optimizationProcessor.process(treeProcessor.buildTree(tokens));

            // surfaced optimizations (empty list is not shown)
            List<String> optimizations = optimizationProcessor.getOptimizations();
            if (!optimizations.isEmpty()) {
                System.out.println("Applied optimizations:");
                optimizations.forEach(optimization -> System.out.println("  - " + optimization));
            } else {
                System.out.println("No optimizations were applied.");
            }

            if (exceptionHandler.printAndClear(value))
                continue;

            System.out.println("Step 2. Optimization: PASSED");
            System.out.println();

            System.out.println("Step 3. Commutative transformation: STARTED");

            // commutative law: terms are sorted by weight and regrouped into balanced branches
            commutativeProcessor.getTransformations().clear();
            TokenNode commutedTree = commutativeProcessor.process(optimizedTree);

            // surfaced commutative transformations (empty list is not shown)
            List<String> transformations = commutativeProcessor.getTransformations();
            if (!transformations.isEmpty()) {
                System.out.println("Applied commutative transformations:");
                transformations.forEach(transformation -> System.out.println("  - " + transformation));
            } else {
                System.out.println("No commutative transformations were applied.");
            }

            if (exceptionHandler.printAndClear(value))
                continue;

            System.out.println("Equivalent form: " + ExpressionPrinter.print(commutedTree));
            System.out.println();
            System.out.println("Step 3. Commutative transformation: PASSED");
            System.out.println();

            System.out.println("Step 4. Parallel tree building: STARTED");

            // building of tree of operations in parallel form (min height / max width)
            TokenNode operatorTree = treeProcessor.balance(commutedTree);

            if (exceptionHandler.printAndClear(value))
                continue;

            System.out.println("Step 4. Parallel tree building: PASSED");
            System.out.println();

            // computation of the result (only when the expression is fully constant)
            Optional<Token> result = treeProcessor.evaluate(operatorTree);
            result.ifPresent(token -> System.out.println("Result: " + token.value() + "\n"));

            System.out.println("Parallel tree (by levels):");
            displayTreeByLevels(operatorTree);
            System.out.println();

            System.out.println("Parallel tree (structure):");
            displayTree(operatorTree, "", true);

            System.out.println();
        }
    }

    // prints the tree level by level, highlighting the parallel tiers
    public static void displayTreeByLevels(TokenNode root) {
        if (root == null)
            return;

        Deque<TokenNode> queue = new ArrayDeque<>();
        queue.add(root);

        int level = 0;
        while (!queue.isEmpty()) {
            int levelSize = queue.size();
            StringBuilder levelLine = new StringBuilder("Level " + level + ": ");

            for (int i = 0; i < levelSize; i++) {
                TokenNode node = queue.poll();

                if (i > 0)
                    levelLine.append(" | ");

                if (node != null && node.getToken() != null)
                    levelLine.append(node.getToken().value());

                if (node instanceof OperationNode operation) {
                    if (operation.getLeft() != null)
                        queue.add(operation.getLeft());
                    if (operation.getRight() != null)
                        queue.add(operation.getRight());
                }

                if (node instanceof FunctionNode function)
                    queue.addAll(function.getArguments());
            }

            System.out.println(levelLine);
            level++;
        }
    }

    public static void displayTree(TokenNode node, String indent, boolean isClosing) {
        if (node == null) return;

        String val = node.getToken().value();
        System.out.println(indent + (isClosing ? "└── " : "├── ") + val);

        if (node instanceof OperationNode op) {
            String newIndent = indent + (isClosing ? "    " : "│   ");

            displayTree(op.getLeft(), newIndent, false);
            displayTree(op.getRight(), newIndent, true);
        }

        if (node instanceof FunctionNode function) {
            String newIndent = indent + (isClosing ? "    " : "│   ");
            List<TokenNode> arguments = function.getArguments();

            for (int i = 0; i < arguments.size(); i++)
                displayTree(arguments.get(i), newIndent, i == arguments.size() - 1);
        }
    }
}
