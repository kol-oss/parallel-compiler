package com.github.kol.oss.compiler.pipeline;

import com.github.kol.oss.compiler.dto.node.TokenNode;

public interface TransformationService {
    String OPTIMIZATIONS = "optimizations";
    String COMMUTATIVE = "commutative transformations";
    String ASSOCIATIVE = "associative transformations";
    String PARALLEL = "parallel transformations";
    String NORMALIZATION = "normalization transformations";

    String name();

    TransformationResult apply(TokenNode tree);
}
