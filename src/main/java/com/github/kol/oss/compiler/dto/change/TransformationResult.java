package com.github.kol.oss.compiler.dto.change;

import com.github.kol.oss.compiler.dto.node.TokenNode;

import java.util.List;

public record TransformationResult(TokenNode tree, List<Transformation> applied) {
    public TransformationResult {
        applied = List.copyOf(applied);
    }
}