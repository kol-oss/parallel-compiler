package com.github.kol.oss.compiler.utils;

import com.github.kol.oss.compiler.dto.node.TokenNode;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// selects equivalent forms that are optimal by height first and width second
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

    // keeps every unique candidate that is optimal by height first and width second,
    // then drops the original input itself: the result contains only new equivalent forms
    public static List<TokenNode> selectOptimal(List<TokenNode> candidates, TokenNode original) {
        if (candidates.isEmpty())
            return List.of();

        Map<String, TokenNode> unique = new LinkedHashMap<>();

        for (TokenNode candidate : candidates)
            unique.putIfAbsent(TreeWalker.structuralKey(candidate), candidate);

        String originalKey = original == null ? null : TreeWalker.structuralKey(original);

        return selectBest(new ArrayList<>(unique.values())).stream()
                .filter(form -> originalKey == null || !TreeWalker.structuralKey(form).equals(originalKey))
                .toList();
    }
}
