package com.github.kol.oss.compiler.utils;

import com.github.kol.oss.compiler.constant.OperatorValue;
import com.github.kol.oss.compiler.dto.node.FunctionNode;
import com.github.kol.oss.compiler.dto.node.OperationNode;
import com.github.kol.oss.compiler.dto.node.TokenNode;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Map;

public class TreeWalker {
    private record NodeFrame(TokenNode node, OperatorValue context) {
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
        if (left == right)
            return true;

        if (left == null || right == null || left.getClass() != right.getClass())
            return false;

        if (!left.getToken().value().equals(right.getToken().value()))
            return false;

        List<TokenNode> leftChildren = children(left);
        List<TokenNode> rightChildren = children(right);

        if (leftChildren.size() != rightChildren.size())
            return false;

        for (int i = 0; i < leftChildren.size(); i++)
            if (!same(leftChildren.get(i), rightChildren.get(i)))
                return false;

        return true;
    }
}