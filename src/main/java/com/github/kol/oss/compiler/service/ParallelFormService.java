package com.github.kol.oss.compiler.service;

import com.github.kol.oss.compiler.constant.OperatorValue;
import com.github.kol.oss.compiler.dto.node.OperationNode;
import com.github.kol.oss.compiler.dto.node.TokenNode;
import com.github.kol.oss.compiler.pipeline.TransformationService;
import com.github.kol.oss.compiler.pipeline.TreeTransformationService;
import com.github.kol.oss.compiler.utils.BalancedTreeBuilder;
import com.github.kol.oss.compiler.utils.NodeFactory;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public class ParallelFormService extends TreeTransformationService {
    @Override
    public String name() {
        return TransformationService.PARALLEL;
    }

    @Override
    protected boolean strictChains() {
        return true;
    }

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
            TokenNode node = context.resolve(frame.node());

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
        return buildRightAssociative(operands, OperatorValue.MINUS, OperatorValue.PLUS);
    }

    private TokenNode buildDivision(List<TokenNode> operands) {
        return buildRightAssociative(operands, OperatorValue.DIVISION, OperatorValue.MULTIPLY);
    }

    private TokenNode buildRightAssociative(
            List<TokenNode> operands,
            OperatorValue separator,
            OperatorValue tailFamily
    ) {
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
            List<TokenNode> tailOperands = operands.subList(chunk[0], chunk[1] + 1);
            TokenNode tail = tailFamily == OperatorValue.PLUS
                    ? BalancedTreeBuilder.buildSum(tailOperands)
                    : BalancedTreeBuilder.buildProduct(tailOperands);

            result = NodeFactory.operation(separator, result, tail);
        }

        return result;
    }

    private record Frame(TokenNode node, boolean term) {
    }
}
