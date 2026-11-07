package com.github.kol.oss.compiler.dto.performance;

import java.util.List;

public record PerformanceMetrics(
        int sequentialTime,
        int parallelTime,
        double speedup,
        double efficiency,
        int blockCount,
        List<BlockEfficiency> blockEfficiencies
) {
    public PerformanceMetrics {
        blockEfficiencies = List.copyOf(blockEfficiencies);
    }
}
