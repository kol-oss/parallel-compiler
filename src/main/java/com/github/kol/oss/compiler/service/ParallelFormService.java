package com.github.kol.oss.compiler.service;

import com.github.kol.oss.compiler.constant.OperatorValue;
import com.github.kol.oss.compiler.dto.node.OperationNode;
import com.github.kol.oss.compiler.dto.node.TokenNode;
import com.github.kol.oss.compiler.pipeline.Pass;
import com.github.kol.oss.compiler.pipeline.TreeRewritePass;
import com.github.kol.oss.compiler.utils.BalancedTreeBuilder;
import com.github.kol.oss.compiler.utils.NodeFactory;
import com.github.kol.oss.compiler.utils.TreeWalker;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public class ParallelFormService extends TreeRewritePass {
    @Override
    public String name() {
        return Pass.PARALLEL;
    }

    @Override
    protected boolean strictChains() {
        return true;
    }

    // build parallel form
    @Override
    protected void rewriteChain(OperationNode operation, Context context) {
        context.replace(operation, rebuildChain(operation, flatten(operation, context)));
    }

    private List<TokenNode> flatten(OperationNode chainRoot, Context context) {
        String operator = chainRoot.getToken().value();
        boolean flattenBoth = OperatorValue.fromSymbol(operator) == OperatorValue.PLUS
                || OperatorValue.fromSymbol(operator) == OperatorValue.MULTIPLY;

        List<TokenNode> operands = new ArrayList<>();
        Deque<Frame> pending = new ArrayDeque<>();
        pending.push(new Frame(chainRoot, false));

        while (!pending.isEmpty()) {
            Frame frame = pending.pop();
            TokenNode node = TreeWalker.resolve(frame.node(), context.replacements());

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

    private record Frame(TokenNode node, boolean term) {
    }
}
