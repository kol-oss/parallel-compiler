package com.github.kol.oss.compiler.utils;

import com.github.kol.oss.compiler.constant.NumberConstant;
import com.github.kol.oss.compiler.constant.OperatorValue;
import com.github.kol.oss.compiler.constant.TermSign;
import com.github.kol.oss.compiler.constant.TokenType;
import com.github.kol.oss.compiler.dto.chain.Chain;
import com.github.kol.oss.compiler.dto.chain.Term;
import com.github.kol.oss.compiler.dto.node.TokenNode;
import com.github.kol.oss.compiler.dto.token.Token;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public class BalancedTreeBuilder {
    private record MergeTask(int start, int end, boolean combine) {
    }

    public static TokenNode buildSigned(Chain chain) {
        List<TokenNode> positives = new ArrayList<>();
        List<TokenNode> negatives = new ArrayList<>();

        for (Term term : chain.terms()) {
            (term.sign() == TermSign.POSITIVE ? positives : negatives).add(term.node());
        }

        boolean sum = chain.family() == OperatorValue.PLUS;
        TokenNode positiveTree = positives.isEmpty() ? anchor(sum) : build(positives, chain.family());

        if (negatives.isEmpty())
            return positiveTree;

        TokenNode negativeTree = build(negatives, chain.family());
        OperatorValue separator = sum ? OperatorValue.MINUS : OperatorValue.DIVISION;
        return NodeFactory.operation(separator, positiveTree, negativeTree);
    }

    public static TokenNode buildSum(List<TokenNode> terms) {
        return build(terms, OperatorValue.PLUS);
    }

    public static TokenNode buildProduct(List<TokenNode> terms) {
        return build(terms, OperatorValue.MULTIPLY);
    }

    private static TokenNode build(List<TokenNode> terms, OperatorValue family) {
        if (terms.size() == 1)
            return terms.getFirst();

        Deque<MergeTask> tasks = new ArrayDeque<>();
        Deque<TokenNode> merged = new ArrayDeque<>();
        tasks.push(new MergeTask(0, terms.size(), false));

        while (!tasks.isEmpty()) {
            MergeTask task = tasks.pop();

            if (task.combine()) {
                TokenNode right = merged.pop();
                TokenNode left = merged.pop();
                merged.push(NodeFactory.operation(family, left, right));
                continue;
            }

            int start = task.start();
            int end = task.end();

            if (end - start == 1) {
                merged.push(terms.get(start));
                continue;
            }

            int split = bestSplit(terms, start, end);
            tasks.push(new MergeTask(start, end, true));
            tasks.push(new MergeTask(split, end, false));
            tasks.push(new MergeTask(start, split, false));
        }

        return merged.pop();
    }

    private static int bestSplit(List<TokenNode> terms, int start, int end) {
        int total = 0;
        for (int i = start; i < end; i++)
            total += terms.get(i).getWeight();

        int best = start + 1;
        long bestDifference = Long.MAX_VALUE;
        int prefix = 0;

        for (int k = start + 1; k < end; k++) {
            prefix += terms.get(k - 1).getWeight();

            long left = prefix + (k - start - 1);
            long right = (total - prefix) + (end - k - 1);
            long difference = Math.abs(left - right);

            if (difference < bestDifference) {
                bestDifference = difference;
                best = k;
            }
        }

        return best;
    }

    private static TokenNode anchor(boolean sum) {
        String value = sum ? NumberConstant.ZERO : NumberConstant.ONE;
        return NodeFactory.value(new Token(value, TokenType.INTEGER, 0));
    }
}