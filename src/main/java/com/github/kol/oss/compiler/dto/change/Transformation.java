package com.github.kol.oss.compiler.dto.change;

import com.github.kol.oss.compiler.dto.node.TokenNode;

public record Transformation(TokenNode before, TokenNode after) {
}