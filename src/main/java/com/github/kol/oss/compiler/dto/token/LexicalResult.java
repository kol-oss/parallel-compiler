package com.github.kol.oss.compiler.dto.token;

import com.github.kol.oss.compiler.exception.PositionedException;

import java.util.List;

public record LexicalResult(List<Token> tokens, List<PositionedException> diagnostics) {
    public LexicalResult {
        tokens = List.copyOf(tokens);
        diagnostics = List.copyOf(diagnostics);
    }
}
