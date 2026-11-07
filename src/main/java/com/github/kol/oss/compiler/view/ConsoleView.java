package com.github.kol.oss.compiler.view;

import com.github.kol.oss.compiler.constant.OperatorValue;
import com.github.kol.oss.compiler.constant.VectorSystemConfig;
import com.github.kol.oss.compiler.dto.change.Transformation;
import com.github.kol.oss.compiler.dto.node.TokenNode;
import com.github.kol.oss.compiler.dto.performance.BlockEfficiency;
import com.github.kol.oss.compiler.dto.performance.PerformanceMetrics;
import com.github.kol.oss.compiler.dto.schedule.ExecutionSchedule;
import com.github.kol.oss.compiler.dto.schedule.ScheduleEntry;
import com.github.kol.oss.compiler.dto.token.Token;
import com.github.kol.oss.compiler.exception.PositionedException;
import com.github.kol.oss.compiler.utils.TreeMetrics;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;
import java.util.stream.Collectors;

public class ConsoleView {
    private static final int MAX_DISPLAYED_FORMS = 100;

    private final Scanner scanner = new Scanner(System.in);
    private final TreeViewRenderer treeRenderer = new TreeViewRenderer();

    public ConsoleView() {
        System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out), true, StandardCharsets.UTF_8));
    }

    public void printInputLine() {
        System.out.print("Enter: ");
    }

    public String readLine() {
        return scanner.nextLine();
    }

    public boolean hasNextLine() {
        return scanner.hasNextLine();
    }

    public void printFailure(String expression, RuntimeException exception) {
        System.out.printf("%-12s%s%n", "Expression:", expression);
        System.out.printf("%-12s%s%n", "Failure:", exception.getMessage());
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

    public void printAssociativeTransformations(List<Transformation> transformations) {
        if (transformations.isEmpty()) {
            System.out.println("No associative transformations were applied.");
            return;
        }

        System.out.println("Applied associative transformations:");
        printTransformations(transformations);
    }

    public void printEquivalentForms(List<TokenNode> forms) {
        int displayed = Math.min(forms.size(), MAX_DISPLAYED_FORMS);
        System.out.println("Equivalent forms (" + forms.size() + "):");

        for (int i = 0; i < displayed; i++) {
            TokenNode form = forms.get(i);
            System.out.printf("  %d. %s [height=%d, width=%d]%n",
                    i + 1,
                    ExpressionFormatter.format(form),
                    TreeMetrics.height(form),
                    TreeMetrics.width(form));
        }

        if (forms.size() > displayed)
            System.out.println("  ... " + (forms.size() - displayed) + " more forms omitted");

        System.out.println();
    }

    public void printEquivalentForm(TokenNode tree) {
        System.out.println("Equivalent form: " + ExpressionFormatter.format(tree));
        System.out.println();
    }

    public void printResult(Token token) {
        System.out.println("Result: " + token.value() + "\n");
    }

    public void printTree(TokenNode tree) {
        System.out.println("Final tree (structure):");
        System.out.print(treeRenderer.renderStructure(tree));
        System.out.println();
    }

    public void printVectorSystemConfiguration() {
        System.out.printf("Vector system: %d adders, %d subtractors, %d multipliers, %d dividers%n",
                VectorSystemConfig.ADDERS,
                VectorSystemConfig.SUBTRACTORS,
                VectorSystemConfig.MULTIPLIERS,
                VectorSystemConfig.DIVIDERS);
        System.out.printf("Operation durations: %s = %d, %s = %d, %s = %d, %s = %d%n",
                OperatorValue.PLUS.toSymbol(),
                VectorSystemConfig.ADDITION_DURATION,
                OperatorValue.MINUS.toSymbol(),
                VectorSystemConfig.SUBTRACTION_DURATION,
                OperatorValue.MULTIPLY.toSymbol(),
                VectorSystemConfig.MULTIPLICATION_DURATION,
                OperatorValue.DIVISION.toSymbol(),
                VectorSystemConfig.DIVISION_DURATION);
        System.out.println();
    }

    public void printExecutionTimeline(ExecutionSchedule schedule) {
        if (schedule.entries().isEmpty()) {
            System.out.println("No operations to execute.");
            System.out.println();
            return;
        }

        for (int tick = 0; tick < schedule.parallelTime(); tick++) {
            int currentTick = tick;
            List<ScheduleEntry> active = schedule.entries().stream()
                    .filter(entry -> entry.start() <= currentTick && currentTick < entry.finish())
                    .sorted(Comparator.comparingInt(ScheduleEntry::start)
                            .thenComparingInt(ScheduleEntry::block))
                    .toList();

            if (active.isEmpty())
                continue;

            String operations = active.stream()
                    .map(entry -> ExpressionFormatter.format(entry.operation()) + " [" + blockLabel(entry) + "]")
                    .collect(Collectors.joining(", "));

            System.out.println((tick + 1) + ": " + operations);
        }

        System.out.println();
    }

    public void printPerformanceMetrics(PerformanceMetrics metrics) {
        System.out.printf(Locale.US, "%-20s%d%n", "Sequential time:", metrics.sequentialTime());
        System.out.printf(Locale.US, "%-20s%d%n", "Parallel time:", metrics.parallelTime());
        System.out.printf(Locale.US, "%-20s%d / %d = %.2f%n",
                "Speedup:", metrics.sequentialTime(), metrics.parallelTime(), metrics.speedup());
        System.out.printf(Locale.US, "%-20s%.2f / %d = %.2f (%.2f%%)%n",
                "Efficiency:", metrics.speedup(), metrics.blockCount(), metrics.efficiency(), metrics.efficiency() * 100);

        if (metrics.parallelTime() > 0) {
            System.out.println("Efficiency by block:");

            for (BlockEfficiency block : metrics.blockEfficiencies()) {
                System.out.printf(Locale.US, "[%s] (%d / %d) / %d = %.2f (%.2f%%)%n",
                        block.operator().toCode(),
                        block.busyTime(),
                        block.parallelTime(),
                        block.blockCount(),
                        block.efficiency(),
                        block.efficiency() * 100);
            }
        }

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

    private String blockLabel(ScheduleEntry entry) {
        return blockLabel(entry.block(), OperatorValue.fromSymbol(entry.operation().getToken().value()));
    }

    private String blockLabel(int block, OperatorValue operator) {
        return operator.toCode() + "-" + (block + 1);
    }
}
