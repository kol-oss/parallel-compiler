package com.github.kol.oss.compiler.processor;

import com.github.kol.oss.compiler.constant.OperatorType;
import com.github.kol.oss.compiler.dto.node.Node;
import com.github.kol.oss.compiler.dto.node.OperandNode;
import com.github.kol.oss.compiler.dto.node.OperatorNode;

import java.util.Map;

public class OptimizationProcessor {
    private static final Map<OperatorType, OperatorType> OPTIMIZATIONS = Map.of(
            OperatorType.MINUS, OperatorType.PLUS,
            OperatorType.DIVISION, OperatorType.MULTIPLY
    );

    public Node process(Node root) {
        return optimizeOperations(root);
    }

    private Node optimizeOperations(Node node) {
        if (node instanceof OperandNode)
            return node;

        OperatorNode operatorNode = (OperatorNode) node;
        Node left = operatorNode.getLeft();
        Node right = operatorNode.getRight();

        // optimization of branches
        operatorNode.setLeft(optimizeOperations(left));
        operatorNode.setRight(optimizeOperations(right));

        // optimization of sequential patterns
        for (Map.Entry<OperatorType, OperatorType> optimization : OPTIMIZATIONS.entrySet()) {
            boolean isOptimized = optimizeSequence(operatorNode, optimization.getKey(), optimization.getValue());
            if (isOptimized)
                return optimizeOperations(left);
        }

        return operatorNode;
    }

    private boolean optimizeSequence(OperatorNode operatorNode, OperatorType base, OperatorType replacement) {
        OperatorType operator = OperatorType.fromString(operatorNode.getValue());
        Node left = operatorNode.getLeft();
        Node right = operatorNode.getRight();

        if (operator == base && left instanceof OperatorNode leftNode) {
            OperatorType leftOperator = OperatorType.fromString(leftNode.getValue());
            if (leftOperator == base) {
                OperatorNode newRight = new OperatorNode(OperatorType.toString(replacement), leftNode.getRight(), right);
                leftNode.setRight(newRight);

                return true;
            }
        }

        return false;
    }
}