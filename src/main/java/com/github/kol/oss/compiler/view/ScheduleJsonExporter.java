package com.github.kol.oss.compiler.view;

import com.github.kol.oss.compiler.constant.OperatorValue;
import com.github.kol.oss.compiler.constant.VectorSystemConfig;
import com.github.kol.oss.compiler.dto.performance.PerformanceMetrics;
import com.github.kol.oss.compiler.dto.schedule.ExecutionSchedule;
import com.github.kol.oss.compiler.dto.schedule.ScheduleEntry;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ScheduleJsonExporter {
    private static final Path OUTPUT = Path.of("src", "main", "resources", "schedule.json");

    public void export(
            String expression,
            String processedExpression,
            ExecutionSchedule schedule,
            PerformanceMetrics metrics
    ) {
        try {
            Files.createDirectories(OUTPUT.getParent());
            Files.writeString(
                    OUTPUT,
                    toJson(expression, processedExpression, schedule, metrics),
                    StandardCharsets.UTF_8
            );
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    private String toJson(
            String expression,
            String processedExpression,
            ExecutionSchedule schedule,
            PerformanceMetrics metrics
    ) {
        return "{\n" +
                "  \"expression\": \"" + escape(expression) + "\",\n" +
                "  \"processedExpression\": \"" + escape(processedExpression) + "\",\n" +
                "  \"durations\": [\n" + durations() + "\n  ],\n" +
                "  \"sequentialTime\": " + metrics.sequentialTime() + ",\n" +
                "  \"parallelTime\": " + metrics.parallelTime() + ",\n" +
                "  \"speedup\": " + metrics.speedup() + ",\n" +
                "  \"efficiency\": " + metrics.efficiency() + ",\n" +
                "  \"blockCount\": " + metrics.blockCount() + ",\n" +
                "  \"blocks\": [\n" + blocks() + "\n  ],\n" +
                "  \"entries\": [\n" + entries(schedule) + "\n  ]\n" +
                "}\n";
    }

    private String durations() {
        List<String> durations = new ArrayList<>();

        for (OperatorValue operator : VectorSystemConfig.OPERATORS)
            durations.add("    { \"operator\": \"" + operator.toCode()
                    + "\", \"symbol\": \"" + escape(operator.toSymbol())
                    + "\", \"duration\": " + VectorSystemConfig.duration(operator) + " }");

        return String.join(",\n", durations);
    }

    private String blocks() {
        List<String> blocks = new ArrayList<>();

        for (OperatorValue operator : VectorSystemConfig.OPERATORS)
            for (int block = 1; block <= VectorSystemConfig.blocks(operator); block++)
                blocks.add("    { \"id\": \"" + blockLabel(operator, block)
                        + "\", \"operator\": \"" + operator.toCode() + "\" }");

        return String.join(",\n", blocks);
    }

    private String entries(ExecutionSchedule schedule) {
        List<String> entries = new ArrayList<>();

        for (ScheduleEntry entry : schedule.entries())
            entries.add("    { \"operation\": \"" + escape(ExpressionFormatter.format(entry.operation()))
                    + "\", \"block\": \"" + blockLabel(entry)
                    + "\", \"start\": " + entry.start()
                    + ", \"finish\": " + entry.finish()
                    + ", \"duration\": " + (entry.finish() - entry.start()) + " }");

        return String.join(",\n", entries);
    }

    private String blockLabel(ScheduleEntry entry) {
        return blockLabel(
                OperatorValue.fromSymbol(entry.operation().getToken().value()),
                entry.block() + 1
        );
    }

    private String blockLabel(OperatorValue operator, int block) {
        return operator.toCode() + "-" + block;
    }

    private String escape(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
