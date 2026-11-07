package com.github.kol.oss.compiler.dto.performance;

import com.github.kol.oss.compiler.constant.OperatorValue;

public record BlockEfficiency(
        OperatorValue operator,
        int busyTime,
        int parallelTime,
        int blockCount,
        double efficiency
) {
}
