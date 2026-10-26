package com.github.kol.oss.compiler.pipeline;

import com.github.kol.oss.compiler.dto.change.Transformation;
import com.github.kol.oss.compiler.dto.node.TokenNode;
import com.github.kol.oss.compiler.exception.PositionedException;

import java.util.List;

public record TransformationResult(
        String name,
        TokenNode tree,
        List<TokenNode> forms,
        List<Transformation> applied,
        List<PositionedException> diagnostics
) {
    public TransformationResult {
        forms = List.copyOf(forms);
        applied = List.copyOf(applied);
        diagnostics = List.copyOf(diagnostics);
    }
}
