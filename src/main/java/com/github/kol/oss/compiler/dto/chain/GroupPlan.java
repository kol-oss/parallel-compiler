package com.github.kol.oss.compiler.dto.chain;

import com.github.kol.oss.compiler.constant.TermSign;

import java.util.List;

public record GroupPlan(
        List<Term> terms,
        Grouping grouping,
        TermSign groupSign,
        List<Term> inner
) {
    public GroupPlan {
        terms = List.copyOf(terms);
        inner = List.copyOf(inner);
    }
}
