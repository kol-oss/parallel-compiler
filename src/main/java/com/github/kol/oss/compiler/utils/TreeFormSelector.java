package com.github.kol.oss.compiler.utils;

import com.github.kol.oss.compiler.dto.node.TokenNode;

import java.util.List;

// keeps only forms with the smallest height and, among them, the largest width
public final class TreeFormSelector {
    private TreeFormSelector() {
    }

    public static List<TokenNode> selectBest(List<TokenNode> forms) {
        if (forms.isEmpty())
            return List.of();

        int minHeight = forms.stream()
                .mapToInt(TreeMetrics::height)
                .min()
                .orElse(0);

        List<TokenNode> shortest = forms.stream()
                .filter(form -> TreeMetrics.height(form) == minHeight)
                .toList();

        int maxWidth = shortest.stream()
                .mapToInt(TreeMetrics::width)
                .max()
                .orElse(0);

        return shortest.stream()
                .filter(form -> TreeMetrics.width(form) == maxWidth)
                .toList();
    }
}
