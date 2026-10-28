package com.github.kol.oss.compiler.dto.chain;

import java.util.List;

public record Grouping(List<Integer> indexes, Factor factor) {
    public Grouping {
        indexes = List.copyOf(indexes);
    }
}
