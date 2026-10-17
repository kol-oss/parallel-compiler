package com.github.kol.oss.compiler.view;

import com.github.kol.oss.compiler.dto.change.Transformation;
import com.github.kol.oss.compiler.dto.node.TokenNode;
import com.github.kol.oss.compiler.dto.token.Token;
import com.github.kol.oss.compiler.exception.PositionedException;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Scanner;

public class ConsoleView {
    private final Scanner scanner = new Scanner(System.in);
    private final TreeViewRenderer treeRenderer = new TreeViewRenderer();

    public ConsoleView() {
        System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out), true, StandardCharsets.UTF_8));
    }

    public String readLine() {
        System.out.print("Enter: ");
        return scanner.nextLine();
    }

    public void printStepStarted(String step) {
        System.out.println(step + ": STARTED");
    }

    public void printStepPassed(String step) {
        System.out.println(step + ": PASSED");
        System.out.println();
    }

    public void printTokens(List<Token> tokens) {
        System.out.println(tokens);
    }

    public void printOptimizations(List<Transformation> transformations) {
        if (transformations.isEmpty()) {
            System.out.println("No optimizations were applied.");
            return;
        }

        System.out.println("Applied optimizations:");
        printTransformations(transformations);
    }

    public void printCommutativeTransformations(List<Transformation> transformations) {
        if (transformations.isEmpty()) {
            System.out.println("No commutative transformations were applied.");
            return;
        }

        System.out.println("Applied commutative transformations:");
        printTransformations(transformations);
    }

    public void printEquivalentForm(TokenNode tree) {
        System.out.println("Equivalent form: " + ExpressionFormatter.format(tree));
        System.out.println();
    }

    public void printResult(Token token) {
        System.out.println("Result: " + token.value() + "\n");
    }

    public void printTree(TokenNode tree) {
        System.out.println("Parallel tree (by levels):");
        System.out.println(treeRenderer.renderByLevels(tree));
        System.out.println();

        System.out.println("Parallel tree (structure):");
        System.out.print(treeRenderer.renderStructure(tree));
        System.out.println();
    }

    public void printErrors(String expression, List<PositionedException> exceptions) {
        System.out.printf("%-12s%s%n", "Expression:", expression);

        StringBuilder pointers = new StringBuilder();
        pointers.repeat(" ", expression.length());

        StringBuilder messages = new StringBuilder();
        for (PositionedException exception : exceptions) {
            int index = exception.getIndex();
            pointers.setCharAt(index, '^');

            messages
                    .append("> (")
                    .append(index)
                    .append("): ")
                    .append(exception.getMessage())
                    .append("\n");
        }

        System.out.printf("%-12s%s%n", "Errors:", pointers);
        System.out.println(messages);
    }

    private void printTransformations(List<Transformation> transformations) {
        transformations.forEach(transformation -> System.out.println("  - "
                + ExpressionFormatter.format(transformation.before())
                + " => "
                + ExpressionFormatter.format(transformation.after())));
    }
}