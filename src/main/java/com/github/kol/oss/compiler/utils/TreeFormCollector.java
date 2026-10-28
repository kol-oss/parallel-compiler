package com.github.kol.oss.compiler.utils;

import com.github.kol.oss.compiler.dto.node.TokenNode;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TreeFormCollector {
    private final Map<String, TokenNode> forms = new LinkedHashMap<>();

    public void add(TokenNode form) {
        forms.putIfAbsent(TreeWalker.structuralKey(form), form);
    }

    public void addAll(Iterable<TokenNode> candidates) {
        for (TokenNode candidate : candidates)
            add(candidate);
    }

    public int size() {
        return forms.size();
    }

    public List<TokenNode> forms() {
        return List.copyOf(forms.values());
    }
}
