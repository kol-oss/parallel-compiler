package com.github.kol.oss.compiler.dto.schedule;

import com.github.kol.oss.compiler.dto.node.TokenNode;

public record ScheduleEntry(
        TokenNode operation,
        int block,
        int start,
        int finish
) {
}
