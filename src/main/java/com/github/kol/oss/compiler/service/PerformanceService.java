package com.github.kol.oss.compiler.service;

import com.github.kol.oss.compiler.constant.OperatorValue;
import com.github.kol.oss.compiler.constant.VectorSystemConfig;
import com.github.kol.oss.compiler.dto.performance.BlockEfficiency;
import com.github.kol.oss.compiler.dto.performance.PerformanceMetrics;
import com.github.kol.oss.compiler.dto.schedule.ExecutionSchedule;

import java.util.List;

public class PerformanceService {
    private static final List<OperatorValue> OPERATORS = List.of(
            OperatorValue.PLUS,
            OperatorValue.MINUS,
            OperatorValue.MULTIPLY,
            OperatorValue.DIVISION
    );

    public PerformanceMetrics evaluate(ExecutionSchedule schedule) {
        int sequentialTime = schedule.sequentialTime();
        int parallelTime = schedule.parallelTime();

        double speedup = parallelTime == 0 ? 0.0 : (double) sequentialTime / parallelTime;
        double efficiency = schedule.blockCount() == 0 ? 0.0 : speedup / schedule.blockCount();
        List<BlockEfficiency> blockEfficiencies = OPERATORS.stream()
                .map(operator -> evaluateBlock(schedule, operator))
                .toList();

        return new PerformanceMetrics(
                sequentialTime,
                parallelTime,
                speedup,
                efficiency,
                schedule.blockCount(),
                blockEfficiencies
        );
    }

    private BlockEfficiency evaluateBlock(ExecutionSchedule schedule, OperatorValue operator) {
        int busyTime = schedule.entries().stream()
                .filter(entry -> OperatorValue.fromSymbol(entry.operation().getToken().value()) == operator)
                .mapToInt(entry -> entry.finish() - entry.start())
                .sum();

        int blockCount = VectorSystemConfig.blocks(operator);
        int totalTime = schedule.parallelTime() * blockCount;
        double efficiency = totalTime == 0 ? 0.0 : (double) busyTime / totalTime;

        return new BlockEfficiency(operator, busyTime, schedule.parallelTime(), blockCount, efficiency);
    }
}
