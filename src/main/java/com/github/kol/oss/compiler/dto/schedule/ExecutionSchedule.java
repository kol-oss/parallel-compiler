package com.github.kol.oss.compiler.dto.schedule;

import java.util.List;

public record ExecutionSchedule(
        List<ScheduleEntry> entries,
        int sequentialTime,
        int parallelTime,
        int blockCount
) {
    public ExecutionSchedule {
        entries = List.copyOf(entries);
    }
}
