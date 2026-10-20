package com.github.kol.oss.compiler.pipeline;

import com.github.kol.oss.compiler.dto.change.Transformation;
import com.github.kol.oss.compiler.dto.node.TokenNode;
import com.github.kol.oss.compiler.exception.PositionedException;

import java.util.List;

public record PassResult(String name, TokenNode tree, List<Transformation> applied, List<PositionedException> diagnostics) {
    public PassResult {
        applied = List.copyOf(applied);
        diagnostics = List.copyOf(diagnostics);
    }
}
