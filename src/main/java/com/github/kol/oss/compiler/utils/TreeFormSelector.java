package com.github.kol.oss.compiler.utils;

import com.github.kol.oss.compiler.dto.node.TokenNode;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// selects equivalent forms by height first and width second: either keeps the optimal
// ones or returns a ranked set, where the incoming form participates like any other
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

    // keeps every unique candidate that is optimal by height first and width second;
    // the original input is passed in by the caller and stays if it is optimal as well
    public static List<TokenNode> selectOptimal(List<TokenNode> candidates) {
        if (candidates.isEmpty())
            return List.of();

        Map<String, TokenNode> unique = new LinkedHashMap<>();

        for (TokenNode candidate : candidates)
            unique.putIfAbsent(TreeWalker.structuralKey(candidate), candidate);

        return selectBest(new ArrayList<>(unique.values()));
    }

    // keeps up to limit unique candidates ranked minimal height first, then maximal width;
    // lower-ranked forms are kept as well, so all of them stay available for selection
    public static List<TokenNode> selectRanked(List<TokenNode> candidates, int limit) {
        if (candidates.isEmpty() || limit <= 0)
            return List.of();

        Map<String, TokenNode> unique = new LinkedHashMap<>();

        for (TokenNode candidate : candidates)
            unique.putIfAbsent(TreeWalker.structuralKey(candidate), candidate);

        return unique.values().stream()
                .map(form -> new RankedForm(form, TreeMetrics.height(form), TreeMetrics.width(form)))
                .sorted(Comparator.comparingInt(RankedForm::height)
                        .thenComparing(Comparator.comparingInt(RankedForm::width).reversed()))
                .limit(limit)
                .map(RankedForm::tree)
                .toList();
    }

    private record RankedForm(TokenNode tree, int height, int width) {
    }
}
