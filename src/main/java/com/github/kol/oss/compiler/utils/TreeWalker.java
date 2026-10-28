package com.github.kol.oss.compiler.utils;

import com.github.kol.oss.compiler.constant.OperatorValue;
import com.github.kol.oss.compiler.dto.node.FunctionNode;
import com.github.kol.oss.compiler.dto.node.OperationNode;
import com.github.kol.oss.compiler.dto.node.TokenNode;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public class TreeWalker {
    private record NodeFrame(TokenNode node, OperatorValue context) {
    }

    private record Pair(TokenNode left, TokenNode right) {
    }

    public static List<TokenNode> children(TokenNode node) {
        if (node instanceof OperationNode operation)
            return List.of(operation.getLeft(), operation.getRight());

        if (node instanceof FunctionNode function)
            return function.getArguments();

        return List.of();
    }

    public static List<TokenNode> postOrder(TokenNode root) {
        List<TokenNode> result = new ArrayList<>();
        Deque<TokenNode> pending = new ArrayDeque<>();
        Deque<TokenNode> ordered = new ArrayDeque<>();

        pending.push(root);
        while (!pending.isEmpty()) {
            TokenNode node = pending.pop();
            ordered.push(node);

            for (TokenNode child : children(node))
                pending.push(child);
        }

        while (!ordered.isEmpty())
            result.add(ordered.pop());

        return result;
    }

    public static List<TokenNode> collectChainRoots(TokenNode root, boolean strictOperator) {
        List<TokenNode> roots = new ArrayList<>();
        Deque<NodeFrame> pending = new ArrayDeque<>();
        pending.push(new NodeFrame(root, null));

        while (!pending.isEmpty()) {
            NodeFrame frame = pending.pop();
            OperatorValue key = chainKey(frame.node(), strictOperator);

            if (key != null && key != frame.context())
                roots.add(frame.node());

            for (TokenNode child : children(frame.node()))
                pending.push(new NodeFrame(child, key));
        }

        Collections.reverse(roots);
        return roots;
    }

    private static OperatorValue chainKey(TokenNode node, boolean strictOperator) {
        OperatorValue family = chainFamily(node);
        if (family == null || !strictOperator)
            return family;

        return OperatorValue.fromSymbol(node.getToken().value());
    }

    public static OperatorValue chainFamily(TokenNode node) {
        if (!(node instanceof OperationNode operation))
            return null;

        OperatorValue operator = OperatorValue.fromSymbol(operation.getToken().value());
        if (operator.isSumFamily())
            return OperatorValue.PLUS;

        if (operator.isProductFamily())
            return OperatorValue.MULTIPLY;

        return null;
    }

    public static TokenNode resolve(TokenNode node, Map<TokenNode, TokenNode> replacements) {
        return replacements.getOrDefault(node, node);
    }

    public static boolean same(TokenNode left, TokenNode right) {
        Deque<Pair> pending = new ArrayDeque<>();
        pending.push(new Pair(left, right));

        while (!pending.isEmpty()) {
            Pair pair = pending.pop();
            TokenNode currentLeft = pair.left();
            TokenNode currentRight = pair.right();

            if (currentLeft == currentRight)
                continue;

            if (currentLeft == null || currentRight == null || currentLeft.getClass() != currentRight.getClass())
                return false;

            if (!currentLeft.getToken().value().equals(currentRight.getToken().value()))
                return false;

            List<TokenNode> leftChildren = children(currentLeft);
            List<TokenNode> rightChildren = children(currentRight);

            if (leftChildren.size() != rightChildren.size())
                return false;

            for (int i = leftChildren.size() - 1; i >= 0; i--)
                pending.push(new Pair(leftChildren.get(i), rightChildren.get(i)));
        }

        return true;
    }

    public static String structuralKey(TokenNode root) {
        Map<TokenNode, String> keys = new IdentityHashMap<>();

        for (TokenNode node : postOrder(root))
            keys.put(node, keyOf(node, keys));

        return keys.get(root);
    }

    private static String keyOf(TokenNode node, Map<TokenNode, String> keys) {
        if (node instanceof FunctionNode function) {
            StringBuilder builder = new StringBuilder(function.getToken().value()).append('(');

            for (int i = 0; i < function.getArguments().size(); i++) {
                if (i > 0)
                    builder.append(',');

                builder.append(keys.get(function.getArguments().get(i)));
            }

            return builder.append(')').toString();
        }

        if (node instanceof OperationNode operation)
            return "(" + keys.get(operation.getLeft())
                    + operation.getToken().value()
                    + keys.get(operation.getRight()) + ")";

        return node.getToken().value();
    }
}
