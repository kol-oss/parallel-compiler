package com.github.kol.oss.compiler.utils;

import com.github.kol.oss.compiler.constant.TokenType;
import com.github.kol.oss.compiler.dto.node.FunctionNode;
import com.github.kol.oss.compiler.dto.node.OperationNode;
import com.github.kol.oss.compiler.dto.node.TokenNode;
import com.github.kol.oss.compiler.dto.token.Token;

import java.util.List;

public class TreeUtils {
    // number of operations inside the subtree, used as the commutative-law term weight
    public static int weight(TokenNode node) {
        if (node instanceof OperationNode operation)
            return 1 + weight(operation.getLeft()) + weight(operation.getRight());

        if (node instanceof FunctionNode function)
            return 1 + function.getArguments().stream().mapToInt(TreeUtils::weight).sum();

        return 0;
    }

    public static Token operatorToken(String value) {
        return new Token(value, TokenType.OPERATOR, 0);
    }

    // rebuilds a sorted term list so both branches carry an equal number of operations
    public static TokenNode buildBalanced(List<TokenNode> terms, String operator) {
        if (terms.size() == 1)
            return terms.getFirst();

        int split = bestSplit(terms);
        return new OperationNode(
                operatorToken(operator),
                buildBalanced(terms.subList(0, split), operator),
                buildBalanced(terms.subList(split, terms.size()), operator)
        );
    }

    // split point where the operation counts of both branches differ as little as possible
    private static int bestSplit(List<TokenNode> terms) {
        int total = terms.stream().mapToInt(TreeUtils::weight).sum();

        int best = 1;
        long bestDifference = Long.MAX_VALUE;
        int prefix = 0;

        for (int k = 1; k < terms.size(); k++) {
            prefix += weight(terms.get(k - 1));

            long left = prefix + (k - 1);
            long right = (total - prefix) + (terms.size() - k - 1);
            long difference = Math.abs(left - right);

            if (difference < bestDifference) {
                bestDifference = difference;
                best = k;
            }
        }

        return best;
    }
}