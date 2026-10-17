package com.github.kol.oss.compiler.dto.chain;

import com.github.kol.oss.compiler.constant.OperatorValue;

import java.util.List;

public record Chain(OperatorValue family, List<Term> terms) {
    public Chain {
        terms = List.copyOf(terms);
    }
}