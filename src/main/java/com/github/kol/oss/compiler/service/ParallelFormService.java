package com.github.kol.oss.compiler.service;

import com.github.kol.oss.compiler.constant.OperatorValue;
import com.github.kol.oss.compiler.dto.node.FunctionNode;
import com.github.kol.oss.compiler.dto.node.OperationNode;
import com.github.kol.oss.compiler.dto.node.TokenNode;
import com.github.kol.oss.compiler.dto.node.ValueNode;
import com.github.kol.oss.compiler.utils.BalancedTreeBuilder;
import com.github.kol.oss.compiler.utils.NodeFactory;
import com.github.kol.oss.compiler.utils.TreeWalker;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ParallelFormService {
    private record Frame(TokenNode node, boolean term) {
    }

    public TokenNode buildParallelTree(TokenNode root) {
        Map<TokenNode, TokenNode> replacements = new IdentityHashMap<>();
        Set<TokenNode> chainRoots = Collections.newSetFromMap(new IdentityHashMap<>());
        chainRoots.addAll(TreeWalker.collectChainRoots(root, true));

        for (TokenNode node : TreeWalker.postOrder(root)) {
            if (node instanceof ValueNode)
                continue;

            if (node instanceof FunctionNode function) {
                List<TokenNode> arguments = function.getArguments().stream()
                        .map(argument -> TreeWalker.resolve(argument, replacements))
                        .toList();

                if (!arguments.equals(function.getArguments()))
                    replacements.put(function, NodeFactory.function(function.getToken(), arguments));

                continue;
            }

            OperationNode operation = (OperationNode) node;

            if (TreeWalker.chainFamily(node) != null) {
                if (!chainRoots.contains(node))
                    continue;

                replacements.put(node, rebuildChain(operation, flatten(operation, replacements)));
                continue;
            }

            TokenNode left = TreeWalker.resolve(operation.getLeft(), replacements);
            TokenNode right = TreeWalker.resolve(operation.getRight(), replacements);

            if (left != operation.getLeft() || right != operation.getRight())
                replacements.put(operation, NodeFactory.operation(operation.getToken(), left, right));
        }

        return TreeWalker.resolve(root, replacements);
    }

    private List<TokenNode> flatten(OperationNode chainRoot, Map<TokenNode, TokenNode> replacements) {
        String operator = chainRoot.getToken().value();
        boolean flattenBoth = OperatorValue.fromSymbol(operator) == OperatorValue.PLUS
                || OperatorValue.fromSymbol(operator) == OperatorValue.MULTIPLY;

        List<TokenNode> operands = new ArrayList<>();
        Deque<Frame> pending = new ArrayDeque<>();
        pending.push(new Frame(chainRoot, false));

        while (!pending.isEmpty()) {
            Frame frame = pending.pop();
            TokenNode node = TreeWalker.resolve(frame.node(), replacements);

            if (!frame.term() && node instanceof OperationNode operation
                    && operation.getToken().value().equals(operator)) {
                pending.push(new Frame(operation.getRight(), !flattenBoth));
                pending.push(new Frame(operation.getLeft(), false));
                continue;
            }

            operands.add(node);
        }

        return operands;
    }

    private TokenNode rebuildChain(OperationNode chainRoot, List<TokenNode> operands) {
        return switch (OperatorValue.fromSymbol(chainRoot.getToken().value())) {
            case PLUS -> BalancedTreeBuilder.buildSum(operands);
            case MULTIPLY -> BalancedTreeBuilder.buildProduct(operands);
            case MINUS -> buildSubtraction(operands);
            case DIVISION -> buildDivision(operands);
            default -> throw new IllegalStateException("Unexpected chain operator: " + chainRoot.getToken().value());
        };
    }

    private TokenNode buildSubtraction(List<TokenNode> operands) {
        Deque<int[]> chunks = new ArrayDeque<>();
        int end = operands.size() - 1;

        while (end > 0) {
            int mid = end / 2;
            chunks.push(new int[]{mid + 1, end});
            end = mid;
        }

        TokenNode result = operands.getFirst();
        while (!chunks.isEmpty()) {
            int[] chunk = chunks.pop();
            TokenNode tail = BalancedTreeBuilder.buildSum(operands.subList(chunk[0], chunk[1] + 1));
            result = NodeFactory.operation(OperatorValue.MINUS, result, tail);
        }

        return result;
    }

    private TokenNode buildDivision(List<TokenNode> operands) {
        Deque<int[]> chunks = new ArrayDeque<>();
        int end = operands.size() - 1;

        while (end > 0) {
            int mid = end / 2;
            chunks.push(new int[]{mid + 1, end});
            end = mid;
        }

        TokenNode result = operands.getFirst();
        while (!chunks.isEmpty()) {
            int[] chunk = chunks.pop();
            TokenNode tail = BalancedTreeBuilder.buildProduct(operands.subList(chunk[0], chunk[1] + 1));
            result = NodeFactory.operation(OperatorValue.DIVISION, result, tail);
        }

        return result;
    }
}