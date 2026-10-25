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

    private record SignedTask(int start, int end, TermSign multiplier, boolean combine, OperatorValue operator) {
    }

    public static TokenNode buildBalanced(Chain chain) {
        TokenNode current = buildSigned(chain);
        TokenNode balanced = buildBalancedSigned(chain);

        if (balanced == null)
            return current;

        int currentHeight = TreeMetrics.height(current);
        int balancedHeight = TreeMetrics.height(balanced);

        if (balancedHeight != currentHeight)
            return balancedHeight < currentHeight ? balanced : current;

        return TreeMetrics.width(balanced) > TreeMetrics.width(current) ? balanced : current;
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

    private static TokenNode buildBalancedSigned(Chain chain) {
        List<Term> terms = chain.terms();

        if (terms.isEmpty() || terms.getFirst().sign() != TermSign.POSITIVE)
            return null;

        if (terms.size() == 1)
            return terms.getFirst().node();

        boolean sum = chain.family() == OperatorValue.PLUS;
        OperatorValue positive = sum ? OperatorValue.PLUS : OperatorValue.MULTIPLY;
        OperatorValue negative = sum ? OperatorValue.MINUS : OperatorValue.DIVISION;

        Deque<SignedTask> tasks = new ArrayDeque<>();
        Deque<TokenNode> merged = new ArrayDeque<>();
        tasks.push(new SignedTask(0, terms.size(), terms.getFirst().sign(), false, null));

        while (!tasks.isEmpty()) {
            SignedTask task = tasks.pop();

            if (task.combine()) {
                TokenNode right = merged.pop();
                TokenNode left = merged.pop();
                merged.push(NodeFactory.operation(task.operator(), left, right));
                continue;
            }

            if (task.end() - task.start() == 1) {
                merged.push(terms.get(task.start()).node());
                continue;
            }

            int split = bestTermSplit(terms, task.start(), task.end());
            OperatorValue operator = terms.get(split).sign() == task.multiplier() ? positive : negative;
            tasks.push(new SignedTask(task.start(), task.end(), task.multiplier(), true, operator));
            tasks.push(new SignedTask(split, task.end(), terms.get(split).sign(), false, null));
            tasks.push(new SignedTask(task.start(), split, task.multiplier(), false, null));
        }

        return merged.pop();
    }

    private static int bestTermSplit(List<Term> terms, int start, int end) {
        int total = 0;
        for (int i = start; i < end; i++)
            total += terms.get(i).node().getWeight();

        int best = start + 1;
        long bestDifference = Long.MAX_VALUE;
        int prefix = 0;

        for (int k = start + 1; k < end; k++) {
            prefix += terms.get(k - 1).node().getWeight();

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