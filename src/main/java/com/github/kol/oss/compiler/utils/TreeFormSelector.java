package com.github.kol.oss.compiler.utils;

import com.github.kol.oss.compiler.dto.node.TokenNode;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

public class TreeFormSelector {
    public static List<TokenNode> selectOptimal(List<TokenNode> candidates) {
        if (candidates.isEmpty())
            return List.of();

        List<MeasuredForm> measured = measure(unique(candidates));
        int minHeight = measured.stream()
                .mapToInt(MeasuredForm::height)
                .min()
                .orElse(0);
        int maxWidth = measured.stream()
                .filter(form -> form.height() == minHeight)
                .mapToInt(MeasuredForm::width)
                .max()
                .orElse(0);

        return measured.stream()
                .filter(form -> form.height() == minHeight && form.width() == maxWidth)
                .map(MeasuredForm::tree)
                .toList();
    }

    public static List<TokenNode> selectRanked(List<TokenNode> candidates, int limit, Set<String> mandatoryKeys) {
        if (candidates.isEmpty() || limit <= 0)
            return List.of();

        List<MeasuredForm> sorted = measure(unique(candidates)).stream()
                .sorted(Comparator.comparingInt(MeasuredForm::height)
                        .thenComparing(Comparator.comparingInt(MeasuredForm::width).reversed()))
                .toList();

        int mandatoryCount = (int) sorted.stream()
                .filter(form -> mandatoryKeys.contains(form.key()))
                .count();
        int optionalBudget = Math.max(0, limit - mandatoryCount);
        int optionalTaken = 0;
        List<TokenNode> selected = new ArrayList<>();

        for (MeasuredForm form : sorted) {
            if (mandatoryKeys.contains(form.key()))
                selected.add(form.tree());
            else if (optionalTaken < optionalBudget) {
                selected.add(form.tree());
                optionalTaken++;
            }

            if (selected.size() >= limit)
                break;
        }

        return List.copyOf(selected);
    }

    private static List<TokenNode> unique(List<TokenNode> candidates) {
        TreeFormCollector collector = new TreeFormCollector();
        collector.addAll(candidates);
        return collector.forms();
    }

    private static List<MeasuredForm> measure(List<TokenNode> forms) {
        return forms.stream()
                .map(form -> new MeasuredForm(
                        form,
                        TreeWalker.structuralKey(form),
                        TreeMetrics.height(form),
                        TreeMetrics.width(form)
                ))
                .toList();
    }

    private record MeasuredForm(TokenNode tree, String key, int height, int width) {
    }
}
