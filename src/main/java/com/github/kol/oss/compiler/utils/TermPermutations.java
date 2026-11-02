package com.github.kol.oss.compiler.utils;

import com.github.kol.oss.compiler.dto.chain.Term;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TermPermutations {
    public static List<List<Term>> generate(List<Term> terms, int limit) {
        List<List<Term>> result = new ArrayList<>();

        if (terms.size() <= 1) {
            result.add(List.copyOf(terms));
            return result;
        }

        if (limit <= 0)
            return result;

        Map<String, Integer> indexes = new LinkedHashMap<>();
        List<Term> representatives = new ArrayList<>();
        int[] counts = new int[terms.size()];
        int groups = 0;

        for (Term term : terms) {
            String key = termKey(term);
            Integer index = indexes.get(key);

            if (index == null) {
                index = groups++;
                indexes.put(key, index);
                representatives.add(term);
            }

            counts[index]++;
        }

        int size = terms.size();
        int[] chosen = new int[size];
        int[] next = new int[size];
        List<Term> current = new ArrayList<>(size);
        int depth = 0;

        while (depth >= 0 && result.size() < limit) {
            if (depth == size) {
                result.add(List.copyOf(current));

                if (result.size() >= limit)
                    break;

                depth--;
                int group = chosen[depth];
                counts[group]++;
                current.removeLast();
                next[depth] = group + 1;
                continue;
            }

            int group = next[depth];

            while (group < groups && counts[group] == 0)
                group++;

            if (group == groups) {
                depth--;

                if (depth < 0)
                    break;

                int parent = chosen[depth];
                counts[parent]++;
                current.removeLast();
                next[depth] = parent + 1;
                continue;
            }

            chosen[depth] = group;
            counts[group]--;
            current.add(representatives.get(group));
            depth++;

            if (depth < size)
                next[depth] = 0;
        }

        return result;
    }

    private static String termKey(Term term) {
        return term.sign() + ":" + TreeWalker.structuralKey(term.node());
    }
}
